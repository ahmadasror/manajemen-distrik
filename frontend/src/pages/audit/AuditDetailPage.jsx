import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Loader2 } from 'lucide-react';
import axiosInstance from '../../api/axiosInstance';
import JsonDiffViewer from '../../components/JsonDiffViewer';
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

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }
  if (!data) return <div className="text-center py-12 text-muted-foreground">Not found</div>;

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <Button variant="outline" size="sm" onClick={() => navigate('/audit-trail')}>
          <ArrowLeft className="h-4 w-4" />
          Back
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Audit Detail</CardTitle>
        </CardHeader>
        <CardContent>
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8">
            <DescItem label="ID">{data.id}</DescItem>
            <DescItem label="Action">
              <Badge variant="info">{data.action}</Badge>
            </DescItem>
            <DescItem label="Entity Type">
              <Badge variant="outline">{data.entityType}</Badge>
            </DescItem>
            <DescItem label="Entity ID">{data.entityId || '-'}</DescItem>
            <DescItem label="Performed By">{data.performedBy}</DescItem>
            <DescItem label="IP Address">{data.ipAddress || '-'}</DescItem>
            <DescItem label="Correlation ID">{data.correlationId || '-'}</DescItem>
            <DescItem label="Pending Action ID">{data.pendingActionId || '-'}</DescItem>
            <DescItem label="Date">{formatDateTime(data.createdAt)}</DescItem>
            {data.changedFields?.length > 0 && (
              <div className="flex flex-col gap-1 py-3 border-b col-span-full">
                <dt className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Changed Fields</dt>
                <dd className="flex flex-wrap gap-1 mt-1">
                  {data.changedFields.map((f) => (
                    <Badge key={f} variant="warning">{f}</Badge>
                  ))}
                </dd>
              </div>
            )}
          </dl>
        </CardContent>
      </Card>

      {(data.beforeState || data.afterState) && (
        <Card>
          <CardHeader>
            <CardTitle>State Changes</CardTitle>
          </CardHeader>
          <CardContent>
            <JsonDiffViewer
              before={data.beforeState}
              after={data.afterState}
              changedFields={data.changedFields}
            />
          </CardContent>
        </Card>
      )}
    </div>
  );
}
