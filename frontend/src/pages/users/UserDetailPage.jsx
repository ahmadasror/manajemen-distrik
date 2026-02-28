import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Pencil, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { userApi } from '../../api/userApi';
import { auditTrailApi } from '../../api/auditTrailApi';
import { usePermission } from '../../hooks/usePermission';
import { formatDateTime } from '../../utils/formatters';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { Card, CardContent } from '@/components/ui/card';
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table';

function DescItem({ label, children }) {
  return (
    <div className="flex flex-col gap-1 py-3 border-b last:border-0">
      <dt className="text-xs font-medium text-muted-foreground uppercase tracking-wide">{label}</dt>
      <dd className="text-sm font-medium">{children}</dd>
    </div>
  );
}

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
      toast.error('Failed to load user');
    }).finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }
  if (!user) return <div className="text-center py-12 text-muted-foreground">User not found</div>;

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <Button variant="outline" size="sm" onClick={() => navigate('/users')}>
          <ArrowLeft className="h-4 w-4" />
          Back
        </Button>
        {canCreate && (
          <Button size="sm" onClick={() => navigate(`/users/${id}/edit`)}>
            <Pencil className="h-4 w-4" />
            Edit
          </Button>
        )}
      </div>

      <Tabs defaultValue="details">
        <TabsList>
          <TabsTrigger value="details">Details</TabsTrigger>
          <TabsTrigger value="audit">Audit Trail</TabsTrigger>
        </TabsList>

        <TabsContent value="details">
          <Card>
            <CardContent className="p-6">
              <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8">
                <DescItem label="Username">{user.username}</DescItem>
                <DescItem label="Full Name">{user.fullName}</DescItem>
                <DescItem label="Email">{user.email}</DescItem>
                <DescItem label="Phone">{user.phone || '-'}</DescItem>
                <DescItem label="Status">
                  <Badge variant={user.isActive ? 'success' : 'destructive'}>
                    {user.isActive ? 'Active' : 'Inactive'}
                  </Badge>
                </DescItem>
                <DescItem label="Roles">
                  <div className="flex flex-wrap gap-1">
                    {user.roles?.map((r) => (
                      <Badge key={r} variant="info">{r}</Badge>
                    ))}
                  </div>
                </DescItem>
                <DescItem label="Created At">{formatDateTime(user.createdAt)}</DescItem>
                <DescItem label="Updated At">{formatDateTime(user.updatedAt)}</DescItem>
                <DescItem label="Created By">{user.createdBy || '-'}</DescItem>
                <DescItem label="Updated By">{user.updatedBy || '-'}</DescItem>
                <DescItem label="Version">{user.version}</DescItem>
              </dl>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="audit">
          <Card>
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Action</TableHead>
                    <TableHead>Performed By</TableHead>
                    <TableHead>Date</TableHead>
                    <TableHead>Correlation ID</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {audits.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={4} className="text-center py-8 text-muted-foreground">
                        No audit entries
                      </TableCell>
                    </TableRow>
                  ) : (
                    audits.map((a) => (
                      <TableRow key={a.id}>
                        <TableCell><Badge variant="secondary">{a.action}</Badge></TableCell>
                        <TableCell>{a.performedBy}</TableCell>
                        <TableCell className="text-xs text-muted-foreground">{formatDateTime(a.createdAt)}</TableCell>
                        <TableCell className="text-xs text-muted-foreground truncate max-w-48">{a.correlationId}</TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
