# Backend walkthrough for the instructor meeting

**Credit Card Transaction Simulator — October 7, 2026**

This guide explains the code currently in this repository. The balance examples are fictional teaching examples, not claims about the current seed balances. Read the numbered sections in order, then rehearse the questions at the end.

## 1. Start with what the backend does today

You can introduce your project this way:

> My backend is the part of the simulator that decides whether a fictional purchase is approved or declined and keeps the account balance and transaction history consistent. It uses Spring Boot and MySQL. Controllers handle API requests, services enforce the rules, and repositories read and write the database through JPA. It also supports full refunds, customer ownership checks, administrator account controls, and safe error responses.

Then state the current boundary clearly:

> The backend transaction functionality is implemented and tested. Registration, login, and JWT verification are still planned, along with the React interface. The current API requires a trusted identity established by the server. Because the login component does not establish that identity yet, ordinary external API calls receive 401. Tests supply the trusted identity inside the server-side test environment to exercise the functionality.

This matters because the architecture documents also show the finished application design. Their Spring Security, JWT, registration, and React components describe planned work. There is currently no Spring Security dependency in the backend's Maven dependencies, no login controller, and no JWT verification component. Do not describe those as finished features.

The simulator does not contact a bank or payment processor and does not move real money.

## 2. Understand the whole path before reading individual files

The intended complete application follows this path:

```text
React page
  -> HTTP API request
  -> server identity check
  -> controller and request validation
  -> service and business rules
  -> JPA repository / Hibernate
  -> MySQL
```

The response comes back through the service and controller. A response DTO selects the fields to return, and Spring converts it to JSON for the browser.

Today, an ordinary external caller stops at the identity check. In the integration tests, a trusted test identity lets the request continue through the remaining implemented layers.

Each layer has a specific job:

| Part | Plain-language responsibility | Example in your code |
| --- | --- | --- |
| Identity filter | Require a server-established caller before protected input is processed. | `ApiIdentityFilter` |
| Controller | Match the URL, receive validated input, call a service, and choose the response. | `TransactionController` |
| Request DTO | Describe the accepted request fields and their basic rules. | `PurchaseRequest` |
| Service | Decide what the operation is allowed to do. | `TransactionService` |
| Entity | Represent a database row as a Java object and describe its mapping. | `CreditAccount` |
| Repository | Retrieve and persist entities. | `CreditAccountRepository` |
| MySQL | Store the data permanently and enforce database constraints. | `credit_accounts` |
| Response DTO | Return an intentional, safe JSON structure. | `AccountResponse` |
| Shared exception handler | Translate failures into consistent HTTP errors. | `ApiExceptionHandler` |

An entity is a data object used by persistence; it is not another processing step after the repository. The service works with entities, and the repository persists them.

## 3. What starts the application

Open `backend/src/main/java/com/marvens/capstone/CardSimulatorApplication.java`.

Java starts at `main()`. `SpringApplication.run(...)` starts Spring Boot. The `@SpringBootApplication` annotation enables configuration and component discovery. Because this class is in `com.marvens.capstone`, Spring discovers the controllers, services, configuration, and other components beneath that package.

Spring Boot configures the web application and embedded web server. Spring Data creates implementations of your repository interfaces. The configured MySQL connection allows Hibernate to map Java objects to the existing database tables.

Open `backend/pom.xml` to explain the libraries:

| Dependency | What it supplies |
| --- | --- |
| Spring Boot Web | HTTP routing, the web server, and JSON request/response handling. |
| Spring Data JPA | Repository support and Hibernate-based persistence. |
| Validation | Rules such as required fields, positive IDs, and valid field lengths. |
| MySQL Connector | The database driver that connects Java to MySQL. |
| Swagger annotations | Descriptions attached to endpoints; these do not provide a Swagger UI here. |
| Spring Boot Test | The tools used by the existing automated tests. |

Java 17 is the language version configured in this project. Maven builds the application and runs its tests. The Maven wrapper lets the project use its configured Maven distribution without requiring a separate Maven installation.

