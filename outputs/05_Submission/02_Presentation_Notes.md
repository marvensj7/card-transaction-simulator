# Presentation rehearsal notes

My target is about nine minutes, including a three-minute live demo. The editable deck includes these notes. The PDF is a slide handout. The screenshots show verified temporary fixtures; passwords and tokens are never displayed.

Before presenting, start MySQL, the backend, and Vite using the [local guide](01_Local_Run_and_Demo.md). Check both private sign-ins, prepare a fresh fictional customer, and open the saved verification reports. Keep the PDF and screenshots available if the live app is interrupted. Do not claim a screenshot is a live interaction.

## Slide 1

0:00–0:30. I built a local credit card transaction simulator for UCI 2123. All cards, identities, merchants, and balances are fictional. The home screen retains the Request, Checks, Outcome teaser. Source: outputs/01_Project_Proposal/03_Submission_Proposal.md and home-desktop.png. The instructor waived AWS and related deployment/DevOps work.

## Slide 2

0:30–1:15. A purchase can exceed available credit or reach the wrong account if the API checks only roles. A network failure can leave a customer unsure whether a purchase saved. I built explicit decisions, safe retries, ownership checks, and consistent balance/history writes. This is a classroom simulator, with no bank/payment network connection. AWS infrastructure, deployment pipelines, and cloud monitoring are waived. Jira and branch protection are outside this pass. Source: current proposal and authentication/financial ADRs.

## Slide 3

1:15–2:15. Trace a page through fetch, Spring Security, controller, service, repository, and MySQL. The bearer token comes only from React memory. Controllers validate shape; services check stored role/ownership and financial rules. JPA repositories query and lock rows. I used the class banking app as a learning reference without copying its supplied solution. Source: outputs/02_Architecture/01_System_Architecture.md. This diagram is editable native slide content.

## Slide 4

2:15–3:00. app_users stores BCrypt and USER/ADMIN. A customer owns one credit account and one fictional card. An administrator has no account. card_transactions stores purchases and linked full refunds. No full number/security code/token column exists. BigDecimal/DECIMAL handle money and available credit is calculated. Foreign keys and unique constraints protect relationships; no cascade deletes financial history. Source: ERD and sql/01_schema.sql.

## Slide 5

3:00–6:00. Open the running app. Use private credentials, never show passwords or tokens. Register/sign in as customer and show the masked card and $1,000 available. Purchase $50 using the displayed expiry and fictional profile. Show $950 available. Purchase $2,000 and show a saved insufficient-credit decline with no balance change. Open history and confirm the full $50 refund. Show original/refund and restored credit. Sign out, sign in as ADMIN, find/freeze the customer account, and show activity. Customer spending declines while frozen. Reactivate for the next demo. The local guide has the full sequence. Screenshot is verified fixture data, not an active credential. Source: browser-results.json and dashboard-desktop.png.

## Slide 6

6:00–7:00. Explain three separate protections. The UUID identifies one submission; account/request uniqueness backs it. A pessimistic account lock serializes competing changes. @Transactional commits balance and history together or rolls both back. READ_COMMITTED lets a waiting retry see a prior commit. An identical retry returns 200 with the original transaction and current account summary. New approval/decline/refund returns 201. Full refunds use the original amount and one unique original_purchase_id. Browser tests lose responses after real saves; MySQL tests verify concurrency/rollback. Source: TransactionService, financial ADR, Java/browser results.

## Slide 7

7:00–7:45. BCrypt checks passwords. Spring Security/Nimbus validate HS256 signatures, expiration, issuer, audience, positive numeric subject, role, and time claims. The random signing key stays outside Git. Registration cannot choose ADMIN. Services still check stored roles and ownership. React holds access in memory and expires/discards it on sign-out/reload/401. A copied token remains valid until expiry; no refresh/revocation infrastructure exists. Explicit bearer headers, no accepted authentication cookies, and credentials=omit justify the API CSRF scope. CORS and a small per-IP auth limit apply. This transport was explicitly reviewed in SonarQube. Sources: authentication ADR, SecurityConfiguration, and https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html .

## Slide 8

7:45–8:30. Reports are from October 8 on the real local application/MySQL. The 36 Java tests include ten MySQL integration tests. JaCoCo line coverage is 95.74%, branch 77.33%, with an enforced 70% line gate; no production class exclusions. Postman executes 47 exported items plus two rate-limit subrequests, 49 HTTP requests and 72 assertions. Eleven browser groups cover real customer/admin workflows and explicitly injected failure/empty/expiration cases. Automated accessibility is Chromium only. SonarQube is the actual local Community Build, not another linter. Its saved report records the processed gate, findings and justified CSRF/table reviews. Source: outputs/03_Verification/ .

## Slide 9

8:30–9:15. A request ID, lock, and transaction solve different problems. Boundary validation and service rules have different jobs. Quality claims need actual tool results and honest limits. I still need two peer reviews and to give/incorporate feedback, private demo access, and personal rehearsal. The 3D card remains planned after the required app works. Rehearse October 12; present/submit October 13. Do not claim reviews/submission have occurred. Source: self-assessment and blank peer-review worksheet.

## Questions I should be ready to answer

- Why is the request ID separate from a database transaction? The ID recognizes the same submission; the transaction keeps related writes together.
- Why use an account lock? Two simultaneous purchases must not both spend the same available credit.
- Why is CSRF scoped this way? The API accepts explicit bearer headers and no automatic cookie credentials. Adding authentication cookies would require reviewing this choice.
- What does logout do? It discards this browser's access. A copied token remains valid until its 15-minute expiry.
- What does coverage prove? It shows executed code, not correctness by itself. Assertions, failure cases, rollback/concurrency tests, and real browser/Postman runs provide additional evidence.
- What is unfinished? The planned 3D card, my two peer reviews and feedback changes, rehearsal on October 12, and presentation/Canvas submission on October 13. I have not claimed those human actions occurred.

Rehearse the actual sequence on October 12, time it, and adjust the spoken detail to stay within five to ten minutes. Supply demo credentials privately. Present and submit on October 13 after the final checklist.
