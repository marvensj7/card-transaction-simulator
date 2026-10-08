# Credit Circuit — React components

Updated October 8, 2026.

I use one Vite application with JavaScript and JSX. `main.jsx` imports the stylesheet, creates the React root, and wraps `App` in `BrowserRouter`. `App` keeps the header, navigation, routes, and footer in one place, inside `UserUiProvider`. Each page has one main content area and a clear job.

```mermaid
flowchart TD
    MAIN[main.jsx: BrowserRouter] --> APP[App: shared shell and Routes]
    APP --> USER[UserUiProvider: user starts null]
    USER -. useUserUi .-> NAV
    USER -. useUserUi .-> ACCESS
    APP --> NAV[SiteNavigation]
    APP --> FOCUS[RouteFocus]
    APP --> HOME[HomePage]
    APP --> LOGIN[LoginPage]
    APP --> DASH[DashboardPage]
    APP --> PURCHASE[PurchasePage]
    APP --> HISTORY[TransactionsPage]
    APP --> ADMIN[AdminPage]
    APP --> MISSING[NotFoundPage]
    DASH --> ACCESS[AccessNotice]
    PURCHASE --> ACCESS
    HISTORY --> ACCESS
    ADMIN --> ACCESS
    LOGIN --> HEADING[PageHeading]
    DASH --> HEADING
    PURCHASE --> HEADING
    HISTORY --> HEADING
    ADMIN --> HEADING
    MISSING --> HEADING
    APP --> MARK[CircuitMark]
    HOME --> MARK
```

## Current pages

| Route | Component | What it shows now |
| --- | --- | --- |
| `/` | `HomePage` | Wordmark, fictional display card, and Request → Checks → Outcome teaser. |
| `/login` | `LoginPage` | Sign-in is still being built; a link returns home. |
| `/dashboard` | `DashboardPage` | Account heading and an access-unavailable notice. |
| `/purchase` | `PurchasePage` | Purchase heading and an access-unavailable notice. |
| `/transactions` | `TransactionsPage` | History heading and an access-unavailable notice. |
| `/admin` | `AdminPage` | Administration heading and an access-unavailable notice. |
| Any other path | `NotFoundPage` | Page-not-found message and a link home. |

I kept the opening screen's dark background, lime accents, large wordmark, thin signal traces, and pale fictional card. The display card shows only a masked ending and a fictional-card label. The introduction sits beside it on desktop and above it on phones. The other pages use the same colors and typography, with a simple bordered notice panel.

The four protected pages show their purpose without presenting a signed-in account. There are no balances, sample transactions, credential fields, purchase forms, refunds, or account-status controls. The pages make no API calls and own no account state yet. The visible navigation links are page destinations; they do not grant account access.

## Shared pieces

| Component | Job and props |
| --- | --- |
| `SiteNavigation` | Six explicit `NavLink` links, current-page marking, shared user status, and a local phone menu. No props. |
| `UserUiProvider` | Hold safe user details in memory, starting with `null`. Required `children` contains the app. |
| `PageHeading` | Display `eyebrow`, `title`, and `description`, all required strings. |
| `AccessNotice` | Keep account tools unavailable and show a sign-in link while anonymous. Required `children` supplies the page's explanation. |
| `CircuitMark` | Draw the decorative SVG mark in the header and on the home card. No props. |
| `RouteFocus` | Update the browser title, move focus to the main content after a route change, and return to the top. No props or visible UI. |

`App` has the shared header and footer directly in its JSX. I kept each page's main content explicit rather than adding a layout wrapper. The navigation shows all six destinations on desktop. At 50rem and below, a Menu button opens the same links in a compact area under the header. Keyboard users can see link and button focus, skip the header, and reach the page content after navigation. The initial page load keeps the normal tab order.

The components that accept props define them in JSDoc comments. `AccessNotice` and `UserUiProvider` accept React content as `children`; `PageHeading` accepts three strings. `npm run check:props` runs the JavaScript checker through `jsconfig.json` and checks the page callers and user-state types too. The files remain JSX. There is no generic form or table system.

## State in the interface

