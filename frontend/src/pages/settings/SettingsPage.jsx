import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { Settings, Eye, EyeOff, Save, Loader2, ShieldCheck, ExternalLink } from 'lucide-react';
import { settingsApi } from '../../api/settingsApi';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select';

export default function SettingsPage() {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [mode, setMode] = useState('free');
  const [apiKey, setApiKey] = useState('');
  const [cx, setCx] = useState('');
  const [showApiKey, setShowApiKey] = useState(false);

  // Track if API key was loaded masked (hasn't been changed yet)
  const [apiKeyMasked, setApiKeyMasked] = useState(true);

  useEffect(() => {
    settingsApi.getValidation()
      .then((res) => {
        const data = res.data.data;
        setMode(data['validation.mode'] || 'free');
        setApiKey(data['google.api.key'] || '');
        setCx(data['google.api.cx'] || '');
        setApiKeyMasked(true);
      })
      .catch(() => toast.error('Gagal memuat pengaturan'))
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async () => {
    if (mode === 'paid' && (!apiKey || apiKey.startsWith('****'))) {
      toast.error('Masukkan Google API Key terlebih dahulu');
      return;
    }
    if (mode === 'paid' && !cx) {
      toast.error('Masukkan Google CX (Search Engine ID) terlebih dahulu');
      return;
    }

    setSaving(true);
    try {
      const updates = { 'validation.mode': mode };

      // Only send API key if user actually changed it (not the masked version)
      if (!apiKeyMasked && apiKey) {
        updates['google.api.key'] = apiKey;
      }
      if (cx) {
        updates['google.api.cx'] = cx;
      }

      const res = await settingsApi.updateValidation(updates);
      const data = res.data.data;
      setApiKey(data['google.api.key'] || '');
      setCx(data['google.api.cx'] || '');
      setApiKeyMasked(true);
      setShowApiKey(false);
      toast.success('Pengaturan berhasil disimpan');
    } catch {
      toast.error('Gagal menyimpan pengaturan');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h2 className="text-xl font-semibold flex items-center gap-2">
          <Settings className="h-5 w-5" />
          Pengaturan Validasi
        </h2>
        <p className="text-sm text-muted-foreground mt-1">
          Konfigurasi sumber data untuk validasi wilayah.
        </p>
      </div>

      {/* Mode Selection */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base">Mode Validasi</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label>Mode</Label>
            <Select value={mode} onValueChange={setMode}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="free">
                  Gratis — Wikipedia + OpenStreetMap
                </SelectItem>
                <SelectItem value="paid">
                  Berbayar — + Google Custom Search (fallback)
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="rounded-lg border p-3 text-xs space-y-2">
            <p className="font-medium">Urutan pencarian:</p>
            <ol className="list-decimal list-inside space-y-1 text-muted-foreground">
              <li>
                <strong>Wikipedia Indonesia</strong> — pencarian utama
                <Badge variant="outline" className="ml-1.5 text-[10px] py-0">gratis</Badge>
              </li>
              <li>
                <strong>OpenStreetMap Nominatim</strong> — fallback pertama
                <Badge variant="outline" className="ml-1.5 text-[10px] py-0">gratis</Badge>
              </li>
              {mode === 'paid' && (
                <li>
                  <strong>Google Custom Search</strong> — fallback terakhir
                  <Badge variant="outline" className="ml-1.5 text-[10px] py-0 border-yellow-400 text-yellow-700">berbayar</Badge>
                </li>
              )}
            </ol>
            <p className="text-muted-foreground">
              {mode === 'free'
                ? 'Google Custom Search tidak digunakan. Jika Wikipedia dan Nominatim tidak menemukan hasil, validasi mengembalikan "Tidak Valid".'
                : 'Google Custom Search digunakan sebagai resort terakhir hanya jika kedua sumber gratis tidak menemukan hasil.'}
            </p>
          </div>
        </CardContent>
      </Card>

      {/* Google API Config */}
      <Card className={mode === 'free' ? 'opacity-50' : ''}>
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">Google Custom Search API</CardTitle>
            {mode === 'free' && (
              <Badge variant="secondary" className="text-xs">Tidak aktif (mode gratis)</Badge>
            )}
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="apiKey">API Key</Label>
            <div className="relative">
              <Input
                id="apiKey"
                type={showApiKey ? 'text' : 'password'}
                value={apiKey}
                onChange={(e) => {
                  setApiKey(e.target.value);
                  setApiKeyMasked(false);
                }}
                placeholder="Masukkan Google API Key"
                disabled={mode === 'free'}
                className="pr-10"
              />
              <button
                type="button"
                onClick={() => setShowApiKey(!showApiKey)}
                className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
              >
                {showApiKey ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
            <p className="text-xs text-muted-foreground">
              Dapatkan API Key dari{' '}
              <a
                href="https://console.cloud.google.com/apis/credentials"
                target="_blank"
                rel="noreferrer"
                className="text-blue-600 hover:underline inline-flex items-center gap-0.5"
              >
                Google Cloud Console <ExternalLink className="h-3 w-3" />
              </a>
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="cx">Search Engine ID (CX)</Label>
            <Input
              id="cx"
              value={cx}
              onChange={(e) => setCx(e.target.value)}
              placeholder="Masukkan CX / Search Engine ID"
              disabled={mode === 'free'}
            />
            <p className="text-xs text-muted-foreground">
              Buat Custom Search Engine di{' '}
              <a
                href="https://programmablesearchengine.google.com/"
                target="_blank"
                rel="noreferrer"
                className="text-blue-600 hover:underline inline-flex items-center gap-0.5"
              >
                Programmable Search Engine <ExternalLink className="h-3 w-3" />
              </a>
            </p>
          </div>

          {apiKey && !apiKey.startsWith('****') && apiKey.length > 10 && mode === 'paid' && (
            <div className="flex items-center gap-1.5 text-xs text-green-700">
              <ShieldCheck className="h-3.5 w-3.5" />
              API Key tersimpan terenkripsi (AES-GCM) di database.
            </div>
          )}
        </CardContent>
      </Card>

      {/* Save Button */}
      <div className="flex justify-end">
        <Button onClick={handleSave} disabled={saving}>
          {saving ? (
            <Loader2 className="h-4 w-4 mr-2 animate-spin" />
          ) : (
            <Save className="h-4 w-4 mr-2" />
          )}
          Simpan Pengaturan
        </Button>
      </div>
    </div>
  );
}
