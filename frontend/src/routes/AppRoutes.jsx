import { Routes, Route, Navigate, Outlet, Link } from 'react-router-dom';
import { ProtectedRoute } from '../auth/ProtectedRoute';
import { Navbar } from '../components/Navbar';
import { UserRole } from '../utils/constants';
import { useAuth } from '../auth/useAuth';

/** Redirects user to the correct portal based on their Keycloak roles */
function RoleBasedHome() {
  const { user, loading, isKeycloakMode } = useAuth();

  if (loading) return null;

  // Dev mode (no Keycloak) — go straight to admin dashboard
  if (!isKeycloakMode) return <Navigate to="/admin" replace />;

  if (!user || !user.roles || user.roles.length === 0) {
    return <Navigate to="/admin" replace />;
  }

  if (user.roles.includes(UserRole.ADMIN) || user.roles.includes(UserRole.COORDINATOR)) {
    return <Navigate to="/admin" replace />;
  }
  if (user.roles.includes(UserRole.DONOR)) return <Navigate to="/donor" replace />;
  if (user.roles.includes(UserRole.RECEIVER)) return <Navigate to="/receiver" replace />;
  if (user.roles.includes(UserRole.DRIVER)) return <Navigate to="/driver" replace />;

  return <Navigate to="/unauthorized" replace />;
}

// Donor Pages
import { DonorDashboardPage } from '../features/donor/pages/DonorDashboardPage';
import { RegisterDonationPage } from '../features/donor/pages/RegisterDonationPage';
import { DonationHistoryPage } from '../features/donor/pages/DonationHistoryPage';

// Receiver Pages
import { ReceiverDashboardPage } from '../features/receiver/pages/ReceiverDashboardPage';
import { CreateAidRequestPage } from '../features/receiver/pages/CreateAidRequestPage';
import { DeliveryConfirmationPage } from '../features/receiver/pages/DeliveryConfirmationPage';

// Driver Pages
import { DriverDashboardPage } from '../features/driver/pages/DriverDashboardPage';
import { ShipmentRunPage } from '../features/driver/pages/ShipmentRunPage';

// Admin Pages
import { AdminDashboardPage } from '../features/admin/pages/AdminDashboardPage';
import { AllocationApprovalPage } from '../features/admin/pages/AllocationApprovalPage';
import { VerificationQueuePage } from '../features/admin/pages/VerificationQueuePage';
import { LiveOperationsMapPage } from '../features/admin/pages/LiveOperationsMapPage';
import { ShipmentTrackingPage } from '../features/admin/pages/ShipmentTrackingPage';

function Layout() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 flex flex-col font-sans">
      <Navbar />
      <main className="flex-1 mx-auto max-w-7xl w-full px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
      <footer className="border-t border-slate-200/80 bg-white py-6 text-center text-xs text-slate-500">
        DisasterReliefNet Multi-Agency Platform &copy; 2026. All rights reserved.
      </footer>
    </div>
  );
}

function UnauthorizedPage() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-center px-4">
      <div className="h-16 w-16 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center mb-4">
        <svg className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth="2"
            d="M12 15v2m0 0v2m0-2h2m-2 0H10m11-3.5a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
      </div>
      <h1 className="text-2xl font-bold text-slate-900">403 - Access Denied</h1>
      <p className="mt-2 text-sm text-slate-600 max-w-md">
        Your authenticated account does not have permission to view this portal. Switch your role
        using the top-right role selector to evaluate this portal.
      </p>
      <Link
        to="/"
        className="mt-6 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow hover:bg-indigo-700"
      >
        Return to Portal Home
      </Link>
    </div>
  );
}

function NotFoundPage() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-center px-4">
      <h1 className="text-4xl font-extrabold text-slate-900">404</h1>
      <p className="mt-2 text-base text-slate-600">
        The requested relief resource could not be found.
      </p>
      <Link
        to="/"
        className="mt-6 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow hover:bg-indigo-700"
      >
        Go Home
      </Link>
    </div>
  );
}

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        {/* Landing: smart redirect based on user's role */}
        <Route index element={<RoleBasedHome />} />

        {/* Donor Portal (role: DONOR, ADMIN) */}
        <Route
          path="donor"
          element={
            <ProtectedRoute allowedRoles={[UserRole.DONOR, UserRole.ADMIN]}>
              <DonorDashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="donor/register"
          element={
            <ProtectedRoute allowedRoles={[UserRole.DONOR, UserRole.ADMIN]}>
              <RegisterDonationPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="donor/history"
          element={
            <ProtectedRoute allowedRoles={[UserRole.DONOR, UserRole.ADMIN]}>
              <DonationHistoryPage />
            </ProtectedRoute>
          }
        />

        {/* Receiver Portal (role: RECEIVER, ADMIN) */}
        <Route
          path="receiver"
          element={
            <ProtectedRoute allowedRoles={[UserRole.RECEIVER, UserRole.ADMIN]}>
              <ReceiverDashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="receiver/request"
          element={
            <ProtectedRoute allowedRoles={[UserRole.RECEIVER, UserRole.ADMIN]}>
              <CreateAidRequestPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="receiver/confirm-delivery/:requestId"
          element={
            <ProtectedRoute allowedRoles={[UserRole.RECEIVER, UserRole.ADMIN]}>
              <DeliveryConfirmationPage />
            </ProtectedRoute>
          }
        />

        {/* Driver Portal (role: DRIVER, ADMIN) */}
        <Route
          path="driver"
          element={
            <ProtectedRoute allowedRoles={[UserRole.DRIVER, UserRole.ADMIN]}>
              <DriverDashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="driver/shipment/:shipmentId"
          element={
            <ProtectedRoute allowedRoles={[UserRole.DRIVER, UserRole.ADMIN]}>
              <ShipmentRunPage />
            </ProtectedRoute>
          }
        />

        {/* Admin & Operations Portal (role: ADMIN, COORDINATOR) */}
        <Route
          path="admin"
          element={
            <ProtectedRoute allowedRoles={[UserRole.ADMIN, UserRole.COORDINATOR]}>
              <AdminDashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/allocations"
          element={
            <ProtectedRoute allowedRoles={[UserRole.ADMIN, UserRole.COORDINATOR]}>
              <AllocationApprovalPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/verification"
          element={
            <ProtectedRoute allowedRoles={[UserRole.ADMIN, UserRole.COORDINATOR]}>
              <VerificationQueuePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/map"
          element={
            <ProtectedRoute allowedRoles={[UserRole.ADMIN, UserRole.COORDINATOR]}>
              <LiveOperationsMapPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/tracking"
          element={
            <ProtectedRoute allowedRoles={[UserRole.ADMIN, UserRole.COORDINATOR]}>
              <ShipmentTrackingPage />
            </ProtectedRoute>
          }
        />

        {/* Error pages */}
        <Route path="unauthorized" element={<UnauthorizedPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