**How objects connect:** The controllers and services use constructor injection. For example, `TransactionController` receives a `TransactionService` in its constructor. Spring supplies that object. You do not repeatedly create services with `new`, and tests can substitute controlled dependencies when testing one layer.

## 4. How the database is prepared

`sql/01_schema.sql` creates the database and four tables. `sql/02_seed.sql` inserts missing fictional demonstration users, accounts, and cards. The seed includes two customers and one administrator. It does not reset existing balances or history when rerun.

These scripts are run manually. They are initial setup scripts, not a system for automatically migrating future schema changes.

The important settings in `application.properties` are:

| Setting | Meaning |
| --- | --- |
| `spring.jpa.hibernate.ddl-auto=validate` | Check that the existing schema matches the mappings; do not generate or change tables. |
| `spring.sql.init.mode=never` | Do not automatically execute SQL setup scripts on startup. |
| `spring.jpa.open-in-view=false` | Do not keep persistence access open throughout response rendering. Required related data must already be available. |
| UTC database connection and Hibernate settings | Use a consistent timezone when exchanging timestamps. |
| SQL, parameter, and request-detail logging disabled | Avoid exposing sensitive input through ordinary framework logging. |
| Strict JSON deserialization settings | Reject fractional integer fields and numeric enum values. |

Hibernate schema validation does not verify every constraint, length, or index. The MySQL integration tests also inspect database metadata.

Database connection credentials are local configuration. Avoid showing connection credential values while screen-sharing. The documented setup uses local credentials and an application database user with only the needed data permissions.

## 5. The four tables and their Java entities

The `entity` package contains one entity for each table. `@Entity` makes a Java class persistent, `@Table` names its table, and `@Column` maps a field to a column. Getters read values; setters change the Java object's values. Business decisions live in services.

An `id` is the row's primary key: its unique identifier. `@GeneratedValue(strategy = IDENTITY)` means MySQL assigns the auto-increment ID. A foreign key refers to a row in another table and prevents references to nonexistent parents.

### `AppUser` maps to `app_users`

This stores who the user is: `id`, `displayName`, `email`, `passwordHash`, and `role`.

Email is unique. The role is `USER` or `ADMIN`. The password field contains a hash; it is excluded from JSON. The fictional seed password values are BCrypt hashes, but registration and BCrypt password checking are not implemented yet. SHA-256 alone would not satisfy the password design.

A user can have zero or one credit account. An administrator can exist without an account. The schema's unique `credit_accounts.user_id` prevents multiple accounts for one user. The service separately checks roles; the foreign key alone does not decide who is a customer.

### `CreditAccount` maps to `credit_accounts`

This stores `id`, its user relationship, `creditLimit`, `outstandingBalance`, and `status`.

The outstanding balance is **how much the customer owes**. It is not the amount they have left to spend.

```text
available credit = credit limit - outstanding balance
```

For a $1,000 limit and a $200 outstanding balance, available credit is $800. An approved $50 purchase increases the outstanding balance to $250 and reduces available credit to $750.

Available credit is calculated in the response; it is not another stored database column. This avoids maintaining two balances that could disagree.

Status is `ACTIVE` or `FROZEN`. SQL requires a positive credit limit and an outstanding balance between zero and that limit.

### `DemoCard` maps to `demo_cards`

This stores `id`, its account relationship, `testProfile`, `label`, `lastFour`, `expiryMonth`, and `expiryYear`.

The unique `account_id` allows at most one assigned demo card per account. The stored `DEMO_4242` profile identifies the allowed fictional number checked in the application. MySQL stores the profile and last four digits, not the complete card number or test security code.

The full fictional input temporarily exists in memory during a purchase request. The test security code is checked only for three or four digits. It is not compared with a stored secret and does not perform real CVV verification.

### `CardTransaction` maps to `card_transactions`

This stores the permanent history of purchases and refunds:

