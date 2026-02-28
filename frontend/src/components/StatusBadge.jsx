import { Badge } from '@/components/ui/badge';
import { formatStatus, formatActionType } from '../utils/formatters';

const STATUS_VARIANT = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'destructive',
  CANCELLED: 'secondary',
};

const ACTION_VARIANT = {
  CREATE: 'info',
  UPDATE: 'warning',
  DELETE: 'destructive',
};

export function StatusBadge({ status }) {
  const { text } = formatStatus(status);
  return <Badge variant={STATUS_VARIANT[status] || 'secondary'}>{text}</Badge>;
}

export function ActionTypeBadge({ type }) {
  const { text } = formatActionType(type);
  return <Badge variant={ACTION_VARIANT[type] || 'secondary'}>{text}</Badge>;
}
