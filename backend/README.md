# Backend

Read the [beginner walkthrough](../outputs/02_Architecture/05_Backend_Walkthrough.md) first. There are 21 application classes: four entities, four repositories, two services, three controllers, five DTOs, one session filter, one error handler, and the application entry point.

The backend keeps account summaries, masked cards, purchases/declines, history, full refunds, request retries, and admin status changes. The SQL schema is unchanged. DTOs are ordinary classes; controllers call services directly; services validate input and build safe responses while database relationships are available. Lists use ArrayList and for loops. There is no pagination, stream mapping, clock bean, custom exception hierarchy, generic page wrapper, or duplicate validation framework.

## Setup

Use JDK 17 and MySQL 8.0.16+. `JAVA_HOME` must point to the JDK. The Maven wrapper downloads Maven if needed.

1. Run the [schema and fictional seed scripts](../sql/README.md).
2. Set `DB_USERNAME` and `DB_PASSWORD` through local environment settings, or use the ignored `src/main/resources/application-local.properties` file with `spring.datasource.username` and `spring.datasource.password`.
3. Optionally set `DB_URL`. The default is `jdbc:mysql://localhost:3306/card_transaction_simulator?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true`; keep the UTC options.

The application database user needs SELECT, INSERT, UPDATE, and DELETE. Use a separate setup login for schema creation. Hibernate validates existing tables, and SQL initialization is disabled. The application never creates or alters the schema.

From `backend/`:

```powershell
.\mvnw.cmd verify
.\mvnw.cmd verify -Pmysql-verification "-Dspring.profiles.active=local"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Omit the local-profile arguments when using environment variables. Use `./mvnw` on macOS/Linux. The first command runs HTTP/service checks with mocked repositories; the second also uses real MySQL. Neither implements browser sign-in. Integration tests insert fictional fixtures and delete only those fixtures; auto-increment IDs may advance. Missing MySQL/schema fails integration verification.

OneDrive can make generated directories read-only and break Maven `clean`. A checkout outside the synced folder avoids this local issue. After removing/renaming classes, use fresh build output to avoid loading old classes.

## Behavior

All successful API operations return 200. A purchase response contains the saved transaction and current account summary; APPROVED/DECLINED is the financial result. An identical retry returns the saved transaction without another balance change. Changed details under the same account/request ID return 409. History/admin lists are ordinary JSON arrays suitable for the small local dataset. There is no page/size parameter or production-scale history claim.

The purchase service checks input in one place. An assigned card may be declined for expiry, a frozen account, or insufficient credit. Approval increases the outstanding balance; a decline preserves it. Refunds use the original amount and add a linked reversal once, including on a frozen account.

BigDecimal handles money. Response DTOs use JSON numbers; the future React screen formats them to two decimal places for display. UTC transaction timestamps use whole-second precision. Table constraints remain in SQL. The service checks the cross-row business rules.

`@Transactional` keeps balance/history together. The account write lock makes simultaneous balance changes wait their turn. READ_COMMITTED lets a waiting retry see the previous committed result. These protections remain because they prevent incorrect balances and duplicate purchases.

`SessionAccessFilter` currently requires a server session containing a positive Long userId before reading protected input. No sign-in endpoint creates it yet, so external calls receive 401. IDs/roles supplied in headers, query parameters, or JSON cannot establish a session. Future sign-in will check BCrypt passwords and use a standard session cookie with CSRF protection; JWT support is outside the MVP.

Errors use HTTP status plus `{"message":"..."}`. Services throw Spring's ResponseStatusException with fixed safe messages; one advice class handles the response. Unexpected errors log only exception type. Card numbers/security codes are request-only, write-only, and excluded from toString. Request/SQL/bind logging is disabled. The [API design](../outputs/02_Architecture/03_API_Design.md) records the exact contract.
