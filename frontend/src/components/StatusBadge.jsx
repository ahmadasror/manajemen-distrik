import { Tag } from 'antd';
import { formatStatus, formatActionType } from '../utils/formatters';

export function StatusBadge({ status }) {
  const { color, text } = formatStatus(status);
  return <Tag color={color}>{text}</Tag>;
}

export function ActionTypeBadge({ type }) {
  const { color, text } = formatActionType(type);
  return <Tag color={color}>{text}</Tag>;
}
