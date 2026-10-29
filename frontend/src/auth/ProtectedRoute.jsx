import PropTypes from 'prop-types';
import { Navigate } from 'react-router-dom';
import { useAuth } from './useAuth';

function AuthSpinner() {
  return (
    <div className="flex h-screen w-full items-center justify-center bg-slate-950">
      <div className="flex flex-col items-center gap-4">
        <div className="h-12 w-12 animate-spin rounded-full border-4 border-indigo-500 border-t-transparent" />
        <p className="text-sm font-medium text-slate-400">Loading portal…</p>
      </div>
    </div>
  );
}

/**
 * ProtectedRoute — guards a route by role.
 *
 * Authentication redirect is handled entirely by Keycloak (onLoad:'login-required').
 * This component only needs to:
 *   1. Show a spinner while KC is still initialising
 *   2. Check role access once authenticated
 */
export function ProtectedRoute({ children, allowedRoles }) {
  const { authenticated, loading, hasRole } = useAuth();

  if (loading) return <AuthSpinner />;

  // Should not normally reach here with login-required, but guard just in case
  if (!authenticated) return <Navigate to="/unauthorized" replace />;

  if (allowedRoles && allowedRoles.length > 0 && !hasRole(allowedRoles)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return children;
}

ProtectedRoute.propTypes = {
  children:     PropTypes.node.isRequired,
  allowedRoles: PropTypes.arrayOf(PropTypes.string),
};

ProtectedRoute.defaultProps = {
  allowedRoles: [],
};
