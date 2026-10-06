# Card simulator backend

The backend maps four MySQL tables to JPA entities and uses Spring Data JPA repositories for persistence. `AccountService` handles customer accounts, cards, and admin account operations. `TransactionService` handles purchases, history, refunds, and admin activity. Spring MVC runs a local web server. The API requires an `AuthenticatedUser` servlet principal established by the server before it reads protected request bodies. No authentication component creates that principal yet, so external `/api` requests receive `401`. A user ID or role in a header, URL, or JSON body does not establish identity. Shared HTTP error handling is implemented. JWT sign-in is planned separately.

## Local setup

Use JDK 17 and MySQL 8.0.16 or newer. The committed Maven wrapper downloads Maven 3.9.16 on its first run; no separate Maven installation is needed. Spring Boot is pinned to 3.5.16, which supports Java 17 ([official requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html)).

1. Run [`../sql/01_schema.sql`](../sql/01_schema.sql) and [`../sql/02_seed.sql`](../sql/02_seed.sql) manually as described in the [SQL setup](../sql/README.md).
2. Configure `DB_USERNAME` and `DB_PASSWORD` locally. Do not put credentials in Git, shell command history, or logs. You can set these through your IDE's local run configuration, Windows environment settings, or an ignored `src/main/resources/application-local.properties` file with `spring.datasource.username` and `spring.datasource.password`; activate that file with `SPRING_PROFILES_ACTIVE=local`.
3. Optionally set `DB_URL` for a different host or port. The default is `jdbc:mysql://localhost:3306/card_transaction_simulator?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true`. Keep the UTC connection options when changing the URL.

Use a MySQL application user with only `SELECT`, `INSERT`, `UPDATE`, and `DELETE` on the simulator database. Schema creation uses a separate setup login. The backend needs no `CREATE`, `ALTER`, or `DROP` privileges.

From `backend/` in PowerShell:

```powershell
.\mvnw.cmd verify
.\mvnw.cmd verify -Pmysql-verification
.\mvnw.cmd spring-boot:run
```

On macOS/Linux, use `./mvnw` instead. `JAVA_HOME` should point to your JDK 17 installation.

The first command builds the executable JAR and runs entity, service, DTO, identity-filter, and MVC tests without a database. Error tests cover each implemented status, malformed and invalid requests, spoofed identity, safe messages and logs, and unexpected failures. The second also runs entity mapping, repository, service, and controller integration tests against MySQL. These checks cover ownership, roles, approvals and declines, full refunds, retries, `201` versus `200`, validation, response fields, page ordering, concurrent requests, and rollback. Controller error checks confirm that invalid purchases, conflicting retries, and ineligible refunds preserve balances and history.

A missing database or schema fails verification. Entity and repository tests roll back their fictional rows. Service and controller tests commit fresh fictional fixtures, then delete only those fixtures after each test. Controller integration tests map responses after the service transaction closes. Existing rows are not changed; MySQL auto-increment counters can advance.

Maven `clean` can fail on read-only generated directories in this OneDrive workspace. A checkout outside the synced directory avoids that local build issue.

`spring.jpa.hibernate.ddl-auto=validate` checks the existing schema. `spring.sql.init.mode=never` disables automatic SQL script execution. Neither the application nor its tests create or alter tables. Hibernate's validation does not fully check lengths, nullability, indexes, or uniqueness, so the integration checks also inspect MySQL metadata ([initialization settings](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html)).

## Customer accounts and cards

`GET /api/accounts` returns the customer's account summary. `GET /api/accounts/{accountId}/cards` returns masked card details after the service checks ownership. Controllers pass the principal's user ID separately from the resource ID. Responses contain decimal money strings and no entity relationships or password hashes.

## Purchases, history, and refunds

`POST /api/accounts/{accountId}/purchases` validates the fictional input and returns a transaction plus the account summary. A new approved or declined purchase returns `201`; an identical retry returns `200` with the saved transaction. `POST /api/transactions/{purchaseId}/refund` uses the same status behavior for a full refund. `GET /api/accounts/{accountId}/transactions` returns `items`, `page`, `size`, `totalItems`, and `totalPages`. Pages start at 0, default to 20 rows, and cap at 50. History keeps the service's descending transaction-ID order.

