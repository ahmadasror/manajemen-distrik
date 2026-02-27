import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import AuditListPage from './AuditListPage';

vi.mock('../../api/auditTrailApi', () => ({
  auditTrailApi: { getAll: vi.fn() },
}));

import { auditTrailApi } from '../../api/auditTrailApi';

describe('AuditListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render table with audit data', async () => {
    auditTrailApi.getAll.mockResolvedValue({
      data: {
        data: {
          content: [
            { id: 1, entityType: 'USER', entityId: 1, action: 'SUBMIT_CREATE', performedBy: 'admin', ipAddress: '127.0.0.1', correlationId: 'abc-123', createdAt: '2024-01-01T00:00:00' },
          ],
          totalElements: 1,
        },
      },
    });

    render(<MemoryRouter><AuditListPage /></MemoryRouter>);

    await waitFor(() => {
      expect(screen.getByText('Audit Trail')).toBeInTheDocument();
    });
    expect(screen.getByText('admin')).toBeInTheDocument();
    expect(screen.getByText('127.0.0.1')).toBeInTheDocument();
  });

  it('should handle empty data', async () => {
    auditTrailApi.getAll.mockResolvedValue({
      data: { data: { content: [], totalElements: 0 } },
    });

    render(<MemoryRouter><AuditListPage /></MemoryRouter>);

    await waitFor(() => {
      expect(screen.getByText('Audit Trail')).toBeInTheDocument();
    });
  });
});
