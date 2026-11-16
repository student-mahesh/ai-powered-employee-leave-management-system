// @vitest-environment jsdom
import React from 'react';
import {MemoryRouter} from 'react-router-dom';
import {cleanup, render, screen} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import '@testing-library/jest-dom/vitest';

const auth = vi.hoisted(() => ({current: {user: null, restoring: false, logout: vi.fn(), login: vi.fn()}}));
vi.mock('./context/AuthContext', () => ({useAuth: () => auth.current}));
vi.mock('./services/api', () => ({default: {get: vi.fn(() => Promise.resolve({data: {content: []}})), post: vi.fn(), put: vi.fn()}}));
import App from './App';

describe('protected routes', () => {
  beforeEach(() => { auth.current = {user: null, restoring: false, logout: vi.fn(), login: vi.fn()}; });
  afterEach(cleanup);

  it('redirects an anonymous user from a protected route to login', () => {
    render(<MemoryRouter initialEntries={['/profile']}><App /></MemoryRouter>);
    expect(screen.getByRole('heading', {name: 'Sign in'})).toBeInTheDocument();
  });

  it('redirects an employee away from manager-only routes', () => {
    auth.current = {user: {email: 'employee@example.test', name: 'Employee', role: 'EMPLOYEE'}, restoring: false, logout: vi.fn(), login: vi.fn()};
    render(<MemoryRouter initialEntries={['/approvals']}><App /></MemoryRouter>);
    expect(screen.getByRole('heading', {name: 'Dashboard'})).toBeInTheDocument();
  });

  it('allows an admin to reach the existing People & audit route', () => {
    auth.current = {user: {email: 'admin@example.test', name: 'Admin', role: 'ADMIN'}, restoring: false, logout: vi.fn(), login: vi.fn()};
    render(<MemoryRouter initialEntries={['/admin/people']}><App /></MemoryRouter>);
    expect(screen.getByRole('heading', {name: 'People & Audit'})).toBeInTheDocument();
  });
});
