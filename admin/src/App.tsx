import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './hooks/useAuth';
import { Layout } from './components/Layout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import LiveMapPage from './pages/LiveMapPage';
import TriagePage from './pages/TriagePage';
import StatusBoardPage from './pages/StatusBoardPage';
import AnalyticsPage from './pages/AnalyticsPage';
import ReportDetailPage from './pages/ReportDetailPage';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/live-map" element={<LiveMapPage />} />
        <Route path="/triage" element={<TriagePage />} />
        <Route path="/board" element={<StatusBoardPage />} />
        <Route path="/analytics" element={<AnalyticsPage />} />
        <Route path="/reports/:id" element={<ReportDetailPage />} />
      </Route>
    </Routes>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  );
}
