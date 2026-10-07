# API Design

**Credit Card Transaction Simulator**<br>
**October 7, 2026**

## Overview

The React app calls one Spring Boot REST API under `/api`. Controllers receive requests and return responses. Services check ownership, validate the purchase or refund, and update the account. Spring Data JPA repositories handle the MySQL reads and writes. The API does not call a bank or payment processor.

Customer and admin controllers return dedicated response DTOs over the existing services and repositories. One request DTO validates the purchase body and passes directly to the service. Refund request IDs and admin status changes use single query parameters without request DTOs. New purchases and refunds return `201`; saved retries return `200`. Shared HTTP error handling is implemented. JWT authentication is planned separately.

Purchase and response bodies use JSON. Refund and account-status requests have no body. Field names use `camelCase`. IDs are positive integers, purchase amounts accept decimal strings such as `"25.00"` or JSON numbers such as `25.00`, response money amounts are decimal strings, and timestamps are ISO 8601 strings in UTC ending in `Z`. Fractional IDs and numeric status values are rejected. Money responses always have two decimal places. The account balance shown in a response is the **outstanding balance**; available credit is the credit limit minus that balance.

The authentication design uses `Authorization: Bearer <JWT>`. The React app will hold the token in memory and clear it on sign-out. Refreshing the page requires signing in again. The current API requires a server-established `AuthenticatedUser` servlet principal and returns `401` without it. No component creates that principal yet; bearer text alone cannot open the API. IDs and roles in headers, query parameters, paths, or bodies are never proof of identity.

## Shared response shapes

These shapes are reused across endpoints so the frontend can handle them consistently. A `?` means the field may be null.

| Shape | Fields |
| --- | --- |
| `User` | `id: number`, `displayName: string`, `email: string`, `role: "USER" \| "ADMIN"` |
| `Account` | `id: number`, `creditLimit: decimal string`, `outstandingBalance: decimal string`, `availableCredit: decimal string`, `status: "ACTIVE" \| "FROZEN"` |
| `Card` | `id: number`, `label: string`, `maskedNumber: string`, `expiryMonth: number`, `expiryYear: number`, `testProfile: string` |
| `Transaction` | `id: number`, `accountId: number`, `cardId: number`, `type: "PURCHASE" \| "REFUND"`, `status: "APPROVED" \| "DECLINED"`, `amount: decimal string`, `outstandingAfter: decimal string`, `merchantName: string`, `reasonCode?: string`, `createdAt: UTC timestamp`, `originalPurchaseId?: number` |
| `TransactionResult` | `transaction: Transaction`, `account: Account` |
| `AdminAccount` | All `Account` fields plus `ownerId: number`, `ownerDisplayName: string`, `ownerEmail: string` |
| `AdminTransaction` | All `Transaction` fields plus `ownerEmail: string` |
| `Page<T>` | `items: T[]`, `page: number`, `size: number`, `totalItems: number`, `totalPages: number` |
| `Error` | `status: number`, `code: string`, `message: string`, `timestamp: UTC timestamp` |

The API never returns a password hash, full card number, test security code, or JWT signing key. `maskedNumber` contains only the last four digits, for example `•••• 4242`.

The shared error DTO contains exactly four fields. Its timestamp records when the error response is created, in UTC ending in `Z`.

## Authentication design

| Method and path | Access | Request body | Success response |
| --- | --- | --- | --- |
| `POST /api/auth/register` | Public | `displayName: string`, `email: string`, `password: string` | `201 Created` → `User`. Registration creates a USER, one credit account, and one demo card together. |
| `POST /api/auth/login` | Public | `email: string`, `password: string` | `200 OK` → `accessToken: string`, `expiresAt: UTC timestamp`, `user: User`. |
| `GET /api/auth/me` | USER or ADMIN | No body | `200 OK` → `User`. |

Registration, login, and `/me` are not implemented yet. The authentication contract excludes a registration role field, uses `409` for duplicate email and `401` for incorrect login without identifying which credential failed, and defers login rate limiting beyond the local MVP.

## Customer accounts and cards

| Method and path | Access | Request | Success response |
| --- | --- | --- | --- |
| `GET /api/accounts` | USER | No body | `200 OK` → `Account[]`, containing only the signed-in customer's account. |
| `GET /api/accounts/{accountId}/cards` | Owner of account | `accountId: number` in path | `200 OK` → `Card[]`, containing only that account's masked demo card. |

