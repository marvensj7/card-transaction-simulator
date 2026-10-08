# Credit Circuit frontend

I built one React application with Vite and plain JavaScript/JSX. `index.html` loads `src/main.jsx`, which imports `styles.css` and renders `App.jsx`. `App` renders `pages/HomePage.jsx`.

Credit Circuit is the app name; Credit Card Transaction Simulator is the formal capstone description. The opening screen pairs a large wordmark with a pale fictional card against a dark background. A lime accent and thin signal traces connect the visual identity. Request → Checks → Outcome hints at the purchase flow without revealing an approval, amount, balance change, or history.

The introduction sits beside the card on wide screens and above it on phones. The card keeps enough width for its labels on small screens, and the header and footer wrap when needed. The card shows only a masked ending and a fictional-card label. The screen has no sign-in controls, purchase actions, API requests, or animation. The layout uses ordinary CSS without a UI framework.

`components/CircuitMark.jsx` supplies the small SVG mark used in the header and on the card. A matching favicon appears beside the Credit Circuit browser title. The `auth/` and `api/` folders contain only `.gitkeep` files so Git retains the empty folders. Routing, session sign-in, account data, and purchase forms remain unfinished. Card animation is outside this MVP. Planned pages use ordinary state and a small fetch helper; history is a simple array.

## Local setup

Use Node.js 20.19+ on the 20.x line, or 22.12+ on a newer line, with npm. From the repository root in PowerShell:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://127.0.0.1:5173/`. Vite stops if port 5173 is already occupied. The home screen runs without Spring Boot or MySQL.

```powershell
npm run build
npm run preview
```

The production build goes into `dist/`. `preview` serves that build locally. Dependencies and generated files stay out of Git; `package-lock.json` records the installed versions. Use `npm ci` for a fresh install from that lockfile.

### Local API connection

Vite forwards paths beginning with `/api` to `http://localhost:8080`, keeping the path intact. For example, `/api/accounts` goes to Spring Boot's `/api/accounts`. Future API calls can use relative paths. The home page makes no API requests.

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
