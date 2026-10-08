# API design — the basic MVP

Updated October 8, 2026. These routes use one Spring Boot API under `/api`.

## Common rules

- Successful operations return **200 OK**, including saved approvals, saved declines, and retries.
- Errors use the HTTP status and one JSON field: `{"message":"..."}`.
- Purchase input uses one JSON DTO. Refund/status changes use one query parameter each.
- Lists are plain arrays with no pagination parameters or wrapper.
- IDs are integers. Purchase money accepts a decimal string or JSON number; responses use JSON numbers. Java calculations use BigDecimal, and the React display will format two decimal places.
- Transaction timestamps are UTC ISO strings ending in Z, with whole-second precision.
- Account IDs and roles supplied by the browser never establish identity.

The current API requires a server session with a positive Long `userId`. No sign-in endpoint creates one yet; external calls return 401 before protected input is read. Tests set that session on mock server requests only.

## Implemented routes

| Method and path | Access | Input | Response |
| --- | --- | --- | --- |
| GET `/api/accounts` | USER | None | Account array with only the current customer's account |
| GET `/api/accounts/{accountId}/cards` | Account owner | Account ID | Masked card array |
| POST `/api/accounts/{accountId}/purchases` | Account owner | Purchase JSON below | TransactionResult |
| GET `/api/accounts/{accountId}/transactions` | Account owner | Account ID | Transaction array, newest first |
| POST `/api/transactions/{purchaseId}/refund?requestId=<UUID>` | Purchase owner | Purchase ID and requestId; no body | TransactionResult |
| GET `/api/admin/accounts` | ADMIN | None | Account array ordered by account ID ascending |
| GET `/api/admin/transactions` | ADMIN | None | Transaction array, newest first |
| PATCH `/api/admin/accounts/{accountId}/status?status=FROZEN` | ADMIN | Account ID and ACTIVE/FROZEN; no body | Updated Account |

Customer and admin endpoints use the same safe response classes. Account summaries contain the owner's display name. An admin matches a transaction's accountId to the account list. There are no separate admin response types or owner email fields.

## Purchase JSON

| Field | Rule |
| --- | --- |
| cardId | Positive ID of the assigned card within this account |
| testCardNumber | The assigned DEMO_4242 fictional number only: 4242424242424242 |
| expiryMonth | Integer 1–12 matching the card |
| expiryYear | Integer 2000–9999 matching the card |
| testSecurityCode | Three or four digits; format only, never stored/logged/returned |
| merchantName | Nonblank, at most 100 characters |
| amount | Positive, at most 12 whole digits and two decimal places, without rounding |
| requestId | UUID created once for this submission; reuse it for an uncertain retry |

The service checks input in one place. Missing/malformed fields return 400 and create no history. A card lookup verifies that the card belongs to the owned account; its number/profile/expiry must match. A matching card is valid through its expiry month in UTC.

A valid new attempt checks CARD_EXPIRED, ACCOUNT_FROZEN, and INSUFFICIENT_CREDIT in that order. A business-rule decline is saved with DECLINED and leaves the balance unchanged. An approval adds the amount to the outstanding balance. Available credit equals credit limit minus outstanding balance.

Under the account lock, a matching account/request ID returns the saved transaction after checking its type, card ID, exact merchant, and numeric amount. UUIDs normalize to lowercase. Changed details or reuse across purchase/refund types return 409. A retry preserves the transaction's original timestamp/balance snapshot and returns the current account summary alongside it. Amounts 50 and 50.00 compare equal.

## Full refund

The purchase must be owned, approved, and not already refunded. The server copies its amount, merchant, account, and card; the caller supplies no refund amount. The refund subtracts the original amount and adds one linked REFUND row. An identical retry returns that row. Another request ID cannot refund the purchase again. A frozen account can receive a refund.

Purchases/refunds use an account write lock and a READ_COMMITTED database transaction. Balance and history commit together or roll back together. MySQL keeps the unique account/request-ID and original-purchase rules.

## Safe response fields

| DTO | Fields |
| --- | --- |
| AccountResponse | id, ownerName, creditLimit, outstandingBalance, availableCredit, status |
| CardResponse | id, label, maskedNumber, expiryMonth, expiryYear |
| TransactionResponse | id, accountId, cardId, type, status, amount, outstandingAfter, merchantName, reasonCode, createdAt, originalPurchaseId |
| TransactionResultResponse | transaction and account |

reasonCode and originalPurchaseId may be null. maskedNumber contains only the last four digits, such as `•••• 4242`. No response includes a password hash, full card number, security code, or request ID. Mapping happens inside service transactions; controllers return DTOs directly.

## Errors

| HTTP status | Meaning |
| --- | --- |
| 400 | Invalid purchase fields, UUID, JSON, parameter type, or account status |
| 401 | No logged-in server session |
| 403 | Stored role does not allow the operation |
| 404 | Missing or unowned account, card, purchase, or route |
| 409 | Conflicting request ID or ineligible full refund |
| 405 / 406 / 415 | Unsupported method, response format, or request content type |
| 500 | Unexpected error, with a generic message |

Services use Spring's ResponseStatusException with fixed messages. ApiExceptionHandler returns those messages for expected failures and safe generic messages for framework/unexpected failures. It preserves Spring's status and headers. Unexpected failures log only exception type, excluding messages, causes, and request values. Request, SQL, and bind-value logging remain disabled.

## Planned sign-in

Registration/sign-in/sign-out and current-user routes are unfinished. The smaller plan is BCrypt password checks plus a standard server session cookie with CSRF protection. Registration creates a USER, one account, and one demo card; the browser never chooses its role. Wrong credentials return 401 and duplicate email returns 409. JWT issuing/verification is outside the MVP.
