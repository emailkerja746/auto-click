# AutoClick — Starter Project

App Android untuk auto-klik yang **berhenti otomatis** kalau target (gambar atau teks) terdeteksi di layar.

## Cara Setup

1. Buka folder `AutoClickApp` ini di **Android Studio** (File → Open).
2. Tunggu Gradle sync selesai.
3. Jalankan app ke HP Android (minimal Android 8.0 / API 26) via USB debugging atau emulator.

## Cara Pakai (di HP)

1. Buka app AutoClick.
2. Tekan **"1. Aktifkan Accessibility Service"** → nyalakan toggle "AutoClick" di pengaturan Accessibility.
3. Tekan **"2. Izinkan Tampil di Atas App Lain"** → izinkan overlay.
4. Isi koordinat X, Y tempat mau diklik (lihat di bawah cara cari koordinat), interval klik (ms), dan kata kunci teks target (opsional).
5. Tekan **"3. Mulai Auto-Click"** → izinkan screen capture saat diminta.
6. App otomatis minimize, overlay dengan label status + tombol **STOP** muncul di layar.
7. Buka game/app target → auto-click akan berjalan.
   - Kalau kata kunci (misal "PERINGATAN") terdeteksi di layar → auto-click **JEDA otomatis** (label overlay berubah jadi "⏸ JEDA"), tidak ngeklik apapun sampai popup-nya hilang.
   - Begitu popup sudah tidak ada lagi di layar → auto-click **lanjut otomatis** (label kembali "▶ Auto-click berjalan").
   - Tekan **STOP** di overlay kapan saja untuk berhenti total.

## Cara Cari Koordinat X, Y

- Aktifkan **"Opsi Pengembang" → "Tampilkan lokasi sentuhan / pointer"** di HP.
- Sentuh titik yang mau diklik, lihat koordinatnya muncul di layar.
- Atau develop fitur "tap layar untuk pilih titik" di app ini nantinya (belum diimplementasikan di starter).

## Struktur Project

```
app/src/main/java/com/example/autoclick/
├── MainActivity.kt              → UI setting & minta izin
├── ClickAccessibilityService.kt → eksekusi klik (dispatchGesture)
├── ScreenCaptureManager.kt      → ambil screenshot layar (MediaProjection)
├── AutoClickController.kt       → loop klik + cek target + auto-stop
├── ImageMatcher.kt              → deteksi gambar target (template matching)
├── TextDetector.kt              → deteksi teks target (ML Kit OCR)
└── OverlayService.kt            → tombol STOP mengambang + jalankan semua service
```

## Yang Masih Perlu Dikembangkan (TODO)

1. **UI pilih titik klik langsung di layar** (bukan input manual angka) — bisa pakai overlay transparan full-screen saat setup, tangkap koordinat sentuhan.
2. **UI crop gambar target** untuk mode deteksi IMAGE — simpan bitmap target ke internal storage, load balik di `OverlayService.setupCaptureAndController()` (lihat komentar `TODO` di file itu).
3. **Multi-titik klik** — sekarang cuma 1 titik, bisa dikembangkan jadi daftar titik + urutan klik.
4. **Ganti ImageMatcher manual → OpenCV** kalau butuh akurasi & kecepatan lebih tinggi untuk deteksi gambar (`Imgproc.matchTemplate`). Import OpenCV Android SDK sebagai module, lalu tambahkan `implementation project(':opencv')` di `app/build.gradle`.
5. **Penanganan device dengan notch/gesture nav** — kadang ada offset koordinat, perlu disesuaikan.

## Catatan Penting

- App ini **tidak butuh root**, tapi wajib izin **Accessibility Service** dan **Overlay** — user harus aktifkan manual di Settings (Android tidak mengizinkan app auto-approve izin ini demi keamanan).
- **Screen capture izin harus diberikan ulang tiap kali proses di-restart** (batasan sistem Android, tidak bisa dilewati).
- Kalau target app (game) mendeteksi otomasi/macro dan melanggar TOS mereka, itu tanggung jawab pengguna — pastikan cek aturan game/app yang dipakai.
