# Credit Circuit final submission checklist

Checked against the UCI 2123 rubric on October 12, 2026.

## Required files

| Requirement | File or evidence | Status |
| --- | --- | --- |
| Custom project approval | Instructor approval recorded in the project scope. | Approved by instructor, as supplied by me. |
| Proposal: problem, users, requirements, scope | [Proposal](../01_Project_Proposal/03_Submission_Proposal.md): 15 user stories, 15 functional requirements, performance/security/scale requirements. | Present |
| System architecture diagram | [Architecture and purchase sequence](../02_Architecture/01_System_Architecture.md) | Present |
| ERD with tables, fields, relationships, constraints | [Four-table ERD](../02_Architecture/02_Entity_Relationship_Diagram.md) | Present |
| API design: methods, paths, request/response fields | [API design](../02_Architecture/03_API_Design.md) and [OpenAPI](Credit_Circuit.openapi.json) | Present |
| React component diagram | [Component design](../02_Architecture/04_React_Component_Diagram.md) | Present |
| Spring Boot source and JUnit suite | backend/src/main and backend/src/test | Present |
| React source and package lock | frontend/src, package.json, package-lock.json, Vite configuration | Present |
| SQL schema and seed | sql/01_schema.sql and sql/02_seed.sql | Present |
| Setup instructions | [Backend README](../../backend/README.md), [frontend README](../../frontend/README.md), [SQL README](../../sql/README.md) | Present |
| Architecture decisions | Design choices and responsibilities in the [architecture document](../02_Architecture/01_System_Architecture.md) | Present |
| Java coverage report, minimum 70% | [Java summary](java-results.json) and [JaCoCo XML](java-coverage.xml): 95.56% lines, enforced gate passed | Rechecked October 12 |
| Exported and exercised Postman collection | [Collection](Credit_Circuit.postman_collection.json), [summary](postman-results.json), [CLI output](postman-results.txt): 56 requests, 83 assertions, zero failures | Recorded October 9 |
| Actual SonarQube report | [Results](sonar-current-results.json), [scanner log](sonar-current-scanner.txt), [dashboard](sonar-current-dashboard.jpg): quality gate passed | Recorded October 9 |
| Presentation slides and required PDF export | [PowerPoint](../04_Presentation/03_Credit_Circuit_Presentation.pptx) and [PDF](../04_Presentation/03_Credit_Circuit_Presentation.pdf): eleven slides/pages | Present and visually checked |
| Demonstration | Local outputs/04_Presentation/04_Credit_Circuit_Demo.mp4, plus the working local application | 2:57 video present; presentation playback still needs rehearsal |
| Peer review with two peers, feedback given and received | No completed feedback record found in the project folder. | Needs my input |
| Self-assessment | No completed self-assessment found in the project folder. | Needs my input |
| Test credentials for the instructor's demo | Provide existing fictional customer/admin sign-in details privately, outside Git and submitted source. | Needs my private handoff |
| All code committed and pushed to main | Public repository and final cleanup commit | Check final Git status and remote after push |
| Repository URL submitted to Canvas | https://github.com/marvensj7/card-transaction-simulator | My Canvas action |
| All documentation uploaded to Canvas | Final local submission package, including slide PDF and Postman JSON | My Canvas action |

## Implementation checks

| Rubric section | What I checked |
| --- | --- |
| 2.1 Database | Four tables, primary/foreign/unique/not-null/check constraints, history index, and rerunnable schema/seed. No schema migration is needed for this cleanup. |
| 2.2 Entities | JPA relationships, explicit lazy loading, validation annotations, and no cascade deletion of financial history. |
| 2.3 Repositories | Spring Data JPA, custom queries, page queries, and account locking. |
| 2.4 Services | Constructor injection, business rules, safe exceptions, and transactional balance/history writes. |
| 2.5–2.6 Controllers and errors | HTTP methods/statuses, @Valid, OpenAPI annotations, and a global safe exception handler. |
| 3.1–3.2 React | Vite, JSX/JSDoc prop checks, reusable controls, pages, React Router, and more than five routes. TypeScript is optional. |
| 3.3 State | useState/useEffect, authentication Context/reducer, useMemo, useCallback, and memory-only token state. |
| 3.4–3.5 API and interface | Named API functions, safe failures, pending controls, loading states, labeled forms, responsive layouts, and keyboard use. |
| 4.1–4.2 Security | Spring Security, signed JWTs, BCrypt, USER/ADMIN roles, ownership checks, external secrets, CORS, authentication rate limiting, validation, and bearer-header CSRF settings. |
| 4.3 Java tests | 47 passed, including 13 real MySQL integration tests. No failures, errors, or skips. |
| 4.4 API integration | Exercised Postman export covers all eleven application endpoints and success/failure behavior. |
| 4.5 Quality | Actual passing SonarQube analysis, zero open security/reliability/maintainability issues in that recorded scan, and meaningful source comments. |

The October 12 frontend pass passed prop checking, 37 API tests, nine route tests, seven state tests, six fictional-card rule tests, and the production build. See [frontend results](frontend-checks.json).

The saved [browser results](browser-results.json) cover thirteen real Spring Boot/MySQL workflow groups. Separate [wallet](card-wallet-results.json) and [model](card-model-results.json) reports cover the card changes. Reports identify mocked data and injected failures/events. The [quality report](02_Quality_Report.md) explains test dates and limitations. Later frontend changes postdate the recorded SonarQube scan.

## Approved exclusions

My instructor waived AWS infrastructure, SSM/cloud secrets services, deployment pipelines, AWS hosting, and cloud monitoring. Those cloud deliverables and URLs are not part of this local submission. Jira is not required. Branch protection remains outside this cleanup under my project instructions.

Peer-review records and the self-assessment need my own input. This checklist records their status and does not recreate the previously removed Phase 6 worksheets.

## Before submitting

1. Add any completed peer-review feedback and finish the self-assessment required by the rubric.
2. Give the instructor fictional demo credentials privately.
3. Upload the documentation package to Canvas and include the repository URL.
4. Confirm Canvas shows the uploaded files and a submission receipt before 5 p.m.
5. Rehearse the presentation and video playback for October 13. The local export uses the separate video after slide 10, then returns to slide 11.

This checklist verifies files and recorded results. It does not confirm a Canvas submission or guarantee a grade.
