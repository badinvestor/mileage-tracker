```json
// FILE: frontend/package.json
{
  "name": "mileage-tracker",
  "version": "1.0.0",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.22.0",
    "prop-types": "^15.8.1"
  },
  "devDependencies": {
    "vite": "^5.1.0",
    "@vitejs/plugin-react": "^4.2.1"
  }
}
```
Declares all npm dependencies and build scripts for the React + Vite project.

```js
// FILE: frontend/vite.config.js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:3001',
        changeOrigin: true,
      },
    },
  },
})
```
Configures Vite with the React plugin and proxies all `/api` requests to the Kotlin backend on port 3001.

```html
// FILE: frontend/index.html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Mileage Tracker</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.jsx"></script>
  </body>
</html>
```
Vite entry HTML file that mounts the React app into the `#root` div.

```jsx
// FILE: frontend/src/main.jsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App.jsx'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>
)
```
Entry point that mounts the React app inside a `BrowserRouter` for client-side routing.

```jsx
// FILE: frontend/src/App.jsx
import { Routes, Route } from 'react-router-dom'
import NavBar from './components/NavBar.jsx'
import TripListPage from './components/TripListPage.jsx'
import VehiclePage from './components/VehiclePage.jsx'
import SummaryPage from './components/SummaryPage.jsx'

export default function App() {
  return (
    <>
      <NavBar />
      <main style={{ padding: '1rem 2rem' }}>
        <Routes>
          <Route path="/" element={<TripListPage />} />
          <Route path="/vehicles" element={<VehiclePage />} />
          <Route path="/summary" element={<SummaryPage />} />
        </Routes>
      </main>
    </>
  )
}
```
Root component that defines the three application routes and renders the persistent navigation bar.

```js
// FILE: frontend/src/services/api.js
const BASE = '/api'

async function handleResponse(res) {
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText)
    throw new Error(`${res.status} ${text}`)
  }
  if (res.status === 204) return null
  return res.json()
}

// GET /api/trips — returns all trips, optional query params: vehicleId, from, to
export async function getTrips(filters = {}) {
  const params = new URLSearchParams()
  if (filters.vehicleId) params.set('vehicleId', filters.vehicleId)
  if (filters.from) params.set('from', filters.from)
  if (filters.to) params.set('to', filters.to)
  const qs = params.toString()
  const res = await fetch(`${BASE}/trips${qs ? '?' + qs : ''}`)
  return handleResponse(res)
}

// GET /api/trips/:id — returns a single trip by id
export async function getTrip(id) {
  const res = await fetch(`${BASE}/trips/${id}`)
  return handleResponse(res)
}

// POST /api/trips — creates a new trip, returns the created object
export async function createTrip(data) {
  const res = await fetch(`${BASE}/trips`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}

// PUT /api/trips/:id — updates an existing trip, returns updated object
export async function updateTrip(id, data) {
  const res = await fetch(`${BASE}/trips/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}

// DELETE /api/trips/:id — deletes a trip, returns null
export async function deleteTrip(id) {
  const res = await fetch(`${BASE}/trips/${id}`, { method: 'DELETE' })
  return handleResponse(res)
}

// GET /api/vehicles — returns array of vehicle objects
export async function getVehicles() {
  const res = await fetch(`${BASE}/vehicles`)
  return handleResponse(res)
}

