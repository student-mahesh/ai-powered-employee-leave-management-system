// @vitest-environment jsdom
import React from 'react';
import {act, cleanup, render, screen, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import '@testing-library/jest-dom/vitest';

vi.mock('../services/api', () => ({default: {get: vi.fn()}}));
import {AuthProvider, useAuth} from './AuthContext';
import api from '../services/api';

function SessionState() {
  const {user, logout, restoring} = useAuth();
  return <><span data-testid="session">{user?.email || 'signed-out'}</span><span data-testid="restoring">{String(restoring)}</span><button onClick={logout}>Logout</button></>;
}

describe('AuthProvider session handling', () => {
  afterEach(cleanup);
  beforeEach(() => {
    const values = new Map();
    globalThis.localStorage = {clear: () => values.clear(), getItem: key => values.get(key) || null, removeItem: key => values.delete(key), setItem: (key, value) => values.set(key, String(value))};
    api.get.mockResolvedValue({data: {email: 'employee@example.test', name: 'Employee', role: 'EMPLOYEE'}});
    localStorage.setItem('leaveflow.user', JSON.stringify({email: 'employee@example.test', name: 'Employee', role: 'EMPLOYEE'}));
  });

  it('retains a valid client session after a 403 event', () => {
    render(<AuthProvider><SessionState /></AuthProvider>);
    act(() => window.dispatchEvent(new Event('leaveflow:forbidden')));
    expect(screen.getByTestId('session')).toHaveTextContent('employee@example.test');
  });

  it('clears token and user on logout or an unauthorized event', () => {
    localStorage.setItem('leaveflow.token', 'token');
    render(<AuthProvider><SessionState /></AuthProvider>);
    act(() => window.dispatchEvent(new Event('leaveflow:unauthorized')));
    expect(screen.getByTestId('session')).toHaveTextContent('signed-out');
    expect(localStorage.getItem('leaveflow.token')).toBeNull();
    expect(localStorage.getItem('leaveflow.user')).toBeNull();
  });

  it('preserves a stored session when restoring it fails because of a network error', async () => {
    localStorage.setItem('leaveflow.token', 'token');
    api.get.mockRejectedValueOnce({request: {}});

    render(<AuthProvider><SessionState /></AuthProvider>);

    await waitFor(() => expect(api.get).toHaveBeenCalledWith('/auth/me'));
    await waitFor(() => expect(screen.getByTestId('restoring')).toHaveTextContent('false'));
    expect(screen.getByTestId('session')).toHaveTextContent('employee@example.test');
    expect(localStorage.getItem('leaveflow.token')).toBe('token');
  });

  it('preserves a stored session when restoration receives a 403', async () => {
    localStorage.setItem('leaveflow.token', 'token');
    api.get.mockRejectedValueOnce({response: {status: 403}});

    render(<AuthProvider><SessionState /></AuthProvider>);

    await waitFor(() => expect(screen.getByTestId('restoring')).toHaveTextContent('false'));
    expect(screen.getByTestId('session')).toHaveTextContent('employee@example.test');
    expect(localStorage.getItem('leaveflow.token')).toBe('token');
  });

  it('clears a stored session only when session restoration receives a 401', async () => {
    localStorage.setItem('leaveflow.token', 'expired-token');
    api.get.mockRejectedValueOnce({response: {status: 401}});

    render(<AuthProvider><SessionState /></AuthProvider>);

    await waitFor(() => expect(screen.getByTestId('session')).toHaveTextContent('signed-out'));
    expect(localStorage.getItem('leaveflow.token')).toBeNull();
  });
});
