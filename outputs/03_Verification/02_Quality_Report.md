# Credit Circuit quality report

The results below come from saved local verification runs. Each run has its own date. The Java checks were repeated during final cleanup on October 12; Postman and SonarQube retain their recorded October 9 results.

## Java tests and coverage

Maven verify with the mysql-verification profile passed 47 tests: 34 HTTP/unit tests and 13 real MySQL integration tests. There were no failures, errors, or skips.

The tests cover registration rollback and duplicate email, signed authentication, roles and account ownership, assigned cards, validation, pagination, approvals, declines, request-ID retries, concurrent spending, full refunds, and failed saves. MySQL tests check actual persistence and transaction behavior.

| JaCoCo metric | Covered / total | Result |
| --- | --- | --- |
| Lines | 516 / 540 | 95.56% |
| Branches | 146 / 172 | 84.88% |
| Instructions | 2223 / 2313 | 96.11% |
| Methods | 151 / 158 | 95.57% |

The enforced 70% line-coverage gate passed. JaCoCo measured all 32 production classes without coverage exclusions. See the [test summary](java-results.json) and [coverage XML](java-coverage.xml). Coverage shows which code ran during tests. It does not prove the absence of bugs.

## Frontend and browser checks

The October 12 cleanup checks passed prop checking, 37 API tests, nine route tests, seven state tests, six fictional-card rule tests, and the production build. See [frontend results](frontend-checks.json).

The saved [browser results](browser-results.json) cover thirteen workflow groups against Spring Boot and MySQL. They include approvals, declines, uncertain retries, refunds, account status, ownership, and pagination. Separate October 10 [wallet checks](card-wallet-results.json) exercise owned-card loading, reveal/prefill, a real purchase, admin freezing, and legacy details.

The [card model checks](card-model-results.json) verify WebGL rendering, shimmer/hover, keyboard/touch flips, reduced motion, phone layouts, and graphics fallbacks. Their account/card display responses are mocked.

The browser runner injects lost responses, expiration/failure/empty responses, and some visibility/graphics events. Its reports identify these cases. Java tests separately verify expired JWTs. Sampled pages at 1440, 768, 390, and 320 pixels had no reported axe WCAG 2 A/AA or 2.1 violations. This does not cover every browser or assistive technology.

Recorded browser V8 coverage is 99.13% lines, 78.98% branches, and 88.88% functions. See [frontend coverage](frontend-coverage.json) and [LCOV](lcov.info). It is separate from the Java gate.

The optional Three.js chunk is about 563 kB, or 142 kB compressed. Vite reports a large-chunk advisory. It loads after the ordinary card appears.

## Postman and OpenAPI

The October 9 Postman CLI run exercised all eleven application endpoints. It passed 56 HTTP requests and 83 assertions with zero failures. The collection covers authentication, roles/ownership, validation, pages, assigned cards, approval/decline, retries, refunds, account status, cookie-only rejection, CORS, and authentication rate limiting.

The [collection](Credit_Circuit.postman_collection.json), [run summary](postman-results.json), and [CLI output](postman-results.txt) contain no active credentials, issued JWTs, full card numbers, security codes, or raw response bodies. Sensitive exported variables are blank. The runner supplies temporary fixture values in memory and removes its own rows.

The [OpenAPI export](Credit_Circuit.openapi.json) documents the backend's request and response schemas. User/account/card responses exclude password hashes, linked entities, and internal card fields.

## SonarQube

The final October 9 analysis completed at 4:14 p.m. Eastern for revision f72dc2c, including the local entity edit recorded in that run. Its processed quality gate passed.

| Metric | Recorded result |
| --- | --- |
| Quality gate | PASSED |
| Open security / reliability / maintainability issues | 0 / 0 / 0 |
| Accepted issues / security hotspots | 0 / 0 |
| Security / reliability / maintainability ratings | A / A / A |
| Overall Java/React coverage | 95.3% |
| New-code coverage / new issues | 96.0% / 0 |
| Duplicated lines | 0.0% |

The [results](sonar-current-results.json), [scanner log](sonar-current-scanner.txt), and [dashboard screenshot](sonar-current-dashboard.jpg) preserve the actual analysis. The JSON is a recorded summary of the scanner/dashboard, not a raw Web API export.

Nine catch-variable naming findings were corrected before the passing scan. It imported fresh Java coverage and existing browser LCOV. The recorded entity edit lacked SCM blame but was included in analysis. Community Build provides limited security analysis. Later frontend changes postdate this scan.

[sonar-project.properties](../../sonar-project.properties) records the analysis paths. A Maven build outside backend/target needs matching binary, test-result, and coverage path overrides.

## Persistence checks and limits

The [restart check](restart-results.json) confirmed that an assigned card and saved purchase survive a backend restart. Retrying the original request returned the same transaction and one balance change. The recorded [seed check](seed-results.json) confirmed that rerunning the then-current SQL seed preserved existing rows and four base tables.

This is a local simulator. The authentication limiter resets on restart. A copied JWT stays valid until expiration after browser sign-out. No production load test, cloud deployment, or universal accessibility certification is claimed.