// POST /api/vehicles — creates a new vehicle, returns the created object
export async function createVehicle(data) {
  const res = await fetch(`${BASE}/vehicles`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}

// GET /api/summary?month=YYYY-MM — returns mileage totals by vehicle for a month
export async function getSummary(month) {
  const res = await fetch(`${BASE}/summary?month=${encodeURIComponent(month)}`)
  return handleResponse(res)
}
```
Exports one async function per API endpoint; each throws a descriptive `Error` on non-2xx responses.

```jsx
// FILE: frontend/src/components/NavBar.jsx
import { NavLink } from 'react-router-dom'
import styles from './NavBar.module.css'

export default function NavBar() {
  return (
    <nav className={styles.nav}>
      <span className={styles.brand}>Mileage Tracker</span>
      <div className={styles.links}>
        <NavLink to="/" end className={({ isActive }) => isActive ? styles.active : undefined}>
          Trips
        </NavLink>
        <NavLink to="/vehicles" className={({ isActive }) => isActive ? styles.active : undefined}>
          Vehicles
        </NavLink>
        <NavLink to="/summary" className={({ isActive }) => isActive ? styles.active : undefined}>
          Summary
        </NavLink>
      </div>
    </nav>
  )
}
```
Persistent navigation bar with links to the trips list, vehicles, and monthly summary pages.

```css
/* FILE: frontend/src/components/NavBar.module.css */
.nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 2rem;
  background: #1e3a5f;
  color: #f0f4f8;
}

.brand {
  font-size: 1.2rem;
  font-weight: 700;
  letter-spacing: 0.03em;
}

.links {
  display: flex;
  gap: 1.5rem;
}

.links a {
  color: #93b4d4;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.15s;
}

.links a:hover {
  color: #f0f4f8;
}

.active {
  color: #5bc8f5 !important;
  border-bottom: 2px solid #5bc8f5;
}
```
Styles the navigation bar with a dark navy background and highlighted active link.

```jsx
// FILE: frontend/src/components/TripListPage.jsx
import { useState, useEffect, useCallback } from 'react'
import { getTrips, getVehicles, deleteTrip } from '../services/api.js'
import TripFilterBar from './TripFilterBar.jsx'
import TripTable from './TripTable.jsx'
import AddTripButton from './AddTripButton.jsx'
import TripFormModal from './TripFormModal.jsx'
import styles from './TripListPage.module.css'

