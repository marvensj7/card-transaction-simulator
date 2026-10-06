# Backend — sections 2.2 and 2.3

The backend maps the four MySQL tables to JPA entities and uses Spring Data JPA repositories for persistence. I kept the repositories as small interfaces so the database lookups stay separate from purchase and refund decisions. Controllers, transaction services, and authentication are later sections. The application starts, validates the database mappings, and exits because no web server is included at this stage.

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

The first command builds the executable JAR and runs six validation/JSON tests without a database. The second also runs fifteen integration tests against the configured MySQL database: six entity mapping checks and nine repository checks. The repository checks cover custom queries, ownership filtering, account-scoped request IDs, linked refunds, ordering, page contents, and total counts. A missing database or schema fails verification instead of silently skipping it. Integration tests create fictional rows and roll back each test; existing rows are not changed. MySQL auto-increment counters can advance even when a test rolls back.

Maven `clean` can fail on read-only generated directories in this OneDrive workspace. A checkout outside the synced directory avoids that local build issue.

`spring.jpa.hibernate.ddl-auto=validate` checks the existing schema. `spring.sql.init.mode=never` disables automatic SQL script execution. Neither the application nor its tests create or alter tables. Hibernate's validation does not fully check lengths, nullability, indexes, or uniqueness, so the integration checks also inspect MySQL metadata ([initialization settings](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html)).

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

`createdAt` is `LocalDateTime` because MySQL `DATETIME(6)` contains no timezone. Timestamps are UTC values with microsecond precision. The API will format them with `Z` through response DTOs. There is no automatic timestamp callback or transaction processing in these entities.

Validation covers required fields, text lengths, email/UUID format, masked digits, expiry ranges, and money size/scale. SQL retains its existing `CHECK` constraints. Card ownership, account-limit comparisons, refund eligibility/amount, retries, expiration decisions, and balance/history updates belong to later services. No entity stores a full card number or security code. `passwordHash` is ignored by Jackson; the later authentication service must supply BCrypt hashes. Relationships are also ignored by Jackson to avoid recursive graph serialization. Future APIs should use the response DTOs in the API design. SQL and bind-value logging are disabled.

## Repository lookups

| Repository | Lookups |
| --- | --- |
| `AppUserRepository` | Email lookup for registration and sign-in. |
| `CreditAccountRepository` | Account by user, account ID with owner ID, and a paginated admin account list ordered by ID ascending. |
| `DemoCardRepository` | Card by account, and card ID with account ID. |
| `CardTransactionRepository` | Account/request ID pair, transaction ID with owner ID, refund by original purchase ID, paginated customer history, and a paginated admin transaction list. |

The transaction ownership query uses JPQL to follow the transaction's account to its user. Customer history filters by both account ID and owner ID. Card lookups use the account ID after the service checks account ownership. Missing single-row lookups return `Optional.empty()`.

Transaction pages use descending transaction ID, matching the append-only history and the schema's `(account_id, id)` index. All three list queries accept `Pageable` and return `Page` with total counts. The later controller or service will apply the API defaults of page 0 and size 20, cap size at 50, and pass an unsorted page request to keep the repository's defined order.

The repositories provide reads and writes only. Role checks, account locking, refund eligibility, retry comparisons, and balance changes remain service responsibilities. The SQL schema and ERD are unchanged; the API design clarifies the transaction ordering.
