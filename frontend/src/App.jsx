import { Routes, Route } from 'react-router-dom'
import NavBar from './components/NavBar.jsx'
import TripListPage from './components/TripListPage.jsx'
import VehiclePage from './components/VehiclePage.jsx'
import SummaryPage from './components/SummaryPage.jsx'

export default function App() {
  return (
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
  )
}
