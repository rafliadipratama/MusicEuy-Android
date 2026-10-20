# SoraFy (Multiplatform Monorepo)

<div align="center">

<img src="shared/assets/logo_monogram.png" alt="SoraFy Logo" width="128" />

### **Aplikasi Streaming Musik Cepat, Elegan, dan Bebas Biaya Langganan untuk Android, Linux, dan Windows**

[![Release](https://img.shields.io/github/v/release/rafliadipratama/sorafy?color=00e599&style=flat-square)](https://github.com/rafliadipratama/sorafy/releases)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android_7.0%2B-brightgreen?style=flat-square&logo=android)](https://www.android.com/)
[![Linux](https://img.shields.io/badge/Platform-Linux_(AppImage%20%7C%20deb)-FCC624?style=flat-square&logo=linux&logoColor=black)](https://kernel.org/)
[![Windows](https://img.shields.io/badge/Platform-Windows_10%2F11-0078D6?style=flat-square&logo=windows)](https://www.microsoft.com/)
[![Rust](https://img.shields.io/badge/Rust-1.80%2B-orange?style=flat-square&logo=rust)](https://www.rust-lang.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org/)

</div>

---

## 🎧 Tentang SoraFy

**SoraFy** adalah aplikasi pemutar dan pengalir musik lintas platform (*cross-platform*) modern yang dibangun dengan arsitektur **Monorepo** dan mengusung filosofi **Domain-Driven Design (DDD)**. 

Tersedia untuk:
- 📱 **Android**: Native Kotlin + Jetpack Compose (Material 3) + AndroidX Media3
- 🐧 **Linux**: Native Desktop App (Rust + Tauri v2)
- 🪟 **Windows**: Native Desktop App (Rust + Tauri v2)

Dirancang dengan nuansa visual gelap premium: kanvas *Obsidian Void*, kartu *Slate Surface*, serta aksen kembar *Priangan Emerald* & *Electric Lime*.

---

## 🎨 Sistem Desain & Palet Warna Resmi

| Token Desain | Kode Hex | Peran Antarmuka |
| :--- | :--- | :--- |
| **Obsidian Void** | `#0B0D10` | Latar belakang kanvas utama (*canvas background*), ramah layar OLED/AMOLED |
| **Slate Surface** | `#181C22` | Warna dasar kartu (*cards*), wadah baris lagu (*track rows*), *mini-player*, dan laci samping |
| **Priangan Emerald** | `#00E599` | Warna aksen utama (*primary brand*): tombol putar (*play*), *wavy seekbar*, logo, dan indikator aktif |
| **Electric Lime** | `#C8FF29` | Aksen sekunder *neon energetic*: *glow active lyrics*, *badge* kualitas audio, dan highlight status |

---

## 🏛️ Arsitektur Monorepo & Domain-Driven Design (DDD)

Repository ini disusun dalam struktur **Monorepo** yang memisahkan tanggung jawab domain bisnis dari implementasi infrastruktur dan presentasi:

```text
sorafy/
├── android/                   # Aplikasi Mobile Android (Kotlin + Jetpack Compose)
│   ├── app/                   # Sumber kode Android native (Media3, UI, Fingerprinting)
│   ├── build.gradle.kts       # Konfigurasi Gradle root Android
│   └── settings.gradle.kts
│
├── desktop/                   # Aplikasi Desktop Windows & Linux (Rust + Tauri v2)
│   ├── src-tauri/             # Backend Rust berarsitektur Hexagonal / DDD
│   │   ├── src/
│   │   │   ├── domain/        # Lapisan Domain Inti (Bebas framework)
│   │   │   │   ├── model.rs   # Entity & Value Object: Track, Artist, Album, Lyrics, PlaybackState
│   │   │   │   ├── ports.rs   # Trait / Port: AudioPlayerPort, CatalogRepositoryPort, LyricsRepositoryPort
│   │   │   │   └── error.rs   # Domain Error types
│   │   │   ├── application/   # Lapisan Aplikasi & Use Cases
│   │   │   │   ├── player_service.rs   # Orchestration antrean & pemutaran
│   │   │   │   ├── catalog_service.rs  # Pencarian & katalog musik
│   │   │   │   ├── lyrics_service.rs   # Sinkronisasi lirik LRCLIB
│   │   │   │   └── commands.rs         # Tauri IPC Command Handlers
│   │   │   └── infrastructure/         # Lapisan Adapter & Infrastruktur
│   │   │       ├── audio_engine.rs     # Implementasi AudioPlayerPort
│   │   │       ├── innertube_client.rs # Implementasi CatalogRepositoryPort (Streaming Resolver)
│   │   │       ├── lrclib_client.rs    # Implementasi LyricsRepositoryPort (LrcLib Parser)
│   │   │       └── local_storage.rs    # Implementasi StoragePort (Riwayat & Favorit)
│   │   └── tauri.conf.json    # Konfigurasi jendela & bundle Tauri v2
│   └── src/                   # Antarmuka Desktop Modern (SoraFy UI + Canvas Squiggly Wave)
│
├── shared/                    # Aset Bersama & Token Desain Lintas Platform
│   ├── design/
│   │   ├── tokens.json        # Nilai token resmi (warna, tipografi, radius, shadow)
│   │   └── colors.css         # Variabel CSS siap pakai
│   └── assets/                # Logo monogram, ikon origami, dan aset visual
│
└── .github/workflows/
    └── release.yml            # CI/CD otomatis untuk rilis APK, AppImage, Deb, dan Windows EXE
```

---

## ✨ Fitur Unggulan

1. **Squiggly Waveform Seekbar (Android & Desktop)**: Bilah progres lagu bergelombang dinamis sinusoidal yang hidup saat lagu berputar dan merata elastis saat dihentikan.
2. **Apple Music-Style Glowing Karaoke Lyrics**: Lirik tersinkronisasi (*timestamped*) dengan pendaran cahaya (*ambient neon glow*) pada baris aktif yang bergerak otomatis (*auto-scroll*).
3. **High-Fidelity Audio Badge**: Indikator langsung kualitas berkas (*FLAC Lossless, Opus Hi-Res 320kbps, AAC High*).
4. **Pendeteksi Musik Shazam Nyata (Android)**: Identifikasi lagu sekitar secara instan menggunakan sidik jari akustik *Fast Fourier Transform* (FFT).
5. **Streaming Unthrottled**: Bebas buffering tanpa throttling dengan *smart preloading*.
6. **Top Toast Notifications**: Notifikasi melayang elegan dari atas layar saat lagu ditambahkan ke antrean atau disukai.
7. **Satu Tautan Distribusi Rilis**: Pengguna cukup membuka satu tautan GitHub Release untuk mengunduh versi Android, Linux, maupun Windows.

---

## 🚀 Panduan Membangun (*Build Instructions*)

### 1. Membangun Android (APK)

```bash
cd android
./gradlew assembleDebug
# atau untuk release APK siap pasang:
./gradlew assembleRelease
```
*Hasil APK berada di `android/app/build/outputs/apk/`.*

### 2. Menjalankan & Membangun Desktop (Linux & Windows)

**Menjalankan Core Engine Rust DDD:**
```bash
cd desktop/src-tauri
cargo run
```

**Membangun Aplikasi Desktop (Tauri v2):**
```bash
cd desktop
npm install
npx tauri build
```
*Hasil binary installer Linux (`.AppImage`, `.deb`) atau Windows (`.exe`, `.msi`) akan otomatis tersedia di `desktop/src-tauri/target/release/bundle/`.*

---

## 📥 Unduh Rilis Resmi

Seluruh versi rilis untuk semua platform (Android APK, Linux AppImage/deb, Windows exe) dipublikasikan secara terpadu di:
👉 **[Unduh SoraFy di GitHub Releases](https://github.com/rafliadipratama/sorafy/releases/latest)**

---

## 📄 Lisensi

Proyek ini dilisensikan di bawah [GNU General Public License v3.0 (GPLv3)](LICENSE).
Dibuat dengan dedikasi oleh [Rafli Adi Pratama](https://github.com/rafliadipratama).
