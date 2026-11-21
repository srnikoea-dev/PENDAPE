# PENDAPE — Proyek Android Studio

Proyek ini merupakan integrasi dari proyek `app.rar` yang dikirim, dengan halaman PENDAPE dan dukungan penyimpanan/pemilihan file native WebView.

## Fitur halaman
- Beranda PENDAPE dan teks identitas sesuai permintaan.
- Mode Guru memakai password antarmuka `123456`.
- Durasi ujian dalam menit dan timer hitung mundur siswa.
- Tambah/edit/hapus soal dengan upload gambar.
- Impor CSV, unduh template CSV, ekspor CSV, dan ekspor paket ujian JSON.
- Form siswa berisi nama dan kelas.
- Navigasi soal, status terjawab/belum, kembali dan selanjutnya.
- Hasil akhir berisi skor, benar, salah tanpa kunci jawaban.
- Simpan hasil sebagai gambar PNG.

## Integrasi Android
- `MainActivity.java` menangani pemilih file untuk JSON, CSV, dan gambar.
- `AndroidBridge.saveFile` menyimpan JSON/CSV melalui Android document picker.
- `AndroidBridge.saveBase64File` menyimpan hasil PNG.
- Label aplikasi di manifest diubah menjadi PENDAPE.
- Workflow GitHub Actions tersedia di `.github/workflows/build-apk.yml`.

## Build melalui GitHub
1. Unggah seluruh isi folder proyek ini ke root repository GitHub (termasuk folder `.github`).
2. Buka tab Actions dan jalankan workflow **Build APK PENDAPE**, atau push ke branch `main`/`master`.
3. Unduh artifact **PENDAPE-debug-APK** setelah workflow selesai.
4. Ekstrak artifact untuk memperoleh `app-debug.apk`.

## Catatan pengujian dan keamanan
- Password guru `123456` hanya pengunci antarmuka sisi klien, bukan autentikasi kuat. Jangan gunakan sebagai perlindungan data sensitif.
- Pembatasan halaman web (misalnya klik kanan) bukan kiosk mode Android. Aplikasi ini belum mengunci Home/Recent, notifikasi, perpindahan aplikasi, screenshot, atau akses sistem. Penguncian sungguhan memerlukan implementasi Android Lock Task Mode dan konfigurasi perangkat sebagai dedicated/kiosk device (umumnya oleh administrator perangkat).
- Timer berjalan pada perangkat siswa; tanpa server, perangkat yang dimodifikasi atau perubahan jam sistem dapat memengaruhi keandalan ujian.
- Uji impor CSV, upload gambar, ekspor file, rotasi/perubahan lifecycle, dan hasil PNG di perangkat Android sasaran sebelum digunakan resmi.
- File sumber ini tidak menyertakan Gradle Wrapper; GitHub Actions memasang Gradle 8.9 untuk build.
