import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Descriptions, Tag, Button, Space, Spin, Tabs, Table, message } from 'antd';
import { EditOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { userApi } from '../../api/userApi';
import { auditTrailApi } from '../../api/auditTrailApi';
import { usePermission } from '../../hooks/usePermission';
import { formatDateTime } from '../../utils/formatters';

export default function UserDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [audits, setAudits] = useState([]);
  const [loading, setLoading] = useState(true);
  const { canCreate } = usePermission();

  useEffect(() => {
    setLoading(true);
    Promise.all([
      userApi.getById(id),
      auditTrailApi.getByEntity('USER', id).catch(() => ({ data: { data: { content: [] } } })),
    ]).then(([userRes, auditRes]) => {
      setUser(userRes.data.data);
      setAudits(auditRes.data.data?.content || []);
    }).catch(() => {
      message.error('Failed to load user');
    }).finally(() => setLoading(false));
  }, [id]);

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />;
  if (!user) return <div>User not found</div>;

  const auditColumns = [
    { title: 'Action', dataIndex: 'action', key: 'action', render: (v) => <Tag>{v}</Tag> },
    { title: 'Performed By', dataIndex: 'performedBy', key: 'performedBy' },
    { title: 'Date', dataIndex: 'createdAt', key: 'createdAt', render: formatDateTime },
    { title: 'Correlation ID', dataIndex: 'correlationId', key: 'correlationId', ellipsis: true },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/users')}>Back</Button>
        {canCreate && <Button type="primary" icon={<EditOutlined />} onClick={() => navigate(`/users/${id}/edit`)}>Edit</Button>}
      </Space>

      <Tabs items={[
        {
          key: 'details',
          label: 'Details',
          children: (
            <Descriptions bordered column={2}>
              <Descriptions.Item label="Username">{user.username}</Descriptions.Item>
              <Descriptions.Item label="Full Name">{user.fullName}</Descriptions.Item>
              <Descriptions.Item label="Email">{user.email}</Descriptions.Item>
              <Descriptions.Item label="Phone">{user.phone || '-'}</Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={user.isActive ? 'green' : 'red'}>{user.isActive ? 'Active' : 'Inactive'}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Roles">
                {user.roles?.map((r) => <Tag key={r} color="blue">{r}</Tag>)}
              </Descriptions.Item>
              <Descriptions.Item label="Created At">{formatDateTime(user.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="Updated At">{formatDateTime(user.updatedAt)}</Descriptions.Item>
              <Descriptions.Item label="Created By">{user.createdBy || '-'}</Descriptions.Item>
              <Descriptions.Item label="Updated By">{user.updatedBy || '-'}</Descriptions.Item>
              <Descriptions.Item label="Version">{user.version}</Descriptions.Item>
            </Descriptions>
          ),
        },
        {
          key: 'audit',
          label: 'Audit Trail',
          children: (
            <Table
              columns={auditColumns}
              dataSource={audits}
              rowKey="id"
              pagination={{ pageSize: 5 }}
              size="small"
            />
          ),
        },
      ]} />
    </div>
  );
}
