# Card Transaction Simulator documents

## 01 — Project proposal

- [Submission proposal](01_Project_Proposal/03_Submission_Proposal.md) — the version prepared for submission.
- [Instructor draft](01_Project_Proposal/02_Instructor_Draft.md) — earlier short pitch, retained for reference.
- [Working proposal](01_Project_Proposal/01_Working_Proposal.md) — detailed design draft, retained for reference.

## 02 — Architecture

- [System architecture](02_Architecture/01_System_Architecture.md) — components, interactions, and key request flows.
- [Entity-relationship diagram](02_Architecture/02_Entity_Relationship_Diagram.md) — four tables, fields, keys, cardinality, and balance examples.
- [API design](02_Architecture/03_API_Design.md) — endpoints, request and response shapes, access rules, and error behavior.
- [React component diagram](02_Architecture/04_React_Component_Diagram.md) — routes, component hierarchy, and state ownership.
- [Backend walkthrough](02_Architecture/05_Backend_Walkthrough.md) — plain-language study guide, purchase/refund traces, file tour, and instructor questions for the implemented backend.

The current architecture and walkthrough reflect the October 8 beginner MVP simplification. Earlier proposal drafts are historical references. Card animation and optional tooling are deferred. The API design records the implemented controller and shared error contracts and identifies the authentication endpoints that are still planned.

## Scope correction - October 8, 2026

The instructor waived AWS and related deployment/DevOps work. Jira and branch protection are outside this completion pass. JWT authentication, BCrypt, validation, pagination, OpenAPI, authentication rate limiting, coverage, Postman, and SonarQube remain required. Java coverage must meet 70%; the Excellent target is 80%+. The 3D card remains planned after the required application works. Presentation rehearsal is October 12; presentation and submission are October 13.

The older session-only implementation is being replaced by one signed JWT approach with tokens in React memory. Required work and evidence are tracked in [the completion checklist](03_Verification/01_Completion_Checklist.md).
