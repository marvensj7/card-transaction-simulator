# Credit Circuit project proposal

Marvens Jean | UCI 2123

## Problem and solution

A customer submits a $50 purchase, loses the confirmation, and tries again. They need to know whether the purchase went through and whether their balance changed twice. An account also needs enough available credit, and its saved balance needs to match its transaction history.

I built Credit Circuit to explore these rules in a small application I can follow from React to MySQL. It gives customers a purchase result, records approvals and declines, recognizes matching retries, and links a full refund to its original purchase. Administrators can review activity and freeze or reactivate accounts.

The project uses fictional identities, cards, merchants, and balances. It connects to no bank or payment network and moves no real money. Its purpose is to demonstrate the account checks and record keeping behind a purchase.

## Users and stories

1. As a visitor, I can read the simulation notice before entering information.
2. As a visitor, I can register a customer account.
3. As a user, I can sign in and sign out, and receive a message when access expires.
4. As a customer, I can see my credit limit, outstanding balance, and available credit.
5. As a customer, I can view my masked fictional card and briefly reveal its test details.
6. As a customer, I can use my assigned card to fill the number and expiry in the purchase form.
7. As a customer, I can submit a purchase and correct field errors.
8. As a customer, I can see an approval or a decline with its reason.
9. As a customer, I can page through my transaction history, newest first.
10. As a customer, I can request one full refund of an approved purchase.
11. As a customer, I can retry an uncertain purchase or refund with its original request ID.
12. As a customer, I can access only my own account and transactions.
13. As an administrator, I can page through account summaries and transaction activity.
14. As an administrator, I can freeze or reactivate an account while eligible refunds remain available.
15. As a keyboard user, I can use navigation, forms, the card flip, dialogs, and scrolling tables.

## Functional requirements

| Feature | Required behavior |
| --- | --- |
| Registration | Create a USER, an active $1,000 account with zero balance, and a fictional card together. Reject duplicate email. |
| Sign-in | Check the BCrypt password hash and issue a signed JWT. |
| Current user | Return the signed-in user's safe identity fields. |
| Sign-out and expiration | Clear the token from React memory and require another sign-in. |
| Access checks | Enforce USER/ADMIN roles and account ownership on the server. |
| Account summary | Show limit, balance, calculated available credit, and status. |
| Assigned card | Mask details by default. Hide revealed details after 20 seconds or focus loss. Keep purchase prefill in memory. |
| Purchase | Check the assigned card, expiry, account status, and available credit. |
| Decline | Save expired-card, frozen-account, and insufficient-credit outcomes without changing the balance. |
| Duplicate protection | Return the saved result for an identical request ID. Reject changed details under the same ID. |
| History | Return bounded pages of purchase and refund outcomes with their relationships. |
| Full refund | Reverse the full amount of one owned approved purchase once, including on a frozen account. |
| Admin review | Return paged account and activity summaries. |
| Admin status change | Freeze spending or reactivate the selected account. |
| Validation and errors | Use Bean Validation, safe error messages, and documented HTTP statuses. |

## Nonfunctional requirements

**Correct balances:** Java uses BigDecimal and MySQL uses DECIMAL(14,2). An account lock makes balance changes wait their turn. A database transaction saves the balance and history together or rolls both back. Unique constraints support request-ID and full-refund rules.

**Security:** BCrypt hashes passwords. Spring Security verifies signed JWTs. The signing key and database credentials stay outside Git. React attaches the token through a bearer header and keeps it in memory. CORS restricts browser origins. Registration and login share a local rate limit. No password, issued token, full card number, security code, or request body is logged or saved as application data.

**Performance and scale:** Paged lists limit returned data, and indexes support common queries. The authentication limiter bounds its stored entries. This is a local classroom dataset. Loading within two seconds is a demo target, not a measured load-test result.

**Usability:** The interface supports desktop, tablet, and phone widths. Forms have labels and errors, pending actions are disabled, and loading states explain waiting. The card supports keyboard input, reduced motion, and an HTML/CSS fallback.

**Testing:** The project requires meaningful Java success/failure tests, an enforced minimum 70% Java coverage gate with an 80% goal, an exercised Postman collection, and actual SonarQube analysis. The [quality report](../03_Verification/02_Quality_Report.md) gives the measured results and their dates.

## Design

The flow is React page → API call → Spring controller → service → JPA repository → MySQL. I used the banking app from class as a learning reference and wrote the simulator's own rules. The four tables are app_users, credit_accounts, demo_cards, and card_transactions.

See the [system architecture](../02_Architecture/01_System_Architecture.md), [ERD](../02_Architecture/02_Entity_Relationship_Diagram.md), [API design](../02_Architecture/03_API_Design.md), and [React diagram](../02_Architecture/04_React_Component_Diagram.md).

## Scope

My instructor waived AWS infrastructure, deployment pipelines, and cloud monitoring. Jira is not required. Branch protection is outside this cleanup under my project instructions. The application runs locally. Real payment processing, partial refunds, refresh tokens, and production load testing are outside this project.
