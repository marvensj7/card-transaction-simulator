# Local run and demo guide

This guide covers the local demonstration approved by my instructor in place of AWS deployment. It uses fictional cards and balances only. No cloud service, pipeline, or monitoring stack is needed.

## 1. Database and private settings

Install JDK 17, Node compatible with frontend/package.json, and MySQL 8.0.16+. Set JAVA_HOME. Run sql/01_schema.sql and sql/02_seed.sql in order using MySQL Workbench or a private MySQL login path. See the [SQL guide](../../sql/README.md). The application login needs SELECT/INSERT/UPDATE/DELETE; a separate setup login can create tables.

Create ignored backend/src/main/resources/application-local.properties locally. If it already exists, edit it without overwriting needed settings. Use these keys with private local values:

```properties
spring.datasource.username=<private local database user>
spring.datasource.password=<private local database password>
```

The default JDBC URL selects card_transaction_simulator and UTC time. A different DB_URL must preserve UTC options. Tracked application.properties uses placeholders. Never paste the local file into a report or commit it.

From the repository root, generate a 32-byte signing key directly into that ignored file without printing it:

```powershell
$localConfig = Join-Path (Get-Location) 'backend/src/main/resources/application-local.properties'
if (!(Select-String -LiteralPath $localConfig -Pattern '^app\.jwt\.secret=' -Quiet)) {
    $keyBytes = New-Object byte[] 32
    $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $random.GetBytes($keyBytes)
    Add-Content -LiteralPath $localConfig -Value ('app.jwt.secret=' + [Convert]::ToBase64String($keyBytes))
    $random.Dispose()
    [Array]::Clear($keyBytes, 0, $keyBytes.Length)
}
```

Environment alternatives are DB_URL, DB_USERNAME, DB_PASSWORD, and JWT_SECRET. JWT_SECRET must be random Base64 decoding to at least 32 bytes. Use private local environment settings rather than command literals or checked-in .env files. Changing the key invalidates outstanding tokens. Never persist an issued access token.

## 2. Start the application

From backend:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local" "-Dspring-boot.run.arguments=--server.address=127.0.0.1"
```

From frontend in another terminal:

```powershell
npm ci
npm run dev
```

Open [Credit Circuit](http://127.0.0.1:5173). Vite proxies /api to Spring Boot on 8080. To change the API target, copy frontend/.env.example to ignored .env.local and edit API_PROXY_TARGET. Keep backend CORS origins consistent if changing the frontend port. Keep this HTTP classroom demo on loopback.

Register a fictional customer in the browser using a password used nowhere else. Registration supplies $1,000 available credit and an assigned fictional card. No existing seed password is published in the repository.

## 3. Prepare private administrator access

Public registration always creates USER. To create/reset a local demo ADMIN, set CAPSTONE_ADMIN_EMAIL to a fictional @example.test address, then run the helper from frontend with a privately entered password:

```powershell
$env:CAPSTONE_ADMIN_EMAIL = 'my-admin@example.test'
$demoSecret = Read-Host 'Private demo admin password' -AsSecureString
$demoPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($demoSecret)
try {
    $env:CAPSTONE_ADMIN_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($demoPointer)
    npm run demo:admin
} finally {
    Remove-Item Env:CAPSTONE_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($demoPointer)
    $demoSecret.Dispose()
}
```

The helper hashes with BCrypt and never prints the password/hash. It creates ADMIN without an account or resets only an existing ADMIN password. It refuses to change a customer into ADMIN. This is private database setup, not an application API. Keep both demo sign-ins private and provide them to the instructor privately if requested.

## 4. Verify

From backend:

```powershell
.\mvnw.cmd verify -Pmysql-verification "-Dspring.profiles.active=local"
```

If OneDrive prevents generated-file cleanup, add `"-Dcapstone.build.directory=C:/Users/marve/.cache/credit-circuit-build"`. Use that same output path for SonarQube binaries/reports. JaCoCo verify enforces at least 70% Java line coverage. MySQL tests remove only their own fixtures and can advance IDs.

From frontend:

```powershell
npm run check:props
npm run check:api
npm run check:routes
npm run check:state
node --test checks/adminSetup.test.mjs
npm run build
npx playwright install chromium
npm run check:browser
```

With the real backend/MySQL still running, wait at least one minute after browser authentication attempts, then run `npm run check:postman`. The exported collection has blank credentials/tokens. The helper supplies temporary private variables in memory and cleans up generated rows. It exports sanitized CLI results, not active environments or raw responses. The final test intentionally consumes the authentication rate-limit window. Postman may state that a login is needed for publishing cloud results; local execution does not publish them.

The [verification checklist](../03_Verification/01_Completion_Checklist.md) links reports and explains injected browser failures/empty states. Unit mocks do not prove database transactions; the MySQL tests and real HTTP/browser runs supply that evidence.

## 5. SonarQube locally

Use a supported SonarQube Community Build with Java 21 and the official SonarScanner CLI. Bind the local server to 127.0.0.1. Create a project and analysis token through the local UI, keep SONAR_TOKEN in a private process environment, and run sonar-scanner from this repository root. sonar-project.properties includes Java and React and imports JaCoCo/LCOV. If using external Maven output, override sonar.java.binaries, sonar.java.test.binaries, sonar.coverage.jacoco.xmlReportPaths, and sonar.junit.reportPaths accordingly. Supply dependency jars through sonar.java.libraries for Java type analysis.

Inspect the processed quality gate and all critical/major issues; a successful scanner upload alone is insufficient. Recheck actual code changes. Preserve sanitized result JSON/logs and justified security/accessibility reviews. Never substitute another tool for SonarQube. The [quality report](../03_Verification/02_Quality_Report.md) records the actual setup/results.

## 6. Demonstration sequence

1. Show Home's fictional-card notice and Request/Checks/Outcome teaser.
2. Register/sign in as customer. Show the $1,000 summary and masked card.
3. Use 4242 repeated four times, the displayed expiry, a fictional three-digit code, a fictional merchant, and $50. Show approval, outstanding $50, available $950.
4. Submit $2,000 with a new request. Show the saved insufficient-credit decline and unchanged balance.
5. Open history. Confirm one full $50 refund. Show original/refund rows and restored available credit. The original cannot be refunded again.
6. Sign out and sign in privately as ADMIN. Find the customer account and freeze it. Show paged activity.
7. Sign in as customer and show a frozen-account decline. An eligible original purchase can still be refunded. Reactivate through ADMIN for the next demonstration.
8. Show the Postman identical-retry assertions and MySQL concurrency/rollback tests. The browser runner also confirms lost-response retries after real saves.
9. Show measured Java coverage and actual SonarQube results. Explain the CSRF bearer-transport review and keyboard table region.

## 7. Recovery and limits

After reload or 15-minute expiry, sign in again. Sign-out clears this browser's token; a copied token remains valid until expiry. There is no refresh/revocation infrastructure. Keep an uncertain purchase/refund page open and retry its original request. If the page was left/reloaded, inspect history before creating another purchase.

If login returns 429, wait one minute. If database startup fails, check the service/schema/private settings without printing credentials. Avoid deleting financial history to reset a demo; use a new fictional customer. The helper/tests remove only their generated fixtures. Do not expose this local HTTP demonstration on a public network.

Official references: [Spring JWT](https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html), [SonarScanner](https://docs.sonarsource.com/sonarqube-community-build/analyzing-source-code/scanners/sonarscanner), [Postman local collection runs](https://learning.postman.com/docs/postman-cli/postman-cli-run-collection).
