import { useEffect, useState, useCallback } from 'react';
import { Table, Button, Input, Space, Tag, message } from 'antd';
import { PlusOutlined, SearchOutlined, EyeOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { userApi } from '../../api/userApi';
import { usePermission } from '../../hooks/usePermission';
import { formatDateTime } from '../../utils/formatters';

export default function UserListPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const navigate = useNavigate();
  const { canCreate } = usePermission();

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const res = await userApi.getAll({
        page: pagination.current - 1,
        size: pagination.pageSize,
        search: search || undefined,
      });
      const data = res.data.data;
      setUsers(data.content);
      setPagination((prev) => ({ ...prev, total: data.totalElements }));
    } catch {
      message.error('Failed to fetch users');
    } finally {
      setLoading(false);
    }
  }, [pagination.current, pagination.pageSize, search]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleDelete = async (id) => {
    try {
      await userApi.delete(id);
      message.success('Delete request submitted for approval');
      fetchUsers();
    } catch (err) {
      message.error(err.response?.data?.message || 'Failed to submit delete request');
    }
  };

  const columns = [
    { title: 'Username', dataIndex: 'username', key: 'username' },
    { title: 'Full Name', dataIndex: 'fullName', key: 'fullName' },
    { title: 'Email', dataIndex: 'email', key: 'email' },
    {
      title: 'Roles',
      dataIndex: 'roles',
      key: 'roles',
      render: (roles) => roles?.map((r) => <Tag key={r} color="blue">{r}</Tag>),
    },
    {
      title: 'Status',
      dataIndex: 'isActive',
      key: 'isActive',
      render: (active) => <Tag color={active ? 'green' : 'red'}>{active ? 'Active' : 'Inactive'}</Tag>,
    },
    {
      title: 'Created',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: formatDateTime,
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Button type="link" icon={<EyeOutlined />} onClick={() => navigate(`/users/${record.id}`)} />
          {canCreate && (
            <>
              <Button type="link" icon={<EditOutlined />} onClick={() => navigate(`/users/${record.id}/edit`)} />
              <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)} />
            </>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>Users</h2>
        <Space>
          <Input
            placeholder="Search users..."
            prefix={<SearchOutlined />}
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPagination(p => ({ ...p, current: 1 })); }}
            style={{ width: 250 }}
            allowClear
          />
          {canCreate && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/users/new')}>
              Add User
            </Button>
          )}
        </Space>
      </div>
      <Table
        columns={columns}
        dataSource={users}
        rowKey="id"
        loading={loading}
        pagination={{
          ...pagination,
          showSizeChanger: true,
          showTotal: (total) => `Total ${total} users`,
        }}
        onChange={(pag) => setPagination({ ...pagination, current: pag.current, pageSize: pag.pageSize })}
      />
    </div>
  );
}
