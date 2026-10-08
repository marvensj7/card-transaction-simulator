# Explain the project from one purchase

Updated October 8, 2026. The application uses fictional cards and balances and runs locally.

## 1. Start with the result

> My application simulates a card purchase. The server checks the account and card, approves or declines the purchase, saves the outcome, and returns the account balance.

Start with a $1,000 credit limit and a $200 outstanding balance. Available credit is $800. A $50 approval changes the balance to $250 and available credit to $750. A $900 decline keeps those balances unchanged. Refunding the $50 purchase returns them to $200 and $800.

| Action | Outstanding balance | Available credit | History |
| --- | --- | --- | --- |
| Start | $200 | $800 | Existing demo starting balance |
| Purchase $50, request A | $250 | $750 | Approved purchase A |
| Retry request A | $250 | $750 | No additional purchase |
| Purchase $900, request B | $250 | $750 | Decline: insufficient credit |
| Refund purchase A, request C | $200 | $800 | Linked refund C |

## 2. Follow six steps

```text
React page → API request → controller → service → repository → MySQL
```

The service builds a safe response and the controller returns it as JSON. The future React page will display it.

## 3. Read the actual purchase code

Open [TransactionController.java](../../backend/src/main/java/com/marvens/capstone/controller/TransactionController.java), then [PurchaseRequest.java](../../backend/src/main/java/com/marvens/capstone/dto/PurchaseRequest.java), then [TransactionService.java](../../backend/src/main/java/com/marvens/capstone/service/TransactionService.java).

Read the service's purchase method from top to bottom:

1. requireRole checks the user's stored role.
2. validatePurchase checks the input once, using ordinary if statements.
3. checkRequestId checks/normalizes the submission's UUID.
4. lockOwnedAccount finds this customer's account and makes other balance-changing requests wait.
5. The card repository finds the assigned card within this account. validateAssignedCard checks its fictional details.
6. An existing identical request returns the saved result. Reusing the ID for different details throws a conflict.
7. availableCredit is calculated by subtracting the outstanding balance from the limit.
8. if/else checks expiry, frozen status, and insufficient credit.
9. Approval adds the amount to the balance. A decline leaves it unchanged.
10. fillHistory sets the common history fields, the repository saves the transaction, and the method returns the transaction plus the current account summary.

`request.amount.compareTo(availableCredit) > 0` means the purchase amount is larger than available credit. `balance.add(amount)` returns a new BigDecimal, so setOutstandingBalance stores that new value on the account object. A credit card purchase increases the amount owed.

`@Transactional` makes the balance change and history save one database operation: both succeed or both roll back. The account lock makes simultaneous changes take turns. READ_COMMITTED means a waiting retry sees the earlier committed result. The request ID identifies one submission so a repeated request creates no second purchase. These are three distinct jobs.

## 4. The Java building blocks in this code

| Code idea | What to say |
| --- | --- |
| Class | A definition of an object: its fields and methods. CreditAccount describes an account. |
| Object | One instance of a class, such as the current customer's account. |
| Field / variable | A named value. request.amount is input; availableCredit is a calculated local value. |
| Constructor | Sets up a new object. Spring passes repositories to a service constructor. A response constructor copies safe fields from an entity. |
| Method | A named action with inputs and a return value. purchase accepts IDs/input and returns a result. |
| if/else | Chooses a path when a condition is true or false. |
| List and for loop | Hold several results and visit each one. There are no stream pipelines or generic page wrappers. |
| null | A missing value. A repository lookup may return null when no row matches. |
| enum | A small allowed set: ACTIVE/FROZEN or APPROVED/DECLINED. |
| throw | Stops the normal path with an error. Spring's ResponseStatusException carries an HTTP status and a fixed message. |
| BigDecimal | Java's decimal number type for money calculations; SQL stores DECIMAL(14,2). |
| DTO | A plain class carrying API input or safe output. Its fields are the JSON fields. |
| Entity | A Java class mapped to a database table. Its private fields use getters/setters. |
| Foreign key | A stored ID connecting one row to another, such as a card's account_id. |

`final` on a response field means its value is assigned in the constructor and cannot be reassigned afterward. `List<AccountResponse>` means a list whose items are AccountResponse objects. `users.findById(id).orElse(null)` uses Spring's standard lookup and gives us null if that user does not exist.

The `@` annotations tell Spring/JPA what a class or field does. Know their purpose: RestController handles HTTP, Service supplies a service object, Entity/Table/Column map stored data, and Transactional groups database work. Repository method names describe their searches; findByAccount_IdOrderByIdDesc means this account's transactions with newest IDs first.

