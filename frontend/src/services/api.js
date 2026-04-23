const BASE = '/api'

const TOKEN_KEY = 'mt_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

function authHeaders() {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function handleResponse(res) {
  if (res.status === 401) {
    clearToken()
    window.location.href = '/login'
    return
  }
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText)
    throw new Error(`${res.status} ${text}`)
  }
  if (res.status === 204) return null
  return res.json()
}

export async function login(username, password) {
  const res = await fetch(`${BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  if (!res.ok) {
    throw new Error('Invalid credentials')
  }
  const { token } = await res.json()
  setToken(token)
  return token
}

export async function getTrips(filters = {}) {
  const params = new URLSearchParams()
  if (filters.vehicleId) params.set('vehicleId', filters.vehicleId)
  if (filters.from) params.set('from', filters.from)
  if (filters.to) params.set('to', filters.to)
  const qs = params.toString()
  const res = await fetch(`${BASE}/trips${qs ? '?' + qs : ''}`, {
    headers: authHeaders(),
  })
  return handleResponse(res)
}

export async function getTrip(id) {
  const res = await fetch(`${BASE}/trips/${id}`, {
    headers: authHeaders(),
  })
  return handleResponse(res)
}

export async function createTrip(data) {
  const res = await fetch(`${BASE}/trips`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}

export async function updateTrip(id, data) {
  const res = await fetch(`${BASE}/trips/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}

export async function deleteTrip(id) {
  const res = await fetch(`${BASE}/trips/${id}`, {
    method: 'DELETE',
    headers: authHeaders(),
  })
  return handleResponse(res)
}

export async function getVehicles() {
  const res = await fetch(`${BASE}/vehicles`, {
    headers: authHeaders(),
  })
  return handleResponse(res)
}

export async function createVehicle(data) {
  const res = await fetch(`${BASE}/vehicles`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}

export async function getSummary(month) {
  const res = await fetch(`${BASE}/summary?month=${encodeURIComponent(month)}`, {
    headers: authHeaders(),
  })
  return handleResponse(res)
}
