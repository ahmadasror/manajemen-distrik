# Resolve ZipCode — Standalone Script

Script untuk mengisi zip code yang kosong/00000 dari file CSV wilayah menggunakan sumber data gratis (Wikipedia + Nominatim).

## Prasyarat

- Python 3.9+
- Koneksi internet (akses ke Wikipedia & Nominatim API)
- File input: `zipcode_kosong.csv` (format 9 kolom standar wilayah)

## Cara Pakai

```bash
cd sample

# Test 5 baris pertama
python3 resolve_zipcode.py --limit 5

# Jalankan semua (resumable — bisa Ctrl+C dan lanjutkan)
python3 resolve_zipcode.py
```

## Output

| File | Keterangan |
|------|------------|
| `zipcode_hasil.csv` | CSV hasil — format sama + 3 kolom baru |
| `progress.json` | File resume — hapus untuk ulang dari awal |

Kolom tambahan di output:

| Kolom | Contoh | Keterangan |
|-------|--------|------------|
| `zipcode_hasil` | `23897` | Zip code yang ditemukan (kosong jika tidak ditemukan) |
| `zipcode_source` | `Wikipedia` | Sumber data: `Wikipedia`, `Wikipedia+Nominatim`, `Nominatim`, atau `NOT_FOUND` |
| `zipcode_similarity` | `100` | Persentase kecocokan nama (Levenshtein similarity) |

## Logic Validasi

```
Untuk setiap SubDistrict di CSV:

1. Wikipedia (id.wikipedia.org)
   ├── Search artikel dengan nama kelurahan/desa + kecamatan
   ├── Pilih artikel dengan similarity tertinggi (threshold >= 40%)
   ├── Parse infobox wikitext → ambil field "kode_pos" / "kode pos"
   ├── Jika kode_pos ditemukan (5 digit) → SELESAI ✓
   └── Jika kode_pos kosong → fallback ke Nominatim postcode
       ├── Ambil nama dari infobox Wikipedia (lebih akurat)
       ├── Query Nominatim search → ambil postcode dari address
       ├── Jika tidak ada → query Nominatim details → calculated_postcode
       └── Jika ditemukan (5 digit) → SELESAI ✓ (source: "Wikipedia+Nominatim")

2. Nominatim / OpenStreetMap (fallback jika Wikipedia gagal total)
   ├── Coba 3 variasi query:
   │   1. "nama_desa, kecamatan, kabupaten, Indonesia"
   │   2. "nama_desa, kabupaten, Indonesia"
   │   3. "nama_desa, Indonesia"
   ├── Ambil postcode dari address field
   ├── Jika tidak ada → query details API → calculated_postcode
   └── Jika ditemukan (5 digit) → SELESAI ✓ (source: "Nominatim")

3. Jika semua gagal → NOT_FOUND
```

## Rate Limit

Script menghormati fair-use policy kedua API:

| API | Delay | Kebijakan |
|-----|-------|-----------|
| Wikipedia | 300ms antar request | Tidak ada limit resmi, tapi sopan |
| Nominatim | 1100ms antar request | Maks 1 request/detik (wajib) |

Estimasi waktu untuk 11.468 baris: **~5-6 jam** (karena rate limit).

## Resume

Script menyimpan progress ke `progress.json` setiap 10 baris. Jika terputus (Ctrl+C, koneksi putus, dll), jalankan ulang dan script akan melanjutkan dari posisi terakhir.

```bash
# Cek progress
python3 -c "import json; d=json.load(open('progress.json')); print(f'Resolved: {len(d)}')"

# Reset (ulang dari awal)
rm progress.json
```

## Hit Rate

Dari pengujian ~60% SubDistrict berhasil ditemukan zip code-nya via Wikipedia. Sisanya NOT_FOUND karena:
- Desa/kelurahan tidak punya halaman Wikipedia
- Halaman Wikipedia ada tapi infobox tidak mengandung kode pos
- Nominatim tidak memiliki data postcode untuk wilayah kecil di Indonesia
