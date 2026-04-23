## Contract Audit

**GET /api/trips**
OK: Ktor route at correct path/method; React `getTrips()` calls with correct URLSearchParams; response shape `Trip[]` (including `vehicleName`) matches frontend destructuring in `TripRow`.

**GET /api/trips/:id**
OK: Ktor route implemented; React `getTrip(id)` calls correct path; `Trip` shape matches.

**POST /api/trips**
MISMATCH: POST /api/trips — Backend returns `HttpStatusCode.OK` (200) instead of the spec-required `HttpStatusCode.Created` (201). Frontend `createTrip()` does not inspect the status code so it passes today, but the API spec requires 201 and standards-compliant HTTP clients (curl, Postman, test harnesses) will treat it as incorrect.

**PUT /api/trips/:id**
OK: Returns 200 with updated trip; 404 for missing trip or missing vehicle; 400 for invalid fields.

**DELETE /api/trips/:id**
OK: Returns 204 on success; 404 when record not found; `SqlExpressionBuilder.eq` imported correctly.

**GET /api/vehicles**
OK: Returns sorted vehicle list; React `getVehicles()` calls correct path; `Vehicle` shape matches `VehicleCard` props.

**POST /api/vehicles**
OK: Returns 201 with created vehicle; React `createVehicle()` sends correct body; `Vehicle` response shape matches.

**GET /api/summary**
OK: Ktor route at `/api/summary`; React `getSummary(month)` sends correct query param; `SummaryResponse` shape (`month`, `totalMiles`, `byVehicle`) matches what `SummaryTable` destructures.

---

## Frontend Review

**App** — exists; defines all three routes (`/`, `/vehicles`, `/summary`); renders `NavBar`. ✓

**NavBar** — exists; uses `NavLink` with `end` prop on `/` to avoid matching all routes; active styling applied. ✓

**TripListPage** — exists; loading/error states present; filter, table, modal all wired correctly; delete with confirmation. ✓

**TripFilterBar** — exists; PropTypes defined; vehicle dropdown, date range inputs, clear button all implemented. ✓

**TripTable** — exists; PropTypes defined; empty-state message when no trips. ✓

**TripRow** — exists; PropTypes defined; vehicle pill badge, right-aligned numeric columns, Edit/Delete buttons. ✓

**AddTripButton** — exists; PropTypes defined; opens `TripFormModal` in create mode. ✓

**TripFormModal** — exists; PropTypes defined; create/edit mode detection; client-side validation for odometer order; loading/error state during save. ✓

**VehiclePage** — exists; loading/error states; passes `loadVehicles` as `onCreated` to `AddVehicleForm` so the list refreshes after adding. ✓

**VehicleList** — exists; PropTypes defined; empty-state message. ✓

**VehicleCard** — exists; PropTypes defined; conditionally renders license plate only when non-empty. ✓

**AddVehicleForm** — exists; PropTypes defined; resets fields after successful submit; error state present. ✓

**SummaryPage** — exists; loading/error states present; passes all required props down. ✓

**MonthPicker** — exists; PropTypes defined; controlled `<input type="month">`. ✓

**SummaryTable** — exists; PropTypes defined; empty-state message for months with no data; grand total footer. ✓

**Overall frontend quality: 5/5** — All 15 components are present, every component handles loading/error states where applicable, PropTypes are defined throughout, and `index.html` is included so Vite starts correctly.

---

## Backend Review

**GET /api/trips** — Fully implemented; INNER JOIN to vehicles for `vehicleName`; optional filters applied via `andWhere`; ordered by date DESC; try/catch present. ✓

**GET /api/trips/{id}** — Fully implemented; JOIN for vehicleName; 404 on missing; 400 on non-integer id; try/catch. ✓

**POST /api/trips** — Implemented but returns HTTP 200 instead of 201; vehicle existence check returns 404; miles computed correctly; `insert { }` used; `stmt[Trips.id].value` retrieves generated id. ✗ (wrong status code)

**PUT /api/trips/{id}** — Fully implemented; checks vehicle existence and trip existence separately; returns correct 404 messages; miles recomputed on update. ✓

**DELETE /api/trips/{id}** — Fully implemented; checks `deleteWhere` count; returns 204/404 correctly. ✓

**GET /api/vehicles** — Fully implemented; alphabetically sorted; 201 on create. ✓

**POST /api/vehicles** — Fully implemented; returns 201; trims whitespace; try/catch present. ✓

**GET /api/summary** — Fully implemented; INNER JOIN for vehicle names; regex validates YYYY-MM; groups in memory; `totalMiles` computed correctly. ✓

No raw SQL string interpolation — all queries use Exposed parameterised builders. No SQL injection risk.

**Overall backend quality: 4/5** — Complete, well-structured implementation with correct error handling; the single defect is the wrong HTTP status code on POST /api/trips.

---

## Priority 1 — Fix Before Running

```
File: backend/src/main/kotlin/com/app/routes/TripRoutes.kt
Issue: POST /api/trips returns HTTP 200 (OK) instead of HTTP 201 (Created) as required by the API spec.
Fix: Change `call.respond(HttpStatusCode.OK, created)` to `call.respond(HttpStatusCode.Created, created)` in the POST handler (the line after the `// BUG:` comment).
```

---

## Priority 2 — Fix Before Shipping

```
File: backend/src/main/kotlin/com/app/routes/TripRoutes.kt
Issue: The summary endpoint loads all matching trip rows into JVM heap then groups in Kotlin. For large datasets this is inefficient.
Fix: Use Exposed's groupBy + Sum aggregate to perform the grouping at the database level.
```

```
File: backend/src/main/kotlin/com/app/DatabaseFactory.kt
Issue: Seed trips are all dated 2026-04-01 (a hardcoded past date). If the app is used in a different month the summary page will show no data by default.
Fix: Compute the current month dynamically: `java.time.LocalDate.now().withDayOfMonth(1).toString()`.
```

---

## Priority 3 — Nice to Have

- `TripFormModal`: Show computed miles preview as the user types odometer values (e.g. "150 miles").
- `TripFilterBar`: Debounce the date inputs to avoid firing API requests on every keystroke.
- `SummaryTable`: Add a simple bar-chart visualization (CSS-only) similar to the expense tracker design.
- Backend: Add a `PRAGMA foreign_keys = ON` statement in `DatabaseFactory.init()` — SQLite disables foreign key enforcement by default.
- Backend: Validate that `date` strings are well-formed ISO dates (YYYY-MM-DD) in the POST/PUT handlers.

---

## Quick Win

The missing `PRAGMA foreign_keys = ON` means SQLite silently accepts trips referencing non-existent vehicles, undermining the `404` check. Takes 30 seconds to add.

**Before** (`DatabaseFactory.kt`):
```kotlin
Database.connect("jdbc:sqlite:data/app.db", driver = "org.sqlite.JDBC")
transaction {
    SchemaUtils.createMissingTablesAndColumns(Vehicles, Trips)
```

**After**:
```kotlin
Database.connect("jdbc:sqlite:data/app.db", driver = "org.sqlite.JDBC")
transaction {
    exec("PRAGMA foreign_keys = ON")
    SchemaUtils.createMissingTablesAndColumns(Vehicles, Trips)
```
