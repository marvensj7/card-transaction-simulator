# Credit Circuit frontend

I built one React application with Vite and plain JavaScript/JSX. `index.html` loads `src/main.jsx`, which imports `styles.css` and wraps `App.jsx` in React Router's `BrowserRouter`. `App` contains the shared header, navigation, route list, and footer inside `UserUiProvider`. Each route renders one page from `src/pages/`.

Credit Circuit is the app name; Credit Card Transaction Simulator is the formal capstone description. The opening screen pairs a large wordmark with a pale fictional card against a dark background. A lime accent and thin signal traces connect the visual identity. Request → Checks → Outcome hints at the purchase flow without revealing an approval, amount, balance change, or history.

The introduction sits beside the card on wide screens and above it on phones and portrait tablets. At 50rem and below, a Menu button opens the navigation links; desktop shows them directly. The footer and long text wrap when needed. The card's grid column can shrink, its emblem stays inside the card, and its heading and footer can wrap when text is enlarged. The layout uses ordinary CSS without a UI framework.

## Pages and shared components

| Route | What exists now |
| --- | --- |
| `/` | The Credit Circuit opening screen, fictional display card, and purchase-path teaser. |
| `/login` | A sign-in status notice and a link home. Registration and sign-in are still being built. |
| `/dashboard` | The account page heading and an access-unavailable notice. |
| `/purchase` | The purchase page heading and an access-unavailable notice. |
| `/transactions` | The history page heading and an access-unavailable notice. |
| `/admin` | The administration heading and an access-unavailable notice. |

An unknown address renders `NotFoundPage` with a link home. Sign-in and the four protected pages share the label “Access unavailable” and explain what is still being built. The protected pages currently show no account data, forms, tables, or banking actions. The sign-in page collects no credentials. All six routes can be opened directly and refreshed locally.

I kept the shared pieces small. `SiteNavigation` uses `NavLink` to mark the current page. `PageHeading` displays an eyebrow, title, and description. `AccessNotice` repeats the same access message on the four protected pages, with each page supplying its own explanation as `children`. `CircuitMark` supplies the SVG mark used in the header and on the home card. `RouteFocus` updates the browser title, moves focus to the content after a route change, and scrolls to the top. A skip link and visible outlines on links, buttons, and focused content support keyboard navigation.