export default function TripListPage() {
  const [trips, setTrips] = useState([])
  const [vehicles, setVehicles] = useState([])
  const [filters, setFilters] = useState({ vehicleId: '', from: '', to: '' })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [editingTrip, setEditingTrip] = useState(null)
  const [showModal, setShowModal] = useState(false)

  const loadVehicles = useCallback(async () => {
    try {
      setVehicles(await getVehicles())
    } catch (err) {
      console.error('Failed to load vehicles', err)
    }
  }, [])

  const loadTrips = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setTrips(await getTrips(filters))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [filters])

  useEffect(() => { loadVehicles() }, [loadVehicles])
  useEffect(() => { loadTrips() }, [loadTrips])

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this trip?')) return
    try {
      await deleteTrip(id)
      setTrips(prev => prev.filter(t => t.id !== id))
    } catch (err) {
      alert('Delete failed: ' + err.message)
    }
  }

  const handleEdit = (trip) => {
    setEditingTrip(trip)
    setShowModal(true)
  }

  const handleModalClose = () => {
    setShowModal(false)
    setEditingTrip(null)
  }

  const handleSaved = () => {
    handleModalClose()
    loadTrips()
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h1>Trips</h1>
        <AddTripButton vehicles={vehicles} onCreated={handleSaved} />
      </div>
      <TripFilterBar
        vehicles={vehicles}
        vehicleId={filters.vehicleId}
        fromDate={filters.from}
        toDate={filters.to}
        onChange={setFilters}
      />
      {loading && <p>Loading...</p>}
      {error && <p className={styles.error}>Error: {error}</p>}
      {!loading && !error && (
        <TripTable trips={trips} onEdit={handleEdit} onDelete={handleDelete} />
      )}
      {showModal && (
        <TripFormModal
          trip={editingTrip}
          vehicles={vehicles}
          onSave={handleSaved}
          onClose={handleModalClose}
        />
      )}
    </div>
  )
}
```
Main page that orchestrates fetching, filtering, deleting, and opening the create/edit modal for trips.

```css
/* FILE: frontend/src/components/TripListPage.module.css */
.page {
  max-width: 960px;
  margin: 0 auto;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.error {
  color: #dc2626;
  font-weight: 500;
}
```
Constrains the page width and aligns the title and add-trip button.

```jsx
// FILE: frontend/src/components/TripFilterBar.jsx
import PropTypes from 'prop-types'
import styles from './TripFilterBar.module.css'

export default function TripFilterBar({ vehicles, vehicleId, fromDate, toDate, onChange }) {
  const set = (key, value) => onChange(prev => ({ ...prev, [key]: value }))

  return (
    <div className={styles.bar}>
      <label className={styles.field}>
        Vehicle
        <select value={vehicleId} onChange={e => set('vehicleId', e.target.value)}>
          <option value="">All vehicles</option>
          {vehicles.map(v => <option key={v.id} value={v.id}>{v.name}</option>)}
        </select>
      </label>
      <label className={styles.field}>
        From
        <input type="date" value={fromDate} onChange={e => set('from', e.target.value)} />
      </label>
      <label className={styles.field}>
        To
        <input type="date" value={toDate} onChange={e => set('to', e.target.value)} />
      </label>
      <button className={styles.clear} onClick={() => onChange({ vehicleId: '', from: '', to: '' })}>
        Clear
      </button>
    </div>
  )
}

TripFilterBar.propTypes = {
  vehicles: PropTypes.arrayOf(PropTypes.shape({ id: PropTypes.number, name: PropTypes.string })).isRequired,
  vehicleId: PropTypes.string.isRequired,
  fromDate: PropTypes.string.isRequired,
  toDate: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
}
```
Renders vehicle dropdown and date range inputs, calling `onChange` whenever any filter changes.

```css
/* FILE: frontend/src/components/TripFilterBar.module.css */
.bar {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: flex-end;
  background: #f0f4f8;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  margin-bottom: 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  font-size: 0.85rem;
  color: #475569;
  gap: 0.25rem;
}

.field select,
.field input {
  padding: 0.35rem 0.5rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.25rem;
  font-size: 0.95rem;
}

.clear {
  padding: 0.4rem 0.9rem;
  background: #e2e8f0;
  border: none;
  border-radius: 0.25rem;
  cursor: pointer;
  font-size: 0.9rem;
}

.clear:hover {
  background: #cbd5e1;
}
```
Styles the filter bar as a horizontal row with consistent input sizing.

```jsx
// FILE: frontend/src/components/TripTable.jsx
import PropTypes from 'prop-types'
import TripRow from './TripRow.jsx'
import styles from './TripTable.module.css'

export default function TripTable({ trips, onEdit, onDelete }) {
  if (trips.length === 0) {
    return <p>No trips found. Add one to get started!</p>
  }
  return (
    <table className={styles.table}>
      <thead>
        <tr>
          <th>Date</th>
          <th>Vehicle</th>
          <th>Purpose</th>
          <th className={styles.right}>Start (mi)</th>
          <th className={styles.right}>End (mi)</th>
          <th className={styles.right}>Miles</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {trips.map(trip => (
          <TripRow key={trip.id} trip={trip} onEdit={onEdit} onDelete={onDelete} />
        ))}
      </tbody>
    </table>
  )
}

TripTable.propTypes = {
  trips: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.number.isRequired,
    vehicleName: PropTypes.string.isRequired,
    date: PropTypes.string.isRequired,
    startOdometer: PropTypes.number.isRequired,
    endOdometer: PropTypes.number.isRequired,
    miles: PropTypes.number.isRequired,
    purpose: PropTypes.string.isRequired,
  })).isRequired,
  onEdit: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
}
```
Renders a table of trips with column headers, delegating each row to `TripRow`.

```css
/* FILE: frontend/src/components/TripTable.module.css */
.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem;
}

.table th,
.table td {
  padding: 0.65rem 0.75rem;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
}

