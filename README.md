# Credit Circuit

Marvens Jean | UCI 2123

My capstone simulates credit card purchases using fictional identities, cards, and balances. Customers can register, sign in, view their credit, make purchases, review history, and request a full refund. Administrators can review accounts and freeze or reactivate them.

I wanted to understand what happens behind a purchase, especially when a customer retries after losing the confirmation. Credit Circuit returns the saved result for a matching request ID. Approved purchases increase the outstanding balance, declines leave it unchanged, and refunds link back to the original purchase.

```text
React page → API call → Spring controller → service → JPA repository → MySQL
```

Spring Security verifies signed JWTs, and BCrypt checks passwords. Tokens stay in React memory and clear on sign-out, reload, or expiration. The server checks the stored role and account ownership. A database transaction saves the balance and history together.

## Run locally

Start with the [SQL setup](sql/README.md), then follow the [backend instructions](backend/README.md) and [frontend instructions](frontend/README.md). The frontend runs at [http://127.0.0.1:5173](http://127.0.0.1:5173), with API requests forwarded to port 8080. Keep local credentials and signing keys in ignored configuration.

## Submission files

- [Project proposal](outputs/01_Project_Proposal/03_Submission_Proposal.md)
- [Architecture, ERD, API design, and React diagram](outputs/README.md)
- [Rubric checklist](outputs/03_Verification/01_Completion_Checklist.md) and [quality report](outputs/03_Verification/02_Quality_Report.md)
- [Postman collection](outputs/03_Verification/Credit_Circuit.postman_collection.json) and [OpenAPI export](outputs/03_Verification/Credit_Circuit.openapi.json)
- [Presentation slides](outputs/04_Presentation/03_Credit_Circuit_Presentation.pptx), [slide PDF](outputs/04_Presentation/03_Credit_Circuit_Presentation.pdf), and [speaker notes](outputs/04_Presentation/02_Speaker_Notes.md)

The local demo video is `outputs/04_Presentation/04_Credit_Circuit_Demo.mp4`. Play it after slide 10, then return to slide 11 for the closing. The video is included in the local submission package and kept out of the public repository.

My instructor waived AWS infrastructure, deployment pipelines, and cloud monitoring. Jira is not required. Branch protection is outside this cleanup under my project instructions. The application runs locally. The flippable card is implemented with keyboard controls, reduced motion, and a graphics fallback.