The controller gets the user ID from the server-established principal; the service checks the stored role and account ownership. Changing `{accountId}` does not establish identity or grant access. An unavailable or unowned account raises an unavailable-resource exception; its `404` HTTP mapping is part of the shared exception handler.

## Purchases, history, and refunds

| Method and path | Access | Request | Success response |
| --- | --- | --- | --- |
| `POST /api/accounts/{accountId}/purchases` | Owner of account | `PurchaseRequest` body below | `201 Created` → `TransactionResult` for a new approved **or declined** purchase. An identical retry returns `200 OK` with the saved result. |
| `GET /api/accounts/{accountId}/transactions` | Owner of account | Optional `page` and `size` query parameters | `200 OK` → `Page<Transaction>`, newest first. |
| `POST /api/transactions/{purchaseId}/refund` | Owner of original purchase | `requestId` UUID query parameter; no body | `201 Created` → `TransactionResult` for the full refund. An identical retry returns `200 OK` with the saved result. |

### `PurchaseRequest`

| Field | Type | Rule |
| --- | --- | --- |
| `cardId` | number | Must name the demo card owned by the account. |
| `testCardNumber` | string | The assigned `DEMO_4242` profile accepts `4242424242424242`; no other number is accepted. |
| `expiryMonth` | number | 1–12 and must match the demo card. |
| `expiryYear` | number | 2000–9999 and matches the demo card. A card remains valid through the end of its expiry month in UTC; an expired matching card is declined. |
| `testSecurityCode` | string | Three or four digits. Format check only; never saved, logged, or returned. |
| `merchantName` | string | Required, at most 100 characters. |
| `amount` | decimal string or JSON number | Standard `BigDecimal` binding; positive, at most 12 whole digits and two decimal places, without rounding. |
| `requestId` | UUID string | Generated once per attempted purchase and reused only for retries of that purchase. |

The server checks the fields again even if React has already shown form feedback. Spring MVC rejects malformed or missing fields with `400`. The service rejects an unrecognized test number or mismatched expiry without creating a transaction; these domain exceptions have a `400` error contract. Amounts fit `DECIMAL(14,2)` without rounding. A valid purchase that fails a business rule creates a `DECLINED` transaction with `CARD_EXPIRED`, `ACCOUNT_FROZEN`, or `INSUFFICIENT_CREDIT`, checked in that order. Its `outstandingAfter` is unchanged.

For a new approved purchase, the service increases the outstanding balance and records the transaction in one database transaction. The account is locked before checking the account/request-ID pair or changing the balance. Read-committed isolation lets a waiting retry see the committed result. UUIDs are normalized to lowercase. Within an account, the service compares `cardId`, exact `merchantName`, and numeric `amount`; `50` and `50.00` are equivalent. Card fields still pass the assigned-profile checks. An identical retry returns the original transaction, including its original balance and timestamp, with the current account summary. Different details or reuse between purchase and refund operations raises a conflict exception, assigned `409` in the error contract.

### Refund parameter

| Field | Type | Rule |
| --- | --- | --- |
| `requestId` | UUID string | Identifies this full-refund submission and its retries. |

Send `POST /api/transactions/{purchaseId}/refund?requestId=<UUID>` with no body. The purchase ID comes from the path. The server checks that it is an approved PURCHASE owned by the signed-in customer and has not been refunded. A successful refund uses the original purchase's amount, merchant, account, and card. It creates a REFUND transaction with `originalPurchaseId` set to the purchase ID and reduces the outstanding balance in the same database transaction. The service raises an ineligible-refund exception for a second refund under a different request ID and an unavailable-resource exception for an unowned purchase. Their error contract uses `409` and `404`, respectively. Freezing an account does not block a valid refund.

Transaction history uses `page=0` and `size=20` by default, with a maximum size of 50. Newest first means descending transaction ID, matching the append-only history and the `(account_id, id)` database index. This gives transactions with the same timestamp a stable order. The original approved purchase remains in history after a refund; the linked REFUND row shows the reversal.

## Administration

