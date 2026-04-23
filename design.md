## API Spec

### GET /api/trips
- **Purpose:** Retrieve all trips, optionally filtered by vehicle or date range
- **Query params:** `vehicleId` (integer, optional), `from` (ISO date YYYY-MM-DD, optional), `to` (ISO date YYYY-MM-DD, optional)
- **Request body:** None
- **Success response:** `200 OK`
  ```json
  [
    {
      "id": 1,
      "vehicleId": 1,
      "vehicleName": "Work Truck",
      "date": "2026-04-20",
      "startOdometer": 10200,
      "endOdometer": 10350,
      "miles": 150,
      "purpose": "Client site visit",
      "notes": ""
    }
  ]
  ```
- **Error responses:**
  - `500 Internal Server Error` — database failure

### GET /api/trips/:id
- **Purpose:** Retrieve a single trip by ID
- **Request body:** None
- **Success response:** `200 OK` — same shape as above (single object)
- **Error responses:**
  - `404 Not Found` — no trip with that ID
  - `400 Bad Request` — ID is not a valid integer

### POST /api/trips
- **Purpose:** Create a new trip
- **Request body:**
  ```json
  {
    "vehicleId": 1,
    "date": "2026-04-20",
    "startOdometer": 10200,
    "endOdometer": 10350,
    "purpose": "Client site visit",
    "notes": ""
  }
  ```
  - `vehicleId`: integer, required
  - `date`: string (ISO date YYYY-MM-DD), required
  - `startOdometer`: number, required, >= 0
  - `endOdometer`: number, required, must be > startOdometer
  - `purpose`: string, required, non-empty
  - `notes`: string, optional
- **Success response:** `201 Created` — full trip object including computed `miles` and `vehicleName`
- **Error responses:**
  - `400 Bad Request` — missing/invalid fields or endOdometer <= startOdometer
  - `404 Not Found` — vehicleId does not exist
  - `500 Internal Server Error` — database failure

### PUT /api/trips/:id
- **Purpose:** Update an existing trip
- **Request body:** same shape as POST
- **Success response:** `200 OK` — updated trip object
- **Error responses:**
  - `404 Not Found` — no trip with that ID, or vehicleId does not exist
  - `400 Bad Request` — missing/invalid fields
  - `500 Internal Server Error` — database failure

### DELETE /api/trips/:id
- **Purpose:** Delete a trip by ID
- **Request body:** None
- **Success response:** `204 No Content`
- **Error responses:**
  - `404 Not Found` — no trip with that ID
  - `500 Internal Server Error` — database failure

### GET /api/vehicles
- **Purpose:** Retrieve all vehicles
- **Request body:** None
- **Success response:** `200 OK`
  ```json
  [
    { "id": 1, "name": "Work Truck", "licensePlate": "ABC-1234" }
  ]
  ```
- **Error responses:**
  - `500 Internal Server Error` — database failure

### POST /api/vehicles
- **Purpose:** Create a new vehicle
- **Request body:**
  ```json
  { "name": "Work Truck", "licensePlate": "ABC-1234" }
  ```
  - `name`: string, required, non-empty
  - `licensePlate`: string, optional
- **Success response:** `201 Created` — full vehicle object
- **Error responses:**
  - `400 Bad Request` — missing/invalid fields
  - `500 Internal Server Error` — database failure

### GET /api/summary
- **Purpose:** Retrieve total miles per vehicle for a given month
- **Query params:** `month` (YYYY-MM, required)
- **Request body:** None
- **Success response:** `200 OK`
  ```json
  {
    "month": "2026-04",
    "totalMiles": 840,
    "byVehicle": [
      { "vehicleId": 1, "vehicleName": "Work Truck", "miles": 620 },
      { "vehicleId": 2, "vehicleName": "Personal Car", "miles": 220 }
    ]
  }
  ```
- **Error responses:**
  - `400 Bad Request` — missing or malformed `month` param
  - `500 Internal Server Error` — database failure

---

## DB Schema

### Table: `vehicles`

| Column       | SQLite Type | Constraints                        |
|--------------|-------------|------------------------------------|
| id           | INTEGER     | PRIMARY KEY AUTOINCREMENT          |
| name         | TEXT        | NOT NULL                           |
| license_plate| TEXT        | DEFAULT ''                         |
| created_at   | TEXT        | NOT NULL DEFAULT (datetime('now')) |

### Table: `trips`

