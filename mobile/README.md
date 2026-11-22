# Paperless Mobile

Expo (React Native, TypeScript strict) smartphone client for the Paperless
backend (US-34). Log in with Keycloak, browse, search, upload and tag documents,
read their generated summaries.

The API has no endpoint that returns the original file, so the app shows
metadata, the summary and the tag, but cannot display or download the PDF.

## Prerequisites

- Node.js 20+ and npm
- Backend running: `docker compose up` in the repository root
- Android Studio emulator **or** Xcode iOS simulator (see the networking note)

## Install and run

```bash
cd mobile
npm install
npx expo start          # then press `a` (Android) or `i` (iOS)
# or: npm run android / npm run ios
```

Camera and the OAuth redirect (`paperlessmobile://`) need a **development
build**, not Expo Go: `npx expo run:android` / `npx expo run:ios`. Expo Go
works for a quick look because `exp://*` is also registered as a redirect URI.

### Android emulator

Keycloak is configured with `KC_HOSTNAME=http://localhost:9445`, so its login
page always points back to `localhost`. Inside the emulator `localhost` is the
emulator itself, so forward the host ports instead of using `10.0.2.2`:

```bash
adb reverse tcp:8081 tcp:8081   # REST API
adb reverse tcp:9445 tcp:9445   # Keycloak
```

### iOS simulator

Nothing to do, `localhost` reaches the host.

## The 127.0.0.1 constraint and physical devices

Every port in `docker-compose.yaml` and `compose-infra.yaml` is bound to
`127.0.0.1`, so a physical phone cannot reach the API or Keycloak. To use one,
the team must change those bindings to `0.0.0.0` (not done here) and start the
app with the LAN IP of the host:

```bash
EXPO_PUBLIC_API_URL=http://192.168.1.20:8081 \
EXPO_PUBLIC_KEYCLOAK_URL=http://192.168.1.20:9445 npx expo start
```

Both values are read once in `app.config.ts`. Keycloak's `KC_HOSTNAME` would
also have to match that address, otherwise the login form posts to `localhost`.

## Keycloak client

`paperless-realm.json` contains the public client `paperless-mobile`
(authorization code + PKCE `S256`, no secret, redirect URIs
`paperlessmobile://*` and `exp://*`).

**A realm import does not update an existing realm.** Either recreate the
Keycloak volume (`docker compose down -v`, **this also wipes all documents**)
or add the client by hand at <http://localhost:9445> (admin console, realm
`paperless` → Clients → Create client: ID `paperless-mobile`, client
authentication off, standard flow on, valid redirect URIs above, advanced →
PKCE method `S256`).

Test user: `test` / `test`.

## Layout

```
app/              expo-router routes, one-line re-exports of the screens
src/screens/      screens (no fetch calls)
src/services/     http.ts (bearer token, ApiError) + one module per resource
src/models/       interfaces mirroring the API JSON exactly
src/auth/         secure token storage, refresh, auth context
src/components/   presentational components
__tests__/        Jest tests
```

Title length, maximum file size and allowed content types are never hardcoded;
they come from `GET /api/v1/config/validation`.

## Tests

```bash
npm test            # Jest + React Native Testing Library, with coverage
npm run typecheck
```

Last run: 93 tests passing, coverage 96.6 % statements / 87.4 % branches /
93.0 % functions / 98.1 % lines.

## AI tooling

The app was generated with Claude Code (model Claude Sonnet 5.5, 2026-11-22)
from the prompt in [`../_docs/mobile-app-prompt.md`](../_docs/mobile-app-prompt.md).

## Verification status

Checked: TypeScript strict compile, all unit/screen tests, Metro export for
iOS and Android, and the multipart upload body (the `metadata` part carries
`Content-Type: application/json`, asserted against React Native's real
`FormData`). **Not yet verified against a running backend**: login, and a real
upload returning `201`. Run through the checklist in the prompt on an emulator
before relying on it.
