# Credit Circuit

My UCI 2123 capstone simulates credit card purchases with fictional identities, cards, and balances. React calls a Spring Boot API backed by four MySQL tables.

```text
React page → fetch call → Spring controller → service → JPA repository → MySQL
```

Customers register, sign in, view credit and a masked card, make purchases, review paged history, and request one full refund. Administrators review paged account/activity lists and freeze or reactivate accounts. A saved decline is a financial outcome. Repeating an identical request ID returns the saved result without another balance change.

Spring Security verifies signed JWTs and BCrypt checks passwords. Access tokens stay in React memory, expire after 15 minutes, and disappear on sign-out or reload. Services check the stored role and account ownership. The API accepts no authentication cookie. Stateless logout cannot revoke a copied token before expiration.

## Run locally

Follow the [backend setup](backend/README.md), [SQL setup](sql/README.md), and [frontend setup](frontend/README.md). Do not commit credentials, signing keys, demo passwords, or issued tokens.

From frontend, use `npm ci` and `npm run dev`. From backend, use `mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"` after creating ignored local configuration. The browser runs at [localhost](http://127.0.0.1:5173). Vite forwards /api to port 8080.

## Evidence and documents

- [Verified results and rubric checklist](outputs/03_Verification/01_Completion_Checklist.md)
- [Submission proposal](outputs/01_Project_Proposal/03_Submission_Proposal.md)
- [Architecture, ERD, API, React diagram, and walkthrough](outputs/README.md)
- [Postman collection](outputs/03_Verification/Credit_Circuit.postman_collection.json)

AWS and related deployment/DevOps requirements are waived by my instructor. Jira and branch protection are outside this pass. JWT, validation, pagination, OpenAPI, coverage, Postman, and SonarQube remain required. The planned 3D card follows the required application work.
