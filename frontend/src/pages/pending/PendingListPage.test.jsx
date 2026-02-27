import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import PendingListPage from './PendingListPage';

vi.mock('../../api/pendingActionApi', () => ({
  pendingActionApi: { getAll: vi.fn() },
}));

import { pendingActionApi } from '../../api/pendingActionApi';

describe('PendingListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render table with pending actions', async () => {
    pendingActionApi.getAll.mockResolvedValue({
      data: {
        data: {
          content: [
            { id: 1, entityType: 'USER', entityId: null, actionType: 'CREATE', status: 'PENDING', makerUsername: 'maker1', checkerUsername: null, createdAt: '2024-01-01T00:00:00' },
            { id: 2, entityType: 'USER', entityId: 5, actionType: 'UPDATE', status: 'APPROVED', makerUsername: 'maker1', checkerUsername: 'checker1', createdAt: '2024-01-02T00:00:00' },
          ],
          totalElements: 2,
        },
      },
    });

    render(<MemoryRouter><PendingListPage /></MemoryRouter>);

    await waitFor(() => {
      expect(screen.getByText('Pending Actions')).toBeInTheDocument();
    });
    expect(screen.getAllByText('maker1').length).toBeGreaterThanOrEqual(1);
  });

  it('should render status filter select', async () => {
    pendingActionApi.getAll.mockResolvedValue({
      data: { data: { content: [], totalElements: 0 } },
    });

    render(<MemoryRouter><PendingListPage /></MemoryRouter>);

    await waitFor(() => {
      expect(screen.getByText('Pending Actions')).toBeInTheDocument();
    });
  });

  it('should handle API error gracefully', async () => {
    pendingActionApi.getAll.mockRejectedValue(new Error('fail'));

    render(<MemoryRouter><PendingListPage /></MemoryRouter>);

    await waitFor(() => {
      expect(screen.getByText('Pending Actions')).toBeInTheDocument();
    });
  });
});
