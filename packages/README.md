# MusicEuy Android Packages & Releases

Berkas APK resmi untuk aplikasi pemutar musik **MusicEuy Android**.

## Berkas Instalasi

| Berkas | Versi | Ukuran | Arsitektur | Keterangan |
| :--- | :--- | :--- | :--- | :--- |
| [`MusicEuy-v1.1.0.apk`](./MusicEuy-v1.1.0.apk) | 1.1.0 | ~16 MB | Universal (arm64-v8a, armeabi-v7a, x86_64) | Signed Release APK (Siap Pasang) |

---

## Panduan Instalasi di Perangkat Android (Poco F3 / Xiaomi / HyperOS / MIUI)

1. **Unduh Berkas APK**:
   - Unduh langsung [`MusicEuy-v1.1.0.apk`](./MusicEuy-v1.1.0.apk) dari repositori ini atau melalui menu [GitHub Releases](https://github.com/rafliadipratama/MusicEuy-Android/releases).
2. **Izin Sumber Tidak Dikenal (*Install Unknown Apps*)**:
   - Jika muncul peringatan saat membuka APK di Chrome atau File Manager, masuk ke:
     `Setelan (Settings) -> Privasi & Keamanan -> Pasang Aplikasi yang Tidak Dikenal (Install Unknown Apps)`.
   - Berikan izin (*Allow*) untuk File Manager atau Browser Anda.
3. **Pasang & Buka**:
   - Ketuk **Instal**.
   - Buka aplikasi **MusicEuy**. Aplikasi langsung siap memutar lagu tanpa login akun apa pun!

---

## Pembaruan di Versi 1.1.0

- **Auto-Stop saat App di Swipe Up / Clean Latar Belakang**: Audio dan foreground playback service langsung berhenti otomatis ketika aplikasi dibersihkan dari recent tasks (tidak lagi menggantung di notification).
- **Music Queue Terintegrasi**: Akses cepat daftar antrean lagu (`musicquee`) langsung dari tombol ikon antrean di Bottom MiniPlayer maupun Player Screen lengkap dengan badge equaliser, hapus item, dan kosongkan antrean.
- **Algoritma Rekomendasi Musik Cerdas Offline (Tanpa Perlu User Profile Cloud)**:
  - Musik pertama kali buka aplikasi langsung menyajikan lagu yang paling sering Anda dengarkan (Most Played) dan lagu favorit, bukan random.
  - Data preferensi, frekuensi putar (*play count*), dan timestamp tersimpan 100% secara lokal di perangkat tanpa login/registrasi akun, aman dan cepat.
  - Menjadi fondasi untuk fitur playlist lokal kustom, halaman detail artis, dan filter genre mendatang.
- **Pembersihan Bersih Ekstensi Spotify**: Seluruh kode dan metadata Spotify Top 50 telah dihapus total dari proyek.
- **Pembersihan Teks Badge**: Menghapus teks "Resmi" berlebih di bawah durasi lagu, mempertahankan centang verifikasi elegan.
