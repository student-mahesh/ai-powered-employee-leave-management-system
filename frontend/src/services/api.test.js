// @vitest-environment jsdom
import { describe, expect, it, beforeEach, vi } from 'vitest';
import api from './api';

describe('API client', () => {
  beforeEach(() => {
    globalThis.localStorage = { getItem: key => key === 'leaveflow.token' ? 'test-token' : null };
  });

  it('uses the backend API base URL and forwards the stored bearer token', () => {
    const interceptor = api.interceptors.request.handlers[0].fulfilled;
    const config = interceptor({ headers: {} });
    expect(api.defaults.baseURL).toBe('http://localhost:8080/api');
    expect(config.headers.Authorization).toBe('Bearer test-token');
  });

  it('dispatches the unauthorized-session event for a 401 response', async () => {
    const listener = vi.fn();
    window.addEventListener('leaveflow:unauthorized', listener);
    await expect(api.interceptors.response.handlers[0].rejected({response: {status: 401}})).rejects.toBeDefined();
    expect(listener).toHaveBeenCalledTimes(1);
    window.removeEventListener('leaveflow:unauthorized', listener);
  });

  it('keeps backend connection failures distinct from credential failures', async () => {
    const {apiMessage} = await import('./errors');
    expect(apiMessage({request: {}})).toContain('Unable to connect to LeaveFlow');
    expect(apiMessage({response: {data: {message: 'Invalid credentials'}}})).toBe('Invalid credentials');
  });

  it('preserves the session and does not dispatch unauthorized for a 403 response', async () => {
    const listener = vi.fn();
    window.addEventListener('leaveflow:unauthorized', listener);
    await expect(api.interceptors.response.handlers[0].rejected({response: {status: 403}})).rejects.toBeDefined();
    expect(listener).not.toHaveBeenCalled();
    expect(localStorage.getItem('leaveflow.token')).toBe('test-token');
    window.removeEventListener('leaveflow:unauthorized', listener);
  });
});
