import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { StatusBadge, ActionTypeBadge } from './StatusBadge';

describe('StatusBadge', () => {
  it('should render Pending tag', () => {
    render(<StatusBadge status="PENDING" />);
    expect(screen.getByText('Pending')).toBeInTheDocument();
  });

  it('should render Approved tag', () => {
    render(<StatusBadge status="APPROVED" />);
    expect(screen.getByText('Approved')).toBeInTheDocument();
  });

  it('should render Rejected tag', () => {
    render(<StatusBadge status="REJECTED" />);
    expect(screen.getByText('Rejected')).toBeInTheDocument();
  });

  it('should render Cancelled tag', () => {
    render(<StatusBadge status="CANCELLED" />);
    expect(screen.getByText('Cancelled')).toBeInTheDocument();
  });
});

describe('ActionTypeBadge', () => {
  it('should render Create tag', () => {
    render(<ActionTypeBadge type="CREATE" />);
    expect(screen.getByText('Create')).toBeInTheDocument();
  });

  it('should render Update tag', () => {
    render(<ActionTypeBadge type="UPDATE" />);
    expect(screen.getByText('Update')).toBeInTheDocument();
  });

  it('should render Delete tag', () => {
    render(<ActionTypeBadge type="DELETE" />);
    expect(screen.getByText('Delete')).toBeInTheDocument();
  });
});
