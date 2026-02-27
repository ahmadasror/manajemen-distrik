import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Descriptions, Button, Space, Spin, Card, Tag } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import axiosInstance from '../../api/axiosInstance';
import JsonDiffViewer from '../../components/JsonDiffViewer';
import { formatDateTime } from '../../utils/formatters';

export default function AuditDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    axiosInstance.get(`/audit-trail/${id}`)
      .then((res) => setData(res.data.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />;
  if (!data) return <div>Not found</div>;

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/audit-trail')}>Back</Button>
      </Space>

      <Card title="Audit Detail" style={{ marginBottom: 16 }}>
        <Descriptions bordered column={2}>
          <Descriptions.Item label="ID">{data.id}</Descriptions.Item>
          <Descriptions.Item label="Action"><Tag color="blue">{data.action}</Tag></Descriptions.Item>
          <Descriptions.Item label="Entity Type"><Tag>{data.entityType}</Tag></Descriptions.Item>
          <Descriptions.Item label="Entity ID">{data.entityId || '-'}</Descriptions.Item>
          <Descriptions.Item label="Performed By">{data.performedBy}</Descriptions.Item>
          <Descriptions.Item label="IP Address">{data.ipAddress || '-'}</Descriptions.Item>
          <Descriptions.Item label="Correlation ID">{data.correlationId || '-'}</Descriptions.Item>
          <Descriptions.Item label="Pending Action ID">{data.pendingActionId || '-'}</Descriptions.Item>
          <Descriptions.Item label="Date" span={2}>{formatDateTime(data.createdAt)}</Descriptions.Item>
          {data.changedFields?.length > 0 && (
            <Descriptions.Item label="Changed Fields" span={2}>
              {data.changedFields.map((f) => <Tag key={f} color="orange">{f}</Tag>)}
            </Descriptions.Item>
          )}
        </Descriptions>
      </Card>

      {(data.beforeState || data.afterState) && (
        <Card title="State Changes">
          <JsonDiffViewer
            before={data.beforeState}
            after={data.afterState}
            changedFields={data.changedFields}
          />
        </Card>
      )}
    </div>
  );
}
