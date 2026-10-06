# API Design

**Credit Card Transaction Simulator**<br>
**October 6, 2026**

## Overview

The React app calls one Spring Boot REST API under `/api`. Controllers receive requests and return responses. Services check ownership, validate the purchase or refund, and update the account. Spring Data JPA repositories handle the MySQL reads and writes. The API does not call a bank or payment processor.

This document defines the planned HTTP contract. The account and transaction services and repository operations are implemented; controllers, JSON DTOs, HTTP error mapping, and JWT authentication are planned.

Request and response bodies use JSON. Field names use `camelCase`. IDs are numbers, money amounts are decimal strings such as `"25.00"`, and timestamps are ISO 8601 strings in UTC. The account balance shown in a response is the **outstanding balance**; available credit is the credit limit minus that balance.

Protected requests send `Authorization: Bearer <JWT>`. The React app holds the token in memory and clears it on sign-out. Refreshing the page requires signing in again. No endpoint accepts a role supplied by the browser as proof of access.

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

## Authentication

| Method and path | Access | Request body | Success response |
| --- | --- | --- | --- |
| `POST /api/auth/register` | Public | `displayName: string`, `email: string`, `password: string` | `201 Created` → `User`. Registration creates a USER, one credit account, and one demo card together. |
| `POST /api/auth/login` | Public | `email: string`, `password: string` | `200 OK` → `accessToken: string`, `expiresAt: UTC timestamp`, `user: User`. |
| `GET /api/auth/me` | USER or ADMIN | No body | `200 OK` → `User`. |

Registration never accepts a role field. Duplicate email returns `409 Conflict`. Incorrect login returns `401 Unauthorized` without saying whether the email or password was wrong. Repeated login attempts can return `429 Too Many Requests`.

## Customer accounts and cards

| Method and path | Access | Request | Success response |
| --- | --- | --- | --- |
| `GET /api/accounts` | USER | No body | `200 OK` → `Account[]`, containing only the signed-in customer's account. |
| `GET /api/accounts/{accountId}/cards` | Owner of account | `accountId: number` in path | `200 OK` → `Card[]`, containing only that account's masked demo card. |

The server gets the signed-in user from the validated JWT and checks account ownership. Changing `{accountId}` to another customer's ID does not return that customer's data. An unavailable or unowned account returns `404 Not Found`.

## Purchases, history, and refunds

| Method and path | Access | Request | Success response |
| --- | --- | --- | --- |
| `POST /api/accounts/{accountId}/purchases` | Owner of account | `PurchaseRequest` body below | `201 Created` → `TransactionResult` for a new approved **or declined** purchase. An identical retry returns `200 OK` with the saved result. |
| `GET /api/accounts/{accountId}/transactions` | Owner of account | Optional `page` and `size` query parameters | `200 OK` → `Page<Transaction>`, newest first. |
| `POST /api/transactions/{purchaseId}/refund` | Owner of original purchase | `RefundRequest` body below | `201 Created` → `TransactionResult` for the full refund. An identical retry returns `200 OK` with the saved result. |

### `PurchaseRequest`

| Field | Type | Rule |
| --- | --- | --- |
| `cardId` | number | Must name the demo card owned by the account. |
| `testCardNumber` | string | The assigned `DEMO_4242` profile accepts `4242424242424242`; no other number is accepted. |
| `expiryMonth` | number | 1–12 and must match the demo card. |
| `expiryYear` | number | 2000–9999 and matches the demo card. A card remains valid through the end of its expiry month in UTC; an expired matching card is declined. |
| `testSecurityCode` | string | Three or four digits. Format check only; never saved, logged, or returned. |
| `merchantName` | string | Required, at most 100 characters. |
| `amount` | decimal string | Positive, no more than two decimal places. |
| `requestId` | UUID string | Generated once per attempted purchase and reused only for retries of that purchase. |

The server checks the fields again even if React has already shown form feedback. A malformed request, unrecognized test number, or mismatched expiry returns `400 Bad Request` and creates no transaction. Amounts fit `DECIMAL(14,2)` without rounding. A valid purchase that fails a business rule creates a `DECLINED` transaction with `CARD_EXPIRED`, `ACCOUNT_FROZEN`, or `INSUFFICIENT_CREDIT`, checked in that order. Its `outstandingAfter` is unchanged.

For a new approved purchase, the service increases the outstanding balance and records the transaction in one database transaction. The account is locked before checking the account/request-ID pair or changing the balance. Read-committed isolation lets a waiting retry see the committed result. UUIDs are normalized to lowercase. Within an account, the service compares `cardId`, exact `merchantName`, and numeric `amount`; `50` and `50.00` are equivalent. Card fields still pass the assigned-profile checks. An identical retry returns the original transaction, including its original balance and timestamp, with the current account summary. Different details or reuse between purchase and refund operations returns `409 Conflict`.

### `RefundRequest`

| Field | Type | Rule |
| --- | --- | --- |
| `requestId` | UUID string | Identifies this full-refund submission and its retries. |

The purchase ID comes from the path. The server checks that it is an approved PURCHASE owned by the signed-in customer and has not been refunded. A successful refund uses the original purchase's amount, merchant, account, and card. It creates a REFUND transaction with `originalPurchaseId` set to the purchase ID and reduces the outstanding balance in the same database transaction. A second refund under a different request ID returns `409 Conflict`. An unowned purchase returns `404 Not Found`. Freezing an account does not block a valid refund.

Transaction history uses `page=0` and `size=20` by default, with a maximum size of 50. Newest first means descending transaction ID, matching the append-only history and the `(account_id, id)` database index. This gives transactions with the same timestamp a stable order. The original approved purchase remains in history after a refund; the linked REFUND row shows the reversal.

## Administration

| Method and path | Access | Request | Success response |
| --- | --- | --- | --- |
| `GET /api/admin/accounts` | ADMIN | Optional `page` and `size` query parameters | `200 OK` → `Page<AdminAccount>`. |
| `GET /api/admin/transactions` | ADMIN | Optional `page` and `size` query parameters | `200 OK` → `Page<AdminTransaction>`, newest first. |
| `PATCH /api/admin/accounts/{accountId}/status` | ADMIN | `status: "ACTIVE" \| "FROZEN"` | `200 OK` → `AdminAccount` with the updated status. |

Admin list endpoints use the same page defaults and maximum size as customer history. Accounts are ordered by ascending account ID; transactions use descending transaction ID. A USER calling an admin endpoint receives `403 Forbidden`. An ADMIN can review accounts and change account status, but cannot submit a purchase using another customer's account.

## Error responses and status codes

All errors use the shared `Error` shape. Messages explain what the user can correct without echoing card details or other sensitive input.

| Status | Used for |
| --- | --- |
| `400 Bad Request` | Missing or malformed fields, invalid amount, unrecognized test card, or unsupported status value. |
| `401 Unauthorized` | Missing, expired, or invalid JWT; incorrect login. |
| `403 Forbidden` | Signed-in user has the wrong role for an endpoint. |
| `404 Not Found` | Account, card, or purchase does not exist or is not owned by the customer. |
| `409 Conflict` | Duplicate email, request ID reused with changed details, or refund of a purchase that is not eligible. |
| `429 Too Many Requests` | Login rate limit reached. |
| `500 Internal Server Error` | Unexpected failure; response contains no stack trace or secrets. |

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
