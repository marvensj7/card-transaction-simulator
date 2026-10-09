# Credit Circuit verification checklist

I verified the application locally on October 9, 2026 with Java 17, MySQL 8.4.9, and Chromium. Current application evidence describes revision cd81a36 with the original local CardTransaction.java edit preserved. My instructor waived AWS and related deployment/DevOps requirements. Jira and branch protection are excluded. Other written requirements remain required.

| Rubric area | Implemented behavior and evidence | Remaining limitation or action |
| --- | --- | --- |
| 1.2-1.3 Planning | Current proposals; system/sequence, ERD, API, and React diagrams in [architecture](../02_Architecture/01_System_Architecture.md). | The planned 3D card is still future work. |
| 2.1-2.4 Data/services | Four constrained tables, BCrypt seed hashes, JPA repositories/custom queries, bounded pages, constructor injection, custom exceptions, locking and atomic balance/history writes. [Java report](java-results.json). | Local fictional dataset; no production/load-test claim. |
| 2.5-2.6 API | Request/entity validation, controller @Valid, safe global errors/logging, 201 new outcome/200 identical retry, generated OpenAPI. [API contract](../02_Architecture/03_API_Design.md). | No request bodies or secret values exported as logs. |
| 3.1-3.3 React | Vite, small shared Button/Input/Card/Table/Modal components, more than five routes, JSX/JSDoc prop checks. State/effects control pages; Context/reducer handles authentication; memo/callback stabilize shared authentication values/actions. | No TypeScript migration or unused hook examples. |
| 3.4-3.5 Workflows/UI | Registration/sign-in, dashboard, purchases, paged history/full refunds, admin account/activity/status. Safe failures, disabled pending actions, spinner/skeleton, keyboard dialog/navigation/scrolling, responsive layouts. [Browser results](browser-results.json). | Chromium checks cover sampled pages, not every assistive technology/browser. |
| 4.1 Authentication | Spring Security, BCrypt, signed JWT, register/login/current-user, USER/ADMIN plus stored-role/ownership checks. [API contract](../02_Architecture/03_API_Design.md). | Sign-out discards local access; a copied token remains valid until expiration. |
| 4.2 Security | External random signing key, memory-only browser access, CORS, small per-IP authentication rate limit, bearer-transport CSRF scope, validation. Tests verify invalid claims/signatures, cookie-only 401 and CORS rejection. | Local process rate limiter resets on restart. HTTP demo stays on loopback. |
| 4.3 Java tests/coverage | 46 passed, including 12 real MySQL integration tests. JaCoCo: 95.83% lines, 85.06% branches; enforced 70% line gate passed. [Current XML](java-coverage.xml), [measured summary](java-results.json). | Coverage is execution evidence, not a guarantee of correctness. The jacoco HTML/XML/CSV directory remains October 8 evidence. |
| 4.4 Postman | [Exported collection](Credit_Circuit.postman_collection.json): all 11 application endpoints, auth/roles/ownership/errors/pages/retries/assigned cards/purchases/refunds/CORS/rate limit. 56 requests and 83 assertions passed. [CLI output](postman-results.txt), [run summary](postman-results.json). | Blank credential/token variables; temporary fixture credentials supplied only during execution. |
| 4.5 SonarQube | Actual October 8 analysis passed its gate for revision e7c9f1. [Historical results](sonar-results.json). A fresh October 9 scan was attempted: [attempt summary](sonar-refresh-results.json), [quality report](02_Quality_Report.md). | Fresh analysis is blocked by HTTP 401 from local SonarQube. Restore its login and analysis credential, then rerun. Older findings are not current-revision results. |

## How the browser evidence was obtained

Twelve workflow groups used the real Spring Boot API and MySQL with temporary fictional fixtures that were removed afterward. Different assigned cards, full-number matching, purchases, saved declines, refunds, account status changes, ownership/role failures, and pagination reached the real application. For uncertain retries, the runner let the backend save before discarding the response, then verified the same UUID and one saved result. Sign-in, sign-out, and reload checks confirmed stable card details.

The runner explicitly injected a 401 expiration response, a 503 dashboard failure, and empty account/card/admin list responses. Empty customer history was real. The React state tests separately exercised its expiration timer; Java tests verified genuinely expired JWTs. These are distinct checks, not a claim that the browser waited 15 minutes.

Widths checked were 1440, 768, 390, and 320 pixels. Keyboard checks covered the skip link, route/menu focus, confirmation Cancel/Escape, and horizontal table scrolling. Sampled pages had zero axe WCAG 2 A/AA and 2.1 violations. Screenshots: [home](home-desktop.png), [dashboard](dashboard-desktop.png), [phone purchase](purchase-phone.png), [tablet history](history-tablet.png), [admin](admin-desktop.png).

## Other checks and boundaries

The final frontend pass passed prop checking, 35 API tests, nine route tests, seven state tests, the private admin-setup MySQL test, and the production build. npm audit reported zero vulnerabilities. [Frontend summary](frontend-checks.json). Browser-derived V8 coverage is recorded separately in [frontend-coverage.json](frontend-coverage.json) and [LCOV](lcov.info); it does not replace the Java gate.

An [actual backend restart](restart-results.json) preserved the assigned card and returned the same saved purchase on an identical retry. The [seed rerun](seed-results.json) preserved existing data and confirmed four tables with no migration. New cards use ACCOUNT_V1: 0000 followed by the account ID padded to 12 digits; only the profile and last four digits are stored. Existing DEMO_4242 cards retain their original behavior and history. The backend checks the complete expected number and owned card/expiry; security codes remain format-only.

This checklist covers application implementation and verification. The fresh SonarQube scan still needs restored local access. The flippable 3D card remains planned.
