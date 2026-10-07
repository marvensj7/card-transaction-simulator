# Card simulator frontend

I built one React application with Vite and plain JavaScript/JSX. `index.html` loads `src/main.jsx`, which imports `styles.css` and renders `App.jsx`. `App` renders `pages/HomePage.jsx`, a public introduction to the fictional simulation. Plain CSS keeps the page readable on desktop and mobile, with no UI framework or inactive controls.

The `components/`, `auth/`, and `api/` folders are reserved for shared components, authentication, and API calls. They contain only `.gitkeep` files so Git retains the empty folders. Routing, sign-in, account data, purchase forms, and the 3D card are planned separately.

## Local setup

Use Node.js 20.19+ on the 20.x line, or 22.12+ on a newer line, with npm. From the repository root in PowerShell:

```powershell
cd frontend
npm install
npm run dev
```

Open the local URL printed by Vite. The home screen runs without Spring Boot or MySQL.

```powershell
npm run build
npm run preview
```

The production build goes into `dist/`. `preview` serves that build locally. Dependencies and generated files stay out of Git; `package-lock.json` records the installed versions. Use `npm ci` for a fresh install from that lockfile.