.table th {
  background: #f8fafc;
  font-weight: 600;
  color: #475569;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.right {
  text-align: right !important;
}
```
Styles the table with subtle borders and a soft header background.

```jsx
// FILE: frontend/src/components/TripRow.jsx
import PropTypes from 'prop-types'
import styles from './TripRow.module.css'

export default function TripRow({ trip, onEdit, onDelete }) {
  return (
    <tr className={styles.row}>
      <td>{trip.date}</td>
      <td><span className={styles.vehicle}>{trip.vehicleName}</span></td>
      <td>{trip.purpose}</td>
      <td className={styles.num}>{trip.startOdometer.toLocaleString()}</td>
      <td className={styles.num}>{trip.endOdometer.toLocaleString()}</td>
      <td className={styles.miles}>{trip.miles.toLocaleString()}</td>
      <td className={styles.actions}>
        <button className={styles.edit} onClick={() => onEdit(trip)}>Edit</button>
        <button className={styles.delete} onClick={() => onDelete(trip.id)}>Delete</button>
      </td>
    </tr>
  )
}

TripRow.propTypes = {
  trip: PropTypes.shape({
    id: PropTypes.number.isRequired,
    vehicleName: PropTypes.string.isRequired,
    date: PropTypes.string.isRequired,
    startOdometer: PropTypes.number.isRequired,
    endOdometer: PropTypes.number.isRequired,
    miles: PropTypes.number.isRequired,
    purpose: PropTypes.string.isRequired,
  }).isRequired,
  onEdit: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
}
```
Renders a single trip row with odometer readings, computed miles, and action buttons.

```css
/* FILE: frontend/src/components/TripRow.module.css */
.row:hover {
  background: #f8fafc;
}

.vehicle {
  display: inline-block;
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 0.8rem;
  font-weight: 500;
}

.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
  color: #64748b;
}

.miles {
  text-align: right;
  font-variant-numeric: tabular-nums;
  font-weight: 700;
  color: #1e3a5f;
}

.actions {
  display: flex;
  gap: 0.5rem;
}

.edit,
.delete {
  padding: 0.25rem 0.6rem;
  border: none;
  border-radius: 0.25rem;
  cursor: pointer;
  font-size: 0.85rem;
}

.edit {
  background: #dbeafe;
  color: #1d4ed8;
}

.delete {
  background: #fee2e2;
  color: #b91c1c;
}
```
Styles each trip row with a vehicle pill badge, right-aligned numeric columns, and colour-coded action buttons.

```jsx
// FILE: frontend/src/components/AddTripButton.jsx
import { useState } from 'react'
import PropTypes from 'prop-types'
import TripFormModal from './TripFormModal.jsx'
import styles from './AddTripButton.module.css'

export default function AddTripButton({ vehicles, onCreated }) {
  const [open, setOpen] = useState(false)

  return (
    <>
      <button className={styles.btn} onClick={() => setOpen(true)}>
        + Log Trip
      </button>
      {open && (
        <TripFormModal
          trip={null}
          vehicles={vehicles}
          onSave={() => { setOpen(false); onCreated() }}
          onClose={() => setOpen(false)}
        />
      )}
    </>
  )
}