Request DTOs use Bean Validation with `@Valid`. Money input is a positive decimal string; fractional IDs and numeric status values are rejected. Sensitive purchase fields are write-only and excluded from the request DTO's `toString()`. Responses use two decimal places, UTC timestamps ending in `Z`, and only the documented fields. Endpoint annotations describe the operation and its success codes; no Swagger UI is exposed.

## Entity mappings

| Entity | Table | Relationships |
| --- | --- | --- |
| `AppUser` | `app_users` | Optional inverse one-to-one `creditAccount`. |
| `CreditAccount` | `credit_accounts` | Required one-to-one `user`, optional inverse one-to-one `demoCard`, many transactions. |
| `DemoCard` | `demo_cards` | Required one-to-one `account`, many transactions. |
| `CardTransaction` | `card_transactions` | Required many-to-one account and card; optional one-to-one `originalPurchase`. |

The optional relationships do not make `user_id` or the card's `account_id` nullable. They mean a user may have no account and an account may have no card. The unique foreign keys enforce at most one of each. The unique nullable `original_purchase_id` allows a purchase to have at most one refund.

Required owning relationships, the refund's purchase link, and transaction collections use `LAZY` fetching. Optional inverse one-to-one relationships use `EAGER` deliberately because reliable lazy loading on those inverse sides would need Hibernate bytecode enhancement. Loading a user can therefore also load its account and card; transaction collections remain lazy. No relationship has persistence/removal cascades or orphan removal. Related rows are saved explicitly, with both sides of bidirectional relationships kept consistent in memory. MySQL foreign keys reject deleting a parent that still has history.

Money is `BigDecimal` with precision 14 and scale 2. `expiryMonth` is `Byte` for MySQL `TINYINT`, and `expiryYear` is `Short` for `SMALLINT`. Nested enums store their names in the existing `VARCHAR(20)` columns; explicit column definitions prevent Hibernate from expecting native MySQL enums. `lastFour` and `requestId` preserve the schema's fixed-width `CHAR` types.

`createdAt` is `LocalDateTime` because MySQL `DATETIME(6)` contains no timezone. Timestamps are UTC values with microsecond precision. Response DTOs format them with `Z`. There is no automatic timestamp callback or transaction processing in these entities.

Validation covers required fields, text lengths, email/UUID format, masked digits, expiry ranges, and money size/scale. SQL retains its existing `CHECK` constraints. Services check ownership, credit limits, refund eligibility, retries, and expiry and coordinate balance/history writes. No entity stores a full card number or security code. Jackson excludes `passwordHash` and relationships. Seed passwords use BCrypt; registration and password verification are planned with BCrypt. HTTP responses use the DTO shapes in the API design. SQL, bind-value, request-detail, and Spring Web validation logging are disabled.

## Repository lookups

| Repository | Lookups |
| --- | --- |
| `AppUserRepository` | Email lookup for planned registration/sign-in and a role-only lookup for service access checks. |
| `CreditAccountRepository` | Account by user, account ID with owner ID, locking account lookups, and a paginated admin account list ordered by ID ascending. |
| `DemoCardRepository` | Card by account, and card ID with account ID. |
| `CardTransactionRepository` | Account/request ID pair, transaction ID with owner ID, owned transaction's account ID, refund by original purchase ID, paginated customer history, and a paginated admin transaction list. |

The transaction ownership query uses JPQL to follow the transaction's account to its user. Customer history filters by both account ID and owner ID. Card lookups use the account ID after the service checks account ownership. Missing single-row lookups return `Optional.empty()`.

Transaction pages use descending transaction ID, matching the append-only history and the schema's `(account_id, id)` index. All three list queries accept `Pageable` and return `Page` with total counts. Services use page 0 and size 20 by default, cap size at 50, reject negative page numbers and nonpositive sizes, and preserve the repository's defined order.

The repositories provide reads and writes. Services make the business decisions; the SQL scripts remain the source of the schema.

## Admin responses

