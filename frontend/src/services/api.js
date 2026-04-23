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
