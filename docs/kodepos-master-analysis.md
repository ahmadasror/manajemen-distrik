# Analisis Master Data Kode Pos (`sample/kodepos_master.csv`)

> Dokumen ini merangkum pemahaman atas struktur, pola ID, anomali, dan kebutuhan sistem
> untuk integrasi data wilayah Indonesia ke dalam Manajemen Distrik.

---

## Ringkasan Data

| Entitas | Jumlah | Kolom ID | Kolom Nama |
|---|---|---|---|
| Provinsi | 33 | `ProvinceID` | `ProvinceName` |
| Kabupaten / Kota | 441 | `StateID` | `StateName` |
| Kecamatan | 5,332 | `DistrictID` | `DistrictName` |
| Kelurahan / Desa | 60,227 | `SubDistrictID` | `SubDistrictName` |
| Kode Pos unik | 7,627 | `ZipCode` | — |

Total baris CSV: **60,227** (satu baris = satu kelurahan/desa).

---

## Pola ID — Dua Sistem Berbeda

> **Penting:** ProvinceID dan StateID menggunakan numbering **app-custom (4 digit)**,
> sedangkan DistrictID dan SubDistrictID menggunakan **kode BPS (Badan Pusat Statistik)**.
> Keduanya **tidak saling terhubung secara concat**.

### ProvinceID — 4 digit, custom

```
0100  → Jawa Barat
0900  → Jawa Tengah
3200  → Nanggroe Aceh D
```

Tidak ada formula turunan dari nomor lain. Berdiri sendiri sebagai PK.

### StateID — 4 digit, custom

```
0121  → Kab. Subang       (di bawah ProvinceID 0100 - Jawa Barat)
3216  → Kab. Aceh Simeuleu (di bawah ProvinceID 3200 - Aceh)
```

Tidak diturunkan dari ProvinceID. Relasi hanya bisa diverifikasi via lookup tabel.

### DistrictID — 7 digit, kode BPS

Format: `[2-digit prov BPS][2-digit kab BPS][3-digit kec]`

```
1101010  →  11=Aceh, 01=Aceh Besar, 010=kecamatan ke-010
3213240  →  32=Jawa Barat, 13=Kab Subang, 240=kecamatan ke-240
```

### SubDistrictID — 10 digit, kode BPS

Format: `[7-digit DistrictID][3-digit urut kelurahan]`

```
1101010001  →  DistrictID=1101010, urut=001
1101010002  →  DistrictID=1101010, urut=002
```

---

## Validasi Concat yang Berlaku

**Satu-satunya concat yang valid dan bisa di-enforce adalah:**

```
SubDistrictID[0:7] == DistrictID
```

Berlaku untuk **99.86%** data (60,084 dari 60,182 baris dengan DistrictID 7 digit).

| Level | Validasi Concat | Keterangan |
|---|---|---|
| Province → State | ❌ Tidak berlaku | Numbering berbeda |
| State → District | ❌ Tidak berlaku | Numbering berbeda |
| District → SubDistrict | ✅ Berlaku | `SubDistrictID[:7] == DistrictID` |

---

## Anomali Data Sumber

| Anomali | Jumlah | Penanganan yang Disarankan |
|---|---|---|
| DistrictID 6 digit (expected 7) | 45 baris | Import apa adanya, simpan as-is |
| SubDistrictID < 10 digit | 12 baris | Import apa adanya, simpan as-is |
| SubDistrictID tidak cocok dengan DistrictID | 87 baris | Import apa adanya, jangan block |
| Tanpa kode pos | 3 baris | Izinkan nullable ZipCode |

Anomali berasal dari data sumber (kemungkinan error swap ID antara kecamatan bertetangga
atau data lama yang belum dikoreksi). Jangan dijadikan hard constraint di DB karena akan
menggagalkan import.

---

## Catatan Kelengkapan Data

- Data mencakup **33 provinsi** — versi lama, sebelum pemekaran Papua (2022) dan sebelum
  Kalimantan Utara terdaftar lengkap. Indonesia saat ini punya 38 provinsi.
- `ZipCode` bersifat **many-to-many** dengan kelurahan: satu kode pos bisa mencakup
  banyak kelurahan. Bukan key, hanya atribut.

---

## Kebutuhan Sistem

### 1. Skema tabel

```
provinces      (province_id VARCHAR(4) PK, name TEXT)
states         (state_id VARCHAR(4) PK, name TEXT, province_id FK → provinces)
districts      (district_id VARCHAR(7) PK, name TEXT, state_id FK → states)
subdistricts   (subdistrict_id VARCHAR(10) PK, name TEXT, district_id FK → districts, zip_code VARCHAR(10) NULL)
```

- Gunakan ID dari CSV sebagai PK — jangan generate surrogate key baru.
- `district_id` VARCHAR(7) tapi toleransi 6 digit karena ada 45 baris anomali
  → lebih aman VARCHAR(10) untuk future-proof.

### 2. Import / seeding

- Source: `sample/kodepos_master.csv` (60,227 baris)
- Skip hard validation concat saat import — simpan apa adanya
- Proses batch insert, bukan one-by-one (performa)

### 3. Validasi saat input manual baru

Jika user input SubDistrictID baru secara manual:
- Warn (bukan block) jika `SubDistrictID[:7] != DistrictID`
- Tidak perlu validasi concat untuk ProvinceID/StateID

### 4. Integrasi dengan Maker-Checker

Perubahan pada master wilayah (tambah/ubah/hapus) mengikuti alur yang sama:
pending-action → approval sebelum efektif.

---

## File Referensi

| File | Keterangan |
|---|---|
| `sample/kodepos_master.csv` | Raw data master wilayah (60,227 baris) |
| `docs/kodepos-master-analysis.md` | Dokumen ini |
