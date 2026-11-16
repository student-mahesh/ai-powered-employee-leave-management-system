// @vitest-environment jsdom
import React from 'react';
import {MemoryRouter} from 'react-router-dom';
import {cleanup, render, screen} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import '@testing-library/jest-dom/vitest';

const auth = vi.hoisted(() => ({current: {user: {email: 'admin@example.test', name: 'Admin', role: 'ADMIN'}, logout: vi.fn()}}));
vi.mock('../context/AuthContext', () => ({useAuth: () => auth.current}));
import Layout from './Layout';

describe('admin navigation', () => {
  beforeEach(() => { auth.current = {user: {email: 'admin@example.test', name: 'Admin', role: 'ADMIN'}, logout: vi.fn()}; });
  afterEach(cleanup);
  it('exposes the existing People & audit route to administrators', () => {
    render(<MemoryRouter><Layout><div>Content</div></Layout></MemoryRouter>);
    const link = screen.getByRole('link', {name: /people & audit/i});
    expect(link).toHaveAttribute('href', '/admin/people');
  });

  it('does not show admin navigation to employees', () => {
    auth.current = {user: {email: 'employee@example.test', name: 'Employee', role: 'EMPLOYEE'}, logout: vi.fn()};
    render(<MemoryRouter><Layout><div>Content</div></Layout></MemoryRouter>);
    expect(screen.queryByRole('link', {name: /people & audit/i})).not.toBeInTheDocument();
  });
});
