# How I explain the backend

## One $50 purchase

A customer with a $1,000 limit and $200 outstanding has $800 available. PurchasePage controls fictional input, creates one UUID, and calls submitPurchase. fetchJson adds the memory token and sends JSON.

Spring Security verifies the signed token and USER authority. TransactionController uses @Valid for format, reads the verified ID, and calls TransactionService.purchase. The service checks the stored USER role and locks the account by both account ID and owner ID. Checking only an account ID would not protect ownership.

The service loads the assigned card, validates its profile/expiry, and finds the account/request ID. An identical retry returns the existing transaction with HTTP 200. Changed details under that UUID return 409.

Explicit if/else decisions check card expiry, frozen status, and available credit. Approval adds $50 with BigDecimal, making outstanding $250/available $750. Declines preserve the balance. Both create history with amount/status/reason/outstandingAfter. A new outcome returns 201 with transaction and current account summary.

@Transactional commits balance/history together or rolls both back. Pessimistic account locks make concurrent changes take turns. READ_COMMITTED lets waiting retries see committed results. The unique account/request ID constraint backs duplicate protection. Each protection has a different job.

## Authentication

AuthController validates register/login JSON and calls AuthService. Registration normalizes email, hashes the password with BCrypt, and saves USER/account/card in one transaction. Callers cannot choose ADMIN. A failed card write rolls back user/account creation. Duplicate email uses fixed 409.

AuthService names its database dependencies userRepository, accountRepository, and cardRepository. passwordEncoder performs BCrypt operations, and jwtTokenService issues the signed token. Registration reads in order: validate password bytes and normalize email, reject a duplicate, save the USER, save customerAccount, save assignedCard, and return the safe user response.

Login validates password bytes, normalizes email, and loads the user. It selects the stored passwordHash or dummyPasswordHash, then calls passwordEncoder.matches. The dummy hash makes an unknown email perform a BCrypt comparison too, reducing response-time differences. Incorrect credentials share one 401 message. A successful check returns safe user/token/expiration. Spring/Nimbus handle signatures and intended claims. JWT_SECRET remains in ignored config/environment. No invented cryptography or refresh-token system exists.

React holds the token in a ref. Sign-out/reload/expiration discard it. A copied token remains valid until expiry. See the [API contract](03_API_Design.md) for CORS/CSRF/rate-limit/logout behavior.

## Other workflows

- Account/card: stored USER role, ownership, safe summary DTOs.
- History: owned account, bounded newest-first page, batch refunded-purchase lookup.
- Refund: owned purchase, locked account, duplicate/eligibility checks, original full amount, linked reversal. Frozen accounts may receive it.
- Admin: stored ADMIN role, paged summaries, locked ACTIVE/FROZEN change.

## Java pieces

| Piece | Job |
| --- | --- |
| Controller | HTTP path, verified identity, @Valid, page bounds, status. |
| Service | Direct business/ownership decisions, locks, transaction boundaries. |
| Repository | Named JPA searches, page queries, explicit queries when needed. |
| Entity | Private mapped fields/getters/setters. JPA needs its no-argument constructor. |
| Request DTO | Input fields and annotations. Sensitive fields are write-only. |
| Response DTO | Account/card/transaction display fields and login token details. Authentication reuses AppUser; @JsonIgnore excludes its password hash. |
| PageResponse | items/page/size/totals for list navigation. |
| Exceptions/handler | Safe missing-resource/conflict/shared errors. |
| Security configuration | One stateless JWT path and explicit route/CORS/CSRF settings. |

Entities follow foreign keys in one direction with no reverse collections or deletion cascades. Four tables suffice. BigDecimal handles decimals; available credit is calculated rather than stored twice. SQL constraints and boundary validation support service rules without duplicating every business decision.

## Practice and evidence

Trace register/login/purchase/retry/decline/refund/freeze from page to database. Explain HTTP 201 versus financial DECLINED. Explain why UUID, lock, and transaction are each necessary. Predict balances before running examples.

HTTP/security tests use mocked repositories; separate MySQL tests prove persistence/concurrency/rollback behavior. Mocks alone cannot prove atomicity. [Verification](../03_Verification/01_Completion_Checklist.md) includes coverage, Postman, and SonarQube.
