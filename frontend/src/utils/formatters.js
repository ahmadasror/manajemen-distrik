import dayjs from 'dayjs';

export const formatDate = (date) => {
  if (!date) return '-';
  return dayjs(date).format('DD MMM YYYY');
};

export const formatDateTime = (date) => {
  if (!date) return '-';
  return dayjs(date).format('DD MMM YYYY HH:mm:ss');
};

export const formatStatus = (status) => {
  const map = {
    PENDING: { color: 'gold', text: 'Pending' },
    APPROVED: { color: 'green', text: 'Approved' },
    REJECTED: { color: 'red', text: 'Rejected' },
    CANCELLED: { color: 'default', text: 'Cancelled' },
  };
  return map[status] || { color: 'default', text: status };
};

export const formatActionType = (type) => {
  const map = {
    CREATE: { color: 'blue', text: 'Create' },
    UPDATE: { color: 'orange', text: 'Update' },
    DELETE: { color: 'red', text: 'Delete' },
  };
  return map[type] || { color: 'default', text: type };
};
