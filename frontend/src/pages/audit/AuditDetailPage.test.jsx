import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import AuditDetailPage from './AuditDetailPage';

vi.mock('../../api/axiosInstance', () => ({
  default: { get: vi.fn() },
}));

import axiosInstance from '../../api/axiosInstance';

describe('AuditDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render audit detail', async () => {
    axiosInstance.get.mockResolvedValue({
      data: {
        data: {
          id: 1, entityType: 'USER', entityId: 5, action: 'APPROVE_CREATE',
          performedBy: 'checker1', ipAddress: '10.0.0.1', correlationId: 'corr-123',
          pendingActionId: 1, createdAt: '2024-01-01T00:00:00',
          beforeState: null, afterState: { username: 'newuser', email: 'new@test.com' },
          changedFields: ['username', 'email'],
        },
      },
    });

    render(
      <MemoryRouter initialEntries={['/audit-trail/1']}>
        <Routes><Route path="/audit-trail/:id" element={<AuditDetailPage />} /></Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Audit Detail')).toBeInTheDocument();
    });
    expect(screen.getByText('checker1')).toBeInTheDocument();
    expect(screen.getByText('10.0.0.1')).toBeInTheDocument();
    expect(screen.getByText('State Changes')).toBeInTheDocument();
  });

  it('should show Not found when data is null', async () => {
    axiosInstance.get.mockRejectedValue(new Error('fail'));

    render(
      <MemoryRouter initialEntries={['/audit-trail/1']}>
        <Routes><Route path="/audit-trail/:id" element={<AuditDetailPage />} /></Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Not found')).toBeInTheDocument();
    });
  });

  it('should show changed fields as tags', async () => {
    axiosInstance.get.mockResolvedValue({
      data: {
        data: {
          id: 1, entityType: 'USER', entityId: 5, action: 'APPROVE_UPDATE',
          performedBy: 'checker1', ipAddress: null, correlationId: null,
          pendingActionId: 2, createdAt: '2024-01-01T00:00:00',
          beforeState: { email: 'old@test.com' }, afterState: { email: 'new@test.com' },
          changedFields: ['email'],
        },
      },
    });

    render(
      <MemoryRouter initialEntries={['/audit-trail/1']}>
        <Routes><Route path="/audit-trail/:id" element={<AuditDetailPage />} /></Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Changed Fields')).toBeInTheDocument();
    });
    // 'email' may appear in multiple places (changed fields, diff viewer), so check at least one exists
    expect(screen.getAllByText('email').length).toBeGreaterThanOrEqual(1);
  });
});