| Column          | SQLite Type | Constraints                        |
|-----------------|-------------|------------------------------------|
| id              | INTEGER     | PRIMARY KEY AUTOINCREMENT          |
| vehicle_id      | INTEGER     | NOT NULL, FOREIGN KEY → vehicles(id)|
| date            | TEXT        | NOT NULL (YYYY-MM-DD)              |
| start_odometer  | REAL        | NOT NULL, CHECK(start_odometer >= 0)|
| end_odometer    | REAL        | NOT NULL                           |
| miles           | REAL        | NOT NULL (computed: end - start)   |
| purpose         | TEXT        | NOT NULL                           |
| notes           | TEXT        | NOT NULL DEFAULT ''                |
| created_at      | TEXT        | NOT NULL DEFAULT (datetime('now')) |

- `miles` is stored (not calculated at query time) for simpler aggregation
- `end_odometer > start_odometer` enforced in application logic

---

## Component Tree

### `App`
- **Route:** all routes (shell/layout)
- **Props:** none
- **Data:** none
- **API calls:** none
- **Children:** `NavBar`, `<Routes>` containing page components

---

### `NavBar`
- **Route:** persistent
- **Props:** none
- **Data:** none
- **API calls:** none
- **Children:** links to `/`, `/vehicles`, `/summary`

---

### `TripListPage`
- **Route:** `/`
- **Props:** none
- **Data:** fetches all trips; fetches vehicles for filter dropdown
- **API calls:**
  - `GET /api/trips` (with optional vehicleId, from, to filters)
  - `GET /api/vehicles`
- **Children:** `TripFilterBar`, `TripTable`, `AddTripButton`

---

### `TripFilterBar`
- **Route:** `/` (inside `TripListPage`)
- **Props:**
  - `vehicles: Vehicle[]`
  - `vehicleId: string`
  - `fromDate: string`
  - `toDate: string`
  - `onChange: (filters) => void`
- **Data:** none (props only)
- **API calls:** none
- **Children:** none

---

### `TripTable`
- **Route:** `/` (inside `TripListPage`)
- **Props:**
  - `trips: Trip[]`
  - `onEdit: (trip: Trip) => void`
  - `onDelete: (id: number) => void`
- **Data:** receives trips via props
- **API calls:** none
- **Children:** `TripRow` (one per trip)

---

### `TripRow`
- **Route:** `/` (inside `TripTable`)
- **Props:**
  - `trip: Trip`
  - `onEdit: (trip: Trip) => void`
  - `onDelete: (id: number) => void`
- **Data:** receives trip via props
- **API calls:** none
- **Children:** none

---

### `AddTripButton`
- **Route:** `/` (inside `TripListPage`)
- **Props:**
  - `vehicles: Vehicle[]`
  - `onCreated: () => void`
- **Data:** none
- **API calls:** none
- **Children:** triggers `TripFormModal` when clicked

---

### `TripFormModal`
- **Route:** `/` (modal overlay)
- **Props:**
  - `trip: Trip | null` (null = create mode)
  - `vehicles: Vehicle[]`
  - `onSave: () => void`
  - `onClose: () => void`
- **Data:** controlled form state
- **API calls:**
  - `POST /api/trips` (create mode)
  - `PUT /api/trips/:id` (edit mode)
- **Children:** none

---

### `VehiclePage`
- **Route:** `/vehicles`
- **Props:** none
- **Data:** fetches all vehicles
- **API calls:**
  - `GET /api/vehicles`
  - `POST /api/vehicles` (via inline add form)
- **Children:** `VehicleList`, `AddVehicleForm`

---

### `VehicleList`
- **Route:** `/vehicles` (inside `VehiclePage`)
- **Props:**
  - `vehicles: Vehicle[]`
- **Data:** receives vehicles via props
- **API calls:** none
- **Children:** `VehicleCard` (one per vehicle)

---

### `VehicleCard`
- **Route:** `/vehicles` (inside `VehicleList`)
- **Props:**
  - `vehicle: Vehicle`
- **Data:** receives vehicle via props
- **API calls:** none
- **Children:** none

---

### `AddVehicleForm`
- **Route:** `/vehicles` (inside `VehiclePage`)
- **Props:**
  - `onCreated: () => void`
- **Data:** controlled form state
- **API calls:**
  - `POST /api/vehicles`
- **Children:** none

---

### `SummaryPage`
- **Route:** `/summary`
- **Props:** none
- **Data:** fetches monthly mileage summary
- **API calls:**
  - `GET /api/summary?month=YYYY-MM`
- **Children:** `MonthPicker`, `SummaryTable`

---

### `MonthPicker`
- **Route:** `/summary` (inside `SummaryPage`)
- **Props:**
  - `value: string`
  - `onChange: (month: string) => void`
- **Data:** none
- **API calls:** none
- **Children:** none

---

### `SummaryTable`
- **Route:** `/summary` (inside `SummaryPage`)
- **Props:**
  - `month: string`
  - `totalMiles: number`
  - `byVehicle: { vehicleId: number; vehicleName: string; miles: number }[]`
- **Data:** receives data via props
- **API calls:** none
- **Children:** none
