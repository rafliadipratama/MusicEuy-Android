# MusicEuy Mobile (Android Native)

<div align="center">

![MusicEuy Logo](app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)

### **Aplikasi Streaming Musik Native Cepat, Ringan, dan Bebas Biaya Langganan untuk Android**

[![Release](https://img.shields.io/github/v/release/rafliadipratama/MusicEuy-Android?color=00e676&style=flat-square)](https://github.com/rafliadipratama/MusicEuy-Android/releases)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android_7.0%2B-brightgreen?style=flat-square&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

</div>

---

## 🎧 Tentang MusicEuy

**MusicEuy** adalah pemutar dan pengalir musik modern untuk perangkat Android yang dibangun menggunakan **Kotlin** dan **Jetpack Compose**. Dirancang untuk menghadirkan pengalaman mendengarkan musik sekelas aplikasi premium: **tanpa iklan, tanpa batasan langganan, dan dengan performa pemutaran instan**.

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

### 🎤 4. Lirik Real-Time Tersinkronisasi
- Lirik lagu bergulir otomatis (*synchronized lyrics*) mengikuti ketukan dan detik lagu secara *real-time* via integrasi LRCLIB.

### 📱 5. Pemutaran Latar Belakang (*Background Playback*) & Kontrol Lockscreen
- Didukung penuh oleh **AndroidX Media3 (ExoPlayer)** dan `MediaSessionService`.
- Kendalikan lagu langsung dari panel notifikasi Android, jam tangan pintar (smartwatch), maupun layar kunci (*lockscreen*).

### 💾 6. Mode Offline & Penyimpanan Lokal
- Simpan lagu favorit ke perangkat untuk didengarkan saat bepergian tanpa kuota data internet.

### 🎛️ 7. Equalizer & Efek Suara
- Kustomisasi kualitas suara sesuai selera dengan Bass Boost, Virtualizer, Reverb, dan pemilihan bitrate audio (Normal 160kbps / High 256kbps).

### 🎨 8. Tampilan Modern Material 3
- Mengadopsi desain gelap elegan beraksen Emerald & Purple khas MusicEuy.
- Dilengkapi animasi fluid waveform visualizer, *floating mini player*, dan antarmuka pemutar layar penuh (*full player*).

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

Kamu dapat mengunduh berkas APK siap pakai langsung dari halaman [**GitHub Releases**](https://github.com/rafliadipratama/MusicEuy-Android/releases).

1. Buka rilis terbaru di tab **Releases**.
2. Unduh berkas `MusicEuy-v1.6.0.apk`.
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
git clone https://github.com/rafliadipratama/MusicEuy-Android.git
cd MusicEuy-Android

# 2. Build APK Release
./gradlew assembleRelease

# 3. APK hasil build akan tersedia di:
# app/build/outputs/apk/release/app-release.apk
```

---

## 📜 Lisensi

Proyek ini dilisensikan di bawah ketentuan [**GNU General Public License v3.0 (GPL-3.0)**](LICENSE).

Dibuat dan dikembangkan oleh **[Rafli Adi Pratama](https://github.com/rafliadipratama)**.
