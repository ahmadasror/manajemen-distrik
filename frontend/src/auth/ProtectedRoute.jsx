import { useEffect } from 'react';
import { Navigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { useAuth } from './AuthContext';
import keycloak from './keycloak';

export default function ProtectedRoute({ children, roles }) {
  const { user, loading } = useAuth();

  useEffect(() => {
    if (!loading && !user && !keycloak.authenticated) {
      keycloak.login();
    }
  }, [loading, user]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (!user) {
    return (
      <div className="flex items-center justify-center h-screen">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (roles && !roles.some((role) => user.roles?.includes(role))) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}
