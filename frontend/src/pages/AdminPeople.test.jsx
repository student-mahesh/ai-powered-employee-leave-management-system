// @vitest-environment jsdom
import React from 'react';
import {render, screen} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import '@testing-library/jest-dom/vitest';

const api = vi.hoisted(() => ({get: vi.fn()}));
vi.mock('../services/api', () => ({default: api}));
import AdminPeople from './AdminPeople';

describe('People & Audit tables', () => {
  afterEach(() => vi.resetAllMocks());

  it('maps employee associations while preserving manager and admin values', async () => {
    api.get.mockImplementation((url, config) => {
      if (url === '/employees' && config?.params?.role === 'MANAGER') {
        return Promise.resolve({data: {content: [{id: 5, fullName: 'Priya Manager'}]}});
      }
      if (url === '/employees') return Promise.resolve({data: {content: [
        {id: 6, fullName: 'Mahesh Savkar', email: 'mahesh@example.test', role: 'EMPLOYEE', departmentId: 1, managerId: 5},
        {id: 7, fullName: 'Unassigned Employee', email: 'unassigned@example.test', role: 'EMPLOYEE', departmentId: null, managerId: null},
        {id: 5, fullName: 'Priya Manager', email: 'priya@example.test', role: 'MANAGER', departmentId: null, managerId: null},
        {id: 1, fullName: 'Admin User', email: 'admin@example.test', role: 'ADMIN', departmentId: null, managerId: null},
      ]}});
      if (url === '/departments') return Promise.resolve({data: [{id: 1, name: 'Engineering'}]});
      if (url === '/leave-types') return Promise.resolve({data: [{id: 1, name: 'Annual Leave', active: true}]});
      if (url === '/audit-logs') return Promise.resolve({data: {content: [
        {id: 1, actorId: 1, action: 'EMPLOYEE_UPDATED', entityType: 'EMPLOYEE', entityId: 6, createdAt: '2026-01-03T10:00:00Z'},
        {id: 2, actorId: 5, action: 'LEAVE_TYPE_UPDATED', entityType: 'LEAVE_TYPE', entityId: 1, createdAt: '2026-01-03T11:00:00Z'},
        {id: 3, actorId: 99, action: 'CUSTOM_AUDIT_EVENT', entityType: 'LEAVE_BALANCE', entityId: 3, createdAt: '2026-01-03T12:00:00Z'},
      ]}});
      throw new Error(`Unexpected request: ${url}`);
    });

    render(<AdminPeople/>);

    const employeeRow = (await screen.findByText('Mahesh Savkar')).closest('tr');
    const unassignedRow = screen.getByText('Unassigned Employee').closest('tr');
    const managerRow = screen.getByText('priya@example.test').closest('tr');
    const adminRow = screen.getByText('admin@example.test').closest('tr');
    expect(employeeRow).toHaveTextContent('Engineering');
    expect(employeeRow).toHaveTextContent('Priya Manager');
    expect(unassignedRow).toHaveTextContent('Not assigned');
    expect(managerRow).toHaveTextContent('—');
    expect(adminRow).toHaveTextContent('—');
    expect(await screen.findByText('Employee Updated')).toBeInTheDocument();
    expect(screen.getByText('Leave Type Updated')).toBeInTheDocument();
    expect(screen.getByText('Custom Audit Event')).toBeInTheDocument();
    expect(screen.getByText('Unknown user')).toBeInTheDocument();
    expect(screen.getByText('Employee: Mahesh Savkar')).toBeInTheDocument();
    expect(screen.getByText('Leave Type: Annual Leave')).toBeInTheDocument();
    const balanceRow = screen.getByText('Leave Balance').closest('tr');
    expect(balanceRow).not.toHaveTextContent('#3');
  });
});
