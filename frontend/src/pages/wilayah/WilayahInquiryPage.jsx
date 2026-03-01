import { useEffect, useState, useCallback, useRef } from 'react';
import { toast } from 'sonner';
import {
  Search, Loader2, ChevronLeft, ChevronRight,
  ShieldCheck, ShieldAlert, ShieldX, ShieldQuestion,
  MapPin, ChevronDown, ChevronUp,
} from 'lucide-react';
import { wilayahApi } from '../../api/wilayahApi';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select';

const PAGE_SIZE = 50;

// ─── Validation badge config ──────────────────────────────────────────────────

const STATUS_CONFIG = {
  VALID:        { label: 'Valid',           icon: ShieldCheck,   className: 'bg-green-100 text-green-800 border-green-300' },
  PARTIAL_ZIP:  { label: 'Nama OK / Kode Pos Beda', icon: ShieldAlert, className: 'bg-yellow-100 text-yellow-800 border-yellow-300' },
  PARTIAL_NAME: { label: 'Kode Pos OK / Nama ~Match', icon: ShieldAlert, className: 'bg-yellow-100 text-yellow-800 border-yellow-300' },
  INVALID:      { label: 'Tidak Valid',     icon: ShieldX,       className: 'bg-red-100 text-red-800 border-red-300' },
};