`GET /api/admin/accounts` returns account pages with the owner's ID, display name, and email, ordered by ascending account ID. `GET /api/admin/transactions` adds the owner's email to transaction pages, ordered by descending transaction ID. Both use the same pagination defaults and cap as customer history. `PATCH /api/admin/accounts/{accountId}/status` accepts `ACTIVE` or `FROZEN` and returns the updated admin account with `200`. Repository entity graphs fetch owner details inside the service transaction, so response mapping works with `open-in-view=false`. No controller serializes a JPA entity.

## Service behavior

Service methods receive a trusted user ID. They read the user's stored role and enforce account ownership. JWT verification is planned separately; passing a user ID alone is not authentication. Both services use constructor injection.

For a purchase, the service locks the owned account, validates the amount and fictional card input, and checks for an existing account/request-ID pair. The assigned profile `DEMO_4242` accepts the fictional test number `4242424242424242`. The submitted expiry matches the stored card, and the test security code is checked only for three or four digits. These input values are never copied to an entity or result, and the input object's `toString()` contains no field values.

A matching card is valid through the end of its expiry month in UTC. An expired card records `CARD_EXPIRED`; otherwise a frozen account records `ACCOUNT_FROZEN`, and an amount exceeding available credit records `INSUFFICIENT_CREDIT`. These declines save history without changing the balance. Invalid input or an unowned account/card creates no history. Approval increases the outstanding balance and saves the purchase in one transaction.

An identical retry compares the account-scoped request ID, card ID, exact merchant name, and numeric amount and returns the saved transaction with `replayed=true`. UUIDs are normalized to lowercase; `50` and `50.00` are the same amount. Different details or reuse across purchase/refund types throws `RequestConflictException`. The saved transaction retains its original outcome and balance; the accompanying account represents the current account summary.

A full refund locks the account and checks that the owned transaction is an approved purchase without an existing refund. It copies the original amount, merchant, account, and card, links the original purchase, decreases the balance, and saves history together. A frozen account can receive a refund. An identical refund retry returns the saved refund; another request ID cannot refund that purchase again.

Purchases and refunds use read-committed isolation with a pessimistic account write lock. Role and refund-account lookups read scalar values before the lock, avoiding an account loaded with an old balance. Waiting requests then see the previous request's committed balance and history. Admin freeze/reactivation takes the same account lock. Write failures roll back balance and history together.

Money calculations use `BigDecimal`. The injected UTC clock produces microsecond transaction timestamps. Specific exceptions distinguish invalid purchase input, malformed request IDs or pagination, unavailable resources, wrong roles, conflicting retries, and ineligible refunds.

## Authentication and error handling

The API is closed to external callers until server authentication establishes the typed principal. There is no demo user-ID header, login shortcut, or token parser. Tests set a trusted principal directly on server-side mock requests; that test mechanism is not an HTTP endpoint. Services still verify the stored role and resource ownership.

Every error has `status`, `code`, `message`, and a UTC `timestamp`. The identity filter returns `401 AUTHENTICATION_REQUIRED` before request validation. The controller identity guard uses the same response. Authentication and registration are still planned; no component accepts bearer text or creates an identity yet.

`ApiExceptionHandler` maps invalid input to `400`, wrong roles to `403`, unavailable or unowned resources to `404`, and conflicting retries or ineligible refunds to `409`. Malformed JSON, failed body validation, and invalid path/query parameters have distinct codes and fixed corrective messages. Unexpected failures return `500 INTERNAL_ERROR` with a generic retry-later message. Framework errors retain `404`, `405`, `406`, and `415` in the same shape. The [API design](../outputs/02_Architecture/03_API_Design.md) lists the codes. Responses omit exception classes, rejected values, internal messages, and stack traces.

Expected rejections log status, code, route template, and trusted user ID at INFO. MVC logs include the method; the identity filter uses `/api/**` and an anonymous user. Unexpected failures log method, route template, user ID, exception type, and stack locations at ERROR, without messages or causes. Request bodies, headers, raw URLs, SQL values, and credentials are excluded. SQL, bind-value, Spring Web request-detail, and Hibernate driver-error logging stay disabled.
