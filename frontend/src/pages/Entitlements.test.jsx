// @vitest-environment jsdom
import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import '@testing-library/jest-dom/vitest';

const api = vi.hoisted(() => ({get: vi.fn()}));
vi.mock('../services/api', () => ({default: api}));
import Entitlements from './Entitlements';

describe('entitlement transaction history', () => {
  afterEach(() => vi.resetAllMocks());

  it('uses readable transaction labels, signed days, and no raw leave reference', async () => {
    api.get.mockImplementation(url => {
      if (url === '/employees') return Promise.resolve({data: {content: [{id: 6, fullName: 'Mahesh Savkar'}]}});
      if (url === '/leave-types') return Promise.resolve({data: [{id: 1, name: 'Annual Leave', active: true}]});
      if (url === '/leave-balances/employee/6') return Promise.resolve({data: []});
      if (url.startsWith('/leave-balances/history?')) return Promise.resolve({data: {content: [
        {id: 1, applicationId: 3, delta: '-4', createdAt: '2026-11-14T19:52:38'},
        {id: 2, applicationId: null, delta: '20', createdAt: '2026-11-14T18:33:46'},
      ]}});
      throw new Error(`Unexpected request: ${url}`);
    });

    render(<Entitlements/>);
    await screen.findByRole('option', {name: 'Mahesh Savkar'});
    const employeeSelect = document.querySelector('select.form-select');
    fireEvent.change(employeeSelect, {target: {value: '6'}});

    expect(await screen.findByText('Leave Deduction')).toBeInTheDocument();
    expect(screen.getByText('Entitlement Adjustment')).toBeInTheDocument();
    expect(screen.getByText('-4 days')).toBeInTheDocument();
    expect(screen.getByText('+20 days')).toBeInTheDocument();
    expect(screen.getByText('Approved leave request')).toBeInTheDocument();
    expect(screen.getByText('Leave entitlement updated')).toBeInTheDocument();
    expect(screen.queryByText('Leave request #3')).not.toBeInTheDocument();
    expect(screen.getByRole('columnheader', {name: 'Date & Time'})).toBeInTheDocument();
  });
});
