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
