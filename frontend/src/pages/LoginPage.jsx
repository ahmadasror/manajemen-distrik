import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldCheck, Loader2 } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';

export default function LoginPage() {
  const { user, loading, login } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (user) {
      navigate('/dashboard', { replace: true });
    }
  }, [user, navigate]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900">
        <Loader2 className="h-8 w-8 animate-spin text-white" />
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900">
      <div className="w-full max-w-sm px-4">
        <div className="flex justify-center mb-6">
          <div className="flex items-center justify-center h-14 w-14 rounded-2xl bg-primary shadow-lg shadow-primary/30">
            <ShieldCheck className="h-8 w-8 text-white" />
          </div>
        </div>

        <Card className="shadow-2xl border-0">
          <CardHeader className="text-center pb-4">
            <CardTitle className="text-2xl font-bold">User Management</CardTitle>
            <CardDescription>Sign in to continue to the District Management System</CardDescription>
          </CardHeader>
          <CardContent>
            <Button className="w-full" onClick={login}>
              <ShieldCheck className="h-4 w-4 mr-2" />
              Sign In with Keycloak
            </Button>
          </CardContent>
        </Card>

        <p className="text-center text-xs text-slate-400 mt-4">
          District Management System
        </p>
      </div>
    </div>
  );
}
