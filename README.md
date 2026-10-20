# SoraFy Mobile (Android Native)

<div align="center">

![SoraFy Logo](app/src/main/res/drawable/ic_sorafy_logo.png)

### **Aplikasi Streaming Musik Native Cepat, Elegan, dan Bebas Biaya Langganan untuk Android**

[![Release](https://img.shields.io/github/v/release/rafliadipratama/sorafy?color=00e599&style=flat-square)](https://github.com/rafliadipratama/sorafy/releases)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android_7.0%2B-brightgreen?style=flat-square&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

</div>

---

## 🎧 Tentang SoraFy

**SoraFy** adalah pemutar dan pengalir musik modern untuk perangkat Android yang dibangun menggunakan **Kotlin** dan **Jetpack Compose**. Dirancang dengan filosofi desain modern, gelap elegan (*Obsidian Void*), beraksen *Priangan Emerald* & *Electric Lime*, menghadirkan pengalaman mendengarkan musik sekelas aplikasi premium: **tanpa iklan, tanpa batasan langganan, dan dengan performa pemutaran instan**.

---

## 🎨 Sistem Desain & Palet Warna Resmi

| Token Warna | Hex Code | Peran di Aplikasi |
| :--- | :--- | :--- |
| **Obsidian Void** | `#0B0D10` | Latar belakang utama (background) yang dalam, elegan, dan ramah layar OLED/AMOLED |
| **Slate Surface** | `#181C22` | Warna dasar kartu (*card*), wadah baris lagu (*track row*), *bottom sheet*, navigasi bar, dan *mini-player* |
| **Priangan Emerald** | `#00E599` | Warna aksen utama (*primary brand*): tombol putar (*play*), *wavy seekbar*, indikator aktif, dan logo |
| **Electric Lime** | `#C8FF29` | Aksen sekunder bernuansa *neon energetic*: *badge* kualitas audio, *highlight*, tag lirik, dan tombol aksi cepat |

---

## ✨ Fitur Unggulan

### 🔍 1. Pengenal Musik Shazam Nyata (*Acoustic Fingerprinting*)
- Deteksi lagu yang sedang diputar di sekitar kamu hanya dengan mendekatkan smartphone ke sumber audio.
- Menggunakan algoritma sidik jari akustik murni (*Fast Fourier Transform / Cooley-Tukey DIT & Hanning windowing*) yang di-porting langsung ke Kotlin tanpa dependensi library eksternal yang berat.
- Lagu yang terdeteksi otomatis disinkronkan ke katalog YouTube Music sehingga **bisa langsung diputar dan distreaming saat itu juga**.

### ⚡ 2. Streaming Unthrottled & Preload Antrean
- **Bypass Throttling YouTube**: Streaming audio dimulai dalam hitungan milidetik tanpa *buffering* berlebih berkat *Unthrottled Stream Resolver*.
- **Smart TTL Caching & Preloading**: Lagu berikutnya dalam antrean otomatis disiapkan di memori latar belakang agar transisi antarlagu terasa mulus tanpa jeda.

### 📈 3. Top 50 Hits Indonesia & Global Resmi
- Jelajahi tangga lagu terpopuler harian dan mingguan langsung dari data resmi YouTube Music Charts dengan sampul album beresolusi tinggi.

### 🎤 4. Apple Music-Style Glow Karaoke Lyrics
- Tampilan lirik tersinkronisasi (*synchronized lyrics*) di mana baris aktif menyala terang dengan pendaran cahaya (*ambient radial aura glow*), sementara baris sekitarnya meredup lembut.

### 🌊 5. Bouncy Squiggly / Wavy Seekbar
- Bilah progres lagu bergelombang dinamis seperti ombak audio hidup saat musik berputar, dan merata (*flatten*) elastis saat disentuh atau di-pause.

### 🏷️ 6. Badge Format & Kualitas Audio
- Indikator kualitas audio teknis (*OPUS 160/256 kbps, AAC, FLAC/WAV Lossless*) langsung di bawah judul lagu.

### 📱 7. Pemutaran Latar Belakang (*Background Playback*) & Kontrol Lockscreen
- Didukung penuh oleh **AndroidX Media3 (ExoPlayer)** dan `MediaSessionService`.
- Kendalikan lagu langsung dari panel notifikasi Android, jam tangan pintar (smartwatch), maupun layar kunci (*lockscreen*).

### 💾 8. Mode Offline & Penyimpanan Lokal
- Simpan lagu favorit ke perangkat untuk didengarkan saat bepergian tanpa kuota data internet.

### 🎛️ 9. Equalizer & Efek Suara
- Kustomisasi kualitas suara sesuai selera dengan Bass Boost, Virtualizer, Reverb, dan pemilihan bitrate audio.

---

## 🛠️ Arsitektur & Teknologi

| Komponen | Teknologi |
| :--- | :--- |
| **Bahasa Utama** | Kotlin 2.1 |
| **Framework UI** | Jetpack Compose + Material 3 (Declarative UI) |
| **Audio Engine** | AndroidX Media3 ExoPlayer (`1.5.0`) & MediaSessionService |
| **Pendeteksi Musik** | Pure Kotlin FFT Acoustic Landmark + Shazam Discovery API |
| **Katalog & Ekstraktor** | NewPipe Extractor & Innertube Protocol |
| **HTTP Client** | OkHttp 4.12 |
| **Pemuat Gambar** | Coil Compose 2.7 |
| **Pola Arsitektur** | Clean MVVM (Model-View-ViewModel) + Coroutines & StateFlow |

---

## 📥 Unduh & Instalasi

Kamu dapat mengunduh berkas APK siap pakai langsung dari halaman [**GitHub Releases**](https://github.com/rafliadipratama/sorafy/releases).

1. Buka rilis terbaru di tab **Releases**.
2. Unduh berkas APK terbaru.
3. Pasang APK pada perangkat Android kamu (pastikan izin instalasi dari sumber tidak dikenal telah diaktifkan).
4. Nikmati musik sepuasnya!

---

## 💻 Cara Membangun dari Sumber (*Build from Source*)

### Prasyarat
- **JDK**: Java Development Kit 21 atau lebih baru
- **Android SDK**: Target API 35 (Android 15)
- **Gradle**: Menggunakan Gradle Wrapper bawaan proyek

### Langkah-langkah
```bash
# 1. Kloning repositori
git clone https://github.com/rafliadipratama/sorafy.git
cd sorafy

# 2. Build APK Release
./gradlew assembleRelease

# 3. APK hasil build akan tersedia di:
# app/build/outputs/apk/release/app-release.apk
```

---

## 📜 Lisensi

Proyek ini dilisensikan di bawah ketentuan [**GNU General Public License v3.0 (GPL-3.0)**](LICENSE).

Dibuat dan dikembangkan oleh **[Rafli Adi Pratama](https://github.com/rafliadipratama)**.
