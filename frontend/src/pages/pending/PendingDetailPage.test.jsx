import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import PendingDetailPage from './PendingDetailPage';

vi.mock('../../api/pendingActionApi', () => ({
  pendingActionApi: { getById: vi.fn(), approve: vi.fn(), reject: vi.fn(), cancel: vi.fn() },
}));
vi.mock('../../hooks/usePermission', () => ({ usePermission: vi.fn() }));
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return { ...actual, message: { error: vi.fn(), success: vi.fn() } };
});

import { pendingActionApi } from '../../api/pendingActionApi';
import { usePermission } from '../../hooks/usePermission';

const mockData = {
  id: 1, entityType: 'USER', entityId: null, actionType: 'CREATE', status: 'PENDING',
  makerUsername: 'maker1', makerId: 2, checkerUsername: null,
  payload: { username: 'newuser', email: 'new@test.com' },
  previousState: null, remarks: null,
  createdAt: '2024-01-01T00:00:00', updatedAt: '2024-01-01T00:00:00',
};

describe('PendingDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render pending action details', async () => {
    pendingActionApi.getById.mockResolvedValue({ data: { data: mockData } });
    usePermission.mockReturnValue({
      canApprovePending: () => false,
      canCancelPending: () => false,
    });

    render(
      <MemoryRouter initialEntries={['/pending-actions/1']}>
        <Routes><Route path="/pending-actions/:id" element={<PendingDetailPage />} /></Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Pending Action Details')).toBeInTheDocument();
    });
    expect(screen.getByText('maker1')).toBeInTheDocument();
    expect(screen.getByText('Changes')).toBeInTheDocument();
  });

  it('should show Approve and Reject buttons when canApprovePending', async () => {
    pendingActionApi.getById.mockResolvedValue({ data: { data: mockData } });
    usePermission.mockReturnValue({
      canApprovePending: () => true,
      canCancelPending: () => false,
    });

    render(
      <MemoryRouter initialEntries={['/pending-actions/1']}>
        <Routes><Route path="/pending-actions/:id" element={<PendingDetailPage />} /></Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Approve')).toBeInTheDocument();
    });
    expect(screen.getByText('Reject')).toBeInTheDocument();
  });

  it('should show Cancel button when canCancelPending', async () => {
    pendingActionApi.getById.mockResolvedValue({ data: { data: mockData } });
    usePermission.mockReturnValue({
      canApprovePending: () => false,
      canCancelPending: () => true,
    });

    render(
      <MemoryRouter initialEntries={['/pending-actions/1']}>
        <Routes><Route path="/pending-actions/:id" element={<PendingDetailPage />} /></Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Cancel')).toBeInTheDocument();
    });
  });

  it('should hide action buttons when status is not PENDING', async () => {
    const approvedData = { ...mockData, status: 'APPROVED', checkerUsername: 'checker1' };
    pendingActionApi.getById.mockResolvedValue({ data: { data: approvedData } });
    usePermission.mockReturnValue({
      canApprovePending: () => true,
      canCancelPending: () => false,
    });

    render(
      <MemoryRouter initialEntries={['/pending-actions/1']}>
        <Routes><Route path="/pending-actions/:id" element={<PendingDetailPage />} /></Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Pending Action Details')).toBeInTheDocument();
    });
    expect(screen.queryByText('Approve')).not.toBeInTheDocument();
    expect(screen.queryByText('Reject')).not.toBeInTheDocument();
  });

  it('should show Not found when data is null', async () => {
    pendingActionApi.getById.mockRejectedValue(new Error('fail'));

    render(
      <MemoryRouter initialEntries={['/pending-actions/1']}>
        <Routes><Route path="/pending-actions/:id" element={<PendingDetailPage />} /></Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Not found')).toBeInTheDocument();
    });
  });
});
