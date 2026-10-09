# Quality report - October 9, 2026

I verified the registration, assigned fictional card, and purchase workflow against the real local application and MySQL. The latest Java and OpenAPI checks cover revision **825927c**. The earlier React, Postman, restart, and seed checks below describe **cd81a36**. Browser evidence was refreshed on October 9 for the 3D card changes, before their commit. The original uncommitted CardTransaction.java edit was preserved during these checks and is not included in the commits for this pass.

The Java and OpenAPI exports were refreshed after removing AccountResponse and CardResponse. CreditAccount and DemoCard now return the same six display fields directly; tests confirm that linked entities and internal card fields remain excluded. The earlier React, Postman, restart, and seed evidence remains dated to its original pass; browser results and screenshots were refreshed for the 3D card. The older SonarQube exports and JaCoCo HTML directory remain historical evidence. The final October 9 SonarQube scan passed; see the current analysis below.

## Java and frontend checks

Maven verify with the mysql-verification profile passed **47 tests**: 34 HTTP/unit tests and 13 real MySQL integration tests. The added MySQL test checks account/card serialization after service transactions close, including customer reads and admin list/status responses. There were no failures, errors, or skips. Tests cover registration rollback and concurrent email conflicts, signed authentication and claims, roles and ownership, different assigned cards, full-number matching even when masks match, validation and pages, approval and saved declines, identical and changed retries, concurrent spending and refunds, full-refund restrictions, and atomic rollback.

JaCoCo 0.8.14 measured all 32 production classes without coverage exclusions:

| Metric | Covered / total | Result |
| --- | --- | --- |
| Java lines | 516 / 540 | 95.56% |
| Java branches | 146 / 172 | 84.88% |
| Java instructions | 2223 / 2313 | 96.11% |
| Java methods | 151 / 158 | 95.57% |

The enforced 70% Java line gate passed, exceeding the rubric's 80% target. The [current XML](java-coverage.xml) and [measured summary](java-results.json) preserve this measurement. The [older HTML report](jacoco/index.html) and its adjacent XML/CSV remain October 8 evidence. They were not replaced with source HTML containing the preserved, uncommitted CardTransaction.java edit. No raw test response output was exported.

Frontend prop checking, 51 API/route/state tests, one private admin-setup MySQL test, and the Vite production build passed. npm audit reported zero vulnerabilities. Thirteen Chromium workflow groups passed against the real backend and MySQL. The [checklist](01_Completion_Checklist.md) describes injected failures, keyboard/responsive/axe checks, and refreshed screenshots. Browser V8 coverage measured 99.13% lines, 78.98% branches, and 88.88% functions. It is separate from the Java coverage gate and does not prove that every condition was asserted.

The [restart check](restart-results.json) stopped and restarted its own backend process after saving a purchase. The assigned card stayed identical; replaying the original UUID returned the same transaction, one history row, and one balance change. The [seed check](seed-results.json) reran the SQL seed and confirmed four base tables and preservation of existing seeded passwords, balances, account/card details, and history. No schema migration was needed.

## Postman and OpenAPI

Postman CLI 1.71.0 exercised the [exported collection](Credit_Circuit.postman_collection.json): 54 collection items plus two rate-limit subrequests, **56 HTTP requests and 83 assertions**, with zero failures. It covers all application endpoints plus authentication, roles and ownership, invalid fields/pages, duplicate emails/requests, assigned cards and mismatched details, unchanged balance/history after invalid input, approval/decline, full refunds, account status, cookie-only access, CORS, and authentication rate limiting.

The [CLI output](postman-results.txt) and [summary](postman-results.json) contain no active credentials, JWTs, full card numbers, or raw response bodies. Exported credential/token/card/code variables are blank. The runner derives fictional numbers and supplies temporary credentials in memory, then removes its own fixtures. No Postman cloud publication is claimed. The refreshed [OpenAPI export](Credit_Circuit.openapi.json) comes from the rebuilt backend with the DTO removal and an expiry-month schema annotation. The annotation keeps the documented month numeric; packaging and the generated schema check passed after this documentation-only correction. Its CreditAccount and DemoCard schemas contain exactly their six display fields; the deleted DTO schemas are absent. The masked card retains its simulation-only numberEntryHint.

## Current SonarQube analysis - October 9

The final scan completed at **4:14 PM Eastern** against revision **f72dc2c**, including the preserved uncommitted CardTransaction.java edit. The actual processed analysis passed its quality gate. The [current summary](sonar-current-results.json), [scanner log](sonar-current-scanner.txt), and [dashboard screenshot](sonar-current-dashboard.jpg) preserve the result. The summary records the scanner log and authenticated dashboard; it is not a raw Web API export.

| Metric | October 9 final result |
| --- | --- |
| Quality gate | PASSED |
| Open security / reliability / maintainability issues | 0 / 0 / 0 |
| Accepted issues / security hotspots | 0 / 0 |
| Security / reliability / maintainability ratings | A / A / A |
| Overall Java/React coverage | 95.3% |
| New-code coverage / new issues | 96.0% / 0 |
| Duplicated lines | 0.0% |

An initial current-code scan failed the zero-new-issues condition for a catch-variable naming finding. All nine javascript:S7718 naming findings were corrected by renaming only the catch variables and their references to error_. Prop checking, all 51 API/route/state tests, and the frontend production build passed afterward. A fresh Java/MySQL verification also passed all 47 tests with 95.56% line coverage and the enforced 70% gate. The final scan imported that fresh JaCoCo XML and the existing October 9 browser-derived LCOV. It did not rerun browser coverage. The updated Java XML reproduces the measured coverage already recorded above.

The scanner warned that the preserved uncommitted CardTransaction.java edit lacks SCM blame. The file was analyzed; it was not excluded or committed as part of this pass. SonarQube Community Build reports limited security analysis, so zero reported issues does not certify the absence of every injection vulnerability.

The earlier HTTP 401 attempt remains recorded in [the attempt summary](sonar-refresh-results.json). Local SonarQube administrator access was recovered after backing up its database, the user selected a new password, and the temporary project analysis token was revoked after the successful scan. No credential values are included in these exports.

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

The final SonarQube scan passed after local administrator access was restored and the user chose a new password. History/refund and admin React pages and their shared table/dialog controls still need a later readability pass. No cloud deployment, production load test, or universal accessibility certification is represented as completed. The rate limiter is local and resets on restart; a copied JWT remains valid until expiration after browser sign-out. The 3D card is implemented and checked in Chromium after the recorded SonarQube scan; that scan does not cover these frontend changes. Phase 6 remains removed.