| Field | Why it exists |
| --- | --- |
| `id` | Unique history-row identifier. |
| `account` and `card` | Identify which account and assigned card the operation concerns. |
| `type` | `PURCHASE` or `REFUND`. |
| `status` | `APPROVED` or `DECLINED`. |
| `amount` | Positive amount of this purchase or refund. |
| `outstandingAfter` | Historical snapshot of the outstanding balance after this outcome. |
| `merchantName` | Fictional merchant associated with the purchase. |
| `reasonCode` | Decline explanation, or null when not needed. |
| `createdAt` | UTC time of the recorded outcome. |
| `requestId` | Identifies one submission and its retries within an account. |
| `originalPurchase` | For a refund, points to the purchase being reversed. Null for a purchase. |

Each account and card can have many history rows. A refund points to another row in this same table. The unique nullable `original_purchase_id` permits at most one refund row for a purchase, while allowing multiple ordinary purchases with a null link.

Refunds do not delete or rewrite the approved purchase. They add a linked `REFUND` row. There is no `REFUNDED` transaction-status enum; the relationship records the reversal. Refund amounts remain positive, and the service determines that they subtract from the outstanding balance.

**Relationship details if asked:** `@OneToOne` represents user/account and account/card relationships; `@ManyToOne` represents many transactions belonging to one account or card. The side with `@JoinColumn` stores the foreign key. `mappedBy` describes the reverse Java view of that same relationship. Most owning relationships and transaction collections use lazy loading, so related data is fetched when needed. The optional inverse one-to-one relationships deliberately use eager loading to keep this implementation simple without bytecode enhancement. No relationship automatically cascades deletion through transaction history.

## 6. Why repositories are interfaces

Each repository extends `JpaRepository<EntityType, Long>`. That supplies common persistence operations such as finding by ID and saving. Spring Data creates the implementation at runtime; you write the interface and any additional query declarations.

JPA is the Java persistence specification. Hibernate is the implementation used here. Spring Data JPA provides the convenient repository layer on top, and Hibernate communicates with MySQL through the database driver.

A method such as `findByIdAndUser_Id(accountId, userId)` means: find this account only if its owner's ID also matches. Spring Data derives that query from the name. Explicit `@Query` methods use JPQL, which refers to Java entities and properties instead of raw SQL table names.

| Repository | Main responsibility |
| --- | --- |
| `AppUserRepository` | Find users by email and retrieve a stored role for access checks. |
| `CreditAccountRepository` | Find owned accounts, acquire account write locks, and page admin account lists. |
| `DemoCardRepository` | Find the card assigned to an account or match a card ID with its account. |
| `CardTransactionRepository` | Find saved requests, owned purchases, linked refunds, and paginated history. |

Single-result queries return `Optional`. That means the row might be absent. The service decides whether absence produces an empty list or an unavailable-resource error.

Repositories retrieve and save data. They do not decide whether a purchase should be approved.

## 7. What happens before a controller runs

`ApiIdentityFilter` examines `/api` and `/api/...` requests before MVC reads protected request bodies. It requires an `AuthenticatedUser` principal.

A principal is the server's representation of the caller. `AuthenticatedUser` contains a positive user ID and implements Java's `Principal` interface. Its type expresses the identity contract, but the type itself does not verify a password or token. A future authentication component must establish it after verification.

If that principal is missing, the filter immediately returns the standard JSON `401 AUTHENTICATION_REQUIRED` response. Sending a user ID, an admin role, or arbitrary bearer text from the client cannot create the trusted principal.

`CurrentUser.id(principal)` is a second guard used by controllers to extract that trusted ID. Tests install the principal on mock server requests. There is no public test-login shortcut.

Two checks must remain distinct:

- **Authentication:** Who is the caller? The actual login/JWT mechanism is planned.
- **Authorization:** May this caller perform this action on this resource? Stored-role and ownership checks are implemented in services.

Role alone is insufficient: one `USER` cannot access every other `USER`'s account.

## 8. Controllers and DTOs turn HTTP into service calls

