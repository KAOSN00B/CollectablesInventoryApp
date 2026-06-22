# CollectOS

**A premium collectibles inventory app for Android** — track your video games, consoles, and trading cards (Magic, Pokémon, Yu-Gi-Oh!), organize them into binders, price them with live market data, and share your collection with a link.

> Built for the Mobile Development course at LaSalle College Vancouver. Package: `com.lasallecollegevancouver.gameinventoryapp` (internally branded "CollectOS").

---

## Features

- **Unified collection** — games, consoles, and TCG cards in one place, browsed as a list of **binders** you open to see contents.
- **Binders** — create named groups (e.g. "Base Set Holos", "PS2 RPGs") and drop any item into them. An **All Items** shortcut always shows your full collection.
- **TCG search & detail** — search Magic, Pokémon, and Yu-Gi-Oh! cards across three live APIs, view a card as a premium "slab on a backlit shelf", and add it (with condition, quantity, foil, and price).
- **Live pricing** — market values pulled from PriceCharting (games) and the TCG APIs (cards).
- **Smart Add** — add items by **barcode scan** (CameraX + ML Kit), **photo/OCR**, **catalog search**, or manual entry.
- **Box & card art** — game box art from RAWG, card art from Scryfall / Pokémon TCG / YGOProDeck, shown as a "digital display case" grid with shimmering skeleton loaders.
- **Wishlist** — track items you want, with a target price and a "grail" flag.
- **Dashboard** — total vault value and per-category breakdowns.
- **Share** — every collection has a public link (`/c/<code>`) anyone can view.

---

## Tech stack

**Android app**
- Kotlin, single-activity + **Navigation Component** (bottom nav: Home · Collection · Wishlist · Settings)
- View Binding, Material 3
- **Retrofit / OkHttp / Gson** for networking
- **Room** for local persistence/caching
- **Glide** for image loading
- **CameraX + ML Kit** (barcode scanning + text recognition)
- Coroutines + Lifecycle (`lifecycleScope`, `repeatOnLifecycle`)
- Splash Screen API, SwipeRefreshLayout
- minSdk 27 · targetSdk 36

**Cloud backend** (separate repo: `collectos-backend`)
- Node + TypeScript, **Express 5**
- **Prisma 7** ORM → **PostgreSQL**
- `zod` validation, `helmet`, `cors`, `express-rate-limit`
- Hosted on **Render** (web service) + **Neon** (Postgres) + **cron-job.org** (keep-alive ping)

---

## Architecture

```
Android app ──HTTPS──> Render (Express/Prisma API) ──> Neon (PostgreSQL)
     │
     └─ also calls third-party APIs directly for images & prices:
        PriceCharting · Pokémon TCG · RAWG · Scryfall · YGOProDeck
```

The backend is keyed by a **collection share code**: `POST /collections` returns a unique `publicCode`, and everything else hangs off `/collections/{code}/...` (items, wishlist, binders). The public view is `/c/{code}`.

---

## External APIs

| Service | Used for | API key required |
|---|---|---|
| **CollectOS backend** | your collection, items, binders, wishlist, catalog | — (your own server) |
| **PriceCharting** | game/console prices | ✅ `priceChartingApiKey` |
| **Pokémon TCG** | Pokémon card data + prices | ✅ `pokemonTcgApiKey` |
| **RAWG** | game box / cover art | ✅ `rawgApiKey` |
| **Scryfall** | Magic: The Gathering cards | ❌ none |
| **YGOProDeck** | Yu-Gi-Oh! cards | ❌ none |

---

## Setup

### 1. API keys
Create a `local.properties` file in the project root (it is gitignored — never commit it) and add your keys:

```properties
priceChartingApiKey=YOUR_KEY
pokemonTcgApiKey=YOUR_KEY
rawgApiKey=YOUR_KEY
```

These are injected as `BuildConfig` fields at build time. Scryfall and YGOProDeck need no key.

### 2. Point at a backend
The base URL lives in **one place** — `app/src/main/java/.../config/AppConfig.kt`:

```kotlin
const val API_BASE_URL = "https://collectablesinventoryapp.onrender.com/"
```

Swap that single line to switch environments:
- **Production:** the Render URL (must end in `/`)
- **Emulator (local backend):** `http://10.0.2.2:3000/`
- **Physical device (local backend):** `http://YOUR_LAPTOP_IP:3000/`

### 3. Build & run
Open in Android Studio and run, or from the command line:

```bash
./gradlew :app:assembleDebug
```

---

## Backend setup (collectos-backend)

The API is a separate Node/TypeScript project.

```bash
npm install
npm run db:generate      # prisma generate
npm run db:migrate       # apply migrations  (or: npm run db:push)
npm run db:seed          # load the bundled game catalog (NES → PS5, Switch, etc.)
npm run dev              # local dev server on port 3000 (tsx watch)
```

Set `DATABASE_URL` (a Neon Postgres connection string) in a `.env` file locally, or as an environment variable on Render. On startup the server enables Postgres `pg_trgm` and creates the search/foreign-key indexes automatically.

**Deploy:** Render builds with `npm install && npx prisma generate && npm run build` and starts with `npm start`. A cron-job.org ping hits `GET /health` every 14 minutes to avoid free-tier cold starts.

---

## Design

CollectOS uses an **"Obsidian Vault / Premium Digital Display Case"** theme:
- Deep charcoal surfaces (`#101418`), neon cyan accent (`#39D6D6`), gold value text
- Items shown as recessed "display case" cards with floating badges (grade slabs, game tags)
- Shimmering skeleton loaders while images load
- TCG detail view presents a card as a "slab" with a thin protective-case border on a backlit cyan glow

The palette is defined in `res/values/colors.xml` and `res/values/themes.xml`; the reusable card components live in `ui/common/`.

---

## Project structure (app)

```
app/src/main/java/.../
├── config/        AppConfig (base URL), PrefsHelper
├── data/          Room entities & database
├── network/       Retrofit services, repositories, API models
│   ├── rawg/      RAWG box-art client
│   └── tcg/       Scryfall / Pokémon / YGOProDeck clients + TcgRepository
└── ui/
    ├── common/    Reusable display-case card, skeleton, image binder
    ├── collection/ binder-first collection list
    ├── binders/   binders list + binder detail
    ├── games/ consoles/ tcg/ collectibles/   per-category screens
    ├── wishlist/  dashboard/ settings/ onboarding/
    └── smart_add/ barcode scan, photo scan, catalog search, add sheets
```

---

## Course

Mobile Development — LaSalle College Vancouver. Educational project; code is heavily commented for learning and review.
