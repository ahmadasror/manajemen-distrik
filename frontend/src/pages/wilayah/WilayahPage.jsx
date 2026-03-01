import { useEffect, useState, useCallback } from 'react';
import { toast } from 'sonner';
import { Plus, Search, Pencil, Trash2, Loader2, ChevronLeft, ChevronRight, X, Check } from 'lucide-react';
import { wilayahApi } from '../../api/wilayahApi';
import { usePermission } from '../../hooks/usePermission';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
} from '@/components/ui/dialog';
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select';
import { Label } from '@/components/ui/label';
import ConfirmModal from '../../components/ConfirmModal';

// ─── Generic Wilayah Tab ──────────────────────────────────────────────────────

/**
 * parentCascade: array of cascade level descriptors (null for Provinsi tab).
 * Each entry: { label, idField, nameField, itemInitField, fetchFn(upperValues[]) }
 * - All levels except the last are filter-only (not saved).
 * - The LAST level's selected value is the parentId that gets saved.
 */
function WilayahTab({ label, idField, nameField, parentField, fetchFn, createFn, updateFn, deleteFn, extraColumns, parentCascade }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const pageSize = 20;
  const { canCreate } = usePermission();

  // Dialog state
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editItem, setEditItem] = useState(null);
  const [formId, setFormId] = useState('');
  const [formName, setFormName] = useState('');
  const [formZipCode, setFormZipCode] = useState('');
  const [saving, setSaving] = useState(false);

  // Cascade state (used when parentCascade prop is provided)
  const [cascadeValues, setCascadeValues] = useState([]);   // selected ID per level
  const [cascadeOptions, setCascadeOptions] = useState([]); // fetched options per level
  const [cascadeLoading, setCascadeLoading] = useState([]); // loading flag per level

  // Delete confirm
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const fetchItems = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size: pageSize, search: search || undefined };
      const res = await fetchFn(params);
      const data = res.data.data;
      setItems(data.content);
      setTotal(data.totalElements);
    } catch {
      toast.error(`Failed to fetch ${label}`);
    } finally {
      setLoading(false);
    }
  }, [page, search, fetchFn, label]);

  useEffect(() => { fetchItems(); }, [fetchItems]);

  // ── Cascade helpers ────────────────────────────────────────────────────────

  /**
   * Initialise cascade state when dialog opens.
   * initValues: pre-selected IDs for each level ('' for create, item fields for edit).
   */
  const initCascade = useCallback(async (initValues) => {
    if (!parentCascade) return;
    const n = parentCascade.length;

    // Mark levels that actually need fetching
    const loadingInit = parentCascade.map((_, i) => {
      if (i === 0) return true;
      return !!initValues[i - 1];
    });
    setCascadeValues([...initValues]);
    setCascadeOptions(new Array(n).fill([]));
    setCascadeLoading(loadingInit);

    const opts = new Array(n).fill([]);

    // Load level 0 (e.g. provinces — always)
    try {
      const res = await parentCascade[0].fetchFn([]);
      opts[0] = res.data.data.content || [];
    } catch { opts[0] = []; }

    // Load each subsequent level if its parent value is pre-set (edit mode)
    for (let i = 1; i < n; i++) {
      if (!initValues[i - 1]) break;
      try {
        const res = await parentCascade[i].fetchFn(initValues.slice(0, i));
        opts[i] = res.data.data.content || [];
      } catch { opts[i] = []; }
    }

    setCascadeOptions(opts);
    setCascadeLoading(new Array(n).fill(false));
  }, [parentCascade]);

  /** Called when the user picks a value in cascade level `level`. */
  const handleCascadeChange = useCallback(async (level, value) => {
    // Update selected value and clear all downstream selections
    const newValues = cascadeValues.map((v, i) => {
      if (i === level) return value;
      if (i > level) return '';
      return v;
    });

    // Clear downstream options
    const newOpts = cascadeOptions.map((o, i) => (i > level ? [] : o));

    if (!value || level + 1 >= parentCascade.length) {
      setCascadeValues(newValues);
      setCascadeOptions(newOpts);
      return;
    }

    // Mark next level as loading
    const newLoading = cascadeLoading.map((l, i) => (i === level + 1 ? true : l));
    setCascadeValues(newValues);
    setCascadeOptions(newOpts);
    setCascadeLoading(newLoading);

    // Fetch next level's options
    try {
      const res = await parentCascade[level + 1].fetchFn(newValues.slice(0, level + 1));
      setCascadeOptions((prev) => prev.map((o, i) => (i === level + 1 ? (res.data.data.content || []) : o)));
    } catch {
      setCascadeOptions((prev) => prev.map((o, i) => (i === level + 1 ? [] : o)));
    } finally {
      setCascadeLoading((prev) => prev.map((l, i) => (i === level + 1 ? false : l)));
    }
  }, [cascadeValues, cascadeOptions, cascadeLoading, parentCascade]);

  // ── Dialog open/close ──────────────────────────────────────────────────────

  const openCreate = () => {
    setEditItem(null);
    setFormId('');
    setFormName('');
    setFormZipCode('');
    setDialogOpen(true);
    if (parentCascade) {
      initCascade(new Array(parentCascade.length).fill(''));
    }
  };

  const openEdit = (item) => {
    setEditItem(item);
    setFormId(item[idField]);
    setFormName(item[nameField]);
    setFormZipCode(item.zipCode || '');
    setDialogOpen(true);
    if (parentCascade) {
      initCascade(parentCascade.map((lvl) => item[lvl.itemInitField] || ''));
    }
  };

  // ── Save / Delete ──────────────────────────────────────────────────────────

  const handleSave = async () => {
    if (!formName.trim()) { toast.error('Name is required'); return; }

    // Zip code validation: numeric, exactly 5 digits
    if (label === 'Kel/Desa' && formZipCode) {
      if (!/^\d{5}$/.test(formZipCode)) {
        toast.error('Kode pos harus berupa 5 digit angka');
        return;
      }
    }

    // Determine parentId: last cascade level when cascade is used
    let parentId;
    if (parentCascade) {
      parentId = cascadeValues[cascadeValues.length - 1] || '';
      if (!parentId) {
        toast.error(`${parentCascade[parentCascade.length - 1].label} harus dipilih`);
        return;
      }
    }

    setSaving(true);
    try {
      const payload = {
        id: formId,
        name: formName,
        parentId: parentId || undefined,
        zipCode: formZipCode || undefined,
      };
      if (editItem) {
        await updateFn(editItem[idField], payload);
        toast.success(`${label} updated`);
      } else {
        await createFn(payload);
        toast.success(`${label} created`);
      }
      setDialogOpen(false);
      fetchItems();
    } catch (err) {
      toast.error(err.response?.data?.message || `Failed to save ${label}`);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteFn(deleteTarget[idField]);
      toast.success(`${label} deleted`);
      setDeleteTarget(null);
      fetchItems();
    } catch (err) {
      toast.error(err.response?.data?.message || `Failed to delete ${label}`);
    } finally {
      setDeleting(false);
    }
  };

  const totalPages = Math.ceil(total / pageSize);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">{total} total {label.toLowerCase()}</p>
        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder={`Search ${label.toLowerCase()}...`}
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }}
              className="pl-8 w-56"
            />
          </div>
          {canCreate && (
            <Button size="sm" onClick={openCreate}>
              <Plus className="h-4 w-4" />
              Add {label}
            </Button>
          )}
        </div>
      </div>

      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Name</TableHead>
                {extraColumns?.map((col) => <TableHead key={col.key}>{col.label}</TableHead>)}
                {canCreate && <TableHead className="text-right">Actions</TableHead>}
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={3 + (extraColumns?.length || 0) + (canCreate ? 1 : 0)} className="text-center py-12">
                    <Loader2 className="h-6 w-6 animate-spin mx-auto text-muted-foreground" />
                  </TableCell>
                </TableRow>
              ) : items.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={3 + (extraColumns?.length || 0) + (canCreate ? 1 : 0)} className="text-center py-12 text-muted-foreground">
                    No {label.toLowerCase()} found
                  </TableCell>
                </TableRow>
              ) : (
                items.map((item) => (
                  <TableRow key={item[idField]}>
                    <TableCell className="font-mono text-xs text-muted-foreground">{item[idField]}</TableCell>
                    <TableCell className="font-medium">{item[nameField]}</TableCell>
                    {extraColumns?.map((col) => (
                      <TableCell key={col.key} className="text-sm text-muted-foreground">{col.render ? col.render(item) : item[col.key]}</TableCell>
                    ))}
                    {canCreate && (
                      <TableCell>
                        <div className="flex items-center justify-end gap-1">
                          <Button variant="ghost" size="icon" onClick={() => openEdit(item)} title="Edit">
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="text-destructive hover:text-destructive"
                            onClick={() => setDeleteTarget(item)}
                            title="Delete"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                    )}
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {totalPages > 1 && (
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>Showing {page * pageSize + 1}–{Math.min((page + 1) * pageSize, total)} of {total}</span>
          <div className="flex items-center gap-1">
            <Button variant="outline" size="icon" className="h-8 w-8" onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}>
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <span className="px-3">Page {page + 1} of {totalPages}</span>
            <Button variant="outline" size="icon" className="h-8 w-8" onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}>
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}

      {/* Create/Edit Dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editItem ? `Edit ${label}` : `Add ${label}`}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">

            {/* ID — only on create */}
            {!editItem && (
              <div className="space-y-1.5">
                <Label htmlFor="wilayah-id">ID</Label>
                <Input id="wilayah-id" value={formId} onChange={(e) => setFormId(e.target.value)} placeholder="e.g. 1101" />
              </div>
            )}

            {/* Cascading parent selects — always shown at top (below ID) */}
            {parentCascade && parentCascade.map((level, i) => {
              const isLast = i === parentCascade.length - 1;
              const isDisabled = saving || (i > 0 && !cascadeValues[i - 1]);
              const isLoading = cascadeLoading[i];
              const opts = cascadeOptions[i] || [];

              return (
                <div key={level.idField} className="space-y-1.5">
                  <Label>
                    {level.label}
                    {isLast
                      ? <span className="text-destructive ml-1">*</span>
                      : <span className="text-muted-foreground text-xs ml-1">(filter)</span>}
                  </Label>
                  <Select
                    value={cascadeValues[i] || ''}
                    onValueChange={(v) => handleCascadeChange(i, v)}
                    disabled={isDisabled || isLoading}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder={
                        isLoading ? 'Memuat...' :
                        isDisabled ? `Pilih ${parentCascade[i - 1].label} dahulu` :
                        `Pilih ${level.label}...`
                      } />
                    </SelectTrigger>
                    <SelectContent>
                      {opts.length === 0 && !isLoading && (
                        <SelectItem value="__empty__" disabled>
                          Tidak ada data
                        </SelectItem>
                      )}
                      {opts.map((opt) => (
                        <SelectItem key={opt[level.idField]} value={opt[level.idField]}>
                          {opt[level.nameField]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              );
            })}

            {/* Name — below cascade selects */}
            <div className="space-y-1.5">
              <Label htmlFor="wilayah-name">Name</Label>
              <Input id="wilayah-name" value={formName} onChange={(e) => setFormName(e.target.value)} placeholder={`${label} name`} />
            </div>

            {/* Zip Code — Kel/Desa only, always at bottom */}
            {label === 'Kel/Desa' && (
              <div className="space-y-1.5">
                <Label>
                  Kode Pos
                  <span className="text-muted-foreground text-xs ml-1">(5 digit angka)</span>
                </Label>
                <Input
                  value={formZipCode}
                  onChange={(e) => setFormZipCode(e.target.value.replace(/\D/g, '').slice(0, 5))}
                  placeholder="e.g. 12345"
                  maxLength={5}
                  inputMode="numeric"
                />
              </div>
            )}
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>
              <X className="h-4 w-4 mr-1" />Cancel
            </Button>
            <Button onClick={handleSave} disabled={saving}>
              {saving ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : <Check className="h-4 w-4 mr-1" />}
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirm */}
      <ConfirmModal
        open={!!deleteTarget}
        title={`Delete ${label}?`}
        message={`Are you sure you want to delete "${deleteTarget?.[nameField]}"? This action cannot be undone.`}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
        loading={deleting}
      />
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function WilayahPage() {
  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-2xl font-bold">Data Wilayah</h2>
        <p className="text-sm text-muted-foreground">Manajemen data master wilayah Indonesia</p>
      </div>

      <Tabs defaultValue="province">
        <TabsList>
          <TabsTrigger value="province">Provinsi</TabsTrigger>
          <TabsTrigger value="state">Kab/Kota</TabsTrigger>
          <TabsTrigger value="district">Kecamatan</TabsTrigger>
          <TabsTrigger value="subdistrict">Kel/Desa</TabsTrigger>
        </TabsList>

        {/* ── Provinsi — no parent ── */}
        <TabsContent value="province" className="mt-4">
          <WilayahTab
            label="Provinsi"
            idField="provinceId"
            nameField="name"
            parentField={null}
            fetchFn={wilayahApi.getProvinces}
            createFn={wilayahApi.createProvince}
            updateFn={wilayahApi.updateProvince}
            deleteFn={wilayahApi.deleteProvince}
          />
        </TabsContent>

        {/* ── Kab/Kota — parent: Provinsi ── */}
        <TabsContent value="state" className="mt-4">
          <WilayahTab
            label="Kab/Kota"
            idField="stateId"
            nameField="name"
            parentField="provinceId"
            fetchFn={wilayahApi.getStates}
            createFn={wilayahApi.createState}
            updateFn={wilayahApi.updateState}
            deleteFn={wilayahApi.deleteState}
            extraColumns={[{ key: 'provinceName', label: 'Provinsi' }]}
            parentCascade={[
              {
                label: 'Provinsi',
                idField: 'provinceId',
                nameField: 'name',
                itemInitField: 'provinceId',
                fetchFn: () => wilayahApi.getProvinces({ size: 200 }),
              },
            ]}
          />
        </TabsContent>

        {/* ── Kecamatan — parent: Kab/Kota (filter: Provinsi → Kab/Kota) ── */}
        <TabsContent value="district" className="mt-4">
          <WilayahTab
            label="Kecamatan"
            idField="districtId"
            nameField="name"
            parentField="stateId"
            fetchFn={wilayahApi.getDistricts}
            createFn={wilayahApi.createDistrict}
            updateFn={wilayahApi.updateDistrict}
            deleteFn={wilayahApi.deleteDistrict}
            extraColumns={[{ key: 'stateName', label: 'Kab/Kota' }]}
            parentCascade={[
              {
                label: 'Provinsi',
                idField: 'provinceId',
                nameField: 'name',
                itemInitField: 'provinceId',
                fetchFn: () => wilayahApi.getProvinces({ size: 200 }),
              },
              {
                label: 'Kab/Kota',
                idField: 'stateId',
                nameField: 'name',
                itemInitField: 'stateId',
                fetchFn: ([pId]) => wilayahApi.getStates({ provinceId: pId, size: 200 }),
              },
            ]}
          />
        </TabsContent>

        {/* ── Kel/Desa — parent: Kecamatan (filter: Provinsi → Kab/Kota → Kecamatan) ── */}
        <TabsContent value="subdistrict" className="mt-4">
          <WilayahTab
            label="Kel/Desa"
            idField="subDistrictId"
            nameField="name"
            parentField="districtId"
            fetchFn={wilayahApi.getSubDistricts}
            createFn={wilayahApi.createSubDistrict}
            updateFn={wilayahApi.updateSubDistrict}
            deleteFn={wilayahApi.deleteSubDistrict}
            extraColumns={[
              { key: 'districtName', label: 'Kecamatan' },
              { key: 'zipCode', label: 'Kode Pos' },
            ]}
            parentCascade={[
              {
                label: 'Provinsi',
                idField: 'provinceId',
                nameField: 'name',
                itemInitField: 'provinceId',
                fetchFn: () => wilayahApi.getProvinces({ size: 200 }),
              },
              {
                label: 'Kab/Kota',
                idField: 'stateId',
                nameField: 'name',
                itemInitField: 'stateId',
                fetchFn: ([pId]) => wilayahApi.getStates({ provinceId: pId, size: 200 }),
              },
              {
                label: 'Kecamatan',
                idField: 'districtId',
                nameField: 'name',
                itemInitField: 'districtId',
                fetchFn: ([, sId]) => wilayahApi.getDistricts({ stateId: sId, size: 200 }),
              },
            ]}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}