`AccountController` handles account and masked-card reads. `TransactionController` handles purchases, history, and refunds. `AdminController` handles administrator lists and account status changes.

`@RestController` identifies an HTTP controller whose return values become response bodies. `@RequestMapping` supplies a common path. `@GetMapping`, `@PostMapping`, and `@PatchMapping` select the operation. `@PathVariable` reads a URL ID, `@RequestParam` reads a query value, and `@RequestBody` reads JSON.

A DTO is a **data transfer object**: the shape of information crossing the API boundary. It is separate from a database entity.

For purchases, `PurchaseRequest` accepts the card ID, fictional card fields, merchant, amount, and UUID request ID. `@Valid` activates its validation rules. Examples include positive IDs, month 1–12, a nonblank merchant of at most 100 characters, and a positive decimal-string amount with at most two fractional digits.

The amount must be JSON text such as `"50.00"`. `DecimalStringDeserializer` rejects a numeric JSON amount. `toCommand()` converts the validated amount to `BigDecimal` and creates the internal `PurchaseCommand` passed to the service.

The service repeats important checks because its business rules must remain safe when called directly, not only through an HTTP controller.

Sensitive request fields are write-only, and request/command `toString()` methods omit their values. Controllers return response DTOs rather than serializing entity relationships. This keeps password hashes out of responses, prevents recursive user/account/card graphs, and gives React a predictable contract.

## 9. Walk through one purchase from top to bottom

Use this fictional scenario: credit limit $1,000, outstanding balance $200, available credit $800, and an active account with a valid assigned card. The customer requests a $50 purchase at Demo Bookstore.

**Step 1 — Route and validate the request.** `POST /api/accounts/{accountId}/purchases` reaches `TransactionController.purchase()` after the identity filter. The controller receives the trusted principal, the account ID from the path, and a validated `PurchaseRequest`.

**Step 2 — Call the service.** The controller extracts the trusted user ID and converts the request to a command. It calls `TransactionService.purchase(userId, accountId, command)`.

**Step 3 — Verify role and lock the owned account.** The service checks that the user's stored role is `USER`. It queries for the account using both the account ID and owner ID, with a pessimistic write lock. An absent or unowned account is unavailable. The lock remains held until this database transaction ends.

**Step 4 — Validate fields and normalize the request ID.** The service validates card-field formats, merchant, and amount. `RequestChecks` validates the UUID-shaped request ID and normalizes it to lowercase.

**Step 5 — Look for an existing result.** The service searches by the account/request-ID pair. If a row exists, its operation type, card ID, exact merchant text, and numeric amount must match. Changed details cause a conflict.

**Step 6 — Verify the assigned card.** The card query includes both card ID and account ID. The service checks the supported fictional profile, allowed fictional number, stored last four, and matching expiry. The security code has already passed its format check. Assigned-card checks also apply before returning a saved purchase retry.

**Step 7 — Return a valid saved retry, if present.** The original transaction is returned without a new row or balance change. Its original timestamp and historical balance stay intact. The accompanying account summary reflects the account as read for this operation.

**Step 8 — Decide a new purchase outcome.** The service checks these rules in this order:

1. Expired assigned card: `CARD_EXPIRED`.
2. Frozen account: `ACCOUNT_FROZEN`.
3. Amount greater than available credit: `INSUFFICIENT_CREDIT`.

A card is valid throughout its expiry month in UTC. Spending exactly the available credit is allowed. If more than one decline condition applies, the first condition above supplies the recorded reason.

**Step 9 — Update the balance only for approval.** In our example, $50 fits within $800. The service adds $50 to the outstanding balance, making it $250. A decline leaves it at $200.

**Step 10 — Save history.** The new row records the card, account, merchant, amount, outcome, reason, request ID, UTC timestamp, and balance after the outcome. The helper `history()` fills common fields used by both purchases and refunds. The service saves the history row even for a business decline.

**Step 11 — Commit the database operation.** Spring commits after the service finishes successfully. If a write fails, the balance and history changes roll back together.