AddTripButton.propTypes = {
  vehicles: PropTypes.arrayOf(PropTypes.shape({ id: PropTypes.number, name: PropTypes.string })).isRequired,
  onCreated: PropTypes.func.isRequired,
}
```
Button that opens `TripFormModal` in create mode when clicked.

```css
/* FILE: frontend/src/components/AddTripButton.module.css */
.btn {
  padding: 0.5rem 1.1rem;
  background: #1e3a5f;
  color: #fff;
  border: none;
  border-radius: 0.375rem;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.btn:hover {
  background: #2d5382;
}

.btn:active {
  background: #163154;
}
```
Styles the primary action button in navy with hover and active states.

```jsx
// FILE: frontend/src/components/TripFormModal.jsx
import { useState, useEffect } from 'react'
import PropTypes from 'prop-types'
import { createTrip, updateTrip } from '../services/api.js'
import styles from './TripFormModal.module.css'

const today = () => new Date().toISOString().slice(0, 10)

export default function TripFormModal({ trip, vehicles, onSave, onClose }) {
  const isEdit = trip !== null
  const [form, setForm] = useState({
    vehicleId: vehicles[0]?.id ?? '',
    date: today(),
    startOdometer: '',
    endOdometer: '',
    purpose: '',
    notes: '',
  })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (isEdit) {
      setForm({
        vehicleId: trip.vehicleId,
        date: trip.date,
        startOdometer: String(trip.startOdometer),
        endOdometer: String(trip.endOdometer),
        purpose: trip.purpose,
        notes: trip.notes ?? '',
      })
    }
  }, [trip, isEdit])

  const set = (key, value) => setForm(prev => ({ ...prev, [key]: value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    const start = parseFloat(form.startOdometer)
    const end = parseFloat(form.endOdometer)
    if (isNaN(start) || start < 0) return setError('Start odometer must be a non-negative number')
    if (isNaN(end) || end <= start) return setError('End odometer must be greater than start odometer')
    if (!form.purpose.trim()) return setError('Purpose is required')
    const payload = {
      vehicleId: Number(form.vehicleId),
      date: form.date,
      startOdometer: start,
      endOdometer: end,
      purpose: form.purpose.trim(),
      notes: form.notes.trim(),
    }
    setSaving(true)
    try {
      if (isEdit) {
        await updateTrip(trip.id, payload)
      } else {
        await createTrip(payload)
      }
      onSave()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className={styles.overlay} onClick={e => { if (e.target === e.currentTarget) onClose() }}>
      <div className={styles.modal}>
        <h2>{isEdit ? 'Edit Trip' : 'Log Trip'}</h2>
        {error && <p className={styles.error}>{error}</p>}
        <form onSubmit={handleSubmit} className={styles.form}>
          <label>
            Vehicle
            <select value={form.vehicleId} onChange={e => set('vehicleId', e.target.value)} required>
              {vehicles.map(v => <option key={v.id} value={v.id}>{v.name}</option>)}
            </select>
          </label>
          <label>
            Date
            <input type="date" required value={form.date} onChange={e => set('date', e.target.value)} />
          </label>
          <div className={styles.row2}>
            <label>
              Start Odometer
              <input type="number" step="0.1" min="0" required value={form.startOdometer}
                onChange={e => set('startOdometer', e.target.value)} />
            </label>
            <label>
              End Odometer
              <input type="number" step="0.1" min="0" required value={form.endOdometer}
                onChange={e => set('endOdometer', e.target.value)} />
            </label>
          </div>
          <label>
            Purpose
            <input type="text" required value={form.purpose} onChange={e => set('purpose', e.target.value)} />
          </label>
          <label>
            Notes (optional)
            <textarea rows={2} value={form.notes} onChange={e => set('notes', e.target.value)} />
          </label>
          <div className={styles.actions}>
            <button type="button" onClick={onClose} className={styles.cancel}>Cancel</button>
            <button type="submit" disabled={saving} className={styles.save}>
              {saving ? 'Saving…' : isEdit ? 'Update' : 'Log Trip'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

TripFormModal.propTypes = {
  trip: PropTypes.shape({
    id: PropTypes.number.isRequired,
    vehicleId: PropTypes.number.isRequired,
    date: PropTypes.string.isRequired,
    startOdometer: PropTypes.number.isRequired,
    endOdometer: PropTypes.number.isRequired,
    purpose: PropTypes.string.isRequired,
    notes: PropTypes.string,
  }),
  vehicles: PropTypes.arrayOf(PropTypes.shape({ id: PropTypes.number, name: PropTypes.string })).isRequired,
  onSave: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
}
```
Modal form for creating and editing trips, with client-side validation that end odometer must exceed start odometer.

```css
/* FILE: frontend/src/components/TripFormModal.module.css */
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  background: #fff;
  border-radius: 0.5rem;
  padding: 1.75rem 2rem;
  width: 100%;
  max-width: 480px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

.modal h2 {
  margin: 0 0 1rem;
  font-size: 1.15rem;
  color: #1e3a5f;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.form label {
  display: flex;
  flex-direction: column;
  font-size: 0.85rem;
  color: #475569;
  gap: 0.3rem;
}

.form input,
.form select,
.form textarea {
  padding: 0.45rem 0.6rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.25rem;
  font-size: 1rem;
  font-family: inherit;
}

.row2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.error {
  color: #dc2626;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.cancel {
  padding: 0.45rem 1rem;
  background: #e2e8f0;
  border: none;
  border-radius: 0.25rem;
  cursor: pointer;
}

.save {
  padding: 0.45rem 1.1rem;
  background: #1e3a5f;
  color: #fff;
  border: none;
  border-radius: 0.25rem;
  cursor: pointer;
  font-weight: 600;
}

.save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
```
Styles the trip modal with a two-column odometer row and navy action button.

```jsx
// FILE: frontend/src/components/VehiclePage.jsx
import { useState, useEffect, useCallback } from 'react'
import { getVehicles } from '../services/api.js'
import VehicleList from './VehicleList.jsx'
import AddVehicleForm from './AddVehicleForm.jsx'
import styles from './VehiclePage.module.css'

export default function VehiclePage() {
  const [vehicles, setVehicles] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const loadVehicles = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setVehicles(await getVehicles())
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadVehicles() }, [loadVehicles])

  return (
    <div className={styles.page}>
      <h1>Vehicles</h1>
      {loading && <p>Loading...</p>}
      {error && <p className={styles.error}>Error: {error}</p>}
      {!loading && !error && <VehicleList vehicles={vehicles} />}
      <AddVehicleForm onCreated={loadVehicles} />
    </div>
  )
}
```
Page that displays all vehicles and an inline form to register new ones.

```css
/* FILE: frontend/src/components/VehiclePage.module.css */
.page {
  max-width: 640px;
  margin: 0 auto;
}

.page h1 {
  margin-bottom: 1.25rem;
}

.error {
  color: #dc2626;
  font-weight: 500;
}
```
Constrains the vehicle page to a readable max-width.

```jsx
// FILE: frontend/src/components/VehicleList.jsx
import PropTypes from 'prop-types'
import VehicleCard from './VehicleCard.jsx'
import styles from './VehicleList.module.css'

export default function VehicleList({ vehicles }) {
  if (vehicles.length === 0) {
    return <p className={styles.empty}>No vehicles yet. Add one below.</p>
  }
  return (
    <div className={styles.list}>
      {vehicles.map(v => <VehicleCard key={v.id} vehicle={v} />)}
    </div>
  )
}

VehicleList.propTypes = {
  vehicles: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    licensePlate: PropTypes.string,
  })).isRequired,
}
```
Renders a grid of `VehicleCard` components or an empty-state message.

```css
/* FILE: frontend/src/components/VehicleList.module.css */
.list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.empty {
  color: #64748b;
  margin-bottom: 1.5rem;
}
```
Lays out vehicle cards in a responsive grid.

```jsx
// FILE: frontend/src/components/VehicleCard.jsx
import PropTypes from 'prop-types'
import styles from './VehicleCard.module.css'

