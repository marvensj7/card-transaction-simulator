# React frontend

Credit Circuit uses JSX, JSDoc prop checking, ordinary page state, and direct handlers. The dark green/lime home page and fictional card teaser remain.

```powershell
npm ci
npm run dev
npm run check:props
npm run check:api
npm run check:routes
npm run check:state
npm run build
```

Vite serves [the app](http://127.0.0.1:5173) and proxies /api to port 8080. Protected workflows need the backend and MySQL. Home and the sign-in form render without account data.

LoginPage registers/signs in. DashboardPage loads the own account/card. PurchasePage loads the owned account/card, validates controlled input, submits one stable request, and displays approval/decline. The assigned masked card includes numberEntryHint from the backend. Follow that simulation-only instruction; new cards use `0000` followed by the account ID padded to 12 digits. Existing legacy cards keep their displayed instruction. Security codes are format-only. No full number is returned or stored. TransactionsPage pages history and confirms full refunds. AdminPage pages account/activity summaries and changes ACTIVE/FROZEN status. ProtectedRoute checks navigation; the backend enforces authorization.

UserUiContext stores the access token in one ref and safe user/expiration/notice state in a reducer. Context shares identity. Stable useCallback handlers configure API/expiration behavior; useMemo supplies the shared Context value. Effects check expiration/visibility. Pages use useState/useEffect for forms/data. No token goes to browser storage. Reload/sign-out require login. A copied token remains valid until server expiration.

Purchase/refund refs retain the original UUID and input while a response is uncertain. Pending actions are disabled and guarded. An identical retry confirms the saved outcome. Leaving/reloading an uncertain page loses memory-only retry details; inspect history after signing in before starting another purchase.

Small Button/Input/Card/Table/Loading/Pagination/ConfirmModal pieces serve real workflows. Labels describe errors. Loading has a spinner/skeleton. The dialog focuses Cancel and supports Escape. A focusable table region permits keyboard scrolling on narrow screens.

## Real verification

With backend/MySQL running and private DB configuration available:

```powershell
npx playwright install chromium
npm run check:browser
node checks/postman.mjs
```

From the repository root, verify that an assigned card and an identical purchase retry survive an actual backend restart:

```powershell
$env:DEMO_RESTART_JAR = 'C:/path/to/verified/card-transaction-simulator-0.0.1-SNAPSHOT.jar'
node frontend/checks/restart.mjs
```

This check starts its own backend on port 8083, stops and restarts that process, and removes only its temporary MySQL fixtures. It requires the ignored local backend configuration and a free port 8083. `DEMO_JAVA` can specify the Java 17 executable if it is not on PATH.

Runners create temporary fictional users with random passwords in memory and clean only their own rows. The browser deliberately loses responses after real saves, injects a 401 for UI expiration, and injects failure/empty responses for otherwise unavailable states. Real expired-token rejection and timer expiration have separate tests. Reports identify injected cases. Browser coverage uses Chromium V8/source maps and is separate from Java coverage.

The Postman runner supplies private values in memory and exports no raw response/environment. Its final test consumes the authentication limit. Wait at least one minute between browser/Postman runs. [Evidence](../outputs/03_Verification/01_Completion_Checklist.md) records actual results.
