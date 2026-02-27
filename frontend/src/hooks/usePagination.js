import { useState, useCallback } from 'react';
import { PAGE_SIZE } from '../utils/constants';

export function usePagination(initialSize = PAGE_SIZE) {
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: initialSize,
    total: 0,
  });

  const handleTableChange = useCallback((pag) => {
    setPagination((prev) => ({
      ...prev,
      current: pag.current,
      pageSize: pag.pageSize,
    }));
  }, []);

  const updateTotal = useCallback((total) => {
    setPagination((prev) => ({ ...prev, total }));
  }, []);

  const getParams = useCallback(() => ({
    page: pagination.current - 1,
    size: pagination.pageSize,
  }), [pagination.current, pagination.pageSize]);

  return { pagination, handleTableChange, updateTotal, getParams };
}
