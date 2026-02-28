import { useEffect, useState } from 'react';
import { Users, ClipboardCheck, ScrollText, UserCheck, Loader2 } from 'lucide-react';
import axiosInstance from '../../api/axiosInstance';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

const STAT_CARDS = [
  {
    key: 'totalUsers',
    title: 'Total Users',
    icon: Users,
    className: 'text-blue-600',
    bg: 'bg-blue-50',
  },
  {
    key: 'activeUsers',
    title: 'Active Users',
    icon: UserCheck,
    className: 'text-green-600',
    bg: 'bg-green-50',
  },
  {
    key: 'pendingActions',
    title: 'Pending Actions',
    icon: ClipboardCheck,
    className: 'text-yellow-600',
    bg: 'bg-yellow-50',
  },
  {
    key: 'totalAuditEntries',
    title: 'Audit Entries',
    icon: ScrollText,
    className: 'text-purple-600',
    bg: 'bg-purple-50',
  },
];

export default function DashboardPage() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    axiosInstance.get('/dashboard/stats')
      .then((res) => setStats(res.data.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div>
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-foreground">Dashboard</h2>
        <p className="text-sm text-muted-foreground mt-1">Overview of your system</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {STAT_CARDS.map(({ key, title, icon: Icon, className, bg }) => (
          <Card key={key} className="hover:shadow-md transition-shadow">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                {title}
              </CardTitle>
              <div className={`h-9 w-9 rounded-lg ${bg} flex items-center justify-center`}>
                <Icon className={`h-5 w-5 ${className}`} />
              </div>
            </CardHeader>
            <CardContent>
              <div className={`text-3xl font-bold ${className}`}>
                {stats?.[key] ?? 0}
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
