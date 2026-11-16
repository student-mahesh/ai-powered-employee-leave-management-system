// @vitest-environment jsdom
import React from 'react';
import {render, screen} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import '@testing-library/jest-dom/vitest';

const api = vi.hoisted(() => ({get: vi.fn()}));
vi.mock('../services/api', () => ({default: api}));
import AdminEmployees from './AdminEmployees';

describe('employee administration', () => {
  afterEach(() => vi.resetAllMocks());

  it('displays assigned department and manager names instead of IDs', async () => {
    api.get.mockImplementation((url, config) => {
      if (url === '/employees' && config?.params?.role === 'MANAGER') {
        return Promise.resolve({data: {content: [{id: 5, fullName: 'Priya Manager'}]}});
      }
      if (url === '/employees') {
        return Promise.resolve({data: {content: [
          {id: 6, fullName: 'Mahesh Savkar', email: 'mahesh@example.test', role: 'EMPLOYEE', departmentId: 1, managerId: 5},
          {id: 7, fullName: 'Unassigned Employee', email: 'unassigned@example.test', role: 'EMPLOYEE', departmentId: null, managerId: null},
        ]}});
      }
      if (url === '/departments') return Promise.resolve({data: [{id: 1, name: 'Engineering', code: 'ENG'}]});
      throw new Error(`Unexpected request: ${url}`);
    });

    render(<AdminEmployees/>);

    const assignedRow = (await screen.findByText('Mahesh Savkar')).closest('tr');
    const unassignedRow = screen.getByText('Unassigned Employee').closest('tr');
    expect(assignedRow).toHaveTextContent('Engineering');
    expect(assignedRow).toHaveTextContent('Priya Manager');
    expect(assignedRow).not.toHaveTextContent(' 1 ');
    expect(assignedRow).not.toHaveTextContent(' 5 ');
    expect(unassignedRow).toHaveTextContent('Not assigned');
  });
});
