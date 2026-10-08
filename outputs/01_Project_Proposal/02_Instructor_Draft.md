# Credit Circuit instructor description

I built a classroom credit card simulator using fictional cards/balances. Customers register, sign in, view credit, submit purchases, review paged history, and request one full refund. Administrators review account/activity pages and freeze/reactivate accounts.

The flow is React page → API call → Spring controller → service → JPA repository → MySQL. I used the class banking app as a learning reference without copying its supplied solution. Signed JWT and BCrypt protect access. Ownership, request IDs, account locks, and database transactions protect the financial workflows.

AWS and related deployment/DevOps work are waived. Jira and branch protection are outside this pass. JWT, validation, pagination, coverage, Postman, and SonarQube remain required. The [proposal](03_Submission_Proposal.md) and [evidence](../03_Verification/01_Completion_Checklist.md) describe the current scope. The flippable 3D card remains planned.
