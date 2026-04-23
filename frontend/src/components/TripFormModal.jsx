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
