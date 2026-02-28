import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { userApi } from '../../api/userApi';
import { ROLES } from '../../utils/constants';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

const ALL_ROLES = Object.values(ROLES);

export default function UserFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const isEdit = Boolean(id);

  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    fullName: '',
    phone: '',
    roles: [],
    isActive: true,
  });
  const [errors, setErrors] = useState({});

  useEffect(() => {
    if (isEdit) {
      setLoading(true);
      userApi.getById(id)
        .then((res) => {
          const u = res.data.data;
          setForm({
            username: u.username || '',
            email: u.email || '',
            password: '',
            fullName: u.fullName || '',
            phone: u.phone || '',
            roles: u.roles ? [...u.roles] : [],
            isActive: u.isActive ?? true,
          });
        })
        .catch(() => toast.error('Failed to load user'))
        .finally(() => setLoading(false));
    }
  }, [id, isEdit]);

  const validate = () => {
    const errs = {};
    if (!isEdit && !form.username.trim()) errs.username = 'Username is required';
    if (!isEdit && form.username.trim() && !/^[a-zA-Z0-9._-]+$/.test(form.username))
      errs.username = 'Letters, numbers, dots, hyphens, underscores only';
    if (!isEdit && form.username.trim().length < 3)
      errs.username = 'Min 3 characters';
    if (!isEdit && !form.email.trim()) errs.email = 'Email is required';
    if (form.email.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))
      errs.email = 'Must be a valid email';
    if (!isEdit && !form.password) errs.password = 'Password is required';
    if (!isEdit && form.password && form.password.length < 6) errs.password = 'Min 6 characters';
    if (!isEdit && !form.fullName.trim()) errs.fullName = 'Full name is required';
    if (form.roles.length === 0) errs.roles = 'At least one role is required';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    setSubmitting(true);
    try {
      if (isEdit) {
        const payload = { ...form };
        if (!payload.password) delete payload.password;
        await userApi.update(id, payload);
        toast.success('Update request submitted for approval');
      } else {
        await userApi.create(form);
        toast.success('Create request submitted for approval');
      }
      navigate('/users');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Operation failed');
    } finally {
      setSubmitting(false);
    }
  };

  const toggleRole = (role) => {
    setForm((f) => ({
      ...f,
      roles: f.roles.includes(role) ? f.roles.filter((r) => r !== role) : [...f.roles, role],
    }));
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <Button variant="outline" size="sm" onClick={() => navigate('/users')}>
          <ArrowLeft className="h-4 w-4" />
          Back
        </Button>
      </div>

      <Card className="max-w-2xl">
        <CardHeader>
          <CardTitle>{isEdit ? 'Edit User' : 'Create New User'}</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-5">
            {!isEdit && (
              <div className="space-y-1.5">
                <Label htmlFor="username">Username</Label>
                <Input
                  id="username"
                  value={form.username}
                  onChange={(e) => setForm({ ...form, username: e.target.value })}
                  placeholder="Enter username"
                />
                {errors.username && <p className="text-xs text-destructive">{errors.username}</p>}
              </div>
            )}

            <div className="space-y-1.5">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                placeholder="Enter email"
              />
              {errors.email && <p className="text-xs text-destructive">{errors.email}</p>}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="password">
                {isEdit ? 'New Password (leave blank to keep current)' : 'Password'}
              </Label>
              <Input
                id="password"
                type="password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                placeholder="Enter password"
              />
              {errors.password && <p className="text-xs text-destructive">{errors.password}</p>}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="fullName">Full Name</Label>
              <Input
                id="fullName"
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                placeholder="Enter full name"
              />
              {errors.fullName && <p className="text-xs text-destructive">{errors.fullName}</p>}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="phone">Phone</Label>
              <Input
                id="phone"
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
                placeholder="Enter phone number"
              />
            </div>

            <div className="space-y-2">
              <Label>Roles</Label>
              <div className="flex flex-wrap gap-2">
                {ALL_ROLES.map((role) => (
                  <button
                    key={role}
                    type="button"
                    onClick={() => toggleRole(role)}
                    className={`px-3 py-1.5 rounded-md text-sm font-medium border transition-colors ${
                      form.roles.includes(role)
                        ? 'bg-primary text-primary-foreground border-primary'
                        : 'bg-background text-foreground border-input hover:bg-accent'
                    }`}
                  >
                    {role}
                  </button>
                ))}
              </div>
              {errors.roles && <p className="text-xs text-destructive">{errors.roles}</p>}
            </div>

            <div className="flex items-center gap-3">
              <Switch
                id="isActive"
                checked={form.isActive}
                onCheckedChange={(v) => setForm({ ...form, isActive: v })}
              />
              <Label htmlFor="isActive">Active</Label>
            </div>

            <div className="flex items-center gap-3 pt-2">
              <Button type="submit" disabled={submitting}>
                {submitting && <Loader2 className="h-4 w-4 animate-spin" />}
                {isEdit ? 'Submit Update' : 'Submit Create'}
              </Button>
              <Button type="button" variant="outline" onClick={() => navigate('/users')}>
                Cancel
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
