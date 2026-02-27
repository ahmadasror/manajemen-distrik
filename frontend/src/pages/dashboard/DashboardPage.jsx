import { useEffect, useState } from 'react';
import { Row, Col, Card, Statistic, Spin } from 'antd';
import { UserOutlined, CheckSquareOutlined, AuditOutlined, TeamOutlined } from '@ant-design/icons';
import axiosInstance from '../../api/axiosInstance';

export default function DashboardPage() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    axiosInstance.get('/dashboard/stats')
      .then((res) => setStats(res.data.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />;

  const cards = [
    { title: 'Total Users', value: stats?.totalUsers || 0, icon: <TeamOutlined />, color: '#1677ff' },
    { title: 'Active Users', value: stats?.activeUsers || 0, icon: <UserOutlined />, color: '#52c41a' },
    { title: 'Pending Actions', value: stats?.pendingActions || 0, icon: <CheckSquareOutlined />, color: '#faad14' },
    { title: 'Audit Entries', value: stats?.totalAuditEntries || 0, icon: <AuditOutlined />, color: '#722ed1' },
  ];

  return (
    <div>
      <h2>Dashboard</h2>
      <Row gutter={[16, 16]}>
        {cards.map((card) => (
          <Col xs={24} sm={12} lg={6} key={card.title}>
            <Card>
              <Statistic
                title={card.title}
                value={card.value}
                prefix={card.icon}
                valueStyle={{ color: card.color }}
              />
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  );
}
