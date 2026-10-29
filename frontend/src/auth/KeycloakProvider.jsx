import { createContext, useEffect, useState, useMemo, useCallback, useRef } from 'react';
import PropTypes from 'prop-types';
import Keycloak from 'keycloak-js';
import { setAuthToken, setAuthCallbacks } from '../api/axiosClient';

export const AuthContext = createContext(null);

/** Strips Spring-style ROLE_ prefix and deduplicates role names */
function normalizeRoles(rawRoles = []) {
  return [...new Set(rawRoles.map((r) => r.toUpperCase().replace(/^ROLE_/, '')))];
}

/** Builds a plain user object from a live Keycloak instance */
function buildUserFromKC(kc) {
  const realmRoles = kc.realmAccess?.roles ?? [];
  const clientRoles = Object.values(kc.resourceAccess ?? {}).flatMap((c) => c.roles ?? []);
  return {
    id: kc.tokenParsed?.sub,
    name: kc.tokenParsed?.name || kc.tokenParsed?.preferred_username || 'User',
    email: kc.tokenParsed?.email || '',
    roles: normalizeRoles([...realmRoles, ...clientRoles]),
    token: kc.token,
  };
}

// Evaluated once at module load — frozen for the lifetime of the app
const USE_KEYCLOAK =
  Boolean(import.meta.env.VITE_KEYCLOAK_URL) &&
  Boolean(import.meta.env.VITE_KEYCLOAK_REALM) &&
  Boolean(import.meta.env.VITE_KEYCLOAK_CLIENT_ID) &&
  import.meta.env.MODE !== 'test';

export function KeycloakProvider({ children }) {
  const kcRef  = useRef(null);
  const initOk = useRef(false); // guard against React dev-mode double-invoke

  // In dev mode (no Keycloak env vars), start as authenticated with no role gates
  const [authenticated, setAuthenticated] = useState(!USE_KEYCLOAK);
  const [user,          setUser]          = useState(null);
  const [loading,       setLoading]       = useState(USE_KEYCLOAK); // true only while KC inits

  useEffect(() => {
    /* ── Dev / Test mode ─────────────────────────────────────────────── */
    if (!USE_KEYCLOAK) {
      setAuthenticated(true);
      setUser(null);          // hasRole() always returns true in dev
      setAuthToken('mock-jwt-token-dev');
      setLoading(false);
      return;
    }

    /* Guard against React StrictMode double-invocation */
    if (initOk.current) return;
    initOk.current = true;

    const kc = new Keycloak({
      url:      import.meta.env.VITE_KEYCLOAK_URL,
      realm:    import.meta.env.VITE_KEYCLOAK_REALM,
      clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
    });
    kcRef.current = kc;

    kc.onTokenExpired = () => kc.updateToken(60).catch(() => kc.login());

    /*
     * onLoad: 'login-required'
     * ─────────────────────────────────────────────────────────────────
     * Keycloak handles ALL redirect logic:
     *   • Not authenticated → KC redirects to login page automatically
     *   • Auth code in URL  → KC exchanges it, resolves promise with auth=true
     * This eliminates the infinite-loop problem entirely because we NEVER
     * call kc.login() ourselves inside ProtectedRoute.
     */
    kc.init({
      onLoad:            'login-required',
      pkceMethod:        'S256',
      checkLoginIframe:  false,
    })
      .then((auth) => {
        if (auth) {
          const u = buildUserFromKC(kc);
          setUser(u);
          setAuthenticated(true);
          setAuthToken(kc.token);

          // Refresh token every 60 s
          setInterval(() => {
            kc.updateToken(70)
              .then((refreshed) => {
                if (refreshed) {
                  setAuthToken(kc.token);
                  setUser(buildUserFromKC(kc));
                }
              })
              .catch(() => kc.login());
          }, 60_000);
        }
        // auth=false with login-required is extremely rare; KC already redirected
        setLoading(false);
      })
      .catch((err) => {
        console.error('[Auth] Keycloak init error:', err);
        // Fail-open: show app, ProtectedRoute will catch unauthenticated state
        setAuthenticated(false);
        setUser(null);
        setLoading(false);
      });

    setAuthCallbacks({
      onExpired:      () => kcRef.current?.login(),
      onAccessDenied: () => { window.location.href = '/unauthorized'; },
    });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const login = useCallback(() => kcRef.current?.login(), []);

  const logout = useCallback(() => {
    if (kcRef.current) {
      kcRef.current.logout({ redirectUri: window.location.origin + '/register' });
    } else {
      setAuthenticated(false);
      setUser(null);
    }
  }, []);

  /** Returns true if user has any of the required roles. Always true in dev mode. */
  const hasRole = useCallback(
    (requiredRole) => {
      if (!USE_KEYCLOAK) return true;
      if (!user?.roles?.length) return false;
      const needed = Array.isArray(requiredRole) ? requiredRole : [requiredRole];
      return needed.some((r) => user.roles.includes(r));
    },
    [user]
  );

  const contextValue = useMemo(
    () => ({
      authenticated,
      user,
      loading,
      login,
      logout,
      hasRole,
      token: user?.token ?? null,
      isKeycloakMode: USE_KEYCLOAK,
    }),
    [authenticated, user, loading, login, logout, hasRole]
  );

  return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>;
}

KeycloakProvider.propTypes = {
  children: PropTypes.node.isRequired,
};
