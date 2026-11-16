import React, {useState} from 'react';
import {Link, Navigate, useNavigate} from 'react-router-dom';
import {useAuth} from '../context/AuthContext';
import api from '../services/api';
import {apiMessage} from '../services/errors';

export default function Register() {
  const {user} = useAuth(); const go = useNavigate();
  const [values, setValues] = useState({fullName: '', email: '', password: '', confirmPassword: ''});
  const [error, setError] = useState(''); const [success, setSuccess] = useState(''); const [busy, setBusy] = useState(false); const [showPassword, setShowPassword] = useState(false);
  if (user) return <Navigate to="/" replace/>;
  const update = event => setValues(current => ({...current, [event.target.name]: event.target.value}));
  const submit = async event => {
    event.preventDefault(); setError(''); setSuccess('');
    if (!values.fullName.trim() || !values.email.trim() || !values.password || !values.confirmPassword) return setError('Complete all required fields.');
    if (!/^\S+@\S+\.\S+$/.test(values.email)) return setError('Enter a valid email address.');
    if (values.password.length < 8 || values.password.length > 100) return setError('Password must be 8–100 characters.');
    if (values.password !== values.confirmPassword) return setError('Passwords do not match.');
    setBusy(true);
    try { await api.post('/auth/register', {fullName: values.fullName.trim(), email: values.email.trim(), password: values.password}); setSuccess('Registration successful. Redirecting to sign in…'); window.setTimeout(() => go('/login', {replace: true}), 900); }
    catch (requestError) { setError(apiMessage(requestError, 'We could not create your account. Please try again.')); }
    finally { setBusy(false); }
  };
  return <main className="login-bg"><form onSubmit={submit} className="card login-card" noValidate><div className="login-brand">LeaveFlow</div><h1 className="h4 mb-4">Create your account</h1>{error&&<div role="alert" className="alert alert-danger app-alert">{error}</div>}{success&&<div role="status" className="alert alert-success app-alert">{success}</div>}<label className="form-label">Full name <span className="text-danger">*</span></label><input name="fullName" className="form-control mb-3" value={values.fullName} onChange={update} autoComplete="name" required/><label className="form-label">Work email <span className="text-danger">*</span></label><input name="email" className="form-control mb-3" value={values.email} onChange={update} type="email" autoComplete="email" required/><label className="form-label">Password <span className="text-danger">*</span></label><div className="input-group mb-3"><input name="password" className="form-control" value={values.password} onChange={update} type={showPassword ? 'text' : 'password'} autoComplete="new-password" required/><button type="button" className="btn btn-outline-secondary" onClick={() => setShowPassword(value => !value)}>{showPassword ? 'Hide' : 'Show'}</button></div><label className="form-label">Confirm password <span className="text-danger">*</span></label><input name="confirmPassword" className="form-control mb-4" value={values.confirmPassword} onChange={update} type={showPassword ? 'text' : 'password'} autoComplete="new-password" required/><button className="btn btn-primary w-100" disabled={busy}>{busy ? <><span className="spinner-border spinner-border-sm me-2"/>Creating account…</> : 'Create account'}</button><p className="text-center text-muted small mt-3 mb-0">Already have an account? <Link to="/login">Login</Link></p></form></main>;
}
