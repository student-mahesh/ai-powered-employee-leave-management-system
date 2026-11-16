// @vitest-environment jsdom
import React from 'react';
import {MemoryRouter} from 'react-router-dom';
import {render, screen} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import '@testing-library/jest-dom/vitest';

const api = vi.hoisted(() => ({get: vi.fn()}));
vi.mock('../services/api', () => ({default: api}));
import ApplyLeave from './ApplyLeave';

describe('apply leave', () => {
  afterEach(() => vi.resetAllMocks());

  it('excludes inactive leave types from new requests', async () => {
    api.get.mockResolvedValue({data: [
      {id: 1, name: 'Active Test Leave', active: true},
      {id: 2, name: 'Inactive Test Leave', active: false},
    ]});

    render(<MemoryRouter><ApplyLeave/></MemoryRouter>);

    expect(await screen.findByRole('option', {name: 'Active Test Leave'})).toBeInTheDocument();
    expect(screen.queryByRole('option', {name: 'Inactive Test Leave'})).not.toBeInTheDocument();
  });
});
