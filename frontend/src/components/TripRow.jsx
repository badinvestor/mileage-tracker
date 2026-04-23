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