**Step 12 — Build the response.** `TransactionOutcome` carries the transaction, account, and internal `replayed` flag. The controller maps this to `TransactionResultResponse`, containing a transaction response and account response. A new result returns `201 Created`; a saved retry returns `200 OK`. The internal replay flag selects the HTTP status and is not a JSON field in this response.

Our example returns a purchase amount of `"50.00"`, historical outstanding balance `"250.00"`, and account available credit `"750.00"`.

## 10. Invalid input, declines, and retries are different

| Situation | HTTP result with a trusted caller | New history row? | Balance changes? |
| --- | --- | --- | --- |
| Valid $50 purchase within credit | `201`, transaction `APPROVED` | Yes | Outstanding balance increases $50. |
| Valid purchase exceeds available credit | `201`, transaction `DECLINED` | Yes | No. |
| Valid purchase on a frozen account | `201`, transaction `DECLINED` | Yes | No. |
| Matching assigned card has expired | `201`, transaction `DECLINED` | Yes | No. |
| Invalid amount or mismatched fictional card | `400` | No | No. |
| Account/card unavailable to this owner | `404` | No | No. |
| Same purchase and same request ID | `200`, saved transaction | No | No additional change. |
| Same request ID with changed purchase details | `409` | No | No. |

HTTP success means the API successfully processed and recorded the attempt. The transaction's status describes the purchase decision. That is why an ordinary decline returns `201` rather than pretending the server failed.

An identical retry preserves a saved decline too. If the customer wants a new purchase decision after the account changes, that is a new submission with a new request ID.

The protection is called **idempotency**: repeating the same intended submission does not repeat its financial effect. It depends on reusing the same request ID. A second click with a newly generated ID represents a new submission; the backend cannot infer that it was accidental.

Numerically, `50` and `50.00` match because the service uses `BigDecimal.compareTo`. Merchant comparison is exact. Reusing an account's request ID between purchase and refund operations also conflicts. MySQL's unique `(account_id, request_id)` constraint backs the service check.

## 11. Why transactions and locks are both necessary

There are two meanings of transaction here. A `CardTransaction` is a purchase/refund history record. A **database transaction** groups database work into one all-or-nothing operation.

`@Transactional` establishes the service's database boundary. The services default to read-only transactions for reads; write methods override that default. Purchase and refund explicitly use `READ_COMMITTED` isolation.

Suppose the balance update succeeds, but saving history fails. The operation must not leave the customer owing more without a corresponding purchase record. The database transaction rolls back both writes. `saveAndFlush()` sends changes to MySQL during the operation, but **flush is not commit**; those writes can still roll back before the service transaction completes.

A lock solves a different problem. Suppose available credit is $100 and two requests each want $80. Without coordination, both might read $100 and approve. The pessimistic account write lock makes the second supported balance-changing operation wait. After the first commits, the second sees only $20 available and declines.

`READ_COMMITTED` lets the waiting operation see history committed by the earlier operation. Role checks retrieve only the role, and the refund's initial ownership lookup retrieves only the account ID. This avoids loading an old account balance into the persistence context before acquiring its lock.

Purchases, refunds, and administrator status changes use the same account lock. A freeze and a purchase are therefore processed in an order: a purchase committed before the freeze remains approved; a new purchase processed after the freeze sees the frozen state. Freezing is not a retroactive reversal.

Locks coordinate concurrent operations; unique constraints prevent duplicate identities; database transactions keep related writes together. None replaces the other two.

## 12. How a full refund works

The route is `POST /api/transactions/{purchaseId}/refund`. The request body contains a request ID. It does not let the customer choose the refund amount.

The service:

1. Checks the stored customer role and request ID.
2. Finds the owned transaction's account ID, then locks that account.
3. Loads the owned transaction and checks for an existing refund request with this request ID.
4. Returns an identical saved refund retry, or rejects conflicting reuse.
5. For a new refund, requires an approved `PURCHASE` with no existing refund.
6. Checks account/card consistency and that subtracting the purchase amount cannot make the outstanding balance negative.
7. Subtracts the original purchase amount from the outstanding balance.
8. Saves an approved `REFUND` with the original amount, merchant, account, and card, linked through `originalPurchaseId`.
9. Commits the balance and refund history together.

