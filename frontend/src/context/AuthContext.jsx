import React, {createContext, useCallback, useContext, useEffect, useState} from 'react';
import api from '../services/api';

const C = createContext(null);
const USER_KEY = 'leaveflow.user';
const TOKEN_KEY = 'leaveflow.token';
const readUser = () => { try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null'); } catch { return null; } };

export function AuthProvider({children}) {
  const [user, setUser] = useState(readUser);
  const [restoring, setRestoring] = useState(Boolean(localStorage.getItem(TOKEN_KEY)));
  const logout = useCallback(() => { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY); setUser(null); setRestoring(false); }, []);
  const login = useCallback(async values => {
    const {data} = await api.post('/auth/login', values);
    const session = {email: data.email, name: data.name, role: data.role};
    localStorage.setItem(TOKEN_KEY, data.token);
    localStorage.setItem(USER_KEY, JSON.stringify(session));
    setUser(session);
    return session;
  }, []);
  useEffect(() => { window.addEventListener('leaveflow:unauthorized', logout); return () => window.removeEventListener('leaveflow:unauthorized', logout); }, [logout]);
  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) { setRestoring(false); return; }
    api.get('/auth/me').then(({data}) => {
      const session = {email: data.email, name: data.name, role: data.role};
      localStorage.setItem(USER_KEY, JSON.stringify(session)); setUser(session);
    }).catch(error => { if (error.response?.status === 401) logout(); }).finally(() => setRestoring(false));
  }, [logout]);
  return <C.Provider value={{user, restoring, login, logout}}>{children}</C.Provider>;
}
export const useAuth = () => useContext(C);
