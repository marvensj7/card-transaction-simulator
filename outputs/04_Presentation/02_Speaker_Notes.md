# Credit Circuit speaker notes

Marvens Jean · UCI 2123

Timing guide: **9:42**, including the **2:57 recorded demo**. These are planned speaking times. A full rehearsal confirms whether I finish within the rubric's ten-minute limit.

The main talk explains what each technology does and follows one customer's purchase. Code details are available in the question section at the end. The slide timings are budgets; an earlier finish is fine.

The local PowerPoint has eleven slides. Play the separate demo video after slide 10, then return to slide 11 for the closing.

## Timing

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
| 10. Demo introduction | 0:15 | 6:00 |
| Recorded demo after slide 10 | 2:57 | 8:57 |
| 11. Lessons Learned and Closing | 0:45 | 9:42 |

## 1. Credit Circuit

**Cue:** Name → project → purchase, balance, refund.

### Script

I'm Marvens Jean, and this is Credit Circuit, my UCI 2123 capstone. It's a credit card transaction simulator built with React, Spring Boot, and MySQL. I'll walk through a purchase, what happens to the account balance, and how a refund brings it back.

## 2. About Me

**Cue:** Retail background → curiosity about everyday processes → finance.

### Script

Most of my background is in retail. What drew me to development was wanting to understand the systems behind everyday things we usually take for granted. Finance interests me, so exploring what happens behind a credit card purchase felt like a good place to start.

## 3. Problem and Solution

**Cue:** $50 → connection drops → retry → one outcome and a matching balance.

### Script

That brings me to the problem I wanted to solve.

Take a $50 purchase: the page hangs, and you never get a confirmation. You try again. Now you're wondering whether you've been charged twice.

That's the problem I built around. A customer needs a clear answer, and their balance needs to match what actually happened.

Credit Circuit recognizes repeated requests, records approvals and declines, and links a full refund to its original purchase. Customers can follow their history, and administrators can control account status. Everything uses fictional cards and balances.

## 4. The Stack

**Cue:** React = screens → Spring Boot = decisions → MySQL = saved records.

### Script

Here's the path a purchase takes through the application.

I used three main technologies. React builds the screens the customer interacts with. Spring Boot runs behind those screens and handles the rules for purchases and refunds. MySQL keeps the account information and transaction history.

When someone submits a purchase, the screen sends the information, the backend checks it, and the database saves the outcome. The result comes back to the customer as a receipt and an updated balance.

Each part has a clear job, which helps me follow a purchase through the application.

## 5. The Purchase Service

**Cue:** Right account and card → allowed purchase? → correct balance and history.

### Script

The purchase decisions happen in the service. This is the part of the backend that decides whether a purchase can go through.

It checks that the customer is using their own account and assigned card. It also checks whether the card has expired, whether the account is frozen, and whether enough credit is available.

For an approved purchase, the outstanding balance goes up by that amount. A decline leaves that balance unchanged. Both outcomes are recorded so the customer can see what happened.

The two lines shown here connect that purchase record to the account and card it belongs to.

## 6. The Relationships

**Cue:** Customer → account → card → purchase → linked refund.

### Script

Every purchase needs a record I can trace back to the right account.

The database has four connected groups of records: users, accounts, fictional cards, and transactions. A customer has an account, the account has a card, and purchases are connected to both.

A refund stays linked to the purchase it reverses. The original purchase remains in history, alongside the refund and the balance after each outcome.

That gives me a clear trail of where a balance change came from.

## 7. The Request and Result

**Cue:** Customer submits details → sees outcome, reason, and updated credit.

### Script

After that decision, the customer needs a clear answer.

The purchase form sends the card details, merchant, and amount to the backend. The result tells the screen whether the purchase was approved or declined and provides the current account balance.

The customer sees a receipt, a reason if the purchase was declined, and updated available credit. They can see the outcome without having to work it out themselves.

## 8. When Something Goes Wrong

**Cue:** Retry = same purchase → simultaneous purchases wait → balance and history stay together.

### Script

That answer needs to stay reliable when something goes wrong.

If the same purchase is retried, the app returns the original result. The retry doesn't add another purchase to the balance.

If two purchases arrive together, the app handles changes to that account one at a time so both can't spend the same remaining credit.

The balance change and purchase history are also saved together. If saving fails, neither change sticks. I did that to prevent a changed balance with no purchase recorded to explain it.

## 9. Security and Testing

**Cue:** Protected access → own account only → successful and failed purchases tested.

### Script

I tested those situations and checked who can access each account.

