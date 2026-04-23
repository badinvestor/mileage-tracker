import { NavLink } from 'react-router-dom'
import styles from './NavBar.module.css'

export default function NavBar() {
  return (
    <nav className={styles.nav}>
      <span className={styles.brand}>Mileage Tracker</span>
      <div className={styles.links}>
        <NavLink to="/" end className={({ isActive }) => isActive ? styles.active : undefined}>
          Trips
        </NavLink>
        <NavLink to="/vehicles" className={({ isActive }) => isActive ? styles.active : undefined}>
          Vehicles
        </NavLink>
        <NavLink to="/summary" className={({ isActive }) => isActive ? styles.active : undefined}>
          Summary
        </NavLink>
      </div>
    </nav>
  )
}