`findLockedByIdAndUser_Id` finds an account matching both its ID and its owner's ID. Spring builds the lookup from the words after `By`; `User_Id` follows the account's user reference. `Locked` is a descriptive label. The `@Lock(PESSIMISTIC_WRITE)` annotation is what makes another account-changing request wait until the current transaction finishes. Admin status changes use `findLockedById` because an admin is allowed to change any account.

The refund lookup in CardTransactionRepository uses the one remaining `@Query`. Its text refers to Java entities and fields, and `@Param` connects the method arguments to the named values. It fetches only the owned purchase's account ID first. The service can then lock that account before loading its balance; this avoids holding an earlier account object with an outdated balance.

The remaining small expressions have concrete meanings:

## 5. Every remaining application file has one job

All Java paths below start under `backend/src/main/java/com/marvens/capstone/`.

That is 21 application classes and five DTOs. The response classes prevent password hashes and entity relationships from reaching the browser. Response constructors run in the service transaction, so related data can be read there. Entities follow their foreign keys; there are no reverse collections.

This is a reasonable stopping point for structural cuts. The controllers, services, and repositories make the workflow visible. The five DTOs each have the specific input/output job listed above. Merging these files or replacing typed responses with loose maps would reduce the file count without making the workflow easier to read. No custom configuration class is needed today; application.properties holds the runtime settings.

The financial checks also have concrete reasons to stay:

| Keep | What it demonstrates |
| --- | --- |
| BigDecimal and available-credit calculation | A purchase increases the amount owed; a refund decreases it, using decimal arithmetic. |
| Approval/decline rules and saved history | The server applies account rules and records the outcome. |
| Role and ownership checks | A customer changes only their own account; admins have defined permissions. |
| Request ID and original-purchase link | A retry cannot charge twice, and a purchase cannot be refunded twice. |
| Transaction and account lock | Balance/history succeed together, and simultaneous purchases cannot spend the same credit. |

For a short presentation, follow one purchase first, then show a decline, retry, refund, and admin freeze. These are concrete examples of the same small Java workflow. The next development work is finishing sign-in and the React screens, keeping their forms and API calls direct.

## 6. Explain the other workflows in one sentence each

- **Account:** check the customer role, find their account, return the limit/balance/available credit.
- **Card:** check account ownership, find its card, return masked details.
- **History:** check account ownership, read its transactions newest first, return an array.
- **Refund:** check the owned approved purchase has no refund, lock the account, subtract the original amount, save a linked refund. A frozen account may receive it.
- **Admin:** check ADMIN role, list safe summaries, or lock an account and save ACTIVE/FROZEN.

Every successful operation returns HTTP 200. APPROVED/DECLINED describes the financial result. Errors have one message; HTTP 400/401/403/404/409/500 says what kind of failure happened. There is no separate error-code catalog or timestamp wrapper to learn.

## 7. What is finished and what is still planned

The backend business workflows above are implemented. The current frontend is only HomePage. Registration/sign-in and the customer/admin pages still need implementation.

## 8. Tests and practice

There are three test files under `backend/src/test/java/com/marvens/capstone/`: TestData creates fictional fixtures; SimulatorApiTest checks HTTP/service behavior with mocked repositories; SimulatorIT checks real MySQL persistence, ownership, retries, refunds, concurrent purchases, and rollback. Mocked-repository checks cannot prove database transactions work; MySQL checks provide that evidence. Test fixtures are cleaned up after each integration test.

From backend, run `mvnw.cmd verify`. Add `-Pmysql-verification "-Dspring.profiles.active=local"` to include MySQL when credentials are in the ignored local profile. Frontend verification is `npm run build`.

Verified October 8 after this simplification: 16 HTTP/service checks and eight MySQL integration checks passed, with no failures/errors/skips. The frontend production build passed too. The old multi-layer test suite was consolidated around the retained business workflows; this count is not a coverage percentage.

Practice in this order: explain the $50 example without code; trace the controller/service/repository calls; change one amount and predict the result; run the matching test. Then repeat for a decline and a refund. Stay on one workflow until you can say what each line changes and why.

## Scope correction - October 8, 2026

The instructor waived AWS and related deployment/DevOps work. Jira and branch protection are outside this completion pass. JWT authentication, BCrypt, validation, pagination, OpenAPI, authentication rate limiting, coverage, Postman, and SonarQube remain required. Java coverage must meet 70%; the Excellent target is 80%+. The 3D card remains planned after the required application works. Presentation rehearsal is October 12; presentation and submission are October 13.

The older session-only implementation is being replaced by one signed JWT approach with tokens in React memory. Required work and evidence are tracked in [the completion checklist](../03_Verification/01_Completion_Checklist.md).
