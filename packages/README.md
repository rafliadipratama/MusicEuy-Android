# SoraFy Android Packages & Releases

Berkas APK resmi untuk aplikasi pemutar musik **SoraFy Android**.

## 📥 Berkas Instalasi

| Berkas | Versi | Ukuran | Arsitektur | Keterangan |
| :--- | :--- | :--- | :--- | :--- |
| **[`SoraFy-v1.9.0.apk`](./SoraFy-v1.9.0.apk)** | **1.9.0** | **~75 MB** | Universal (arm64-v8a, armeabi-v7a, x86_64, x86) | **Rilis Resmi SoraFy (Terbaru)**: Rebranding resmi, palet Obsidian Void & Priangan Emerald, Squiggly Slider, Glowing Karaoke Lyrics, Shazam Acoustic Fingerprinting |
| [`MusicEuy-v1.5.0.apk`](./MusicEuy-v1.5.0.apk) | 1.5.0 | ~74 MB | Universal (arm64-v8a, armeabi-v7a, x86_64) | Rilis Versi Sebelumnya (Fondasi Solver Engine) |
| [`MusicEuy-v1.4.0.apk`](./MusicEuy-v1.4.0.apk) | 1.4.0 | ~16 MB | Universal (arm64-v8a, armeabi-v7a, x86_64) | Signed Release APK (Versi Sebelumnya) |
| [`MusicEuy-v1.3.0.apk`](./MusicEuy-v1.3.0.apk) | 1.3.0 | ~16 MB | Universal (arm64-v8a, armeabi-v7a, x86_64) | Signed Release APK (Versi Sebelumnya) |
| [`MusicEuy-v1.1.0.apk`](./MusicEuy-v1.1.0.apk) | 1.1.0 | ~16 MB | Universal (arm64-v8a, armeabi-v7a, x86_64) | Signed Release APK (Versi Sebelumnya) |

---

## 📱 Panduan Instalasi di Perangkat Android

1. **Unduh Berkas APK**:
   - Unduh langsung [`SoraFy-v1.9.0.apk`](./SoraFy-v1.9.0.apk) dari repositori ini atau melalui menu [GitHub Releases](https://github.com/rafliadipratama/sorafy/releases).
2. **Izin Sumber Tidak Dikenal (*Install Unknown Apps*)**:
   - Jika muncul dialog keamanan saat membuka berkas APK di File Manager atau Browser:
     - Masuk ke: `Setelan (Settings) -> Privasi & Keamanan -> Pasang Aplikasi yang Tidak Dikenal (Install Unknown Apps)`.
     - Berikan izin (*Allow from this source*) untuk File Manager atau Browser yang Anda gunakan.
3. **Pasang & Nikmati**:
   - Ketuk **Instal**.
   - Buka aplikasi **SoraFy**. Aplikasi langsung siap memutar musik tanpa batasan iklan maupun biaya langganan!

---

## 🌟 Yang Baru di Versi 1.9.0 (Rilis Resmi SoraFy)

### 🎨 1. Rebranding Total & Identitas Desain SoraFy
- **Palet Warna Resmi**:
  - **Obsidian Void (`#0B0D10`)**: Kanvas gelap murni ramah layar OLED/AMOLED.
  - **Slate Surface (`#181C22`)**: Wadah kartu, baris lagu, bottom sheet, dan mini-player.
  - **Priangan Emerald (`#00E599`)**: Aksen primer pada tombol play, seekbar, dan logo.
  - **Electric Lime (`#C8FF29`)**: Aksen neon highlight untuk pendaran lirik dan badge status audio.
- **Ikon Peluncur Adaptif**: Menggunakan logo monogram "S" origami isometrik resolusi tinggi di semua varian DPI (`mipmap-anydpi-v26`, `xxxhdpi`, `xxhdpi`, `xhdpi`, `hdpi`, `mdpi`).

### 🌊 2. Bouncy Squiggly / Wavy Seekbar
- Bilah durasi lagu bergelombang sinusoidal dinamis yang hidup dan berdenyut mengikuti lagu saat berputar, serta merata elastis (*flatten*) saat disentuh atau dijeda.

### 🎤 3. Glowing Karaoke Lyrics (Apple Music Style)
- Tampilan lirik lagu tersinkronisasi per milidetik dengan pendaran cahaya ambient radial neon pada baris aktif yang bergerak otomatis (*auto-scroll*).

### 🔍 4. Pengenal Musik Shazam Nyata (Acoustic Fingerprinting)
- Deteksi lagu sekitar secara mandiri menggunakan transformasi Fourier FFT tanpa library eksternal berlebih, langsung terhubung ke katalog musik untuk pemutaran instan.

### 🏷️ 5. Badge Format & Kualitas Audio Teknis
- Label langsung pada pemutar: *FLAC Lossless, Opus Hi-Res (160/256/320 kbps), AAC High*.

### ⚡ 6. Migrasi Arsitektur Monorepo
- Struktur monorepo baru yang bersih dan terisolasi dengan repositori inti di `android/`.