Spring Security protects account access. BCrypt protects stored passwords, and signed JWTs let the server verify who is signed in. Customers can only use their own accounts; administrators have separate controls.

The tests cover approvals, declines, retries, purchases arriving together, and failed saves. The recorded Java run passed 47 tests, exercising over 95 percent of the code's lines. The October 9 Postman API checks and SonarQube code quality review also passed.

## 10. Demo introduction

**Cue:** Recorded application → five actions → play video.

### Script

Here's a recording of that workflow running locally. I'll walk through the dashboard, an approved purchase, a decline, a full refund, and an administrator freezing the account.

## Demo narration

**Cue:** Follow the visible action. Leave room to read each result.

The sections below follow the video in order. The finished recording is 2:57 and silent. I will follow the visible action rather than treat these sections as fixed speaking intervals.

### Dashboard and card

I'll start on the customer's dashboard. This account shows its outstanding balance and available credit. The assigned fictional card is masked, and I can flip it to see the back.

### Approved purchase

I'm submitting a $50 purchase. The assigned card details were filled in before recording.

The receipt confirms the approval. The outstanding balance increases by $50, and the available credit decreases by the same amount.

### Declined purchase

This purchase is above the remaining available credit. It is declined with a reason, and the balance stays at the amount shown after the first purchase.

### History and full refund

Both outcomes are in the transaction history. I can refund the approved $50 purchase in full.

The original purchase stays in history, and a separate refund points back to it. The confirmation shows that my available credit has returned to where it started.

### Admin freeze

I've switched to a separately signed-in administrator. Here is the same customer account.

I can freeze it, and the status changes to Frozen. The service declines new purchases on a frozen account, while eligible refunds remain available.

## 11. Lessons Learned and Closing

**Cue:** Checks behind a purchase → clear history → what happens when things go wrong.

### Script

Building that workflow showed me how much happens behind the Buy button. Checking the account, making a decision, and keeping the balance and history consistent all contribute to the receipt the customer sees.

Linking refunds to their original purchases made that history easier to follow. Testing what happens when someone retries, two purchases arrive together, or a save fails also helped me check that the account stays consistent.

Credit Circuit brings that together in a complete purchase and refund workflow. Thank you. I'm happy to take questions.

## Short answers for questions

These details are available if someone asks. They aren't extra paragraphs to add to the main talk.

**How do the backend layers work?**

The controller receives and validates the request. TransactionService makes the purchase decisions. JPA repositories load and save the records in MySQL. Spring Security checks authentication before protected requests reach the controller.

**What are the request and response classes for?**

PurchaseRequest carries the submitted fields. The result response carries the saved transaction outcome and current account back to React. The controller returns 201 for a newly saved outcome, including a decline, and 200 for an identical retry.

**How are the records connected in Java?**

A transaction holds account and card references. JPA saves those relationships as foreign keys in MySQL. A refund also references its original purchase. That preserves the links without copying full account or card objects into each history row.

**What do BCrypt and JWT each do?**

BCrypt hashes passwords so I don't store the original password. A signed JWT lets the server verify authentication on later requests. The token stays in React memory, and the server still checks the customer's role and account ownership.

**How does duplicate protection work?**

One submission gets a request ID. If the same request comes back, the service returns the existing result. Changed details under that ID cause a conflict. Clicking the disabled Submit button twice isn't a demonstration of that server behavior.

**Why both a lock and a database transaction?**

The lock makes purchases on one account wait their turn. The database transaction makes that purchase's balance and history save succeed or fail together.

**Why BigDecimal?**

Money needs exact decimal arithmetic. BigDecimal avoids the binary floating-point rounding problems I could get with double.

**What does the card form validate?**

The service checks that the fictional number and expiry match the assigned card. The security code is checked for format only. This simulator doesn't connect to a payment network or verify a real CVV.

**What do the testing numbers refer to?**

The documented Java, Postman, and final SonarQube verification runs are from October 9. The Java run passed 47 tests, including 13 real MySQL integration tests, with 95.56% Java line coverage. Postman passed 56 requests and 83 assertions. The final SonarQube scan passed its quality gate. Later frontend checks have their own October 10 evidence. Coverage measures code exercised during a run; it doesn't prove the absence of bugs.

## Sources

The technical narration follows the [architecture](../02_Architecture/01_System_Architecture.md), [ERD](../02_Architecture/02_Entity_Relationship_Diagram.md), [API design](../02_Architecture/03_API_Design.md), TransactionService, and the current React workflows. Testing figures come from the [quality report](../03_Verification/02_Quality_Report.md). My personal background comes from the information I supplied for the About Me slide.
