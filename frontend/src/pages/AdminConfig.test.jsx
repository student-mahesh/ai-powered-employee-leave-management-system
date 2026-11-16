// @vitest-environment jsdom
import React from 'react';
import {fireEvent, render, screen, within} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import '@testing-library/jest-dom/vitest';

const api = vi.hoisted(() => ({get: vi.fn(), put: vi.fn()}));
vi.mock('../services/api', () => ({default: api}));
import AdminConfig from './AdminConfig';

describe('leave type activation', () => {
  afterEach(() => vi.resetAllMocks());

  it('confirms and displays a successful deactivation without changing other records', async () => {
    api.get.mockImplementation(url => {
      if (url === '/departments') return Promise.resolve({data: []});
      if (url === '/leave-types') return Promise.resolve({data: [{id: 7, name: 'Safe Test Leave', active: true}]});
      if (url === '/holidays') return Promise.resolve({data: {content: []}});
      throw new Error(`Unexpected request: ${url}`);
    });
    api.put.mockResolvedValue({data: {id: 7, name: 'Safe Test Leave', active: false}});

    render(<AdminConfig/>);
    await screen.findByText('Safe Test Leave');
    fireEvent.click(screen.getByRole('button', {name: 'Deactivate'}));

    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveTextContent('Deactivate Safe Test Leave?');
    fireEvent.click(within(dialog).getByRole('button', {name: 'Deactivate'}));

    expect(await screen.findByText('Safe Test Leave is now inactive.')).toBeInTheDocument();
    expect(api.put).toHaveBeenCalledWith('/leave-types/7/active', null, {params: {value: false}});
    expect(screen.getByText('Inactive')).toBeInTheDocument();
  });
});
