# Credit Circuit verification checklist

I verified the application locally on October 8, 2026 with Java 17, MySQL 8.4.9, and Chromium. My instructor waived AWS and related deployment/DevOps requirements. Jira and branch protection are excluded. Other written requirements remain required.

| Rubric area | Implemented behavior and evidence | Remaining limitation or action |
| --- | --- | --- |
| 1.2-1.3 Planning | Current proposals; system/sequence, ERD, API, and React diagrams in [architecture](../02_Architecture/01_System_Architecture.md). | The planned 3D card is still future work. |
| 2.1-2.4 Data/services | Four constrained tables, BCrypt seed hashes, JPA repositories/custom queries, bounded pages, constructor injection, custom exceptions, locking and atomic balance/history writes. [Java report](java-results.json). | Local fictional dataset; no production/load-test claim. |
| 2.5-2.6 API | Request/entity validation, controller @Valid, safe global errors/logging, 201 new outcome/200 identical retry, generated OpenAPI. [API contract](../02_Architecture/03_API_Design.md). | No request bodies or secret values exported as logs. |
| 3.1-3.3 React | Vite, small shared Button/Input/Card/Table/Modal components, more than five routes, JSX/JSDoc prop checks. State/effects control pages; Context/reducer handles authentication; memo/callback stabilize shared authentication values/actions. | No TypeScript migration or unused hook examples. |
| 3.4-3.5 Workflows/UI | Registration/sign-in, dashboard, purchases, paged history/full refunds, admin account/activity/status. Safe failures, disabled pending actions, spinner/skeleton, keyboard dialog/navigation/scrolling, responsive layouts. [Browser results](browser-results.json). | Chromium checks cover sampled pages, not every assistive technology/browser. |
| 4.1 Authentication | Spring Security, BCrypt, signed JWT, register/login/current-user, USER/ADMIN plus stored-role/ownership checks. [Authentication decision](../04_Decisions/01_Authentication.md). | Sign-out discards local access; a copied token remains valid until expiration. |
| 4.2 Security | External random signing key, memory-only browser access, CORS, small per-IP authentication rate limit, bearer-transport CSRF scope, validation. Tests verify invalid claims/signatures, cookie-only 401 and CORS rejection. | Local process rate limiter resets on restart. HTTP demo stays on loopback. |
| 4.3 Java tests/coverage | 36 passed, including ten real MySQL integration tests. JaCoCo: 95.74% lines, 77.33% branches; enforced 70% line gate passed. [HTML report](jacoco/index.html), [measured summary](java-results.json). | Coverage is execution evidence, not a guarantee of correctness. |
| 4.4 Postman | [Exported collection](Credit_Circuit.postman_collection.json): all 11 application endpoints, auth/roles/ownership/errors/pages/retries/purchases/refunds/CORS/rate limit. 49 requests and 72 assertions passed. [CLI output](postman-results.txt), [run summary](postman-results.json). | Blank credential/token variables; temporary fixture credentials supplied only during execution. |
| 4.5 SonarQube | Actual Community Build Java/React analysis processed successfully. Gate OK; zero bugs/vulnerabilities/open critical or major findings. [Quality report](02_Quality_Report.md), [actual results](sonar-results.json), [scanner log](sonar-scanner.txt). | Nine minor naming findings remain; two justified false-positive reviews are disclosed. |
| 6.1 Documentation | Updated READMEs, proposals, diagrams, API, walkthrough, three ADRs, [local run/demo guide](../05_Submission/01_Local_Run_and_Demo.md). | Private configuration/demo credentials stay outside the repository. |
| 6.2 Presentation | [Editable nine-slide deck](../05_Submission/Credit_Circuit_Presentation.pptx), [PDF](../05_Submission/Credit_Circuit_Presentation.pdf), [speaker/rehearsal notes](../05_Submission/02_Presentation_Notes.md). | I must rehearse October 12 and present October 13. |
| 6.3 Peer review | [Blank worksheet](../05_Submission/04_Peer_Review_Worksheet.md) ready. | Receive two peer reviews, give feedback, and record/incorporate actual findings. Not completed. |
| 6.4 Submission | Code/evidence/documents committed and pushed; [self-assessment](../05_Submission/03_Self_Assessment_and_Next_Steps.md) prepared. | I must supply private demo access, verify final links, and submit/confirm Canvas receipt October 13. Not completed. |

## How the browser evidence was obtained

Eleven workflow groups used the real Spring Boot API and MySQL with temporary fictional fixtures that were removed afterward. Purchases, saved declines, refunds, account status changes, ownership/role failures, and pagination reached the real application. For uncertain retries, the runner let the backend save before discarding the response, then verified the same UUID and one saved result.

The runner explicitly injected a 401 expiration response, a 503 dashboard failure, and empty account/card/admin list responses. Empty customer history was real. The React state tests separately exercised its expiration timer; Java tests verified genuinely expired JWTs. These are distinct checks, not a claim that the browser waited 15 minutes.

Widths checked were 1440, 768, 390, and 320 pixels. Keyboard checks covered the skip link, route/menu focus, confirmation Cancel/Escape, and horizontal table scrolling. Sampled pages had zero axe WCAG 2 A/AA and 2.1 violations. Screenshots: [home](home-desktop.png), [dashboard](dashboard-desktop.png), [phone purchase](purchase-phone.png), [tablet history](history-tablet.png), [admin](admin-desktop.png).

## Other checks and boundaries

The final frontend pass passed prop checking, 35 API tests, nine route tests, seven state tests, the private admin-setup MySQL test, and the production build. npm audit reported zero vulnerabilities. [Frontend summary](frontend-checks.json). Browser-derived V8 coverage is recorded separately in [frontend-coverage.json](frontend-coverage.json) and [LCOV](lcov.info); it does not replace the Java gate.

No external service or instructor login blocked the locally achievable application/quality work. Peer feedback, personal rehearsal, private demo access, and final submission are human actions still outstanding. The 3D card stays on the remaining-work plan. I do not claim the entire capstone or its human-dependent sections are complete.