A frozen account can receive a refund. The original purchase remains unchanged. Retrying the same refund request returns `200`; a first refund returns `201`; another refund under a different request ID returns `409`.

The unique original-purchase link in MySQL is an additional safeguard against refunding one purchase twice.

## 13. Account reads, history, and administrator operations

| Endpoint | What it does |
| --- | --- |
| `GET /api/accounts` | Return the customer's own account summary as a list of zero or one account. |
| `GET /api/accounts/{accountId}/cards` | Check ownership, then return a list of zero or one masked card. |
| `GET /api/accounts/{accountId}/transactions` | Check ownership and return a page of account history. |
| `GET /api/admin/accounts` | Require ADMIN and return paginated accounts with owner summaries. |
| `GET /api/admin/transactions` | Require ADMIN and return paginated activity with owner emails. |
| `PATCH /api/admin/accounts/{accountId}/status` | Require ADMIN, lock the account, and set `ACTIVE` or `FROZEN`. |

Customer and admin permissions are explicit: an ADMIN may inspect activity and control status but cannot submit purchases through the customer service methods.

Pagination means returning one limited group of records. Page numbering starts at 0; the default size is 20 and the maximum is 50. Negative pages and nonpositive sizes are invalid. The response contains `items`, `page`, `size`, `totalItems`, and `totalPages`.

History and admin activity use descending transaction ID. This makes the append order clear, including when timestamps are equal. Admin accounts use ascending account ID. The `(account_id, id)` index supports account-history queries.

Admin repository methods use `@EntityGraph` to fetch owner information needed by responses while the service transaction is open. This lets the controllers map responses with `open-in-view=false` rather than relying on later hidden database access.

## 14. Money, time, errors, and sensitive data

**Money:** Java uses `BigDecimal`; MySQL uses `DECIMAL(14,2)`, meaning 14 total decimal digits with two after the decimal point. Binary floating-point values are unsuitable for exact decimal accounting. Inputs with excessive decimal places are rejected rather than silently rounded. Response money is formatted as two-decimal text.

**Time:** `UtcClockConfiguration` supplies a UTC `Clock` to the transaction service. Injecting the clock lets tests control the date, especially for expiry checks. The service creates UTC timestamps truncated to microseconds to match `DATETIME(6)`. MySQL `DATETIME` and Java `LocalDateTime` do not carry a timezone themselves; this project treats these stored values as UTC. Response formatting adds the UTC `Z` representation. The API error timestamp is generated separately with `Instant.now()`.

**Errors:** `ApiExceptionHandler`, annotated with `@RestControllerAdvice`, centralizes MVC and service error responses. The pre-controller identity filter writes the same four-field shape itself because it can reject a request before a controller runs.

| Status | Meaning in this project |
| --- | --- |
| `400` | Invalid JSON, fields, parameters, or purchase details. |
| `401` | Missing trusted identity. |
| `403` | The stored role does not permit the operation. |
| `404` | Resource is missing or unavailable to this owner; these share a safe message. |
| `409` | Conflicting request-ID reuse or an ineligible refund. |
| `405`, `406`, `415` | Unsupported method, unacceptable response format, or unsupported request media type. |
| `500` | Unexpected internal failure with a generic client message. |

Every API error has `status`, `code`, `message`, and a UTC `timestamp`. Messages explain corrective rules without echoing rejected card data or exposing stack traces. Returning the same unavailable-resource message for absent and unowned resources avoids confirming another customer's records.

**Sensitive data:** No entity stores a full card number or test security code. Response DTOs expose only masked card details. Logging records safe operation context, not request bodies, credentials, raw SQL values, or arbitrary exception messages. Unexpected-error logs retain exception type and stack locations to support debugging without printing messages or causes that might contain input.

