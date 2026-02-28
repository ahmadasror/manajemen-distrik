import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Check, X, Square, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { pendingActionApi } from '../../api/pendingActionApi';
import { StatusBadge, ActionTypeBadge } from '../../components/StatusBadge';
import JsonDiffViewer from '../../components/JsonDiffViewer';
import ConfirmModal from '../../components/ConfirmModal';
import { usePermission } from '../../hooks/usePermission';
import { formatDateTime } from '../../utils/formatters';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

function DescItem({ label, children }) {
  return (
    <div className="flex flex-col gap-1 py-3 border-b last:border-0">
      <dt className="text-xs font-medium text-muted-foreground uppercase tracking-wide">{label}</dt>
      <dd className="text-sm font-medium">{children || '-'}</dd>
    </div>
  );
}

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
      toast.error('Failed to load pending action');
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
        toast.success('Action approved');
      } else if (modal.action === 'reject') {
        await pendingActionApi.reject(id, remarks);
        toast.success('Action rejected');
      } else if (modal.action === 'cancel') {
        await pendingActionApi.cancel(id);
        toast.success('Action cancelled');
      }
      setModal({ open: false, action: null });
      fetchData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Operation failed');
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }
  if (!data) return <div className="text-center py-12 text-muted-foreground">Not found</div>;

  const isPending = data.status === 'PENDING';

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-2">
        <Button variant="outline" size="sm" onClick={() => navigate('/pending-actions')}>
          <ArrowLeft className="h-4 w-4" />
          Back
        </Button>
        {isPending && canApprovePending(data) && (
          <>
            <Button size="sm" onClick={() => setModal({ open: true, action: 'approve' })}>
              <Check className="h-4 w-4" />
              Approve
            </Button>
            <Button size="sm" variant="destructive" onClick={() => setModal({ open: true, action: 'reject' })}>
              <X className="h-4 w-4" />
              Reject
            </Button>
          </>
        )}
        {isPending && canCancelPending(data) && (
          <Button size="sm" variant="outline" onClick={() => setModal({ open: true, action: 'cancel' })}>
            <Square className="h-4 w-4" />
            Cancel
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Pending Action Details</CardTitle>
        </CardHeader>
        <CardContent>
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8">
            <DescItem label="ID">{data.id}</DescItem>
            <DescItem label="Status"><StatusBadge status={data.status} /></DescItem>
            <DescItem label="Entity Type">
              <Badge variant="outline">{data.entityType}</Badge>
            </DescItem>
            <DescItem label="Entity ID">{data.entityId || 'New'}</DescItem>
            <DescItem label="Action Type"><ActionTypeBadge type={data.actionType} /></DescItem>
            <DescItem label="Maker">{data.makerUsername}</DescItem>
            <DescItem label="Checker">{data.checkerUsername || '-'}</DescItem>
            <DescItem label="Remarks">{data.remarks || '-'}</DescItem>
            <DescItem label="Created">{formatDateTime(data.createdAt)}</DescItem>
            <DescItem label="Updated">{formatDateTime(data.updatedAt)}</DescItem>
          </dl>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Changes</CardTitle>
        </CardHeader>
        <CardContent>
          <JsonDiffViewer
            before={data.previousState}
            after={data.payload}
            changedFields={null}
          />
        </CardContent>
      </Card>

      <ConfirmModal
        open={modal.open}
        title={
          modal.action === 'approve' ? 'Approve Action'
            : modal.action === 'reject' ? 'Reject Action'
            : 'Cancel Action'
        }
        message={`Are you sure you want to ${modal.action} this pending action?`}
        onConfirm={handleAction}
        onCancel={() => setModal({ open: false, action: null })}
        loading={actionLoading}
        showRemarks={modal.action === 'approve' || modal.action === 'reject'}
      />
    </div>
  );
}
