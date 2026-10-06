# Credit Card Transaction Simulator

A full-stack capstone project that simulates credit card purchases with fictional users, cards, and balances. Customers will be able to submit a purchase, see an approval or decline, review transaction history, and request a full refund. Administrators will be able to review activity and freeze or reactivate accounts.

The project is independent and does not connect to a bank, payment network, or real money. It must be used with the documented fictional test cards only.

## Planned application

- **Frontend:** React with Vite, React Router, and a flippable 3D card on the purchase page.
- **Backend:** Java, Spring Boot, Spring Security, and Spring Data JPA.
- **Database:** MySQL with users, credit accounts, demo cards, and transaction history.
- **Security:** BCrypt password hashes, signed JWTs, role and account-ownership checks, and no stored full card numbers or security codes.

The code will follow a direct path: React page → API request → controller → service → repository → MySQL. The backend will make the purchase and refund decisions.

## Project status

The project proposal and four architecture documents are complete. Section 2.1 provides the MySQL schema and fictional seed data. Section 2.2 adds the minimal Spring Boot backend and four JPA entities. Section 2.3 adds repositories with ownership lookups, request ID and refund lookups, and paginated customer history and admin lists. See the [SQL setup](sql/README.md) and [backend setup and verification](backend/README.md). API endpoints, transaction services, authentication, and the React application are later sections.

## Repository layout

| Path | Contents |
| --- | --- |
| [`outputs/01_Project_Proposal/`](outputs/01_Project_Proposal/) | Submission proposal and retained drafts. |
| [`outputs/02_Architecture/`](outputs/02_Architecture/) | System architecture, ERD, API design, and React component diagram. |
| `frontend/` | React application, to be added. |
| [`backend/`](backend/) | Spring Boot setup, JPA entities, repositories, and focused verification checks. |
| [`sql/`](sql/) | MySQL schema and fictional seed data. |
| `work/` | Local scratch files; ignored by Git. |

Start with the [document index](outputs/README.md) or the [submission proposal](outputs/01_Project_Proposal/03_Submission_Proposal.md).

## Demonstration

The application will run locally with fictional test credentials and balances. AWS deployment and a Jira board are outside this capstone's approved scope.
