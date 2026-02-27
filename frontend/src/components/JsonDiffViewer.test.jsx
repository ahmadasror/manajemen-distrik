import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import JsonDiffViewer from './JsonDiffViewer';

describe('JsonDiffViewer', () => {
  it('should show No data when both before and after are null', () => {
    render(<JsonDiffViewer before={null} after={null} changedFields={[]} />);
    expect(screen.getByText('No data')).toBeInTheDocument();
  });

  it('should show No data when both are undefined', () => {
    render(<JsonDiffViewer changedFields={[]} />);
    expect(screen.getByText('No data')).toBeInTheDocument();
  });

  it('should render table with fields from before and after', () => {
    const before = { username: 'admin', email: 'old@test.com' };
    const after = { username: 'admin', email: 'new@test.com' };
    render(<JsonDiffViewer before={before} after={after} changedFields={['email']} />);
    expect(screen.getByText('username')).toBeInTheDocument();
    expect(screen.getByText('email')).toBeInTheDocument();
  });

  it('should filter out passwordHash field', () => {
    const before = { username: 'admin', passwordHash: 'secret123' };
    const after = { username: 'admin', passwordHash: 'newsecret' };
    render(<JsonDiffViewer before={before} after={after} changedFields={['passwordHash']} />);
    expect(screen.queryByText('passwordHash')).not.toBeInTheDocument();
    expect(screen.getByText('username')).toBeInTheDocument();
  });

  it('should render with only before state', () => {
    const before = { username: 'admin', email: 'test@test.com' };
    render(<JsonDiffViewer before={before} after={null} changedFields={[]} />);
    expect(screen.getByText('username')).toBeInTheDocument();
  });

  it('should render with only after state', () => {
    const after = { username: 'newuser', email: 'new@test.com' };
    render(<JsonDiffViewer before={null} after={after} changedFields={[]} />);
    expect(screen.getByText('username')).toBeInTheDocument();
  });

  it('should display string values from before and after', () => {
    const before = { fullName: 'Old Name' };
    const after = { fullName: 'New Name' };
    render(<JsonDiffViewer before={before} after={after} changedFields={['fullName']} />);
    expect(screen.getByText('Old Name')).toBeInTheDocument();
    expect(screen.getByText('New Name')).toBeInTheDocument();
  });

  it('should display dash for null values', () => {
    const before = { phone: null };
    const after = { phone: '123456' };
    render(<JsonDiffViewer before={before} after={after} changedFields={['phone']} />);
    expect(screen.getByText('-')).toBeInTheDocument();
    expect(screen.getByText('123456')).toBeInTheDocument();
  });
});
