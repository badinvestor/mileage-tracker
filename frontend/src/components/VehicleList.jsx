import PropTypes from 'prop-types'
import VehicleCard from './VehicleCard.jsx'
import styles from './VehicleList.module.css'

export default function VehicleList({ vehicles }) {
  if (vehicles.length === 0) {
    return <p className={styles.empty}>No vehicles yet. Add one below.</p>
  }
  return (
    <div className={styles.list}>
      {vehicles.map(v => <VehicleCard key={v.id} vehicle={v} />)}
    </div>
  )
}

VehicleList.propTypes = {
  vehicles: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    licensePlate: PropTypes.string,
  })).isRequired,
}
