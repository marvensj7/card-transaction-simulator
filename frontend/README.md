# Credit Circuit frontend

I built one React application with Vite and plain JavaScript/JSX. `index.html` loads `src/main.jsx`, which imports `styles.css` and wraps `App.jsx` in React Router's `BrowserRouter`. `App` contains the shared header, navigation, route list, and footer. Each route renders one page from `src/pages/`.

Credit Circuit is the app name; Credit Card Transaction Simulator is the formal capstone description. The opening screen pairs a large wordmark with a pale fictional card against a dark background. A lime accent and thin signal traces connect the visual identity. Request → Checks → Outcome hints at the purchase flow without revealing an approval, amount, balance change, or history.

The introduction sits beside the card on wide screens and above it on phones. The card keeps enough width for its labels on small screens, and the navigation and footer wrap when needed. The card shows only a masked ending and a fictional-card label. The layout uses ordinary CSS without a UI framework.

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

`PageHeading` and `AccessNotice` define their props in JSDoc comments. `npm run check:props` checks those props and their JSX callers through `jsconfig.json`. TypeScript is only a development checker; the application stays in `.js` and `.jsx` files. [JSDoc checking](https://www.typescriptlang.org/docs/handbook/jsdoc-supported-types.html) provides feedback without changing the source language.

The `auth/` and `api/` folders still contain only `.gitkeep` files. There are no API calls, browser tokens, or simulated signed-in users. Registration, BCrypt password checks, server sessions, and CSRF protection come before enabling browser sign-in. Later pages will use ordinary state and a small fetch helper, with plain arrays for history and admin lists. Account ownership and administrator permissions will still be checked by the server. Card animation is outside this MVP.

The [React architecture document](../outputs/02_Architecture/04_React_Component_Diagram.md) describes the component relationships and remaining page behavior.

## Local setup

Use Node.js 20.19+ on the 20.x line, or 22.12+ on a newer line, with npm. From the repository root in PowerShell:

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
npm run preview
```

The production build goes into `dist/`. `preview` serves that build locally. Dependencies and generated files stay out of Git; `package-lock.json` records the installed versions. Use `npm ci` for a fresh install from that lockfile.

`check:routes` uses Node's test runner and Vite's JSX loader. Its 13 checks cover route headings, one current navigation link, the unknown-route fallback, the home artwork labels, and unavailable pages without banking controls or credential fields. These render checks do not replace browser checks.

On October 8, I checked the production preview at 1440 × 900, 390 × 844, and 320 × 844. Direct URLs and refresh worked for all six routes and an unknown nested route. All pages fit the phone widths without horizontal scrolling. I also checked Tab, Shift+Tab, Enter, visible focus, the skip link, content focus after navigation, and browser back/forward. The build, props check, and all 13 route checks passed.

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