export default function VehicleCard({ vehicle }) {
  return (
    <div className={styles.card}>
      <p className={styles.name}>{vehicle.name}</p>
      {vehicle.licensePlate && (
        <p className={styles.plate}>{vehicle.licensePlate}</p>
      )}
    </div>
  )
}

VehicleCard.propTypes = {
  vehicle: PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    licensePlate: PropTypes.string,
  }).isRequired,
}
```
Displays a vehicle's name and optional license plate in a card.

```css
/* FILE: frontend/src/components/VehicleCard.module.css */
.card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 0.5rem;
  padding: 1rem 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.name {
  font-weight: 600;
  font-size: 1rem;
  color: #1e3a5f;
  margin-bottom: 0.25rem;
}

.plate {
  font-size: 0.85rem;
  color: #64748b;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.05em;
}
```
Styles the vehicle card with a subtle border, shadow, and navy title.

```jsx
// FILE: frontend/src/components/AddVehicleForm.jsx
import { useState } from 'react'
import PropTypes from 'prop-types'
import { createVehicle } from '../services/api.js'
import styles from './AddVehicleForm.module.css'

export default function AddVehicleForm({ onCreated }) {
  const [name, setName] = useState('')
  const [licensePlate, setLicensePlate] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    if (!name.trim()) return setError('Vehicle name is required')
    setSaving(true)
    try {
      await createVehicle({ name: name.trim(), licensePlate: licensePlate.trim() })
      setName('')
      setLicensePlate('')
      onCreated()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className={styles.wrapper}>
      <h2>Add Vehicle</h2>
      {error && <p className={styles.error}>{error}</p>}
      <form onSubmit={handleSubmit} className={styles.form}>
        <input
          type="text"
          placeholder="Vehicle name (e.g. Work Truck)"
          value={name}
          onChange={e => setName(e.target.value)}
          required
        />
        <input
          type="text"
          placeholder="License plate (optional)"
          value={licensePlate}
          onChange={e => setLicensePlate(e.target.value)}
        />
        <button type="submit" disabled={saving} className={styles.btn}>
          {saving ? 'Adding…' : 'Add Vehicle'}
        </button>
      </form>
    </div>
  )
}

