import React, {useState} from 'react';
import {Link, Navigate, useNavigate} from 'react-router-dom';
import {useAuth} from '../context/AuthContext';
import {apiMessage} from '../services/errors';
import './Login.css';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const {login, user} = useAuth();
  const go = useNavigate();
  if (user) return <Navigate to="/" replace/>;
  const submit = async event => {
    event.preventDefault(); setBusy(true); setError('');
    try { await login({email, password}); go('/', {replace: true}); }
    catch (requestError) { setError(apiMessage(requestError, 'We could not sign you in. Check your details and try again.')); }
    finally { setBusy(false); }
  };
  return <main className="login-bg"><form onSubmit={submit} className="card login-card" noValidate><div className="login-brand">LeaveFlow</div><h1 className="h4 mb-4">Sign in</h1>{error&&<div className="alert alert-danger app-alert"><i className="bi bi-exclamation-circle"/>{error}</div>}<label className="form-label">Work email <span className="text-danger">*</span></label><input className="form-control mb-3" value={email} onChange={event=>setEmail(event.target.value)} type="email" autoComplete="email" required/><label className="form-label">Password <span className="text-danger">*</span></label><input className="form-control mb-4" value={password} onChange={event=>setPassword(event.target.value)} type="password" autoComplete="current-password" required/><button className="btn btn-primary w-100" disabled={busy}>{busy?<><span className="spinner-border spinner-border-sm me-2"/>Signing in…</>:'Sign in'}</button><p className="text-center text-muted small mt-3 mb-0">Don&apos;t have an account? <Link to="/register">Register</Link></p></form></main>;
}
