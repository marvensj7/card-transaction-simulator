# React frontend

I use React, Vite, and React Router. Pages keep their own form and request state. Shared components handle repeated buttons, inputs, tables, loading states, and confirmation dialogs.

## Setup

Use Node.js 20.19+, 22.13+, or 24+. From this folder:

```powershell
npm ci
npm run dev
```

Open [http://127.0.0.1:5173](http://127.0.0.1:5173). Vite forwards /api requests to Spring Boot on port 8080. Start MySQL and the backend using the [backend README](../backend/README.md) before signing in. If the backend uses another address, copy .env.example to the ignored .env file and set API_PROXY_TARGET.

A 502 or 504 sign-in error usually means the backend is stopped or still starting. Wait for its Started message and check the [OpenAPI UI](http://127.0.0.1:8080/swagger-ui/index.html), then retry.

## Pages and state

LoginPage handles registration and sign-in. DashboardPage loads the customer's account and assigned card. PurchasePage submits purchases and shows the result. TransactionsPage pages history and confirms full refunds. AdminPage pages accounts/activity and changes account status.

Pages call functions in creditCircuitApi. fetchJson attaches the bearer header, omits cookies, and handles safe errors. UserUiContext keeps the token in a ref and user/expiration/notice state in a reducer. Context shares sign-in state. useCallback keeps authentication handlers stable, and useMemo supplies the Context value. ProtectedRoute controls navigation, while the backend enforces roles and ownership.

Sign-out, reload, expiration, and a protected 401 clear browser access. Nothing stores a token in browser storage. A copied token remains valid until server expiration.

A purchase or refund keeps its UUID and submitted details while the response is uncertain. Retry uses that same request. Leaving or reloading the page loses those memory-only details, so check history after signing in before starting another purchase.

## Card and purchase display

Home, dashboard, and purchase share FlippableCard. Click, tap, Enter, or Space flips it. Three.js adds thickness, a hover gradient, and shimmer. Reduced motion stops tilt/shimmer. If graphics cannot load, the HTML/CSS card remains usable.

Cards start masked. The dashboard can reveal fictional details temporarily, with automatic hiding after 20 seconds, focus loss, or navigation. Use this card fills the assigned number and expiry in React memory. It sends only a useCard=1 navigation flag and leaves the security-code field empty.

The assigned card's numberEntryHint supplies the simulation rule. The helper checks that rule and its mask before reveal or prefill. Security codes are checked for format only. Full numbers and codes are never stored or logged.

AccountSummary shows credit totals and the percentage used. A purchase receipt shows approval/decline, merchant, amount, transaction ID, reason, and the saved balance after that outcome.

## Checks

```powershell
npm run check:props
npm run check:api
npm run check:routes
npm run check:state
node --test checks/fictionalCardNumber.test.mjs
npm run build
```

For browser checks, install Chromium:

```powershell
npx playwright install chromium
```

With Vite running, npm run check:card checks the WebGL model with mocked dashboard/purchase data. With Vite, Spring Boot, MySQL, and private database configuration available, npm run check:browser and npm run check:wallet exercise real workflows. Fixtures use random fictional credentials in memory and remove only their own rows.

Browser reports identify injected failures and events. Sampled accessibility checks cover keyboard use and responsive layouts. Browser V8 coverage is separate from Java coverage.

With Spring Boot and MySQL running, npm run check:postman exercises the exported collection. It supplies private values in memory and exports no raw responses or environment. Its final test consumes the authentication limit. Leave at least one minute between browser/Postman runs.

The restart check starts its own backend on port 8083. From the repository root:

```powershell
$env:DEMO_RESTART_JAR = 'C:/path/to/card-transaction-simulator-0.0.1-SNAPSHOT.jar'
node frontend/checks/restart.mjs
```

It needs ignored local backend configuration and a free port 8083. DEMO_JAVA can specify Java 17 if needed. The [quality report](../outputs/03_Verification/02_Quality_Report.md) records measured results.
