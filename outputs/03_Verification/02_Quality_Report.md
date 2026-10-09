# Quality report - October 9, 2026

I verified the registration, assigned fictional card, and purchase workflow against the real local application and MySQL. The current application revision is **cd81a36**. The original uncommitted CardTransaction.java edit was preserved during these checks and is not included in the commits for this pass.

The fresh Java, React, browser, Postman, restart, seed, and OpenAPI evidence describes this pass. The SonarQube exports and JaCoCo HTML directory still describe October 8; they are historical evidence. A fresh SonarQube scan was attempted but requires restored access to the local SonarQube server.

## Java and frontend checks

Maven verify with the mysql-verification profile passed **46 tests**: 34 HTTP/unit tests and 12 real MySQL integration tests. There were no failures, errors, or skips. Tests cover registration rollback and concurrent email conflicts, signed authentication and claims, roles and ownership, different assigned cards, full-number matching even when masks match, validation and pages, approval and saved declines, identical and changed retries, concurrent spending and refunds, full-refund restrictions, and atomic rollback.

JaCoCo 0.8.14 measured all 34 production classes without coverage exclusions:

| Metric | Covered / total | Result |
| --- | --- | --- |
| Java lines | 529 / 552 | 95.83% |
| Java branches | 148 / 174 | 85.06% |
| Java instructions | 2303 / 2390 | 96.36% |
| Java methods | 150 / 156 | 96.15% |

The enforced 70% Java line gate passed, exceeding the rubric's 80% target. The [current XML](java-coverage.xml) and [measured summary](java-results.json) preserve this measurement. The [older HTML report](jacoco/index.html) and its adjacent XML/CSV remain October 8 evidence. They were not replaced with source HTML containing the preserved, uncommitted CardTransaction.java edit. No raw test response output was exported.

Frontend prop checking, 51 API/route/state tests, one private admin-setup MySQL test, and the Vite production build passed. npm audit reported zero vulnerabilities. Twelve Chromium workflow groups passed against the real backend and MySQL. The [checklist](01_Completion_Checklist.md) describes injected failures, keyboard/responsive/axe checks, and refreshed screenshots. Browser V8 coverage measured 99.12% lines, 80.47% branches, and 88.60% functions. It is separate from the Java coverage gate and does not prove that every condition was asserted.

The [restart check](restart-results.json) stopped and restarted its own backend process after saving a purchase. The assigned card stayed identical; replaying the original UUID returned the same transaction, one history row, and one balance change. The [seed check](seed-results.json) reran the SQL seed and confirmed four base tables and preservation of existing seeded passwords, balances, account/card details, and history. No schema migration was needed.

## Postman and OpenAPI

Postman CLI 1.71.0 exercised the [exported collection](Credit_Circuit.postman_collection.json): 54 collection items plus two rate-limit subrequests, **56 HTTP requests and 83 assertions**, with zero failures. It covers all application endpoints plus authentication, roles and ownership, invalid fields/pages, duplicate emails/requests, assigned cards and mismatched details, unchanged balance/history after invalid input, approval/decline, full refunds, account status, cookie-only access, CORS, and authentication rate limiting.

The [CLI output](postman-results.txt) and [summary](postman-results.json) contain no active credentials, JWTs, full card numbers, or raw response bodies. Exported credential/token/card/code variables are blank. The runner derives fictional numbers and supplies temporary credentials in memory, then removes its own fixtures. No Postman cloud publication is claimed. The refreshed [OpenAPI export](Credit_Circuit.openapi.json) comes from the verified running backend and includes the masked card's simulation-only numberEntryHint.

## Fresh SonarQube attempt

SonarScanner CLI 8.1.0.6389 attempted to connect to local SonarQube Community Build 26.9.0.129388 on October 9. The server returned **HTTP 401 Unauthorized** while the scanner queried its version, before analysis or upload could start. No new quality gate or findings are available. The [attempt summary](sonar-refresh-results.json) and [sanitized scanner output](sonar-refresh-attempt.txt) record the failure.

SonarQube has a separate login from Credit Circuit. Its earlier setup used a temporary analysis credential that was revoked after that scan; a usable current credential was not available. Restoring the local SonarQube login and obtaining an analysis credential is the remaining action before rerunning the required scan. Authentication was not reset or weakened, and no findings were suppressed to obtain a result.

## Historical SonarQube analysis - October 8

The saved [results](sonar-results.json), [successful scanner log](sonar-scanner.txt), and [before-cleanup findings](sonar-before.json) describe revision **e7c9f1fad8b53372fe5c26505308da9919ea085a**, not this pass. That actual processed analysis used local Java 21 and SonarQube Community Build with Java/React sources and JaCoCo/LCOV imports.

| Metric | October 8 result |
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

Those nine javascript:S7718 findings preferred the catch name error_ over failure. Two findings were reviewed as false positives, with the recorded reasons preserved: java:S4502 concerned CSRF for an API accepting explicit bearer headers without automatic cookie/session authentication; javascript:S6845 concerned keyboard focus on a named horizontal table scroll region. The saved review records explain the tests and reasoning. Their historical status does not establish the findings for the current revision.

[sonar-project.properties](../../sonar-project.properties) supplies the reproducible project paths. A Maven build outside backend/target needs matching binary, test-result, and JaCoCo path overrides. Database settings, signing keys, and analysis credentials remain outside source control.

## Limits and remaining work

The fresh SonarQube scan remains blocked by local server authentication. History/refund and admin React pages and their shared table/dialog controls still need a later readability pass. No cloud deployment, production load test, or universal accessibility certification is represented as completed. The rate limiter is local and resets on restart; a copied JWT remains valid until expiration after browser sign-out. The planned 3D card remains future work, and Phase 6 remains removed.
