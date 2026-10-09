# Backend

I use Java 17, Spring Boot, Spring Security, Spring Data JPA, Bean Validation, and MySQL 8.0.16+. The Maven wrapper supplies Maven. SQL scripts own the schema; Hibernate validates rather than changing tables.

## Setup and checks

Run the [SQL setup](../sql/README.md), then configure database credentials and a random Base64 signing key of at least 32 bytes through environment variables or ignored src/main/resources/application-local.properties. Tracked application.properties contains environment placeholders.

From this folder:

```powershell
.\mvnw.cmd verify
.\mvnw.cmd verify -Pmysql-verification "-Dspring.profiles.active=local"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

The first command runs HTTP/service/security tests with mocked repositories and applies the JaCoCo 70% line-coverage gate. The MySQL profile also runs real persistence/concurrency/rollback tests. Tests remove only their own fictional rows. Missing MySQL/schema fails integration verification. Omit profile arguments when using environment variables. Use ./mvnw on macOS/Linux.

OneDrive can prevent cleaning generated files. Add `"-Dcapstone.build.directory=C:/Users/marve/.cache/credit-circuit-build"` to use output outside OneDrive. Use the same output for packaging, verification, and SonarQube. After removing classes, use fresh output to avoid stale compiled classes.

## Behavior

Registration creates USER, a $1,000 active account with zero outstanding balance, and one ACCOUNT_V1 fictional card in one transaction. Callers cannot choose ADMIN. Login checks BCrypt and issues an HS256 JWT. Nimbus validates signature, expiration, issuer, exact audience, positive numeric subject, role, and time claims. JWT_SECRET must decode to at least 256 bits. No refresh tokens, sessions, or revocation table exist.

Registration and newly saved purchases/refunds return 201. An identical financial retry returns 200 with the saved transaction and current account summary. A new DECLINED purchase also returns 201. History/admin lists return bounded pages. See the [API contract](../outputs/02_Architecture/03_API_Design.md) and [OpenAPI UI](http://127.0.0.1:8080/swagger-ui/index.html).

Bean Validation checks format at the request boundary. Services check the assigned card, stored role, ownership, account status, available credit, duplicate requests, and full-refund eligibility. BigDecimal and DECIMAL(14,2) handle money. Account write locks, READ_COMMITTED, and database transactions keep balance/history consistent. A frozen account can receive an eligible refund.

Errors have fixed safe messages and optional field messages. ConflictException and ResourceNotFoundException distinguish common failures. Unexpected errors log only exception type. Request bodies, passwords, issued tokens, full card numbers, security codes, SQL, and bind values are never logged. Account/card models expose the same display fields through getters, with linked entities and internal card fields excluded. Transaction and login DTOs provide their own response fields.

CORS allows listed localhost origins without cookie credentials. Authentication POSTs share ten attempts per remote IP per minute in one process. CSRF ignores /api/** because authentication accepts only explicitly attached bearer headers. The [API contract](../outputs/02_Architecture/03_API_Design.md) explains transport and logout limits.
