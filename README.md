# Credit Circuit

Credit Card Transaction Simulator — a small UCI 2123 capstone using React, Java/Spring Boot, and MySQL with fictional cards and balances.

The core workflows are account summary, masked card, purchase approval/decline, transaction history, one full refund, and admin freeze/reactivate. Request IDs stop a repeated submission from adding another purchase. The database has four tables.

```text
React page → API request → controller → service → JPA repository → MySQL
```

Start with the [beginner walkthrough](outputs/02_Architecture/05_Backend_Walkthrough.md). It explains each remaining file, follows one $50 purchase, and gives short practice questions.

## Current status

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
npm run check:props
npm run check:routes
```

The frontend runs at `http://127.0.0.1:5173/`. All current routes work without the backend. Vite forwards future `/api` calls to Spring Boot on port 8080. See the [frontend setup](frontend/README.md).

The [document index](outputs/README.md) links the proposal, ERD, API design, and React plan. This repository is public by the owner's choice. `work/`, local credentials, dependencies, and generated builds are ignored.

## Scope correction - October 8, 2026

The instructor waived AWS and related deployment/DevOps work. Jira and branch protection are outside this completion pass. JWT authentication, BCrypt, validation, pagination, OpenAPI, authentication rate limiting, coverage, Postman, and SonarQube remain required. Java coverage must meet 70%; the Excellent target is 80%+. The 3D card remains planned after the required application works. Presentation rehearsal is October 12; presentation and submission are October 13.

The older session-only implementation is being replaced by one signed JWT approach with tokens in React memory. Required work and evidence are tracked in [the completion checklist](outputs/03_Verification/01_Completion_Checklist.md).
