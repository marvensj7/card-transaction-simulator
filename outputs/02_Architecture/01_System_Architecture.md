# System Architecture

**Credit Card Transaction Simulator**<br>
**October 6, 2026**

## Overview

The design has one React frontend, one Spring Boot backend, and one MySQL database. The MySQL mappings, repositories, and account and transaction services are implemented. HTTP controllers, authentication, and the React interface are planned. In the complete application, customers use the frontend to view their credit account, submit fictional card purchases, request refunds, and read history. Administrators use a separate view to review activity and freeze or reactivate accounts. The Spring Boot services make the account and transaction decisions; the browser displays the result.

The system runs locally for the capstone demonstration. It does not connect to a bank, payment network, or payment processor. All cards, balances, and transactions are fictional.

## Component diagram

```mermaid
flowchart LR
    PERSON[Customer or administrator]

    subgraph BROWSER[Browser]
        PAGES[React pages and forms]
        CARD[Flippable 3D card view]
        CLIENT[Fetch API helper and in-memory JWT]
        PAGES --> CARD
        PAGES --> CLIENT
    end

    subgraph SERVER[Spring Boot application]
        SECURITY[Spring Security<br/>JWT verification and role checks]
        CONTROLLERS[Auth, account, purchase,<br/>transaction, and admin controllers]
        AUTH[Registration and<br/>JWT issuing service]
        SERVICES[Account and<br/>transaction services]
        REPOSITORIES[Spring Data JPA repositories]
        TESTCARDS[Allowed fictional<br/>test card numbers]
        SECURITY --> CONTROLLERS
        CONTROLLERS --> AUTH
        CONTROLLERS --> SERVICES
        AUTH --> REPOSITORIES
        SERVICES --> REPOSITORIES
        SERVICES --> TESTCARDS
    end

    DATABASE[(MySQL<br/>users, accounts, cards, transactions)]
    SECRET[JWT signing key<br/>environment variable]

    PERSON --> PAGES
    CLIENT -->|REST requests and responses| SECURITY
    REPOSITORIES -->|Read and write| DATABASE
    SECRET --> SECURITY
    SECRET --> AUTH
```

The 3D card is a view of the form state. It does not make a separate API request or decide whether a transaction is valid. The regular HTML fields remain usable without the 3D view.

## What each part does

| Part | Responsibility |
| --- | --- |
| React pages and forms | Show credit information, collect fictional purchase details, display errors and transaction results, and provide customer and admin navigation. |
| Flippable 3D card | Preview a masked test card and flip between its front and back. It never displays the entered security code. |
| Fetch API helper | Send requests, attach the JWT to protected requests, and handle loading and error responses. The token is held in memory; a page refresh requires a new sign-in. |
| Spring Security | Allow public registration and login, validate JWTs on protected routes, and enforce USER or ADMIN access. |
| Controllers | Receive HTTP requests and return consistent status codes and response bodies. They do not calculate balances. |
| Registration and JWT service | Create customer accounts, use BCrypt for passwords, and issue signed tokens after successful login. |
| Account and transaction services | Check account ownership, validate purchases and refunds, apply credit rules, and coordinate database transactions. |
| JPA repositories | Read and save users, credit accounts, demo cards, and transaction records. |
| MySQL | Persist users, BCrypt password hashes, account balances and states, masked card references, and transaction history. Constraints protect relationships and unique request IDs. |

## Main request flows

### Sign-in

1. The customer submits an email and password to the public login endpoint.
2. Spring Security verifies the password against its BCrypt hash in MySQL.
3. The backend signs a short-lived JWT with HS256 and returns it to React. The signing key is generated securely, has at least 256 bits, and is kept outside source control.
4. React includes the JWT in protected API requests. The backend checks its signature and expiry before a controller handles the request.

### Purchase

1. The customer enters a documented fictional card number, expiration, test security code, merchant, and amount. React checks the fields and shows immediate feedback.
2. The purchase request includes the account ID, demo card ID, purchase details, and a request ID generated once for that submission.
3. Planned Spring Security checks that the user is signed in. The transaction service checks the user's stored customer role.
4. The service locks the owned credit account row before reading the saved request result or changing the balance. It validates the assigned test card, checks its expiry in UTC, then checks account status and available credit. Purchases and refunds use read-committed isolation so waiting requests see committed history.
5. An approved purchase increases the outstanding balance and inserts an APPROVED transaction in one database transaction. A business-rule decline inserts a DECLINED transaction without changing the balance. Invalid input returns an error without recording a purchase.
6. The API returns the transaction outcome and updated credit summary. React shows the result and refreshes the history.

### Retry and refund

The planned frontend reuses the same request ID when retrying an uncertain purchase. MySQL enforces a unique account/request-ID pair. The service compares the card ID, exact merchant name, and numeric amount within that account. An identical retry returns the saved transaction; different details conflict. The transaction keeps its original outcome and balance, while the account summary reflects the current account. This prevents a double click or network retry from creating a second purchase.

A full refund points to one approved purchase. The service checks ownership and whether that purchase has already been refunded, then decreases the outstanding balance and saves the refund record together. A frozen account cannot make new purchases; freezing does not erase history or block a valid refund.

### Administration

The services check the user's stored role before returning admin lists or changing an account between ACTIVE and FROZEN. A status change takes the same account write lock as a purchase or refund. Planned admin responses contain the details needed for oversight without full card numbers or security codes. Admin access does not grant permission to submit purchases as a customer.

## Data and trust boundaries

- The browser is treated as untrusted. Every field, account ID, card ID, role, and request ID is checked on the server.
- The database stores a demo card ID, label, last four digits, and expiry. It does not store a full card number or security code. The test security code is checked for format and discarded.
- Money uses `BigDecimal` in Java and `DECIMAL(14,2)` in MySQL. Available credit is the credit limit minus the outstanding balance.
- The JWT signing key and database credentials come from local environment configuration and are not committed to Git.
- The services do not log purchase input. SQL and bind-value logging are disabled. Passwords, JWTs, full card numbers, security codes, and complete request bodies are excluded from logs.

## Local runtime and build checks

The planned React development server sends `/api` requests to the Spring Boot backend, which connects to local MySQL. Maven checks validate the existing schema, repository lookups, service rules, concurrent purchases and refunds, and rollback. Hibernate uses `validate` and SQL initialization is disabled. A basic GitHub Actions workflow is planned to build React and run Maven tests. The local demonstration uses seeded fictional users and card accounts. Cloud infrastructure and a Jira board are outside this project, as agreed with the instructor.