## 15. How to explain your tests

There are several levels of evidence:

- **Entity and DTO tests:** Verify field rules, safe serialization, money/date formatting, and response shapes.
- **Service unit tests:** Check decisions with controlled repository behavior, including approval, declines, ownership, retries, and refund rules.
- **MVC tests:** Exercise HTTP routing, request validation, status codes, identity rejection, and safe errors with mock services.
- **MySQL integration tests:** Exercise real mappings, repository queries, service transactions, controller responses, rollback, and concurrent requests against the existing database.

Unit tests isolate a piece of code. Integration tests establish that pieces work together. Your `ControllerIT` uses mock HTTP requests with real services and MySQL; it does not prove browser integration or JWT authentication.

The integration suite includes two simultaneous purchases competing for credit, simultaneous identical retries, simultaneous refunds, waiting for an existing account lock, and deliberately failing writes to verify rollback. Controller integration checks also verify that rejected input, conflicting retries, and ineligible refunds preserve balances and history.

The checks use fictional fixtures. Entity/repository integration tests roll back their rows; service/controller integration tests create and remove their own fixtures. Auto-increment IDs can advance. A test count does not establish a coverage percentage; the proposal's 70% coverage target should not be claimed without a measured report.

From `backend/`, `mvnw.cmd verify` builds and runs checks that do not require a database. Adding `-Pmysql-verification` also runs the MySQL integration checks.

**Verified October 7, 2026:** `mvnw.cmd verify -Pmysql-verification` completed with BUILD SUCCESS: 99 tests without a database and 27 MySQL integration tests, for 126 total. There were zero failures, errors, or skipped tests. This verifies the current backend paths; authentication and browser integration remain planned.

## 16. A worked example you can explain without looking at code

Start with a $1,000 credit limit and a $200 outstanding balance.

| Operation | Outcome | Outstanding balance | Available credit | Added history |
| --- | --- | --- | --- | --- |
| Starting point | — | $200 | $800 | — |
| Submit $50 purchase, request A | Approved | $250 | $750 | Approved purchase A, snapshot $250. |
| Retry the same purchase with request A | Saved result | $250 | $750 | None. |
| Submit $900 purchase, request B | Insufficient credit | $250 | $750 | Declined purchase B, snapshot $250. |
| Admin freezes the account | Frozen | $250 | $750 | No purchase/refund row. |
| Submit valid $10 purchase, request C | Account frozen | $250 | $750 | Declined purchase C, snapshot $250. |
| Refund purchase A, request D | Approved refund | $200 | $800 | Refund D linked to A, snapshot $200. |
| Retry refund D | Saved result | $200 | $800 | None. |
| Request another refund of A with request E | Conflict | $200 | $800 | None. |

The account remains frozen after the refund until an administrator reactivates it. Purchase A still has its original $250 snapshot. That value describes history; the account's current balance is now $200. A later retry of purchase A returns the original purchase alongside the current account summary, which is why those balance fields can differ.

## 17. Likely instructor questions and answers

**Why separate controllers, services, and repositories?**

Controllers handle HTTP, services handle rules, and repositories handle persistence. I can change the interface or test the rules without putting everything into one method.

**Where does approval actually happen?**

In `TransactionService.purchase()`. After role, ownership, input, and retry checks, it checks expiry, frozen status, and available credit.

**Why can't React decide approval?**

Client input can be changed, and another request may already have changed the account. The server validates again and reads the account under its write lock before deciding.

**How do you stop one customer from using another account ID?**

The trusted user ID comes from the server principal. The service queries by both resource ID and owner ID. A supplied path ID is not proof of ownership.

**Is authentication finished?**

No. The identity boundary and authorization checks are implemented, but login and JWT verification are planned. External API calls remain closed with 401 until a server authentication component establishes the trusted principal.

**How can tests work before login exists?**

They install a trusted principal on mock server requests. This tests the controller, service, and persistence paths without claiming that a browser can sign in yet.

**Why is a declined purchase a successful HTTP response?**

