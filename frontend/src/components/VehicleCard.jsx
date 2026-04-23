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
