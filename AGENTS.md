# Codex guidance for this capstone

This is my individual UCI 2123 credit card transaction simulator. Build code I can read from top to bottom, debug, and explain in my presentation on October 13. My instructor approved this project and confirmed that AWS implementation and a Jira board are not required.

Use the relevant documents in `outputs/01_Project_Proposal/` and `outputs/02_Architecture/` for the current task. For database work, the ERD and API design define the planned fields and behavior. If documents disagree, make the smallest clear correction and keep them consistent.

Follow the simple pattern from the banking app we used in class: React page → API call → Spring controller → service → JPA repository → MySQL. The class app is a learning reference; do not copy its supplied solution into this project. Prefer plain React state, straightforward Java classes, and four well-defined tables over extra frameworks or layers.

Use fictional test cards and balances only. Never store or log a full card number, test security code, password, or JWT. Use BCrypt for passwords. SHA-256 alone is not a password hash.

Work on one numbered capstone section at a time. Inspect nearby files, make a complete small change, run the relevant local checks, and report what passed. Commit cohesive changes with specific, natural messages and push verified work to this public repository. Avoid unrelated rewrites and extra services.

Keep the implementation at a beginner level: ordinary Java classes, explicit if/else decisions and loops, and direct service calls. Keep new abstractions only when the current workflow needs them. Prefer one clear place for validation and reuse the same simple response for customer/admin views when the fields are safe.

The October 8 MVP uses server sessions with BCrypt password checks and CSRF protection before browser sign-in is enabled; JWT support is out of scope. History/admin lists are plain arrays for the local dataset. Keep account ownership, role checks, request-ID duplicate protection, full-refund rules, and all-or-nothing balance/history writes.
