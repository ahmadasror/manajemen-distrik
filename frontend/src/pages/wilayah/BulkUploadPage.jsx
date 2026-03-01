import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { Upload, FileText, AlertCircle, CheckCircle2, Loader2, ExternalLink } from 'lucide-react';
import { bulkUploadApi } from '../../api/bulkUploadApi';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

const EXPECTED_HEADERS = ['ProvinceID', 'ProvinceName', 'StateID', 'StateName', 'DistrictID', 'DistrictName', 'SubDistrictID', 'SubDistrictName', 'ZipCode'];

function parseCsvLine(line) {
  const tokens = [];
  let sb = '';
  let inQuotes = false;
  for (let i = 0; i < line.length; i++) {
    const c = line[i];
    if (c === '"') { inQuotes = !inQuotes; }
    else if (c === ',' && !inQuotes) { tokens.push(sb); sb = ''; }
    else { sb += c; }
  }
  tokens.push(sb);
  return tokens;
}

export default function BulkUploadPage() {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);

  const [file, setFile] = useState(null);
  const [previewRows, setPreviewRows] = useState([]);
  const [headerError, setHeaderError] = useState('');
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState(null);

  const handleFileChange = (e) => {
    const f = e.target.files?.[0];
    if (!f) return;
    setFile(f);
    setResult(null);
    setHeaderError('');
    setPreviewRows([]);

    const reader = new FileReader();
    reader.onload = (ev) => {
      const text = ev.target.result;
      const lines = text.split('\n').filter((l) => l.trim());
      if (lines.length === 0) { setHeaderError('File is empty'); return; }

      const headers = parseCsvLine(lines[0]);
      const normalizedHeaders = headers.map((h) => h.trim().replace(/^"|"$/g, ''));
      const valid = EXPECTED_HEADERS.every((h, i) => normalizedHeaders[i]?.toLowerCase() === h.toLowerCase());
      if (!valid) {
        setHeaderError(`Invalid headers. Expected: ${EXPECTED_HEADERS.join(', ')}`);
        return;
      }

      const rows = [];
      for (let i = 1; i <= Math.min(20, lines.length - 1); i++) {
        const values = parseCsvLine(lines[i]).map((v) => v.trim().replace(/^"|"$/g, ''));
        const row = {};
        normalizedHeaders.forEach((h, idx) => { row[h] = values[idx] || ''; });
        rows.push(row);
      }
      setPreviewRows(rows);
    };
    reader.readAsText(f);
  };

  const handleUpload = async () => {
    if (!file) return;
    setUploading(true);
    try {
      const res = await bulkUploadApi.uploadWilayah(file);
      const data = res.data.data;
      setResult(data);
      toast.success('Upload berhasil — menunggu persetujuan');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Upload gagal');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h2 className="text-2xl font-bold">Bulk Upload Wilayah</h2>
        <p className="text-sm text-muted-foreground">Upload CSV untuk memperbarui data wilayah secara massal (memerlukan persetujuan checker)</p>
      </div>

      {/* Format info */}
      <div className="flex items-start gap-2 p-3 rounded-lg bg-muted text-sm">
        <FileText className="h-4 w-4 mt-0.5 shrink-0 text-muted-foreground" />
        <span>Format CSV: <code className="font-mono text-xs bg-background px-1 rounded border">{EXPECTED_HEADERS.join(', ')}</code></span>
      </div>

      {/* Upload card */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Pilih File CSV</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div
            className="border-2 border-dashed border-muted-foreground/25 rounded-lg p-8 text-center cursor-pointer hover:border-primary/50 transition-colors"
            onClick={() => fileInputRef.current?.click()}
          >
            <Upload className="h-8 w-8 mx-auto text-muted-foreground mb-2" />
            {file ? (
              <div>
                <p className="font-medium">{file.name}</p>
                <p className="text-sm text-muted-foreground">{(file.size / 1024).toFixed(1)} KB</p>
              </div>
            ) : (
              <p className="text-muted-foreground">Klik untuk memilih file CSV</p>
            )}
          </div>
          <input
            ref={fileInputRef}
            type="file"
            accept=".csv"
            className="hidden"
            onChange={handleFileChange}
          />

          {headerError && (
            <div className="flex items-start gap-2 p-3 rounded-lg bg-destructive/10 text-destructive text-sm">
              <AlertCircle className="h-4 w-4 mt-0.5 shrink-0" />
              <span>{headerError}</span>
            </div>
          )}

          {file && !headerError && (
            <Button onClick={handleUpload} disabled={uploading || !!result} className="w-full">
              {uploading ? (
                <><Loader2 className="h-4 w-4 animate-spin mr-2" />Uploading...</>
              ) : (
                <><Upload className="h-4 w-4 mr-2" />Upload & Submit for Approval</>
              )}
            </Button>
          )}
        </CardContent>
      </Card>

      {/* Result */}
      {result && (
        <Card className="border-green-200 bg-green-50/50">
          <CardContent className="pt-4">
            <div className="flex items-start gap-3">
              <CheckCircle2 className="h-5 w-5 text-green-600 mt-0.5 shrink-0" />
              <div className="space-y-2 flex-1">
                <p className="font-medium text-green-800">Upload berhasil!</p>
                <div className="grid grid-cols-3 gap-3 text-sm">
                  <div>
                    <p className="text-muted-foreground">Total baris</p>
                    <p className="font-semibold">{result.rowCount}</p>
                  </div>
                  <div>
                    <p className="text-muted-foreground">Valid</p>
                    <p className="font-semibold text-green-700">{result.validCount}</p>
                  </div>
                  <div>
                    <p className="text-muted-foreground">Error</p>
                    <p className="font-semibold text-red-600">{result.errorCount}</p>
                  </div>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => navigate(`/pending-actions/${result.pendingActionId}`)}
                >
                  <ExternalLink className="h-3.5 w-3.5 mr-1.5" />
                  Lihat Pending Action #{result.pendingActionId}
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Preview table */}
      {previewRows.length > 0 && (
        <Card>
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <CardTitle className="text-base">Preview (20 baris pertama)</CardTitle>
              <Badge variant="secondary">{previewRows.length} baris</Badge>
            </div>
          </CardHeader>
          <CardContent className="p-0 overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  {EXPECTED_HEADERS.map((h) => (
                    <TableHead key={h} className="text-xs whitespace-nowrap">{h}</TableHead>
                  ))}
                </TableRow>
              </TableHeader>
              <TableBody>
                {previewRows.map((row, i) => (
                  <TableRow key={i}>
                    {EXPECTED_HEADERS.map((h) => (
                      <TableCell key={h} className="text-xs py-1 whitespace-nowrap">{row[h] || '—'}</TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
