// @vitest-environment jsdom
import {cleanup, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';
import AdminPolicy from './AdminPolicy';

const api = vi.hoisted(() => ({get: vi.fn(), post: vi.fn()}));
vi.mock('../services/api', () => ({default: api}));

describe('AdminPolicy', () => {
  afterEach(() => { cleanup(); vi.resetAllMocks(); });

  it('loads and displays existing uploaded documents', async () => {
    api.get.mockResolvedValueOnce({data: [{id: 1, fileName: 'leave-policy.pdf', version: null, createdAt: '2026-11-16T13:18:08', uploadedById: 1}]});
    render(<AdminPolicy/>);

    expect(await screen.findByText('leave-policy.pdf')).toBeTruthy();
    expect(api.get).toHaveBeenCalledWith('/ai/policies');
  });

  it('keeps the successful upload message and refreshes the document list', async () => {
    api.get.mockResolvedValueOnce({data: []}).mockResolvedValueOnce({data: [{id: 2, fileName: 'leave-policy.pdf', version: '2026', createdAt: '2026-11-16T13:18:08', uploadedById: 1}]});
    api.post.mockResolvedValueOnce({data: {fileName: 'leave-policy.pdf', version: '2026'}});
    const {container} = render(<AdminPolicy/>);

    const pdf = new File(['policy'], 'leave-policy.pdf', {type: 'application/pdf'});
    fireEvent.change(container.querySelector('input[type="file"]'), {target: {files: [pdf]}});
    fireEvent.submit(container.querySelector('form'));

    await waitFor(() => expect(screen.getByText('Uploaded leave-policy.pdf (2026).')).toBeTruthy());
    expect(await screen.findByText('leave-policy.pdf')).toBeTruthy();
    expect(api.post).toHaveBeenCalledWith('/ai/policies', expect.any(FormData));
    expect(api.post.mock.calls[0][1].get('file')).toBe(pdf);
  });

  it('shows a backend error message when one is returned', async () => {
    api.get.mockResolvedValueOnce({data: []});
    api.post.mockRejectedValueOnce({response: {data: {message: 'Only PDF policy files are supported'}}});
    const {container} = render(<AdminPolicy/>);

    const pdf = new File(['policy'], 'not-a-policy.txt', {type: 'text/plain'});
    fireEvent.change(container.querySelector('input[type="file"]'), {target: {files: [pdf]}});
    fireEvent.submit(container.querySelector('form'));

    expect(await screen.findByText('Only PDF policy files are supported')).toBeTruthy();
  });
});
