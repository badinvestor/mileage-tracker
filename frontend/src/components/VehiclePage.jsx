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
