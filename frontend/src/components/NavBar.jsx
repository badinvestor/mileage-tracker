import { NavLink, useNavigate } from 'react-router-dom'
import styles from './NavBar.module.css'
import { clearToken } from '../services/api.js'

export default function NavBar() {
  const navigate = useNavigate()

  function handleLogout() {
    clearToken()
    navigate('/login')
  }

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
        <button className={styles.logout} onClick={handleLogout}>Logout</button>
      </div>
    </nav>
  )
}
