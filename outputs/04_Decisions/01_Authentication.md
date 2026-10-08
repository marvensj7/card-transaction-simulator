# Decision: one stateless JWT authentication path

I replaced the unfinished session guard with Spring Security's resource-server bearer filter and Nimbus JWT encoder/decoder. HS256 uses a random key of at least 32 bytes in ignored local configuration or JWT_SECRET. Tokens contain a numeric user ID, USER/ADMIN role, issuer, audience, issue time, and expiration. Signatures, HS256, issuer, exact audience, subject/role shape, and time claims are checked before controllers run. Services still check the stored role and ownership.

React keeps the short-lived access token only in memory and sends it explicitly in Authorization. Reload and sign-out discard it. Stateless logout cannot revoke a copied token before its expiration; there is no refresh token or revocation table.

The API accepts no session cookie or Basic/form login credential. CSRF ignores only /api/** because those credentials are never attached automatically by the browser. CORS allows explicit local frontend origins without cookie credentials. Switching to cookie-based identity would require revisiting CSRF. Authentication POST requests share a bounded per-IP, per-process rate limit. It is appropriate for this one-process local demo and resets on restart.

References: [Spring JWT resource server](https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html), [Spring CSRF guidance](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html).
