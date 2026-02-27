import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Descriptions, Button, Space, Spin, Card, message, Tag } from 'antd';
import { ArrowLeftOutlined, CheckOutlined, CloseOutlined, StopOutlined } from '@ant-design/icons';
import { pendingActionApi } from '../../api/pendingActionApi';
import { StatusBadge, ActionTypeBadge } from '../../components/StatusBadge';
import JsonDiffViewer from '../../components/JsonDiffViewer';
import ConfirmModal from '../../components/ConfirmModal';
import { usePermission } from '../../hooks/usePermission';
import { formatDateTime } from '../../utils/formatters';

export default function PendingDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [modal, setModal] = useState({ open: false, action: null });
  const { canApprovePending, canCancelPending } = usePermission();

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await pendingActionApi.getById(id);
      setData(res.data.data);
    } catch {
      message.error('Failed to load pending action');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [id]);

  const handleAction = async (remarks) => {
    setActionLoading(true);
    try {
      if (modal.action === 'approve') {
        await pendingActionApi.approve(id, remarks);
        message.success('Action approved');
      } else if (modal.action === 'reject') {
        await pendingActionApi.reject(id, remarks);
        message.success('Action rejected');
      } else if (modal.action === 'cancel') {
        await pendingActionApi.cancel(id);
        message.success('Action cancelled');
      }
      setModal({ open: false, action: null });
      fetchData();
    } catch (err) {
      message.error(err.response?.data?.message || 'Operation failed');
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />;
  if (!data) return <div>Not found</div>;

  const isPending = data.status === 'PENDING';

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/pending-actions')}>Back</Button>
        {isPending && canApprovePending(data) && (
          <>
            <Button type="primary" icon={<CheckOutlined />}
              onClick={() => setModal({ open: true, action: 'approve' })}>Approve</Button>
            <Button danger icon={<CloseOutlined />}
              onClick={() => setModal({ open: true, action: 'reject' })}>Reject</Button>
          </>
        )}
        {isPending && canCancelPending(data) && (
          <Button icon={<StopOutlined />}
            onClick={() => setModal({ open: true, action: 'cancel' })}>Cancel</Button>
        )}
      </Space>

      <Card title="Pending Action Details" style={{ marginBottom: 16 }}>
        <Descriptions bordered column={2}>
          <Descriptions.Item label="ID">{data.id}</Descriptions.Item>
          <Descriptions.Item label="Status"><StatusBadge status={data.status} /></Descriptions.Item>
          <Descriptions.Item label="Entity Type"><Tag>{data.entityType}</Tag></Descriptions.Item>
          <Descriptions.Item label="Entity ID">{data.entityId || 'New'}</Descriptions.Item>
          <Descriptions.Item label="Action Type"><ActionTypeBadge type={data.actionType} /></Descriptions.Item>
          <Descriptions.Item label="Maker">{data.makerUsername}</Descriptions.Item>
          <Descriptions.Item label="Checker">{data.checkerUsername || '-'}</Descriptions.Item>
          <Descriptions.Item label="Remarks">{data.remarks || '-'}</Descriptions.Item>
          <Descriptions.Item label="Created">{formatDateTime(data.createdAt)}</Descriptions.Item>
          <Descriptions.Item label="Updated">{formatDateTime(data.updatedAt)}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="Changes">
        <JsonDiffViewer
          before={data.previousState}
          after={data.payload}
          changedFields={null}
        />
      </Card>

      <ConfirmModal
        open={modal.open}
        title={`${modal.action === 'approve' ? 'Approve' : modal.action === 'reject' ? 'Reject' : 'Cancel'} Action`}
        message={`Are you sure you want to ${modal.action} this pending action?`}
        onConfirm={handleAction}
        onCancel={() => setModal({ open: false, action: null })}
        loading={actionLoading}
        showRemarks={modal.action === 'approve' || modal.action === 'reject'}
      />
    </div>
  );
}
