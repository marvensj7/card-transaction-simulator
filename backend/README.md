# Card simulator backend

The backend follows controller → service → JPA repository → MySQL. Start with the [short workflow walkthrough](../outputs/02_Architecture/05_Backend_Walkthrough.md), which explains every package, each DTO, and the purchase/refund paths.

## Current scope

- Four entities and four repositories map the existing MySQL tables.
- `AccountService` handles accounts, masked cards, and admin status changes.
- `TransactionService` handles purchases, history, full refunds, and admin activity.
- Three controllers handle HTTP only. DTOs, security, errors, and configuration have separate packages.
- One purchase request DTO passes directly to the service. Refunds take a `requestId` query parameter; status changes take a `status` query parameter.
- Purchases/refunds return transaction and account summaries. New outcomes return 201; identical retries return 200. Safe response DTOs format money as two-decimal strings and dates as UTC strings.

Authentication is unfinished. `/api` requires a server-established `AuthenticatedUser` principal; external requests receive 401 until JWT verification is implemented. A user ID, role header, or unverified bearer token cannot establish identity. Tests set the principal on mock server requests only.

## Local setup

Use JDK 17 and MySQL 8.0.16+. The Maven wrapper downloads Maven when needed.

1. Run the [schema and fictional seed scripts](../sql/README.md).
2. Configure `DB_USERNAME` and `DB_PASSWORD` through local environment settings or the ignored `src/main/resources/application-local.properties`. Do not put credentials in Git, command history, or logs.
3. Optionally configure `DB_URL`. Its default is `jdbc:mysql://localhost:3306/card_transaction_simulator?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true`; keep the UTC options.

The application database user needs only SELECT, INSERT, UPDATE, and DELETE privileges. Schema creation uses a separate setup login. Hibernate uses `validate`, SQL initialization is off, and the application never creates or alters tables.

From `backend/` in PowerShell:

```powershell
.\mvnw.cmd verify
.\mvnw.cmd verify -Pmysql-verification "-Dspring.profiles.active=local"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

The local-profile arguments load the ignored credential file; omit them if using environment variables. `JAVA_HOME` must point to JDK 17. On macOS/Linux use `./mvnw`.

`verify` builds the JAR and runs checks without MySQL. The optional profile also checks real mappings, queries, service rules, HTTP responses, ownership, retries, simultaneous balance changes, and rollback against MySQL. Missing MySQL/schema fails those checks. Fixtures use fictional data and are rolled back or deleted after each test; auto-increment IDs may advance.

OneDrive can leave generated build directories read-only and cause Maven `clean` to fail. A checkout outside the synced directory avoids this local issue. After moving/renaming Java classes, use a fresh build output so deleted classes cannot remain on the classpath.

## Rules worth explaining

Approval increases the outstanding balance. Expired assigned cards, frozen accounts, and insufficient credit produce saved declines without balance changes. Invalid input creates no history. A full refund copies the original purchase amount and adds one linked reversal; a frozen account can receive it.

Money uses `BigDecimal` and `DECIMAL(14,2)`. Purchase amounts accept standard JSON decimal strings or numbers, with a positive value, at most 12 whole digits, and at most two decimal places. No custom deserializer or intermediate command class is needed. Full fictional numbers/security codes exist only in request memory and are write-only; entities store only the card profile and last four digits. Seed password hashes use BCrypt.

Purchases/refunds lock the owned account and use read-committed transactions. The lock makes changes take turns, the database transaction keeps balance/history together, and the account/request-ID uniqueness rule prevents a retry from creating a second transaction. Request IDs normalize to lowercase. A changed card, merchant, amount, or operation conflicts. A purchase can have only one full refund.

Entities follow foreign keys in one direction. There are no inverse one-to-one fields or transaction collections to synchronize. Repositories fetch the account/card/history; admin queries load owner details before their service transaction closes. `open-in-view` is off, and controllers never serialize JPA entities.

The [API design](../outputs/02_Architecture/03_API_Design.md) defines routes and response shapes. `ApiExceptionHandler` uses fixed safe messages and preserves framework status codes/headers. Unexpected failures log exception type and source locations, excluding messages/causes. Request, SQL, and bind-value logging are disabled.
