# Quality report - October 8, 2026

I tested the actual local application with MySQL and kept sanitized reports. This report separates measured results, reviewed findings, and limits.

**October 9, 2026 follow-up:** The AuthService naming/readability change passed Maven verify with the mysql-verification profile: 27 HTTP/unit tests and ten MySQL integration tests, with no failures, errors, or skips. Fresh JaCoCo measured 470/491 Java lines (95.72%) and 120/152 branches (78.95%); the 70% line gate passed. AuthService covered all 54 lines and 12 branches.

The sections and linked exports below preserve the earlier October 8 verification. Frontend/browser checks, Postman, and SonarQube were not rerun for this behavior-preserving change. The fresh Java report remains in the local Maven build output.

## Java and frontend checks

Maven verify with the mysql-verification profile passed 36 tests: 26 HTTP/unit tests and ten MySQL integration tests. No failures, errors, or skips were reported. The tests cover authentication/claims, registration rollback and concurrent email conflicts, roles/ownership, validation/pages, purchase approval/decline, identical/changed retries, concurrent spending/refunds, full-refund rules, frozen accounts, and transaction rollback.

JaCoCo 0.8.14 measured all 34 production classes without coverage exclusions:

| Metric | Covered / total | Result |
| --- | --- | --- |
| Java lines | 472 / 493 | 95.74% |
| Java branches | 116 / 150 | 77.33% |
| Java instructions | 2199 / 2300 | 95.61% |
| Java methods | 147 / 153 | 96.08% |

The 70% line gate passed; line coverage exceeds the rubric's 80% Excellent target. Branch coverage is stated separately. The [HTML](jacoco/index.html), [XML](jacoco/jacoco.xml), and [summary](java-results.json) preserve the measurement. Raw test response output was not exported.

Frontend prop checking, 51 API/route/state tests, one admin-setup MySQL test, and Vite build passed. npm audit reported zero vulnerabilities. Eleven Chromium workflow groups passed against the real backend/MySQL. The [checklist](01_Completion_Checklist.md) describes injected failures, keyboard/responsive/axe checks, and screenshots. V8 browser coverage converted through source maps measured 98.69% lines, 81.25% branches, and 88.46% functions. V8 counts differ from JaCoCo; broad browser execution is not proof that every condition was asserted.

## Postman

Postman CLI 1.71.0 ran the [exported collection](Credit_Circuit.postman_collection.json) against the real local server/MySQL: 47 collection items plus two rate-limit subrequests, 49 HTTP requests, and 72 assertions all passed. It covers every application endpoint plus authentication, wrong role/ownership, invalid input/pages, duplicate email/requests, purchase approval/decline, full-refund restrictions, status, cookie-only access, CORS, and rate limiting.

The [run output](postman-results.txt) and [summary](postman-results.json) contain no active credentials, tokens, or raw response bodies. Collection credential/token/card/code variables are blank; the runner supplies fictional fixtures in memory and cleans up its rows. No Postman cloud publication is claimed.

## Actual SonarQube analysis

I used SonarQube Community Build 26.9.0.129388 locally on Java 21 and official SonarScanner CLI 8.1.0.6389. The root scanner analyzed backend/src/main/java and frontend/src, with their tests and JaCoCo/LCOV imports. The supported local setup meets the analysis requirement without AWS or a pipeline. [sonar-project.properties](../../sonar-project.properties) supplies reproducible project paths; external Maven output required matching binary/report overrides. Signing keys, database settings, and the analysis token were kept outside source control.

The processed analysis for source revision **e7c9f1fad8b53372fe5c26505308da9919ea085a** reports:

| Metric | Actual result |
| --- | --- |
| Quality gate | OK |
| Bugs / vulnerabilities / security hotspots | 0 / 0 / 0 |
| Open blocker / critical / major issues | 0 / 0 / 0 |
| Open minor issues | 9 naming findings |
| Reliability / security / maintainability ratings | A / A / A |
| Combined Java/React coverage | 93.9% |
| Combined line / branch coverage | 97.6% / 79.7% |
| Duplicated lines | 0.0% |
| New-code coverage / new violations | 91.9% / 0 |

These are SonarQube's combined metrics, not a substitute for Java's separate JaCoCo report. The saved [actual results](sonar-results.json) include analysis ID/time, gate conditions, metrics, all open issues, and review records. The [successful scanner log](sonar-scanner.txt) and [before-cleanup findings](sonar-before.json) are preserved. A scanner upload alone was not treated as a passing gate.

I addressed Java readability findings, React nested conditionals, semantic status feedback, and fetch error-formatting complexity. Nine javascript:S7718 minor findings prefer the catch name error_ over failure. They remain disclosed; no critical/major finding was left open.

Two findings were reviewed and marked false positive with explanations saved in SonarQube, rather than hidden through rule exclusions:

1. **java:S4502, API CSRF scope.** The application accepts explicit bearer headers and no automatic authentication cookies, sessions, Basic, or form login. React uses credentials=omit; CORS disallows cookie credentials. Cookie-only requests returned 401, an untrusted preflight returned 403, and allowed local preflight passed. The [API contract](../02_Architecture/03_API_Design.md) requires revisiting CSRF if credential transport changes.
2. **javascript:S6845, focusable table region.** The named native section is an interactive horizontal scroll container. Its tabIndex enables keyboard access; the tablet test verifies ArrowRight changes scrollLeft. Removing focusability would reduce accessibility. The saved review cites [MDN's overflow accessibility guidance](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Properties/overflow).

## Limits and remaining actions

No cloud deployment, production load test, or universal accessibility certification is represented as completed. The rate limiter is local and resets on restart. A copied JWT remains valid until expiration after browser sign-out.
