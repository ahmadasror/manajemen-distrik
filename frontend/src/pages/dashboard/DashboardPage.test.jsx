import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import DashboardPage from './DashboardPage';

vi.mock('../../api/axiosInstance', () => ({
  default: {
    get: vi.fn(),
  },
}));

import axiosInstance from '../../api/axiosInstance';

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should display stats after loading', async () => {
    axiosInstance.get.mockResolvedValue({
      data: {
        data: {
          totalUsers: 42,
          activeUsers: 38,
          pendingActions: 5,
          totalAuditEntries: 150,
        },
      },
    });

    render(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByText('Dashboard')).toBeInTheDocument();
    });

    expect(screen.getByText('Total Users')).toBeInTheDocument();
    expect(screen.getByText('Active Users')).toBeInTheDocument();
    expect(screen.getByText('Pending Actions')).toBeInTheDocument();
    expect(screen.getByText('Audit Entries')).toBeInTheDocument();
    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText('38')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByText('150')).toBeInTheDocument();
  });

  it('should handle API error gracefully', async () => {
    axiosInstance.get.mockRejectedValue(new Error('Network error'));

    render(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByText('Dashboard')).toBeInTheDocument();
    });

    // Values default to 0
    expect(screen.getByText('Total Users')).toBeInTheDocument();
  });
});
