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
