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
