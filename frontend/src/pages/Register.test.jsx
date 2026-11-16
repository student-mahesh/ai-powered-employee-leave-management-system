// @vitest-environment jsdom
import React from 'react';
import {MemoryRouter} from 'react-router-dom';
import {cleanup, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import '@testing-library/jest-dom/vitest';

const api = vi.hoisted(() => ({post: vi.fn()}));
vi.mock('../context/AuthContext', () => ({useAuth: () => ({user: null})}));
vi.mock('../services/api', () => ({default: api}));
import Register from './Register';

const renderPage = () => render(<MemoryRouter><Register /></MemoryRouter>);
describe('registration page', () => {
  beforeEach(() => api.post.mockReset()); afterEach(() => { cleanup(); vi.restoreAllMocks(); });
  it('renders and validates required fields, email, and confirmation', () => {
    renderPage(); fireEvent.click(screen.getByRole('button', {name: 'Create account'}));
    expect(screen.getByRole('alert')).toHaveTextContent('Complete all required fields.');
    const [name, email] = screen.getAllByRole('textbox'); const password = document.querySelector('[name="password"]'); const confirmation = document.querySelector('[name="confirmPassword"]');
    fireEvent.change(name, {target: {value: 'New User'}}); fireEvent.change(email, {target: {value: 'invalid'}});
    fireEvent.change(password, {target: {value: 'secure-pass-123'}}); fireEvent.change(confirmation, {target: {value: 'different-pass'}});
    fireEvent.click(screen.getByRole('button', {name: 'Create account'}));
    expect(screen.getByRole('alert')).toHaveTextContent('Enter a valid email address.');
  });
  it('submits only safe registration fields and shows duplicate errors', async () => {
    api.post.mockRejectedValue({response: {data: {message: 'Email already exists'}}}); renderPage();
    const [name, email] = screen.getAllByRole('textbox'); const password = document.querySelector('[name="password"]'); const confirmation = document.querySelector('[name="confirmPassword"]');
    fireEvent.change(name, {target: {value: 'New User'}}); fireEvent.change(email, {target: {value: 'new@example.test'}});
    fireEvent.change(password, {target: {value: 'secure-pass-123'}}); fireEvent.change(confirmation, {target: {value: 'secure-pass-123'}});
    fireEvent.click(screen.getByRole('button', {name: 'Create account'}));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/auth/register', {fullName: 'New User', email: 'new@example.test', password: 'secure-pass-123'}));
    expect(screen.getByRole('alert')).toHaveTextContent('Email already exists');
  });
});
