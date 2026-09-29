import { createContext, useContext, useEffect, useState } from "react";
import { configureSession } from "../api/client";
import { authApi } from "../api";
const AuthContext = createContext(null);
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  async function restore() {
    setLoading(true);
    setError(null);
    try {
      await authApi.refresh();
    } catch (failure) {
      if (failure.status !== 401) setError(failure);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    configureSession(setUser);
    restore();
    return () => configureSession(() => {});
  }, []);
  return (
    <AuthContext.Provider value={{ user, loading, error, restore }}>
      {children}
    </AuthContext.Provider>
  );
}
export const useAuth = () => useContext(AuthContext);
