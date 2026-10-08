# Self-assessment and next steps

## What I can demonstrate

I built the local customer/admin workflows using the familiar React/controller/service/repository/MySQL pattern. Registration creates three related rows atomically and cannot assign ADMIN. BCrypt and signed JWT protect access. Ownership checks remain in services. Purchases save approval/decline, retries return the same transaction, and full refunds restore credit once. Admin status affects new spending while eligible refunds remain available.

The interface has labeled controlled forms, pending feedback, safe errors, spinner/skeleton, route checks, paged lists, and a keyboard-accessible confirmation dialog. Context/reducer/memoized actions have actual authentication jobs. Four tables remain sufficient.

## Evidence by rubric area

| Area | Evidence and assessment |
| --- | --- |
| Planning/architecture | Current proposal with fifteen stories, system/sequence diagram, ERD, API/component design, concise ADRs. |
| Backend | JPA constraints, request/entity validation, custom exceptions/global handler, bounded pages, correct creation/retry statuses, OpenAPI. |
| React | Real customer/admin pages, direct fetch modules, controlled forms, justified hooks, responsive/focus/loading/error checks. |
| Security | BCrypt, signature/claim validation, roles/ownership, memory tokens, CORS/auth limit, documented CSRF transport and logout limit. |
| Testing | 36 Java tests including ten MySQL integration tests. JaCoCo line coverage 95.74% exceeds the 70% gate and 80% Excellent target; branch coverage is 77.33%. |
| Integration | Exercised exported Postman and real browser/MySQL flows, with injected cases labeled in reports. |
| Quality | Actual SonarQube Java/React analysis and findings/reviews in the quality report. I do not equate a scanner upload with a passing quality gate. |
| Documentation/presentation | Local run/demo guide, walkthrough, deck/PDF and rehearsal notes. I still need to rehearse and explain the implementation myself. |
| Peer review/submission | Still requires two peer reviews, feedback given/incorporated, private demo access, and Canvas submission. No feedback or submission is fabricated. |

I have not assigned myself a final grade. My instructor determines the effect of the AWS waiver on scoring. The saved reports are evidence of tested cases, not proof of production readiness or universal accessibility. Chromium automation does not replace personal screen-reader/cross-browser review. The original two-second performance target has not been established by load testing.

## Remaining work in order

1. Review the code/walkthrough and reproduce the local customer/admin demo. Keep credentials private and verify the final pushed revision.
2. Build the planned flippable 3D card after the required application works. Connect it to the existing form, retain keyboard/reduced-motion behavior, and repeat relevant browser checks.
3. Receive code reviews from two peers, provide constructive feedback on their projects, and record/incorporate the actual findings. Use the blank worksheet.
4. Prepare private customer/admin demo credentials and a fresh fictional account. Check the classroom machine and keep local evidence/screenshots ready as backup.
5. Rehearse October 12 for 5–10 minutes, including live demo, architecture, retry/locking explanation, limitations, and questions. Confirm the wording reflects what I understand.
6. Present October 13. Verify all final changes are committed/pushed and submit code links, proposal/diagrams, SQL/API/run guide/ADRs, tests/coverage/Postman/SonarQube evidence, slides/PDF, actual peer feedback, and this self-assessment to Canvas. Confirm receipt myself.
