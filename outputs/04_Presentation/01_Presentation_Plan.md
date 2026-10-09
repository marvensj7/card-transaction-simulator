# Credit Circuit presentation plan

## Timing and order

My target is 9 minutes and 15 seconds, with 45 seconds available for pauses or a slow page load. The presentation covers the problem and solution, architecture, live demo, technical deep dive, and lessons learned from section 6.2. Questions follow the closing if the class format allows separate time.

| Time | Section | What I show |
| --- | --- | --- |
| 0:00–0:30 | Introduction | Credit Circuit title and project purpose. |
| 0:30–1:30 | Problem and solution | Customer problem, simulator purpose, customer/admin roles. |
| 1:30–3:00 | Architecture overview | Application flow and four-table relationship diagram. |
| 3:00–5:45 | Live demo | Sign-in, approved purchase, decline, history/refund, admin freeze. |
| 5:45–7:45 | Technical deep dive | Purchase service, duplicate protection, atomic writes, security and test evidence. |
| 7:45–8:45 | Lessons learned | Keeping the workflow understandable, testing failures, and using AI critically. |
| 8:45–9:15 | Closing | What the finished simulator demonstrates. |

## 1. Introduction

### Title slide

**Credit Circuit**

Credit card transaction simulator

React · Spring Boot · MySQL

### Opening narration

“I'm presenting Credit Circuit, a credit card transaction simulator I built with React, Spring Boot, and MySQL. Customers can make fictional purchases and manage their credit, while administrators can review activity and control account status. I'll show the application, walk through the purchase service, and explain how I tested the decisions behind it.”

## 2. Problem and solution

### Slide: Keeping a purchase correct

**The problem**

- Purchases can exceed available credit.
- Repeated submissions can create duplicate charges.
- Account balances and history need to agree.

**My solution**

Credit Circuit checks ownership and credit, protects against duplicate submissions, and records purchase and refund outcomes.

### One-minute narration

“The problem I focused on is what happens when someone submits a purchase. The account might not have enough available credit, the same request might be sent twice, or a balance update could fail before the transaction is recorded. The application also needs to make sure the customer is using their own account.

I built Credit Circuit to handle those situations using fictional cards and balances. Customers can sign in, check their available credit, submit a purchase, and see whether it was approved or declined. They can also review their history and request a full refund. Administrators can review activity and freeze accounts.

My goal was to build a complete workflow where the purchase decision, account balance, and saved history stay consistent.”

### Transition to architecture

“Here's how I organized the application to support that workflow.”

## 3. Architecture overview

The main flow is:

```text
React page → API call → Spring controller → service → JPA repository → MySQL
```

React handles the form and loading/error feedback. The controller receives and validates the request. The service makes purchase decisions. Repositories load and save the records.

My four tables are users, credit accounts, fictional cards, and transactions. The account belongs to a user. The card belongs to the account. Each transaction references its account and card. A refund also references its original purchase.

React matches the interface work from class. Spring Boot provides the API, Spring Security protects access, and JPA maps the Java relationships to MySQL foreign keys. `BigDecimal` and database decimal columns handle the monetary amounts.

Visuals: the [system architecture](../02_Architecture/01_System_Architecture.md) and a simplified view of the [ERD](../02_Architecture/02_Entity_Relationship_Diagram.md).

## 4. Live demo

| Demo time | Action | What it proves |
| --- | --- | --- |
| 0:00–0:25 | Sign in with a prepared fictional customer and show the dashboard. | Protected access, masked card, available credit. |
| 0:25–1:10 | Submit a $50 purchase using the assigned fictional card. | Approval and an updated balance. |
| 1:10–1:35 | Submit an amount above the displayed available credit. | Saved decline with no additional balance change. |
| 1:35–2:10 | Open history and refund the approved $50 purchase. | Recorded outcomes and a full reversal of that purchase. |
| 2:10–2:45 | Switch to a separately prepared admin session and freeze the account. | Admin account controls and role separation. |

The customer account must be active, its card unexpired, and its available credit above $50 before the presentation. The card input follows its current entry hint. The starting balance determines the amounts shown; no specific balance is assumed unless I prepare it first.

Registration, reactivation, pagination, and detailed error cases can be shown during questions. Duplicate retries are explained with the service and test evidence rather than simulated by double-clicking a disabled button.

The implemented 3D card can appear briefly in the dashboard segment of the demo video. It needs no dedicated slide or narration; explain its React state and CSS flip only if asked.

## 5. Technical deep dive

### Purchase service — 60 seconds

“The purchase logic lives in TransactionService. It checks the stored customer role, loads and locks the owned account, and checks the assigned card. It then checks whether the request ID has already been processed. For a new purchase, it checks card expiry, account status, and available credit. An approval increases the outstanding balance. Both approvals and declines are saved in history.”

The relationship mapping appears in two lines:

```java
transaction.setAccount(customerAccount);
transaction.setCard(assignedCard);
```

JPA saves those references as foreign keys. The request holds the form input; the result holds the transaction outcome and current account. The explanation follows the service's decisions without listing every DTO field.

### Duplicate requests and consistent writes — 30 seconds

“The request ID identifies one submission. A matching retry returns the existing result. A changed purchase under the same ID is rejected. The account lock prevents concurrent purchases from spending the same available credit. The database transaction makes the balance change and history save commit together or roll back together.”

### Security and verification — 30 seconds

Passwords use BCrypt. Signed JWTs protect API access and stay in React memory. The server checks roles and account ownership. Full fictional card numbers and security codes are not stored or logged.

The October 9 evidence records 47 Java tests, including 13 real MySQL integration tests, and 95.56% Java line coverage. The Postman run passed 56 requests and 83 assertions. The final SonarQube analysis passed its quality gate. These results are dated evidence; any later application changes need relevant checks before I present them as verified.

Visuals: a small purchase-service excerpt and one evidence slide. Sources: [service walkthrough](../02_Architecture/05_Backend_Walkthrough.md), [verification checklist](../03_Verification/01_Completion_Checklist.md), and [quality report](../03_Verification/02_Quality_Report.md).

## 6. Lessons learned

My main lesson was that working code also needs to be code I can understand and explain. I simplified duplicate account/card response classes and kept the familiar controller/service/repository flow.

I learned to test the failure paths: declined purchases, repeated submissions, unauthorized access, and failed history writes. A successful purchase alone does not prove that the balance stays correct.

AI helped with implementation and review, but I had to question unnecessary complexity, check the requirements, and trace the generated code myself. The final explanation is built around what each part actually does.

## 7. Closing

“Credit Circuit demonstrates the full path from a customer submitting a purchase to a saved transaction and updated credit balance. The main focus is making that workflow correct, protected, and understandable. I can trace the same purchase through the interface, service, and database.”

## Slides and rehearsal

The planned slides are title, problem/solution, application architecture, database relationships, purchase logic, security/test evidence, and lessons/closing. The live application appears between the architecture and purchase-logic slides. Speaker notes hold the explanation; the slides hold short points and readable visuals.

For the October 12 trial run, I will time the full presentation with the application open, including account switching. I will have customer and admin sessions ready, verify the backend/database are running, and keep screenshots of the demo outcomes available if a local service fails. Credentials, tokens, and configuration secrets stay out of slides and screenshots. The presentation is October 13.

If the rehearsal exceeds 9:15, I will shorten architecture narration and optional demo actions before cutting the purchase explanation or verification evidence.
