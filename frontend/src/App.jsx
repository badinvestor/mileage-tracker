import { Routes, Route, Navigate } from 'react-router-dom'
import NavBar from './components/NavBar.jsx'
import TripListPage from './components/TripListPage.jsx'
import VehiclePage from './components/VehiclePage.jsx'
import SummaryPage from './components/SummaryPage.jsx'
import LoginPage from './components/LoginPage.jsx'
import { getToken } from './services/api.js'

function RequireAuth({ children }) {
  return getToken() ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/*" element={
        <RequireAuth>
          <>
            <NavBar />
            <main style={{ padding: '1rem 2rem' }}>
              <Routes>
                <Route path="/" element={<TripListPage />} />
                <Route path="/vehicles" element={<VehiclePage />} />
                <Route path="/summary" element={<SummaryPage />} />
              </Routes>
            </main>
          </>
        </RequireAuth>
      } />
    </Routes>
  )
}
