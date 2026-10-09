# API design

Local base URL: `http://127.0.0.1:8080`. JSON bodies are listed below. Public generated documentation is at /v3/api-docs and /swagger-ui/index.html. The [OpenAPI snapshot](../03_Verification/Credit_Circuit.openapi.json) records the running API. The exported [Postman collection](../03_Verification/Credit_Circuit.postman_collection.json) exercises every application endpoint.

## Access

Protected requests explicitly send `Authorization: Bearer <access token>`. React holds access only in memory. The API accepts no login session/authentication cookie. Missing, invalid, or expired credentials return 401. Wrong roles return 403. Services check the stored role and account owner. Inaccessible customer resources use the same safe 404 as missing resources.

POST register/login share ten attempts per remote IP per minute in one process. Limits return 429 and Retry-After. The bounded map expires entries and resets on restart; it does not trust forwarded IP headers. CORS permits configured localhost origins without cookie credentials.

## Endpoints

| Method and path | Access | Input | Success |
| --- | --- | --- | --- |
| POST /api/auth/register | Public | JSON displayName, email, password | 201 AppUser JSON |
| POST /api/auth/login | Public | JSON email, password | 200 LoginResponse |
| GET /api/auth/me | Signed in | Bearer header | 200 AppUser JSON |
| GET /api/accounts | USER | Bearer header | 200 CreditAccount JSON array |
| GET /api/accounts/{accountId}/cards | Owning USER | Positive account ID | 200 DemoCard JSON array |
| POST /api/accounts/{accountId}/purchases | Owning USER | PurchaseRequest JSON | 201 new outcome, 200 identical retry |
| GET /api/accounts/{accountId}/transactions | Owning USER | page and size query | 200 transaction page |
| POST /api/transactions/{purchaseId}/refund | Owning USER | UUID requestId query, no body | 201 new refund, 200 identical retry |
| GET /api/admin/accounts | ADMIN | page and size query | 200 account page |
| GET /api/admin/transactions | ADMIN | page and size query | 200 transaction page |
| PATCH /api/admin/accounts/{accountId}/status | ADMIN | status=ACTIVE or FROZEN query, no body | 200 CreditAccount JSON |

Account/card arrays remain small because one customer has one of each. History/admin pages default to page=0 and size=10. Page is zero based, 0–10000; size is 1–50. Invalid bounds return 400. History/activity order by descending transaction ID; accounts by ascending account ID. A valid page beyond the end has empty items and accurate totals.

```json
{"items":[],"page":0,"size":10,"totalElements":0,"totalPages":0}
```

## Registration and login

Registration validates nonblank displayName up to 100 characters, email up to 150, and password of 12–72 characters within BCrypt's 72 UTF-8-byte limit. Email is trimmed/lowercased. Unknown fields, including role, are rejected. Registration always creates USER, an ACTIVE $1,000 account with zero outstanding balance, and one ACCOUNT_V1 fictional card in one transaction. Duplicate email returns safe 409. ADMIN is provisioned privately rather than selected by registration.

AppUser JSON contains id, displayName, email, role. Its passwordHash field and getter are excluded with @JsonIgnore. Registration, current-user reads, and the user inside LoginResponse share this representation. LoginResponse also contains accessToken, expiresAt, tokenType=Bearer. Incorrect email/password combinations receive one 401 message. HS256 uses an external random key of at least 256 bits. Validation checks signature/algorithm, issuer=credit-circuit, exact audience=credit-circuit-api, positive numeric subject, USER/ADMIN role, issue time, and expiration. Default lifetime is 900 seconds.

## Purchases, refunds, and results

PurchaseRequest contains cardId, testCardNumber, expiryMonth, expiryYear, testSecurityCode, merchantName, amount, and requestId. IDs are positive. The number has 16 fictional digits and must match the complete number derived for the owned assigned card. Month is 1–12, year 2000–9999, security code 3–4 fictional digits, merchant nonblank/up to 100 characters, amount positive with at most 12 whole digits/two decimals, and requestId a UUID. The security code checks format only and is discarded. Full numbers/codes never appear in stored rows, responses, or logs.

New cards use ACCOUNT_V1. FictionalCardNumbers forms a simulation-only number from `0000` followed by the account ID padded to 12 digits with leading zeros. It compares the entire number and stored expiry, not just last_four. The account ID and profile remain in MySQL, so sign-out, sign-in, reload, and backend restart do not change the assigned details. Supported account IDs are 1–999999999999; registration fails atomically beyond that explicit simulation limit. This predictable derivation is not a payment credential or an authentication secret. DemoCard JSON returns maskedNumber plus numberEntryHint with the entry instruction, never a full number. Existing DEMO_4242 cards keep their legacy repeated-4242 rule; no existing card, balance, or history is rewritten. No schema migration is required.

Results contain transaction and account. CreditAccount JSON contains id, ownerName, creditLimit, outstandingBalance, availableCredit, status. DemoCard JSON contains id, label, maskedNumber, numberEntryHint, expiryMonth, expiryYear. TransactionResponse contains id, accountId, type, status, amount, outstandingAfter, merchantName, reasonCode, createdAt, originalPurchaseId, refunded. Account and card models provide these display fields directly. Their linked user/account objects and internal card profile/lastFour fields are excluded with @JsonIgnore. Money is JSON numbers backed by BigDecimal and formatted as USD in React. Timestamps use UTC with whole-second service precision.

Malformed/mismatched card input returns 400 without history. An assigned expired card, frozen account, or insufficient credit saves DECLINED with CARD_EXPIRED, ACCOUNT_FROZEN, or INSUFFICIENT_CREDIT and returns 201. Approval also returns 201 and increases outstanding balance. Declines preserve it. These are financial outcomes, distinct from HTTP/network failures.

Request IDs are unique per account across purchases/refunds. An identical retry returns 200, the original transaction, and the account's current summary without another write. Changed purchase details under the same ID return 409. The browser preserves the UUID/body during uncertain retries. A full refund copies the original owned approved purchase's amount, subtracts it, and creates one linked reversal. A new ID for an already-refunded purchase returns 409. Frozen accounts can receive eligible refunds. History marks originals refunded even across separate pages.

## Errors and logout

Errors use `{"message":"safe explanation"}`. Bean Validation adds fields, a field-name/message map. Rejected values, SQL details, passwords, and tokens never appear. Common statuses: 400 format/parameters, 401 authentication, 403 role, 404 inaccessible/missing resource, 409 duplicate/state conflict, 429 auth limit, safe 500 unexpected error. Unexpected logging contains only exception type.

Sign-out/reload/expiry timer/visibility recheck/protected 401 discard React access. No logout endpoint, refresh token, or revocation list exists. A copied token remains usable until expiration. Leaving/reloading an uncertain page loses memory-only retry details; inspect history after signing in before starting another purchase.

CSRF ignores only /api/** because identity uses an explicit bearer header and cookie/Basic/form authentication is disabled. CORS disallows cookie credentials. Adding automatic browser credentials would require revisiting this decision. The actual Postman run verifies allowed/untrusted origins and cookie-only 401 behavior.
