import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import keycloak from './keycloak';
import { authApi } from '../api/authApi';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const initialized = useRef(false);

  const fetchUser = useCallback(async () => {
    if (!keycloak.authenticated) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const res = await authApi.me();
      setUser(res.data.data);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;

    keycloak
      .init({
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
        pkceMethod: 'S256',
      })
      .then(() => {
        fetchUser();
      })
      .catch(() => {
        setLoading(false);
      });

    // Refresh token before it expires
    const interval = setInterval(() => {
      if (keycloak.authenticated) {
        keycloak.updateToken(60).catch(() => {
          keycloak.logout();
        });
      }
    }, 30000);

    return () => clearInterval(interval);
  }, [fetchUser]);

  const login = () => {
    keycloak.login();
  };

  const logout = () => {
    setUser(null);
    keycloak.logout({ redirectUri: window.location.origin });
  };

  const hasRole = (role) => {
    return user?.roles?.includes(role);
  };

  const hasAnyRole = (...roles) => {
    return roles.some((role) => user?.roles?.includes(role));
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, hasRole, hasAnyRole, fetchUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
};
