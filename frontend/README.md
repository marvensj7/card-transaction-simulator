# Credit Circuit frontend

I built one React application with Vite and plain JavaScript/JSX. `index.html` loads `src/main.jsx`, which imports `styles.css` and wraps `App.jsx` in React Router's `BrowserRouter`. `App` contains the shared header, navigation, route list, and footer inside `UserUiProvider`. Each route renders one page from `src/pages/`.

Credit Circuit is the app name; Credit Card Transaction Simulator is the formal capstone description. The opening screen pairs a large wordmark with a pale fictional card against a dark background. A lime accent and thin signal traces connect the visual identity. Request → Checks → Outcome hints at the purchase flow without revealing an approval, amount, balance change, or history.

The introduction sits beside the card on wide screens and above it on phones. The card keeps enough width for its labels on small screens. At 50rem and below, a Menu button opens the navigation links; desktop shows them directly. The footer wraps when needed. The card shows only a masked ending and a fictional-card label. The layout uses ordinary CSS without a UI framework.

## Pages and shared components

| Route | What exists now |
| --- | --- |
| `/` | The Credit Circuit opening screen, fictional display card, and purchase-path teaser. |
| `/login` | A sign-in status notice and a link home. Registration and sign-in are still being built. |
| `/dashboard` | The account page heading and an access-unavailable notice. |
| `/purchase` | The purchase page heading and an access-unavailable notice. |
| `/transactions` | The history page heading and an access-unavailable notice. |
| `/admin` | The administration heading and an access-unavailable notice. |

An unknown address renders `NotFoundPage` with a link home. The protected pages currently show no account data, forms, tables, or banking actions. The sign-in page collects no credentials. All six routes can be opened directly and refreshed locally.

I kept the shared pieces small. `SiteNavigation` uses `NavLink` to mark the current page. `PageHeading` displays an eyebrow, title, and description. `AccessNotice` repeats the same access message on the four protected pages, with each page supplying its own explanation as `children`. `CircuitMark` supplies the SVG mark used in the header and on the home card. `RouteFocus` updates the browser title, moves focus to the content after a route change, and scrolls to the top. A skip link and visible link outlines support keyboard navigation.

`PageHeading`, `AccessNotice`, and `UserUiProvider` define their props in JSDoc comments. `npm run check:props` checks those props, their JSX callers, and the safe user-state fields through `jsconfig.json`. TypeScript is only a development checker; the application stays in `.js` and `.jsx` files. [JSDoc checking](https://www.typescriptlang.org/docs/handbook/jsdoc-supported-types.html) provides feedback without changing the source language.

## State and navigation

`auth/UserUiContext.jsx` holds one shared `user` value in memory. It starts as `null`, meaning anonymous. `UserUiProvider` uses `useState`; navigation and access notices read the value with `useContext` through `useUserUi`. They currently show “Not signed in” and “Secure sign-in comes first.” All six page links remain available, and protected pages keep their unavailable notices without showing account data or actions.

The context allows safe user details (`id`, `displayName`, and `role`) for display. Nothing in the application calls `setUser` yet. Only a later response from a verified server session may establish those details. This value is not authentication, and changing it cannot open the unfinished account tools or grant API access. A reload starts anonymous again. There are no API calls, browser-storage reads or writes, browser tokens, or simulated signed-in users in the application. The `api/` folder still contains only `.gitkeep`.

`SiteNavigation` keeps `isMenuOpen` in its own `useState`. `useEffect` closes it on a route change and manages Escape and desktop-resize listeners while it is open, removing them on close or unmount. `useRef` returns focus to the button after Escape or selecting the current page. Selecting another page closes the menu and moves focus to its content through `RouteFocus`. The button supports Enter and Space, reports `aria-expanded`, and points to the navigation with `aria-controls`. Closed links are hidden on phones and stay out of the tab order.

I kept ordinary functions and simple state updates. There is no complex transition that needs `useReducer`, and no expensive calculation or measured rendering issue that needs `useMemo` or `useCallback`.

Registration, BCrypt password checks, server sessions, and CSRF protection come before enabling browser sign-in. The future sign-in/current-user calls will update shared user details only after verification; sign-out or session expiry will clear them. Form fields, loading, errors, purchase request IDs, and API results will belong to their pages when those workflows exist. Later pages will use a small fetch helper, with plain arrays for history and admin lists. Account ownership and administrator permissions will still be checked by the server. Card animation is outside this MVP.

The [React architecture document](../outputs/02_Architecture/04_React_Component_Diagram.md) describes the component relationships and remaining page behavior.

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
npm run preview
```

The production build goes into `dist/`. `preview` serves that build locally. Dependencies and generated files stay out of Git; `package-lock.json` records the installed versions. Use `npm ci` for a fresh install from that lockfile.

`check:routes` uses Node's test runner and Vite's JSX loader. Its 13 checks cover route headings, one current navigation link, anonymous status, the unknown-route fallback, the home artwork labels, and unavailable pages without banking controls or credential fields.

`check:state` uses the same test runner and JSX loader, with JSDOM to mount the real components in React StrictMode. Its 7 checks cover shared user UI state without unlocking tools, anonymous remounts, navigation while browser storage is blocked, menu toggling, Escape focus, current-page selection, route and query changes, browser back/forward, desktop resizing, and listener cleanup. JSDOM is only a development dependency. It has no layout engine, so I check phone layout and keyboard behavior in the browser too.

On October 8, I checked the production preview at 1440 × 900, 390 × 844, and 320 × 844. Direct URLs and refresh worked for all six routes and an unknown nested route. All pages fit the phone widths without horizontal scrolling, with the menu closed or open. I also checked Tab, Shift+Tab, Enter, Space, Escape, visible focus, the skip link, content focus after navigation, browser back/forward, and switching between phone and desktop navigation. The build, props check, all 13 route checks, and all 7 state checks passed.

React Router uses regular URL paths. A deployed web server would need to serve `index.html` for frontend routes so direct links and refresh work. API and asset requests need their own handling. Local Vite development and preview already provide the frontend fallback.

### Local API connection

Vite forwards paths beginning with `/api` to `http://localhost:8080`, keeping the path intact. For example, `/api/accounts` goes to Spring Boot's `/api/accounts`. Future API calls can use relative paths. The current pages make no API requests.

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
