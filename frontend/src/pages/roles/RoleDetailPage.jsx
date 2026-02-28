import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, UserPlus, Trash2, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { rolesApi } from '../../api/rolesApi';
import { userApi } from '../../api/userApi';
import { formatDateTime } from '../../utils/formatters';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select';
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from '@/components/ui/table';

const ROLE_VARIANT = {
  ADMIN: 'destructive',
  MAKER: 'info',
  CHECKER: 'success',
  VIEWER: 'secondary',
};

function DescItem({ label, children }) {
  return (
    <div className="flex flex-col gap-1 py-3 border-b last:border-0">
      <dt className="text-xs font-medium text-muted-foreground uppercase tracking-wide">{label}</dt>
      <dd className="text-sm font-medium">{children || '-'}</dd>
    </div>
  );
}

export default function RoleDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [role, setRole] = useState(null);
  const [loading, setLoading] = useState(true);
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [allUsers, setAllUsers] = useState([]);
  const [selectedUserId, setSelectedUserId] = useState('');
  const [assigning, setAssigning] = useState(false);
  const [removingId, setRemovingId] = useState(null);

  useEffect(() => {
    rolesApi.getById(id)
      .then((res) => setRole(res.data.data))
      .catch(() => toast.error('Failed to load role'))
      .finally(() => setLoading(false));
  }, [id]);

  const openAssignModal = () => {
    userApi.getAll({ size: 200 })
      .then((res) => {
        const assignedIds = new Set((role?.users || []).map((u) => u.id));
        const available = (res.data.data?.content || []).filter((u) => !assignedIds.has(u.id));
        setAllUsers(available);
      })
      .catch(() => toast.error('Failed to load users'));
    setSelectedUserId('');
    setAssignModalOpen(true);
  };

  const handleAssign = () => {
    if (!selectedUserId) return;
    setAssigning(true);
    rolesApi.assignUser(id, selectedUserId)
      .then((res) => {
        setRole(res.data.data);
        setAssignModalOpen(false);
        toast.success('User assigned to role');
      })
      .catch((err) => {
        toast.error(err.response?.data?.message || 'Failed to assign user');
      })
      .finally(() => setAssigning(false));
  };

  const handleRemove = (userId) => {
    setRemovingId(userId);
    rolesApi.removeUser(id, userId)
      .then((res) => {
        setRole(res.data.data);
        toast.success('User removed from role');
      })
      .catch((err) => {
        toast.error(err.response?.data?.message || 'Failed to remove user');
      })
      .finally(() => setRemovingId(null));
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }
  if (!role) return <div className="text-center py-12 text-muted-foreground">Role not found</div>;

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <Button variant="outline" size="sm" onClick={() => navigate('/roles')}>
          <ArrowLeft className="h-4 w-4" />
          Back
        </Button>
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center gap-3 pb-4">
          <CardTitle>Role Detail</CardTitle>
          <Badge variant={ROLE_VARIANT[role.name] || 'secondary'} className="text-sm px-3 py-1">
            {role.name}
          </Badge>
        </CardHeader>
        <CardContent>
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8">
            <DescItem label="Name">{role.name}</DescItem>
            <DescItem label="User Count">{role.userCount}</DescItem>
            <DescItem label="Description">{role.description || '-'}</DescItem>
            <div />
            <DescItem label="Created At">{formatDateTime(role.createdAt)}</DescItem>
            <DescItem label="Updated At">{formatDateTime(role.updatedAt)}</DescItem>
          </dl>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between pb-4">
          <CardTitle className="text-base">Users in this Role</CardTitle>
          <Button size="sm" onClick={openAssignModal}>
            <UserPlus className="h-4 w-4" />
            Assign User
          </Button>
        </CardHeader>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Username</TableHead>
                <TableHead>Full Name</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Action</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(role.users || []).length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center py-8 text-muted-foreground">
                    No users in this role
                  </TableCell>
                </TableRow>
              ) : (
                (role.users || []).map((u) => (
                  <TableRow key={u.id}>
                    <TableCell className="font-medium">{u.username}</TableCell>
                    <TableCell>{u.fullName}</TableCell>
                    <TableCell className="text-muted-foreground">{u.email}</TableCell>
                    <TableCell>
                      <Badge variant={u.isActive ? 'success' : 'destructive'}>
                        {u.isActive ? 'Active' : 'Inactive'}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex justify-end">
                        <Button
                          size="sm"
                          variant="destructive"
                          disabled={removingId === u.id}
                          onClick={() => handleRemove(u.id)}
                        >
                          {removingId === u.id
                            ? <Loader2 className="h-4 w-4 animate-spin" />
                            : <Trash2 className="h-4 w-4" />
                          }
                          Remove
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* Assign User Dialog */}
      <Dialog open={assignModalOpen} onOpenChange={setAssignModalOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Assign User to Role</DialogTitle>
          </DialogHeader>
          <div className="py-2">
            <Select value={selectedUserId} onValueChange={setSelectedUserId}>
              <SelectTrigger>
                <SelectValue placeholder="Select a user..." />
              </SelectTrigger>
              <SelectContent>
                {allUsers.length === 0 ? (
                  <SelectItem value="_empty" disabled>No available users</SelectItem>
                ) : (
                  allUsers.map((u) => (
                    <SelectItem key={u.id} value={String(u.id)}>
                      {u.username} — {u.fullName}
                    </SelectItem>
                  ))
                )}
              </SelectContent>
            </Select>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setAssignModalOpen(false)} disabled={assigning}>
              Cancel
            </Button>
            <Button onClick={handleAssign} disabled={!selectedUserId || assigning}>
              {assigning && <Loader2 className="h-4 w-4 animate-spin" />}
              Assign
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
