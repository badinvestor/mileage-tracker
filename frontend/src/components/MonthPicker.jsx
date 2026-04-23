import PropTypes from 'prop-types'
import styles from './MonthPicker.module.css'

export default function MonthPicker({ value, onChange }) {
  return (
    <div className={styles.wrapper}>
      <label htmlFor="month-picker">Month</label>
      <input
        id="month-picker"
        type="month"
        value={value}
        onChange={e => onChange(e.target.value)}
        className={styles.input}
      />
    </div>
  )
}

MonthPicker.propTypes = {
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
}
