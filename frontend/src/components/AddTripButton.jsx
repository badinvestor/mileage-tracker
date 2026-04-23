import { useState } from 'react'
import PropTypes from 'prop-types'
import TripFormModal from './TripFormModal.jsx'
import styles from './AddTripButton.module.css'

export default function AddTripButton({ vehicles, onCreated }) {
  const [open, setOpen] = useState(false)

  return (
    <>
      <button className={styles.btn} onClick={() => setOpen(true)}>
        + Log Trip
      </button>
      {open && (
        <TripFormModal
          trip={null}
          vehicles={vehicles}
          onSave={() => { setOpen(false); onCreated() }}
          onClose={() => setOpen(false)}
        />
      )}
    </>
  )
}

AddTripButton.propTypes = {
  vehicles: PropTypes.arrayOf(PropTypes.shape({ id: PropTypes.number, name: PropTypes.string })).isRequired,
  onCreated: PropTypes.func.isRequired,
}
