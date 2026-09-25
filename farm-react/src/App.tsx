import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import PrivateRoute from './components/PrivateRoute'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import FarmersPage from './pages/FarmersPage'
import FarmerDetailPage from './pages/FarmerDetailPage'
import UnitsPage from './pages/UnitsPage'
import AnomaliesPage from './pages/AnomaliesPage'
import RecommendationsPage from './pages/RecommendationsPage'
import ExportPage from './pages/ExportPage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/*" element={
            <PrivateRoute>
              <Layout>
                <Routes>
                  <Route path="/dashboard" element={<DashboardPage />} />
                  <Route path="/farmers" element={<FarmersPage />} />
                  <Route path="/farmers/:id" element={<FarmerDetailPage />} />
                  <Route path="/units" element={<UnitsPage />} />
                  <Route path="/anomalies" element={<AnomaliesPage />} />
                  <Route path="/recommendations" element={<RecommendationsPage />} />
                  <Route path="/reports" element={<ExportPage />} />
                  <Route path="*" element={<Navigate to="/dashboard" replace />} />
                </Routes>
              </Layout>
            </PrivateRoute>
          } />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