Each page has one main area and one top-level heading. The page language is English, notice sections use their headings as labels, and decorative SVGs stay out of the accessibility tree. The card displays only a masked ending. It supplies “Card ending in 4242” as readable text for assistive technology and hides the visual bullets from it. I use that text instead of naming a paragraph with `aria-label`, following the [ARIA naming rules](https://www.w3.org/TR/html-aria/).

`PageHeading`, `AccessNotice`, and `UserUiProvider` define their props in JSDoc comments. `npm run check:props` checks those props, their JSX callers, and the safe user-state fields through `jsconfig.json`. TypeScript is only a development checker; the application stays in `.js` and `.jsx` files. [JSDoc checking](https://www.typescriptlang.org/docs/handbook/jsdoc-supported-types.html) provides feedback without changing the source language.

## State and navigation

`auth/UserUiContext.jsx` holds one shared `user` value in memory. It starts as `null`, meaning anonymous. `UserUiProvider` uses `useState`; navigation and access notices read the value with `useContext` through `useUserUi`. They currently show “Not signed in” and “Secure sign-in comes first.” All six page links remain available, and protected pages keep their unavailable notices without showing account data or actions.

The context allows safe user details (`id`, `displayName`, and `role`) for display. Nothing in the application calls `setUser` yet. Only a later response from a verified server session may establish those details. This value is not authentication, and changing it cannot open the unfinished account tools or grant API access. A reload starts anonymous again. The pages make no API calls and use no browser storage, browser tokens, or simulated signed-in users. The API functions under `src/api/` are tested separately with mocked `fetch`.

`SiteNavigation` keeps `isMenuOpen` in its own `useState`. One effect closes it on a route change, and an Escape listener runs while it is open. A separate viewport listener runs while navigation is mounted. When resizing hides a focused desktop link, focus moves to Menu; when resizing hides the focused Menu button, focus moves to the first desktop link. A blur handler also covers the browser hiding a control before the viewport listener runs. Resizing leaves content focus alone and closes an open menu on desktop. Listeners are removed when no longer needed or on unmount. Refs identify the button and navigation without adding more state.

Escape and selecting the current page return focus to the button. Selecting another page closes the menu and moves focus to its content through `RouteFocus`. The button supports Enter and Space, reports `aria-expanded`, and points to the navigation with `aria-controls`. Closed links are hidden on phones and stay out of the tab order.

I kept ordinary functions and simple state updates. There is no complex transition that needs `useReducer`, and no expensive calculation or measured rendering issue that needs `useMemo` or `useCallback`.

## Work still pending

Registration, BCrypt password checks, server sessions, and CSRF protection come before enabling browser sign-in. The future sign-in/current-user calls will update shared user details only after verification; sign-out or session expiry will clear them. The pages will then call the API functions described below. Account ownership and administrator permissions will still be checked by the server. Card animation is outside this MVP.

Form validation is still pending for registration, sign-in, and fictional purchases. Those real forms will need labeled fields, input checks, specific errors beside the affected fields, and a clear submission result. Purchase request IDs and retries will stay on the purchase page.

Loading feedback is also pending for sign-in, account/card reads, purchase submissions, history and admin lists, full refunds, and account-status changes. It will belong beside each real request, with pending actions kept from submitting twice. The current pages collect no input and make no API requests, so they have no form validation or loading indicators yet. Fields, errors, loading, and results will use ordinary page state when these workflows exist.

The [React architecture document](../outputs/02_Architecture/04_React_Component_Diagram.md) describes the component relationships and remaining page behavior.

## API functions

I use one small helper, `src/api/fetchJson.js`, to send relative `/api` requests with `credentials: 'same-origin'`. The browser will carry its server session cookie. The helper asks for JSON and adds `Content-Type: application/json` only when sending a JSON body. It parses successful JSON directly, including plain arrays and purchase/refund results. An HTTP 200 purchase can be APPROVED or DECLINED; a decline is a saved result, not an HTTP error.

`src/api/creditCircuitApi.js` exports these ordinary named functions:

| Function | Request |
| --- | --- |
| `getAccounts()` | GET `/api/accounts` |
| `getCards(accountId)` | GET `/api/accounts/{accountId}/cards` |
| `submitPurchase(accountId, purchase)` | POST `/api/accounts/{accountId}/purchases` with the eight purchase fields as JSON |
| `getTransactions(accountId)` | GET `/api/accounts/{accountId}/transactions` |
| `refundPurchase(purchaseId, requestId)` | POST `/api/transactions/{purchaseId}/refund?requestId=<UUID>`, without a body |
| `getAdminAccounts()` | GET `/api/admin/accounts` |
| `getAdminTransactions()` | GET `/api/admin/transactions` |
| `updateAccountStatus(accountId, status)` | PATCH `/api/admin/accounts/{accountId}/status?status=ACTIVE` or `FROZEN`, without a body |

Account, card, history, and admin lists return arrays without a paging wrapper. Purchase/refund results contain `transaction` and `account`; a status change returns the updated account. The [API design](../outputs/02_Architecture/03_API_Design.md) defines their fields. The purchase caller supplies `cardId`, `testCardNumber`, `expiryMonth`, `expiryYear`, `testSecurityCode`, `merchantName`, `amount`, and `requestId`. The helper keeps that request ID and amount type unchanged. A later purchase page must create the ID once for a new submission and keep the same details for an uncertain retry. Refunds also take a caller-supplied ID and never send an amount. Query values are URL-encoded.

On an HTTP error, the helper throws an `Error` with the server's safe `message`. A missing, invalid, or unreadable error message falls back to a message containing the HTTP status. A message that repeats the submitted full fictional card number or security code also uses that fallback. Network failures say “Cannot reach Credit Circuit. Check your connection and try again.” Unreadable successful JSON has its own clear error. Raw failure details, request bodies, and response objects are not attached to errors or logged.

I can exercise these functions with `npm run check:api` now. The pages still make no protected requests on load and keep their unavailable notices. There is no browser sign-in or verified session yet. The backend sign-in work must establish the CSRF cookie/header contract before browser writes are enabled; these functions do not invent one. Protected browser flows cannot run end to end until session authentication and CSRF protection are complete. I use no Axios, caching layer, bearer tokens, or browser token storage.

## Local setup

Use Node.js 20.19+ on the 20.x line, 22.13+ on the 22.x line, or 24+, with npm. These versions also support the JSDOM interaction checks. From the repository root in PowerShell:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://127.0.0.1:5173/`. Vite stops if port 5173 is already occupied. All current pages run without Spring Boot or MySQL.

```powershell
npm run build
npm run check:props
npm run check:routes
npm run check:state
npm run check:api
npm run preview
```

The production build goes into `dist/`. `preview` serves that build locally. Dependencies and generated files stay out of Git; `package-lock.json` records the installed versions. Use `npm ci` for a fresh install from that lockfile.

`check:routes` uses Node's test runner and Vite's JSX loader. Its 13 checks cover route headings, one current navigation link, anonymous status, the unknown-route fallback, the readable card ending, the home teaser without a purchase result, and consistent unavailable notices without banking controls or credential fields.

`check:state` uses the same test runner and JSX loader, with JSDOM to mount the real components in React StrictMode. Its 10 checks cover direct page loads, shared user UI state without unlocking tools, anonymous remounts, navigation while browser storage is blocked, menu toggling, Escape focus, current-page selection, route and query changes, browser back/forward, focus handoffs when resizing or when CSS blurs a hidden control first, and listener cleanup. Every state check verifies that no `fetch` call occurs. JSDOM is only a development dependency. It has no layout engine, so I check layout and keyboard behavior in the browser too.

`check:api` runs 33 checks with Node's test runner and mocked `fetch`, without Spring Boot or MySQL. They cover all eight endpoint paths and methods, same-origin credentials, JSON bodies, body-free query parameters, plain arrays, approved/declined results, unchanged retry IDs and bodies, HTTP errors including 400/401, network failure, unreadable JSON, safe error messages, and no console logging. The build, props check, 13 route checks, 10 state checks, and 33 API checks pass. These checks verify the API functions and the unavailable interface; they do not establish a server session or complete a browser transaction.

On October 8, I visually checked every route and the unknown nested route in the production preview at 1440 × 900, 768 × 1024, and 320 × 844. I also checked 390 × 844 and open tablet/phone menus. Direct URLs and refresh worked, and no page or open menu needed horizontal scrolling. At 320px, I temporarily doubled the preview's root text size from 16px to 32px and checked every page and open menu again. Long headings and card labels wrapped, and the card stayed inside its column. I restored the normal text size afterward.

Tab, Shift+Tab, Enter, Space, Escape, the skip link, visible focus, route-change focus, browser back/forward, and the focus handoff across the 50rem navigation breakpoint worked. I checked the browser accessibility tree for headings, menu state, notice labels, and the readable card ending. The measured text contrast was at least 4.99:1, so the existing colors stayed in place. I also reviewed all frontend source: pages do not import the API functions, UI user details do not unlock account tools, and display content and console output contain no full card numbers, security codes, passwords, or session secrets.

React Router uses regular URL paths. A deployed web server would need to serve `index.html` for frontend routes so direct links and refresh work. API and asset requests need their own handling. Local Vite development and preview already provide the frontend fallback.

### Local API connection

Vite forwards paths beginning with `/api` to `http://localhost:8080`, keeping the path intact. For example, `/api/accounts` goes to Spring Boot's `/api/accounts`. The API functions use these relative paths. The current pages make no API requests.

Start Spring Boot in a second terminal using the [backend setup](../backend/README.md). The API currently returns `401` for protected requests because sign-in is not implemented.

If Spring Boot uses a different port, copy the example and change the origin:

```powershell
Copy-Item .env.example .env.local
```

| Variable | Default | Used by |
| --- | --- | --- |
| `API_PROXY_TARGET` | `http://localhost:8080` | Vite's local proxy configuration; use only the backend origin, without `/api` or credentials. |

Restart Vite after changing `.env.local`. No browser environment variables are needed. Vite exposes `VITE_` variables to browser code, so those variables must never contain database credentials, passwords, JWTs, or signing keys ([Vite environment documentation](https://vite.dev/guide/env-and-mode)). Backend credentials stay in the backend's local configuration. `.env.local` is ignored; `.env.example` contains only a nonsecret default.

The proxy is local Vite tooling and is not included in `dist/`. A deployed build would need a web server that forwards `/api` to Spring Boot.