AddVehicleForm.propTypes = {
  onCreated: PropTypes.func.isRequired,
}
```
Inline form for registering a new vehicle with a name and optional license plate.

```css
/* FILE: frontend/src/components/AddVehicleForm.module.css */
.wrapper {
  border-top: 1px solid #e2e8f0;
  padding-top: 1.5rem;
}

.wrapper h2 {
  font-size: 1rem;
  color: #475569;
  margin-bottom: 0.75rem;
}

.form {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  align-items: center;
}

.form input {
  padding: 0.45rem 0.7rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.25rem;
  font-size: 0.95rem;
  flex: 1;
  min-width: 160px;
}

.error {
  color: #dc2626;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}

.btn {
  padding: 0.45rem 1.1rem;
  background: #1e3a5f;
  color: #fff;
  border: none;
  border-radius: 0.25rem;
  cursor: pointer;
  font-weight: 600;
  white-space: nowrap;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
```
Styles the add vehicle form as an inline row of inputs and a submit button.

```jsx
// FILE: frontend/src/components/SummaryPage.jsx
import { useState, useEffect } from 'react'
import { getSummary } from '../services/api.js'
import MonthPicker from './MonthPicker.jsx'
import SummaryTable from './SummaryTable.jsx'
import styles from './SummaryPage.module.css'

function currentMonth() {
  return new Date().toISOString().slice(0, 7)
}

export default function SummaryPage() {
  const [month, setMonth] = useState(currentMonth())
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    setLoading(true)
    setError(null)
    getSummary(month)
      .then(data => { setSummary(data); setLoading(false) })
      .catch(err => { setError(err.message); setLoading(false) })
  }, [month])

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h1>Monthly Summary</h1>
        <MonthPicker value={month} onChange={setMonth} />
      </div>
      {loading && <p>Loading...</p>}
      {error && <p className={styles.error}>Error: {error}</p>}
      {!loading && !error && summary && (
        <SummaryTable
          month={summary.month}
          totalMiles={summary.totalMiles}
          byVehicle={summary.byVehicle}
        />
      )}
    </div>
  )
}
```
Page that fetches and displays a monthly mileage summary, switching months via `MonthPicker`.

```css
/* FILE: frontend/src/components/SummaryPage.module.css */
.page {
  max-width: 700px;
  margin: 0 auto;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.error {
  color: #dc2626;
  font-weight: 500;
}
```
Constrains the summary page and aligns the title and month picker.

```jsx
// FILE: frontend/src/components/MonthPicker.jsx
import PropTypes from 'prop-types'
import styles from './MonthPicker.module.css'

export default function MonthPicker({ value, onChange }) {
  return (
    <div className={styles.wrapper}>
      <label htmlFor="month-picker">Month</label>
      <input
        id="month-picker"
        type="month"
        value={value}
        onChange={e => onChange(e.target.value)}
        className={styles.input}
      />
    </div>
  )
}

MonthPicker.propTypes = {
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
}
```
Controlled `<input type="month">` for selecting the summary month.

```css
/* FILE: frontend/src/components/MonthPicker.module.css */
.wrapper {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: #475569;
}

.input {
  padding: 0.4rem 0.6rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.25rem;
  font-size: 0.95rem;
}

.input:focus {
  outline: 2px solid #5bc8f5;
  border-color: transparent;
}
```
Styles the month picker as an inline label-and-input pair.

```jsx
// FILE: frontend/src/components/SummaryTable.jsx
import PropTypes from 'prop-types'
import styles from './SummaryTable.module.css'

export default function SummaryTable({ month, totalMiles, byVehicle }) {
  if (!byVehicle || byVehicle.length === 0) {
    return <p>No trips recorded for {month}.</p>
  }
  return (
    <div className={styles.wrapper}>
      <h2 className={styles.heading}>Mileage Breakdown — {month}</h2>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>Vehicle</th>
            <th className={styles.right}>Miles</th>
            <th className={styles.right}>% of Total</th>
          </tr>
        </thead>
        <tbody>
          {byVehicle.map(row => (
            <tr key={row.vehicleId}>
              <td>{row.vehicleName}</td>
              <td className={styles.right}>{row.miles.toLocaleString()}</td>
              <td className={styles.right}>
                {totalMiles > 0 ? ((row.miles / totalMiles) * 100).toFixed(1) : '0.0'}%
              </td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr className={styles.total}>
            <td>Total</td>
            <td className={styles.right}>{totalMiles.toLocaleString()}</td>
            <td className={styles.right}>100%</td>
          </tr>
        </tfoot>
      </table>
    </div>
  )
}

SummaryTable.propTypes = {
  month: PropTypes.string.isRequired,
  totalMiles: PropTypes.number.isRequired,
  byVehicle: PropTypes.arrayOf(PropTypes.shape({
    vehicleId: PropTypes.number.isRequired,
    vehicleName: PropTypes.string.isRequired,
    miles: PropTypes.number.isRequired,
  })).isRequired,
}
```
Tabular breakdown of monthly mileage per vehicle with percentage share and a grand total footer.

```css
/* FILE: frontend/src/components/SummaryTable.module.css */
.wrapper {
  margin-top: 1rem;
}

.heading {
  font-size: 1rem;
  color: #475569;
  margin-bottom: 0.75rem;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem;
}

.table th,
.table td {
  padding: 0.6rem 0.75rem;
  border-bottom: 1px solid #e2e8f0;
}

.table th {
  background: #f8fafc;
  font-weight: 600;
  color: #475569;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.right {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.total td {
  font-weight: 700;
  border-top: 2px solid #94a3b8;
  color: #1e3a5f;
}
```
Styles the summary table with consistent column alignment and a bold total footer row.

```css
/* FILE: frontend/src/index.css */
*,
*::before,
*::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: #f0f4f8;
  color: #1e293b;
  line-height: 1.5;
}

h1 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #0f172a;
}

button {
  font-family: inherit;
}
```
Global CSS reset and base body styles shared across the entire application.
