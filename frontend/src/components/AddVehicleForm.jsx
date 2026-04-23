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
