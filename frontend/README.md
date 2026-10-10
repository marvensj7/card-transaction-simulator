# React frontend

Credit Circuit uses JSX, JSDoc prop checking, ordinary page state, and direct handlers. Home, dashboard, and purchase share FlippableCard. It loads a Three.js model with rounded edges, actual thickness, a pointer-controlled foil gradient, and an idle shimmer. Click, tap, Enter, or Space flips it. Reduced-motion preferences stop shimmer/tilt and switch faces immediately. If WebGL2 or the graphics download is unavailable, or the graphics context is lost, the ordinary HTML/CSS card remains usable. Cards start masked. Only the owned dashboard card offers Show/Hide details: the front reveals the derived fictional number and the back reveals a temporary sample security code. Both are cleared after 20 seconds, window blur, a hidden tab, or navigation. Home remains masked.

Use this card navigates to `/purchase?useCard=1`. That flag contains no card details. PurchasePage reconstructs the assigned fictional number and fills it and the expiry once in React memory; the security-code field stays empty and the ordinary form remains editable. Unknown instructions or a mismatched mask disable reveal/prefill. The helper follows the current ACCOUNT_V1 and legacy rules in the backend's entry instruction without changing API fields. ACTIVE/FROZEN badges reflect the loaded account; a frozen card is muted with shimmer and tilt stopped. Admin permissions and server purchase checks are unchanged.

FlippableCard owns the side and input handlers. cardScene creates the geometry, face shader, rendering loop, and cleanup; cardArtwork draws safe labels onto textures. The scene pauses drawing while offscreen or the document is hidden, limits pixel density to 2, and releases graphics resources when its page unmounts. Three.js loads in a separate optional bundle (about 142 kB compressed) after the ordinary card renders.

```powershell
npm ci
npm run dev
npm run check:props
npm run check:api
npm run check:routes
npm run check:state
npm run build
```

With Vite running, `npm run check:card` verifies the real WebGL model in Chromium using software graphics, hover/idle changes, keyboard/touch flips, reduced motion, phone layouts, and graphics fallbacks. Its dashboard/purchase display checks use explicitly mocked safe API data. [Card model results](../outputs/03_Verification/card-model-results.json) are separate from the real backend workflow results below.

With Vite, backend, and MySQL running, `npm run check:wallet` runs six fictional-rule checks and the real owned-card reveal/prefill/purchase/freeze/legacy workflow with temporary fixtures. The timer uses an advanced browser clock; blur, visibility, and graphics-loss events are explicitly injected. [Wallet results](../outputs/03_Verification/card-wallet-results.json) record the checks. Only masked screenshots are exported; revealed pixels and credentials stay in process memory.

Vite serves [the app](http://127.0.0.1:5173) and proxies /api to port 8080. Protected workflows need the backend and MySQL. Home and the sign-in form render without account data.

If signing in returns HTTP 502 or 504, check that Spring Boot is running. The React server can display the form while the API is stopped. Start MySQL, then run the following in a separate terminal from the repository root and keep that terminal open:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local" "-Dcapstone.build.directory=C:/Users/marve/.cache/credit-circuit-build"
```

Wait for the backend's `Started` message and confirm that the [OpenAPI UI](http://127.0.0.1:8080/swagger-ui/index.html) opens, then retry signing in. If startup fails, check the ignored local configuration described in the [backend README](../backend/README.md).

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
