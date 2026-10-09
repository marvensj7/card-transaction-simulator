# React component design

```mermaid
flowchart TD
    App --> UserUiProvider
    UserUiProvider --> SiteNavigation
    UserUiProvider --> RouteFocus
    UserUiProvider --> HomePage
    UserUiProvider --> LoginPage
    UserUiProvider --> ProtectedRoute
    ProtectedRoute --> DashboardPage
    ProtectedRoute --> PurchasePage
    ProtectedRoute --> TransactionsPage
    ProtectedRoute --> AdminPage
    HomePage --> FlippableCard
    DashboardPage --> FlippableCard
    PurchasePage --> FlippableCard
    DashboardPage --> AccountSummary
    PurchasePage --> AccountSummary
    LoginPage --> Input
    PurchasePage --> Input
    TransactionsPage --> TransactionTable
    AdminPage --> TransactionTable
    TransactionTable --> Table
    TransactionsPage --> ConfirmModal
    AdminPage --> ConfirmModal
    TransactionsPage --> Pagination
    AdminPage --> Pagination
```

Button supplies pending/disabled feedback, Input connects labels/errors with useId, Card provides section headings, Table provides caption/headers and a focusable scroll region, ConfirmModal uses native dialog, and Loading supplies a spinner/skeleton. These small pieces serve current workflows. FlippableCard uses one local useState boolean and a native button to flip CSS card faces on home, dashboard, and purchase. It receives only the safe DemoCard response; the home teaser uses a masked placeholder. Hidden faces are excluded from assistive technology, and reduced-motion preferences switch faces without rotation. Purchase form fields remain ordinary independent inputs.

| React item | Use and reason |
| --- | --- |
| useState | Controlled forms, page/data/errors/pending/selected confirmation. |
| useEffect | API loading, stale-update cleanup, navigation/resize, expiration. |
| Context | Share safe user state and actions with pages/navigation. |
| useReducer | signedIn/signedOut/expired transitions keep user/expiration/notice consistent. |
| useCallback | Stable auth handlers avoid repeatedly configuring token readers/expiry effects. |
| useMemo | Memoized Context value avoids a new shared identity object on unrelated renders. |
| useRef | Memory token, in-flight guard, stable uncertain request, dialog/focus elements. |
| JSX/JSDoc | Ordinary JSX with checked prop/DTO shapes. |

Pages call API functions directly. fetchJson attaches protected bearer headers and omits cookies; protected 401 responses expire UI identity. Route guards redirect signed-out users/restrict wrong roles, while services enforce actual permissions.

## Responsibilities

Pages own their form values, request state, and event handlers. They call named functions in creditCircuitApi rather than building fetch requests. fetchJson handles HTTP, token attachment, JSON parsing, and safe errors. UserUiContext owns the token and sign-in transitions; it supplies a token reader and expiration callback to fetchJson.

Shared components receive data and callbacks through props. ConfirmModal manages its dialog and focus, while its page decides which refund or status change to request. AccountSummary and TransactionTable display API data. format.js provides currency and reason-label formatting, and types.js describes shared JSON shapes for JSDoc checks. Neither file makes requests. These two small files currently live beside the API modules because the pages share them.

Page components contain both JSX and the local interaction logic for their workflow. This keeps each page readable in one file. Financial rules and database access remain on the backend.

Purchase/refund actions keep the original UUID while uncertain and guard repeated clicks. Declines are saved results. Forms have labeled errors/loading/recovery. Dialogs focus Cancel, support Escape, and restore focus. Lists show empty states and bounded navigation. Wide tables scroll within a keyboard-focusable region.

[Browser evidence](../03_Verification/browser-results.json) distinguishes real MySQL workflows from injected lost responses, expiration, failures, and empty states. It checks desktop/tablet/phone widths, keyboard focus/navigation, and automated accessibility rules. It does not replace personal screen-reader or cross-browser review.
