# Credit Card Transaction Simulator

A small UCI 2123 capstone using React, Java/Spring Boot, and MySQL with fictional cards and balances.

The core workflows are account summary, masked card, purchase approval/decline, transaction history, one full refund, and admin freeze/reactivate. Request IDs stop a repeated submission from adding another purchase. The database has four tables.

```text
React page → API request → controller → service → JPA repository → MySQL
```

The backend uses ordinary Java classes, explicit if/else decisions and loops, one place for input validation, and five small DTOs. Customer/admin views share safe responses. Lists are plain arrays with no paging. Successful purchases/refunds return 200; their APPROVED/DECLINED field explains the financial outcome. Amounts use BigDecimal in Java and DECIMAL(14,2) in MySQL; response money uses JSON numbers.

Start with the [beginner walkthrough](outputs/02_Architecture/05_Backend_Walkthrough.md). It explains each remaining file, follows one $50 purchase, and gives short practice questions.

## Current status

The account, purchase, decline, history, refund, retry, and admin rules are implemented in the backend. The frontend is only a public home page. Registration, sign-in, and the customer/admin React screens are unfinished.

Authentication will use a server session and cookie with BCrypt password checks. The current session filter closes `/api` to external callers because no sign-in endpoint establishes a session yet. Tests supply a session directly on mock server requests; there is no public user-ID shortcut. JWTs and card animation are outside this MVP. AWS and a Jira board are not required.

## Run and verify

Follow the [MySQL setup](sql/README.md), then the [backend setup](backend/README.md). From `backend/`:

```powershell
.\mvnw.cmd verify
.\mvnw.cmd verify -Pmysql-verification "-Dspring.profiles.active=local"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

The local profile reads the ignored credential file. Omit the profile arguments when using backend environment variables. No credentials belong in Git, terminal commands, or logs.

From `frontend/`:

```powershell
npm install
npm run dev
npm run build
```

The home page runs at `http://127.0.0.1:5173/`. Vite forwards `/api` to Spring Boot on port 8080. See the [frontend setup](frontend/README.md).

The [document index](outputs/README.md) links the proposal, ERD, API design, and React plan. This repository is public by the owner's choice. `work/`, local credentials, dependencies, and generated builds are ignored.
