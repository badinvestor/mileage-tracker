import PropTypes from 'prop-types'
import styles from './SummaryTable.module.css'

export default function SummaryTable({ month, totalMiles, byVehicle }) {
  if (!byVehicle || byVehicle.length === 0) {
    return <p>No trips recorded for {month}.</p>
  }
  return (
    <div className={styles.wrapper}>
      <h2 className={styles.heading}>Mileage Breakdown — {month}</h2>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>Vehicle</th>
            <th className={styles.right}>Miles</th>
            <th className={styles.right}>% of Total</th>
          </tr>
        </thead>
        <tbody>
          {byVehicle.map(row => (
            <tr key={row.vehicleId}>
              <td>{row.vehicleName}</td>
              <td className={styles.right}>{row.miles.toLocaleString()}</td>
              <td className={styles.right}>
                {totalMiles > 0 ? ((row.miles / totalMiles) * 100).toFixed(1) : '0.0'}%
              </td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr className={styles.total}>
            <td>Total</td>
            <td className={styles.right}>{totalMiles.toLocaleString()}</td>
            <td className={styles.right}>100%</td>
          </tr>
        </tfoot>
      </table>
    </div>
  )
}

SummaryTable.propTypes = {
  month: PropTypes.string.isRequired,
  totalMiles: PropTypes.number.isRequired,
  byVehicle: PropTypes.arrayOf(PropTypes.shape({
    vehicleId: PropTypes.number.isRequired,
    vehicleName: PropTypes.string.isRequired,
    miles: PropTypes.number.isRequired,
  })).isRequired,
}