The server successfully processed a valid attempt and saved the outcome. HTTP 201 says a record was created; `DECLINED` says the purchase was not approved.

**Why not keep only successful purchases?**

Declines explain what happened without changing the balance. Invalid input creates no history because it never becomes a valid purchase attempt.

**What prevents duplicate charges?**

The same account/request ID returns the saved result after checking details. The account lock coordinates simultaneous retries, and MySQL also enforces uniqueness. The caller must reuse the original request ID.

**What if saving history fails after updating the balance?**

Both writes are in one Spring-managed database transaction. Failure rolls them back, including an update already flushed to MySQL. Integration tests check this.

**Why is a lock needed if you already use `@Transactional`?**

All-or-nothing writes alone do not stop two requests from making decisions against the same available credit. The account write lock makes them take turns.

**Why can a frozen account receive a refund?**

Freezing blocks new spending. A refund reverses an eligible existing purchase and reduces the amount owed.

**Why not accept an amount in the refund request?**

The project supports full refunds only. The service copies the original amount, so the client cannot increase it or request a partial refund.

**How do you know the security code is correct?**

I do not verify a real security code. This is a fictional simulator, so I check only its format and never store it. The assigned fictional profile and expiry are checked separately.

**Do database constraints enforce every rule?**

No. They enforce foreign keys, uniqueness, allowed values, and basic amount/balance checks. Services enforce cross-row rules such as matching card/account ownership and refund eligibility.

**Does the backend create its tables automatically?**

No. SQL scripts create the schema manually. Hibernate validates the mappings at startup; integration tests check additional schema details.

**What is still outside the completed functionality?**

Registration, login, JWT verification, and the React interface are planned. Real payments, interest, statements, partial refunds, and production payment-network processing are outside this simulator's scope. AWS and a Jira board are not required for the approved capstone.

## 18. A practical order for showing the code

Use these paths relative to the repository root:

1. `backend/src/main/java/com/marvens/capstone/CardSimulatorApplication.java` — show where startup begins.
2. `sql/01_schema.sql` and the four classes in `backend/src/main/java/com/marvens/capstone/entity/` — explain what is stored and how rows connect.
3. `backend/src/main/java/com/marvens/capstone/controller/ApiIdentityFilter.java` — explain why a real external request currently receives 401.
4. `backend/src/main/java/com/marvens/capstone/controller/TransactionController.java` — point to the purchase route and its service call.
5. `backend/src/main/java/com/marvens/capstone/controller/dto/PurchaseRequest.java` — show input validation and conversion to a command.
6. `backend/src/main/java/com/marvens/capstone/service/TransactionService.java` — follow `purchase()` from role check to saved outcome; then show `refund()`.
7. `backend/src/main/java/com/marvens/capstone/repository/CreditAccountRepository.java` — show the owned-account query and write lock.
8. `backend/src/main/java/com/marvens/capstone/repository/CardTransactionRepository.java` — show request-ID lookup, refund link lookup, and history paging.
9. `backend/src/main/java/com/marvens/capstone/controller/dto/TransactionResultResponse.java` and `AccountResponse.java` — show response mapping and available-credit calculation.
10. `backend/src/main/java/com/marvens/capstone/controller/ApiExceptionHandler.java` — show the consistent failure contract.
11. `backend/src/test/java/com/marvens/capstone/service/ServiceIT.java` and `backend/src/test/java/com/marvens/capstone/controller/ControllerIT.java` — show evidence for the rules, rollback, concurrency, and HTTP/database path.

For today's meeting, use code and tests to demonstrate the implemented business functionality. A normal Postman call can demonstrate the current 401 identity boundary; successful external purchase requests require the authentication section to be implemented first.

End your spoken walkthrough with the result:

> A valid new purchase produces one recorded outcome. Approval increases the amount owed; a decline preserves it. A full refund adds a linked reversal and reduces the amount owed. Ownership checks protect whose account is used, request IDs protect retries, and account locks plus database transactions keep the balance and history consistent.