| Method and path | Access | Request | Success response |
| --- | --- | --- | --- |
| `GET /api/admin/accounts` | ADMIN | Optional `page` and `size` query parameters | `200 OK` → `Page<AdminAccount>`. |
| `GET /api/admin/transactions` | ADMIN | Optional `page` and `size` query parameters | `200 OK` → `Page<AdminTransaction>`, newest first. |
| `PATCH /api/admin/accounts/{accountId}/status` | ADMIN | `status=ACTIVE` or `status=FROZEN` query parameter; no body | `200 OK` → `AdminAccount` with the updated status. |

Admin list endpoints use the same page defaults and maximum size as customer history. Accounts are ordered by ascending account ID; transactions use descending transaction ID. Services check the user's stored role, and their wrong-role exception has a `403` error contract. An ADMIN can review accounts and change account status, but cannot submit a purchase as a customer. Owner details are fetched before the service transaction closes, so admin response mapping works without an open persistence session.

## Error responses and status codes

`exception/ApiExceptionHandler` returns the four-field `Error` shape. Messages use fixed, safe guidance without repeating rejected values; the frontend form can show field-specific feedback. Missing and unowned resources have the same message. The identity filter returns `401 AUTHENTICATION_REQUIRED` with `Authentication is required.` before MVC reads protected input; the controller identity guard uses the same response.

| Status | Code | Used for |
| --- | --- | --- |
| `400` | `INVALID_PURCHASE` | Invalid amount, merchant, or assigned fictional card details. |
| `400` | `INVALID_REQUEST` | Invalid request ID or pagination rejected by a service. |
| `400` | `MALFORMED_JSON` | Malformed or missing purchase JSON, or incorrect body field types. |
| `400` | `VALIDATION_FAILED` | Missing, null, or invalid body fields. |
| `400` | `INVALID_PARAMETER` | Missing required query parameter, invalid path/query types or ranges, invalid refund request ID, or unsupported status value. |
| `401` | `AUTHENTICATION_REQUIRED` | No trusted server-side principal. |
| `403` | `ACCESS_DENIED` | Stored role does not allow the operation. |
| `404` | `RESOURCE_NOT_FOUND` | Unavailable or unowned account, card, or purchase; unknown route. |
| `409` | `REQUEST_CONFLICT` | Request ID reused with changed details or a different operation. |
| `409` | `REFUND_NOT_ELIGIBLE` | Purchase is not eligible for a full refund. |
| `405` | `METHOD_NOT_ALLOWED` | Unsupported HTTP method; the `Allow` header is preserved. |
| `406` | `NOT_ACCEPTABLE` | Requested response format is unavailable; the API returns JSON. |
| `415` | `UNSUPPORTED_MEDIA_TYPE` | Request body is not sent as JSON. |
| `500` | `INTERNAL_ERROR` | Unexpected failure. |

Unexpected failures return `An unexpected error occurred. Please try again later.` Responses contain no exception class, stack trace, or internal detail. Expected rejections do not produce application request logs. Unexpected failures log only the exception type and stack locations at ERROR, excluding messages and causes. Request, SQL, and bind-value logging are disabled, including Hibernate driver-error text.

Rejected purchase input, conflicting retries, and ineligible refunds leave account balances and transaction history unchanged. An approved or declined purchase remains a recorded outcome with a success HTTP status.

JWT failures and incorrect login will use `401`; duplicate email will use `409`. Login rate limiting is deferred beyond the local MVP. These authentication behaviors are not implemented yet.

```json
{
  "status": 401,
  "code": "AUTHENTICATION_REQUIRED",
  "message": "Authentication is required.",
  "timestamp": "2026-10-06T20:30:00Z"
}
```

An approved or declined purchase is a recorded outcome, so both use a success HTTP status with the transaction's `status` field distinguishing them. This lets the frontend show a meaningful decline rather than treating it as a server failure.

## Example purchase result

```json
{
  "transaction": {
    "id": 42,
    "accountId": 7,
    "cardId": 7,
    "type": "PURCHASE",
    "status": "APPROVED",
    "amount": "50.00",
    "outstandingAfter": "250.00",
    "merchantName": "Demo Bookstore",
    "reasonCode": null,
    "createdAt": "2026-10-06T15:30:00Z",
    "originalPurchaseId": null
  },
  "account": {
    "id": 7,
    "creditLimit": "1000.00",
    "outstandingBalance": "250.00",
    "availableCredit": "750.00",
    "status": "ACTIVE"
  }
}
```
