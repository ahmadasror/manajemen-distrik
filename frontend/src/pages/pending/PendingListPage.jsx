import { useEffect, useState, useCallback } from 'react';
import { Table, Button, Select, Space, Tag } from 'antd';
import { EyeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { pendingActionApi } from '../../api/pendingActionApi';
import { StatusBadge, ActionTypeBadge } from '../../components/StatusBadge';
import { formatDateTime } from '../../utils/formatters';

export default function PendingListPage() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState(undefined);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const navigate = useNavigate();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await pendingActionApi.getAll({
        page: pagination.current - 1,
        size: pagination.pageSize,
        status: statusFilter,
      });
      const result = res.data.data;
      setData(result.content);
      setPagination((prev) => ({ ...prev, total: result.totalElements }));
    } catch {
      // handled by interceptor
    } finally {
      setLoading(false);
    }
  }, [pagination.current, pagination.pageSize, statusFilter]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: 'Entity Type', dataIndex: 'entityType', key: 'entityType', render: (v) => <Tag>{v}</Tag> },
    { title: 'Entity ID', dataIndex: 'entityId', key: 'entityId', render: (v) => v || 'New' },
    { title: 'Action', dataIndex: 'actionType', key: 'actionType', render: (v) => <ActionTypeBadge type={v} /> },
    { title: 'Status', dataIndex: 'status', key: 'status', render: (v) => <StatusBadge status={v} /> },
    { title: 'Maker', dataIndex: 'makerUsername', key: 'makerUsername' },
    { title: 'Checker', dataIndex: 'checkerUsername', key: 'checkerUsername', render: (v) => v || '-' },
    { title: 'Created', dataIndex: 'createdAt', key: 'createdAt', render: formatDateTime },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Button type="link" icon={<EyeOutlined />} onClick={() => navigate(`/pending-actions/${record.id}`)}>
          View
        </Button>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>Pending Actions</h2>
        <Space>
          <Select
            placeholder="Filter by status"
            allowClear
            style={{ width: 150 }}
            value={statusFilter}
            onChange={(v) => { setStatusFilter(v); setPagination(p => ({ ...p, current: 1 })); }}
            options={[
              { label: 'Pending', value: 'PENDING' },
              { label: 'Approved', value: 'APPROVED' },
              { label: 'Rejected', value: 'REJECTED' },
              { label: 'Cancelled', value: 'CANCELLED' },
            ]}
          />
        </Space>
      </div>
      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showTotal: (total) => `Total ${total} actions`,
        }}
        onChange={(pag) => setPagination({ ...pagination, current: pag.current, pageSize: pag.pageSize })}
      />
    </div>
  );
}
