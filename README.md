# Credit Card Transaction Simulator

A full-stack capstone project that simulates credit card purchases with fictional users, cards, and balances. Customers will be able to submit a purchase, see an approval or decline, review transaction history, and request a full refund. Administrators will be able to review activity and freeze or reactivate accounts.

The project is independent and uses only its assigned fictional test cards. It does not connect to a bank, payment network, or real money.

## Planned application

- **Frontend:** React with Vite, React Router, and a flippable 3D card on the purchase page.
- **Backend:** Java, Spring Boot, Spring Security, and Spring Data JPA.
- **Database:** MySQL with users, credit accounts, demo cards, and transaction history.
- **Security:** BCrypt password hashes, signed JWTs, role and account-ownership checks, and no stored full card numbers or security codes.

The design follows a direct path: React page → API request → controller → service → repository → MySQL. The backend services make the purchase and refund decisions.

## Current application

The backend has four JPA entities, repositories, two services, and REST controllers over the MySQL schema. Purchases record approvals or declines, full refunds link to their original purchases, and customer lookups enforce ownership. Admin operations list accounts and activity and freeze or reactivate accounts. Validated request DTOs and dedicated response DTOs keep money, UTC dates, masked cards, and pagination consistent. New purchases and refunds return `201`; saved retries return `200`. Account locks and request IDs protect balance changes.

Protected API routes require a server-established identity and currently return `401` to external callers because authentication is not implemented. IDs supplied by the browser cannot establish identity. Errors use consistent status, code, message, and UTC timestamp fields, including requests blocked by the identity filter. JWT sign-in is planned. See the [SQL setup](sql/README.md) and [backend setup and verification](backend/README.md).

The frontend uses Vite, React, and plain JavaScript/JSX. `main.jsx` renders `App.jsx`, which renders a home page identifying the fictional simulation. Routing, authentication, account data, purchase flows, and the 3D card are planned separately. See the [frontend setup](frontend/README.md).

## Repository layout

| Path | Contents |
| --- | --- |
| [`outputs/01_Project_Proposal/`](outputs/01_Project_Proposal/) | Submission proposal and retained drafts. |
| [`outputs/02_Architecture/`](outputs/02_Architecture/) | System architecture, ERD, API design, and React component diagram. |
| [`frontend/`](frontend/) | Vite and React application with a public home page. |
| [`backend/`](backend/) | Spring Boot REST API, DTOs, entities, repositories, services, and verification checks. |
| [`sql/`](sql/) | MySQL schema and fictional seed data. |
| `work/` | Local scratch files; ignored by Git. |

Start with the [document index](outputs/README.md) or the [submission proposal](outputs/01_Project_Proposal/03_Submission_Proposal.md).

## Local setup

With Node.js 20.19+ on the 20.x line, or 22.12+ on a newer line, and npm installed, run from the repository root:

```powershell
cd frontend
npm install
npm run dev
```

The home page runs at `http://127.0.0.1:5173/` without the backend. `npm run build` creates the frontend production build. Vite forwards `/api` to Spring Boot at `http://localhost:8080`; the optional `API_PROXY_TARGET` setting is described in the [frontend setup](frontend/README.md). No browser environment variables or secrets are needed.

For API work, follow the [SQL setup](sql/README.md) and start Spring Boot in a separate terminal using the [backend setup](backend/README.md). Keep datasource credentials in backend environment variables or the ignored `application-local.properties` profile.

## Demonstration

The application will run locally with fictional test credentials and balances. AWS deployment and a Jira board are outside this capstone's approved scope.
