# Credit Circuit speaker notes

Target: 9:15, including a 2:45 live demo. These notes follow the eleven-slide Canva presentation.

| Slide | Time | Running total |
| --- | --- | --- |
| 1. Credit Circuit | 0:20 | 0:20 |
| 2. About Me | 0:20 | 0:40 |
| 3. Problem and Solution | 0:45 | 1:25 |
| 4. The Stack | 0:45 | 2:10 |
| 5. The Purchase Service | 1:00 | 3:10 |
| 6. The Relationships | 0:45 | 3:55 |
| 7. The Request and Result | 0:30 | 4:25 |
| 8. When Something Goes Wrong | 0:35 | 5:00 |
| 9. Security and Testing | 0:45 | 5:45 |
| 10. Live Demo | 2:45 | 8:30 |
| 11. Lessons Learned | 0:45 | 9:15 |

## 1. Credit Circuit

“This is Credit Circuit, my credit card transaction simulator. I built it with React, Spring Boot, and MySQL. It uses fictional cards and balances. I'll explain how a purchase works, show the relationships behind it, and then demonstrate the application.”

## 2. About Me

“I've spent the last fifteen weeks learning full-stack development. I was selected for an Accenture apprenticeship supporting a Capital One project, which influenced my decision to build something related to credit card transactions. I wanted a project I could connect to that work and explain myself.”

## 3. Problem and Solution

“The problem I focused on is keeping a purchase correct. The customer might not have enough available credit. A connection problem might cause the same purchase to be submitted again. A balance change could also fail before history is saved.

Credit Circuit checks that the customer owns the account, decides whether the purchase can be approved, and keeps the account balance consistent with the saved transaction. Customers can make purchases, review history, and request a full refund. Administrators can review activity and freeze accounts. It is a classroom simulator with no real money or payment processor.”

## 4. The Stack

“The application follows the same basic flow as our class banking app. React handles the form and displays the result. An API call reaches the Spring controller, which validates the input and passes it to the service. The service makes the purchase decisions. JPA repositories load and save records in MySQL.

Spring Security checks the signed token before a protected request reaches the controller. I kept one backend and four tables. This gives each part a clear job and lets me trace a purchase through the application.”

## 5. The Purchase Service

“Starting inside the backend, my purchase logic lives in TransactionService. It checks the customer's stored role and loads the account using both account ID and customer ID. The account is locked while its balance is being changed.

Next, it loads the card using both card ID and account ID, then checks the assigned fictional number and expiry. It looks for an existing request ID before processing a new purchase.

For a new purchase, the service checks card expiry, frozen status, and available credit, in that order. An approval adds the purchase amount to the outstanding balance using BigDecimal. A decline leaves that balance unchanged.

Both outcomes are saved in transaction history. The two lines shown link that history row to the account and card that were already checked.”

## 6. The Relationships

“The four tables are users, credit accounts, fictional cards, and transactions. A customer owns one account, and that account has one assigned card. An account and a card can each appear in many transactions. Administrators do not have customer accounts.

In Java, the transaction holds references to its account and card. JPA saves those references as foreign keys, rather than copying the full objects into the transaction row. A refund has one more relationship: it points to the original purchase being reversed. History keeps the balance at the time of each outcome.”

## 7. The Request and Result

“The purchase request carries the form fields into the service. The result carries the transaction outcome and current account back to React. Those are one submitted form and one returned response.

The controller gets the customer ID from the verified token. It returns 201 for a newly saved outcome, including a decline, or 200 for an identical retry. React reads the transaction status to display approval or decline and uses the returned account to refresh the credit display.”

## 8. When Something Goes Wrong

“A financial decline is saved history, so I can see why the purchase was declined. The balance does not change. Invalid card details are rejected before that decision.

An identical retry returns the existing purchase. Reusing its request ID for different details returns a conflict. The account lock prevents two purchases from spending the same available credit at once. The database transaction makes the balance change and history save commit together. If a write fails, they roll back together.”

## 9. Security and Testing

“Passwords are hashed with BCrypt. Signed JWTs stay in React memory, and the server checks roles and account ownership. Full fictional card numbers and security codes are not stored or logged.

The recorded Java verification passed 47 tests, including 13 real MySQL integration tests, with 95.56 percent Java line coverage. The Postman run passed 56 requests and 83 assertions. The October 9 SonarQube scan passed its quality gate.

The tests include declines, retries, concurrent spending, and rollback. I wanted evidence that the failure paths preserve the balance too.”

## 10. Live Demo

### Sign in and dashboard — 25 seconds

“Here is the customer's available credit and assigned fictional card. The card can flip, and the purchase form still works without the graphics.”

### Approved purchase — 45 seconds

“I'll submit a $50 purchase. The receipt shows the approval, and the outstanding balance increases while available credit decreases.”

### Declined purchase — 25 seconds

“Now I'll enter more than the displayed available credit. The purchase is declined, and the balance stays unchanged.”

### History and full refund — 35 seconds

“Both outcomes are recorded here. I'll refund the approved purchase in full. That creates a linked refund and reverses the $50 balance change.”

### Admin freeze — 35 seconds

“In my separately prepared administrator session, I can review the account and freeze it. The purchase service rejects new spending on a frozen account, while an eligible refund is still allowed.”

## 11. Lessons Learned

“My biggest lesson was that code needs to be understandable as well as functional. I simplified parts of the application until I could follow and explain the workflow. I also learned to test failures and retries, instead of only testing a successful purchase.

AI helped with implementation and review, but I had to question unnecessary complexity, check the requirements, and trace the code myself.

Credit Circuit demonstrates a purchase from form submission to saved history and an updated balance. Thank you.”

## Rehearsal setup

I will rehearse with an active customer account, an unexpired assigned card, and more than $50 available credit. Customer and admin sessions will be ready before the timed run. I will use the current card entry hint and keep credentials, revealed card details, and tokens out of exported slides and screenshots.

Test numbers describe their recorded verification runs. Java, Postman, and SonarQube evidence is dated October 9; later frontend changes have separate October 10 browser evidence. A timed rehearsal establishes the actual speaking duration.