I keep one shared value in `auth/UserUiContext.jsx`: `user`, initially `null`. `UserUiProvider` uses `useState`, and `useUserUi` reads it with `useContext`. Navigation and `AccessNotice` use that same value, so they agree about the displayed sign-in status. All six navigation links remain page destinations, including for an anonymous visitor. Opening a protected URL keeps its heading and access-unavailable notice; it does not redirect to a working login or show account tools.

The context can hold only the safe user fields `id`, `displayName`, and `role`. Nothing in the application calls `setUser` today. Later, a verified server-session response may supply those details. Changing this UI value cannot authorize an API request or open the unfinished account tools. The backend still checks the server session, stored role, and account ownership. A reload discards the value and starts anonymous. I do not save user details, credentials, or session identifiers in browser storage.

`SiteNavigation` owns one local `useState` value, `isMenuOpen`. Its `useEffect` closes the menu when the route changes, including browser back/forward. A second effect listens for Escape and a switch to desktop while the menu is open, then removes both listeners when it closes or unmounts. `useRef` keeps the button available for focus after Escape or selecting the current page. Selecting a different page closes the menu and lets `RouteFocus` move focus to the content. The closed phone menu is hidden from both display and keyboard navigation. It uses a normal button with `aria-expanded` and `aria-controls` and ordinary links, without a focus trap.

The state transitions are simple assignments and one menu toggle, so I do not use `useReducer`. There is no expensive calculation or measured rendering issue that needs `useMemo` or `useCallback`; the event handlers are ordinary functions. I will reconsider those hooks if a later workflow needs them.

## Remaining behavior

Registration and sign-in still need implementation. Sign-in will check a BCrypt password and establish a server session. The browser will carry the session cookie, with CSRF protection included before browser sign-in is enabled. A verified sign-in/current-user response will update the shared UI details; sign-out or an expired session will clear them. There is no JWT state or browser password store. `AccessNotice` describes unavailable functionality; it is not an authorization check. Future account access still requires server role and ownership checks.

| Page | Planned behavior after secure sign-in |
| --- | --- |
| Login | Registration/sign-in fields, loading, errors, and session handling. |
| Dashboard | Own account summary and masked fictional card. |
| Purchase | A labeled form with eight request fields, one request ID per submission, loading, and the returned outcome. |
| Transactions | A newest-first history array and one full-refund action for an eligible purchase. |
| Admin | Account/activity arrays and ACTIVE/FROZEN controls for an administrator. |

Each page will own its fields, results, loading, errors, and ordinary `useState` values when its form and API calls exist. Purchase request IDs and retry results will stay on the purchase page. A small API helper will call `fetch` and read the server's `message` on an HTTP error. History and admin lists will remain plain arrays without paging. A shared safe response shape can serve customer and admin views. Reusable UI will be extracted when more than one page needs it.

The [API design](03_API_Design.md) defines the planned request and response fields. Money will display with two decimal places, while approval/refund math stays in Java. A successful HTTP 200 may contain a DECLINED financial result. Ownership, role checks, duplicate request IDs, full-refund rules, and all-or-nothing balance/history writes stay on the server.

Card animation, global state libraries, caching layers, and generic form/table engines are outside this MVP.

## Verification

`npm run build`, `npm run check:props`, all 13 `npm run check:routes` checks, and all 7 `npm run check:state` checks passed. The route checks render the real JSX with a memory router and cover matching, current links, anonymous status, the fallback, home labels, and unavailable pages without banking controls. The state checks mount the real components in JSDOM with React StrictMode. They cover shared UI updates without unlocking tools, anonymous remounts, blocked browser storage, link navigation, menu toggling, Escape focus, same-page selection, route changes, back/forward, desktop resizing, and listener cleanup. JSDOM is a development dependency and does not test CSS layout.

I also checked the production preview in the browser at 1440 × 900, 390 × 844, and 320 × 844. All six direct URLs and refresh worked, along with an unknown nested route and its home link. Every page fit both phone widths with the menu closed or open. Tab, Shift+Tab, Enter, Space, Escape, the skip link, visible focus, route-change focus, page titles, and browser back/forward worked. The menu closed on selection and route changes, and switching to desktop restored the full navigation. Local Vite serves the frontend fallback; a deployed server would also need to return `index.html` for frontend routes.
