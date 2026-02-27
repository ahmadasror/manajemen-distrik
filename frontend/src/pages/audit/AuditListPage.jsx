import { useEffect, useState, useCallback } from 'react';
import { Table, Tag } from 'antd';
import { useNavigate } from 'react-router-dom';
import { auditTrailApi } from '../../api/auditTrailApi';
import { formatDateTime } from '../../utils/formatters';

export default function AuditListPage() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const navigate = useNavigate();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await auditTrailApi.getAll({
        page: pagination.current - 1,
        size: pagination.pageSize,
      });
      const result = res.data.data;
      setData(result.content);
      setPagination((prev) => ({ ...prev, total: result.totalElements }));
    } catch {
      // handled by interceptor
    } finally {
      setLoading(false);
    }
  }, [pagination.current, pagination.pageSize]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: 'Entity Type', dataIndex: 'entityType', key: 'entityType', render: (v) => <Tag>{v}</Tag> },
    { title: 'Entity ID', dataIndex: 'entityId', key: 'entityId', render: (v) => v || '-' },
    { title: 'Action', dataIndex: 'action', key: 'action', render: (v) => <Tag color="blue">{v}</Tag> },
    { title: 'Performed By', dataIndex: 'performedBy', key: 'performedBy' },
    { title: 'IP Address', dataIndex: 'ipAddress', key: 'ipAddress', render: (v) => v || '-' },
    { title: 'Correlation ID', dataIndex: 'correlationId', key: 'correlationId', ellipsis: true },
    { title: 'Date', dataIndex: 'createdAt', key: 'createdAt', render: formatDateTime },
  ];

  return (
    <div>
      <h2>Audit Trail</h2>
      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showTotal: (total) => `Total ${total} entries`,
        }}
        onChange={(pag) => setPagination({ ...pagination, current: pag.current, pageSize: pag.pageSize })}
        onRow={(record) => ({
          onClick: () => navigate(`/audit-trail/${record.id}`),
          style: { cursor: 'pointer' },
        })}
      />
    </div>
  );
}
