import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ConfirmModal from './ConfirmModal';

describe('ConfirmModal', () => {
  const defaultProps = {
    open: true,
    title: 'Confirm Action',
    message: 'Are you sure?',
    onConfirm: vi.fn(),
    onCancel: vi.fn(),
    loading: false,
  };

  it('should render when open is true', () => {
    render(<ConfirmModal {...defaultProps} />);
    expect(screen.getByText('Confirm Action')).toBeInTheDocument();
    expect(screen.getByText('Are you sure?')).toBeInTheDocument();
  });

  it('should call onConfirm without remarks when showRemarks is false', () => {
    const onConfirm = vi.fn();
    render(<ConfirmModal {...defaultProps} onConfirm={onConfirm} />);
    fireEvent.click(screen.getByText('Confirm'));
    expect(onConfirm).toHaveBeenCalledWith(undefined);
  });

  it('should call onCancel when cancel is clicked', () => {
    const onCancel = vi.fn();
    render(<ConfirmModal {...defaultProps} onCancel={onCancel} />);
    fireEvent.click(screen.getByText('Cancel'));
    expect(onCancel).toHaveBeenCalled();
  });

  it('should show remarks textarea when showRemarks is true', () => {
    render(<ConfirmModal {...defaultProps} showRemarks={true} />);
    expect(screen.getByPlaceholderText('Enter remarks...')).toBeInTheDocument();
  });

  it('should not show remarks textarea when showRemarks is false', () => {
    render(<ConfirmModal {...defaultProps} showRemarks={false} />);
    expect(screen.queryByPlaceholderText('Enter remarks...')).not.toBeInTheDocument();
  });

  it('should call onConfirm with remarks text when showRemarks is true', () => {
    const onConfirm = vi.fn();
    render(<ConfirmModal {...defaultProps} onConfirm={onConfirm} showRemarks={true} />);
    fireEvent.change(screen.getByPlaceholderText('Enter remarks...'), {
      target: { value: 'My remark' },
    });
    fireEvent.click(screen.getByText('Confirm'));
    expect(onConfirm).toHaveBeenCalledWith('My remark');
  });
});