function ValidationBadge({ status }) {
  const cfg = STATUS_CONFIG[status] || STATUS_CONFIG.INVALID;
  const Icon = cfg.icon;
  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full border text-xs font-medium ${cfg.className}`}>
      <Icon className="h-3 w-3" />
      {cfg.label}
    </span>
  );
}

function ValidationDetail({ result }) {
  return (
    <div className="text-xs space-y-1.5 py-2 px-3 bg-muted/40 rounded-lg border">
      <div className="flex flex-wrap gap-x-6 gap-y-1">
        <span>
          <span className="text-muted-foreground">Nama OSM: </span>
          <strong>{result.nominatimName || '—'}</strong>
          {result.nameSimilarity != null && (
            <span className={`ml-1.5 font-semibold ${result.nameSimilarity >= 80 ? 'text-green-700' : 'text-red-600'}`}>
              ({result.nameSimilarity}% match)
            </span>
          )}
        </span>
        <span>
          <span className="text-muted-foreground">Kode Pos OSM: </span>
          <strong className={result.zipCodeMatch ? 'text-green-700' : 'text-red-600'}>
            {result.nominatimZipCode || '—'}
          </strong>
          {result.localZipCode && (
            <span className="text-muted-foreground ml-1">
              (lokal: {result.localZipCode})
            </span>
          )}
        </span>
        <span>
          <span className="text-muted-foreground">Tipe: </span>
          <strong>{result.nominatimType || '—'}</strong>
        </span>
        {result.nominatimCounty && (
          <span>
            <span className="text-muted-foreground">Kab/Kota OSM: </span>
            <strong>{result.nominatimCounty}</strong>
          </span>
        )}
        {result.nominatimProvince && (
          <span>
            <span className="text-muted-foreground">Provinsi OSM: </span>
            <strong>{result.nominatimProvince}</strong>
          </span>
        )}
        {result.lat && result.lon && (
          <span>
            <span className="text-muted-foreground">Koordinat: </span>
            <a
              href={`https://www.openstreetmap.org/?mlat=${result.lat}&mlon=${result.lon}&zoom=14`}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-0.5 text-blue-600 hover:underline"
            >
              <MapPin className="h-3 w-3" />
              {Number(result.lat).toFixed(4)}, {Number(result.lon).toFixed(4)}
            </a>
          </span>
        )}
      </div>
      <p className="text-muted-foreground italic">
        {result.nominatimDisplayName}
      </p>
      <p className="text-muted-foreground">Sumber: {result.source}</p>
    </div>
  );
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function WilayahInquiryPage() {
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);

  const [provinces, setProvinces] = useState([]);
  const [states, setStates] = useState([]);
  const [districts, setDistricts] = useState([]);

  const [selectedProvince, setSelectedProvince] = useState('');
  const [selectedState, setSelectedState] = useState('');
  const [selectedDistrict, setSelectedDistrict] = useState('');

  const [searchText, setSearchText] = useState('');
  const [zipCode, setZipCode] = useState('');

  // validation state: { [subDistrictId]: { loading, result, expanded } }
  const [validations, setValidations] = useState({});

  const debounceRef = useRef(null);

  useEffect(() => {
    wilayahApi.getProvinces({ size: 100 })
      .then((res) => setProvinces(res.data.data.content))
      .catch(() => {});
  }, []);

  useEffect(() => {
    setStates([]);
    setSelectedState('');
    setDistricts([]);
    setSelectedDistrict('');
    if (!selectedProvince) return;
    wilayahApi.getStates({ provinceId: selectedProvince, size: 200 })
      .then((res) => setStates(res.data.data.content))
      .catch(() => {});
  }, [selectedProvince]);

  useEffect(() => {
    setDistricts([]);
    setSelectedDistrict('');
    if (!selectedState) return;
    wilayahApi.getDistricts({ stateId: selectedState, size: 200 })
      .then((res) => setDistricts(res.data.data.content))
      .catch(() => {});
  }, [selectedState]);

  const doSearch = useCallback(async (currentPage = 0) => {
    setLoading(true);
    setValidations({});
    try {
      const params = {
        page: currentPage,
        q: searchText || undefined,
        zipCode: zipCode || undefined,
        provinceId: selectedProvince || undefined,
        stateId: selectedState || undefined,
        districtId: selectedDistrict || undefined,
      };
      const res = await wilayahApi.inquiry(params);
      const data = res.data.data;
      setResults(data.content);
      setTotal(data.totalElements);
      setPage(currentPage);
    } catch (err) {
      if (err.response?.status === 429) {
        toast.error('Rate limit exceeded — please wait a moment');
      } else {
        toast.error('Search failed');
      }
    } finally {
      setLoading(false);
    }
  }, [searchText, zipCode, selectedProvince, selectedState, selectedDistrict]);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    if (searchText.length >= 2 || searchText.length === 0) {
      debounceRef.current = setTimeout(() => doSearch(0), 400);
    }
    return () => clearTimeout(debounceRef.current);
  }, [searchText, doSearch]);

  useEffect(() => { doSearch(0); }, [zipCode, selectedProvince, selectedState, selectedDistrict]);

  const handleValidate = async (sd) => {
    const id = sd.subDistrictId;

    // Toggle collapse if already validated
    if (validations[id]?.result) {
      setValidations((prev) => ({
        ...prev,
        [id]: { ...prev[id], expanded: !prev[id].expanded },
      }));
      return;
    }

    setValidations((prev) => ({ ...prev, [id]: { loading: true, result: null, expanded: false } }));
    try {
      const res = await wilayahApi.validate({
        name:         sd.name,
        zipCode:      sd.zipCode  || undefined,
        provinceName: sd.provinceName || undefined,
        stateName:    sd.stateName    || undefined,
        districtName: sd.districtName || undefined,
      });
      setValidations((prev) => ({
        ...prev,
        [id]: { loading: false, result: res.data.data, expanded: true },
      }));
    } catch {
      setValidations((prev) => ({ ...prev, [id]: { loading: false, result: null, expanded: false } }));
      toast.error('Validasi gagal — coba lagi');
    }
  };

  const totalPages = Math.ceil(total / PAGE_SIZE);

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold">Inquiry Wilayah</h2>
        <p className="text-sm text-muted-foreground">Cari data wilayah berdasarkan nama, kode pos, atau hierarki</p>
      </div>

      {/* Filter Card */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base">Filter</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>Cari Nama Kel/Desa</Label>
              <div className="relative">
                <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Min 2 karakter..."
                  value={searchText}
                  onChange={(e) => setSearchText(e.target.value)}
                  className="pl-8"
                />
              </div>
            </div>
            <div className="space-y-1.5">
              <Label>Kode Pos</Label>
              <Input
                placeholder="e.g. 12345"
                value={zipCode}
                onChange={(e) => setZipCode(e.target.value)}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="space-y-1.5">
              <Label>Provinsi</Label>
              <Select
                value={selectedProvince || '__ALL__'}
                onValueChange={(v) => setSelectedProvince(v === '__ALL__' ? '' : v)}
              >
                <SelectTrigger><SelectValue placeholder="Semua Provinsi" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="__ALL__">Semua Provinsi</SelectItem>
                  {provinces.map((p) => (
                    <SelectItem key={p.provinceId} value={p.provinceId}>{p.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label>Kab/Kota</Label>
              <Select
                value={selectedState || '__ALL__'}
                onValueChange={(v) => setSelectedState(v === '__ALL__' ? '' : v)}
                disabled={!selectedProvince}
              >
                <SelectTrigger>
                  <SelectValue placeholder={selectedProvince ? 'Pilih Kab/Kota' : 'Pilih Provinsi dahulu'} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="__ALL__">Semua Kab/Kota</SelectItem>
                  {states.map((s) => (
                    <SelectItem key={s.stateId} value={s.stateId}>{s.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label>Kecamatan</Label>
              <Select
                value={selectedDistrict || '__ALL__'}
                onValueChange={(v) => setSelectedDistrict(v === '__ALL__' ? '' : v)}
                disabled={!selectedState}
              >
                <SelectTrigger>
                  <SelectValue placeholder={selectedState ? 'Pilih Kecamatan' : 'Pilih Kab/Kota dahulu'} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="__ALL__">Semua Kecamatan</SelectItem>
                  {districts.map((d) => (
                    <SelectItem key={d.districtId} value={d.districtId}>{d.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Results */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            {loading ? 'Searching...' : `${total} hasil ditemukan${total >= PAGE_SIZE ? ' (max 50 per halaman)' : ''}`}
          </p>
          <p className="text-xs text-muted-foreground flex items-center gap-1">
            <ShieldQuestion className="h-3.5 w-3.5" />
            Cek Validasi menggunakan OpenStreetMap Nominatim
          </p>
        </div>

        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Kel/Desa</TableHead>
                  <TableHead>Kecamatan</TableHead>
                  <TableHead>Kab/Kota</TableHead>
                  <TableHead>Provinsi</TableHead>
                  <TableHead>Kode Pos</TableHead>
                  <TableHead className="text-center">Validasi</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-12">
                      <Loader2 className="h-6 w-6 animate-spin mx-auto text-muted-foreground" />
                    </TableCell>
                  </TableRow>
                ) : results.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-12 text-muted-foreground">
                      Tidak ada data — gunakan filter di atas
                    </TableCell>
                  </TableRow>
                ) : (
                  results.map((sd) => {
                    const vState = validations[sd.subDistrictId];
                    return [
                      // Main data row
                      <TableRow key={sd.subDistrictId}>
                        <TableCell className="font-medium">{sd.name}</TableCell>
                        <TableCell className="text-sm">{sd.districtName}</TableCell>
                        <TableCell className="text-sm">{sd.stateName}</TableCell>
                        <TableCell className="text-sm">{sd.provinceName}</TableCell>
                        <TableCell className="font-mono text-sm">{sd.zipCode || '—'}</TableCell>
                        <TableCell className="text-center">
                          {vState?.loading ? (
                            <div className="flex items-center justify-center gap-1 text-xs text-muted-foreground">
                              <Loader2 className="h-3.5 w-3.5 animate-spin" />
                              Mengecek...
                            </div>
                          ) : vState?.result ? (
                            <button
                              onClick={() => handleValidate(sd)}
                              className="inline-flex items-center gap-1 cursor-pointer"
                            >
                              <ValidationBadge status={vState.result.status} />
                              {vState.expanded
                                ? <ChevronUp className="h-3 w-3 text-muted-foreground" />
                                : <ChevronDown className="h-3 w-3 text-muted-foreground" />
                              }
                            </button>
                          ) : (
                            <Button
                              variant="outline"
                              size="sm"
                              className="h-7 text-xs"
                              onClick={() => handleValidate(sd)}
                            >
                              <ShieldQuestion className="h-3.5 w-3.5 mr-1" />
                              Cek Validasi
                            </Button>
                          )}
                        </TableCell>
                      </TableRow>,

                      // Expanded detail row
                      vState?.result && vState.expanded && (
                        <TableRow key={`${sd.subDistrictId}-detail`} className="bg-muted/20 hover:bg-muted/20">
                          <TableCell colSpan={6} className="py-2 px-4">
                            {vState.result.found ? (
                              <ValidationDetail result={vState.result} />
                            ) : (
                              <p className="text-xs text-red-600 py-1">
                                Tidak ditemukan di OpenStreetMap untuk nama "{sd.name}".
                              </p>
                            )}
                          </TableCell>
                        </TableRow>
                      ),
                    ];
                  })
                )}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        {totalPages > 1 && (
          <div className="flex items-center justify-between text-sm text-muted-foreground">
            <span>Halaman {page + 1} dari {totalPages}</span>
            <div className="flex items-center gap-1">
              <Button variant="outline" size="icon" className="h-8 w-8"
                onClick={() => doSearch(page - 1)} disabled={page === 0 || loading}>
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <Button variant="outline" size="icon" className="h-8 w-8"
                onClick={() => doSearch(page + 1)} disabled={page >= totalPages - 1 || loading}>
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
