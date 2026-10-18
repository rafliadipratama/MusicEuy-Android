# MusicEuy Android Packages & Releases

Berkas APK resmi untuk aplikasi pemutar musik **MusicEuy Android**.

## Berkas Instalasi

| Berkas | Versi | Ukuran | Arsitektur | Keterangan |
| :--- | :--- | :--- | :--- | :--- |
| [`MusicEuy-v1.5.0.apk`](./MusicEuy-v1.5.0.apk) | 1.5.0 | ~74 MB | Universal (arm64-v8a, armeabi-v7a, x86_64) | Signed Release APK (Terbaru - Musicfy & Apple Music Architecture) |
| [`MusicEuy-v1.4.0.apk`](./MusicEuy-v1.4.0.apk) | 1.4.0 | ~16 MB | Universal (arm64-v8a, armeabi-v7a, x86_64) | Signed Release APK (Versi Sebelumnya) |
| [`MusicEuy-v1.3.0.apk`](./MusicEuy-v1.3.0.apk) | 1.3.0 | ~16 MB | Universal (arm64-v8a, armeabi-v7a, x86_64) | Signed Release APK (Versi Sebelumnya) |
| [`MusicEuy-v1.1.0.apk`](./MusicEuy-v1.1.0.apk) | 1.1.0 | ~16 MB | Universal (arm64-v8a, armeabi-v7a, x86_64) | Signed Release APK (Versi Sebelumnya) |

---

## Panduan Instalasi di Perangkat Android (Poco F3 / Xiaomi / HyperOS / MIUI)

1. **Unduh Berkas APK**:
   - Unduh langsung [`MusicEuy-v1.5.0.apk`](./MusicEuy-v1.5.0.apk) dari repositori ini atau melalui menu [GitHub Releases](https://github.com/rafliadipratama/MusicEuy-Android/releases).
2. **Izin Sumber Tidak Dikenal (*Install Unknown Apps*)**:
   - Jika muncul peringatan saat membuka APK di Chrome atau File Manager, masuk ke:
     `Setelan (Settings) -> Privasi & Keamanan -> Pasang Aplikasi yang Tidak Dikenal (Install Unknown Apps)`.
   - Berikan izin (*Allow*) untuk File Manager atau Browser Anda.
3. **Pasang & Buka**:
   - Ketuk **Instal**.
   - Buka aplikasi **MusicEuy**. Aplikasi langsung siap memutar lagu tanpa login akun apa pun!

---

## Pembaruan di Versi 1.5.0 (Fondasi Arsitektur Musicfy & Apple Music)

- **Perbaikan Algoritma Pencarian & Pemutaran Instan**:
  - **Direct Track Playback**: Memperbaiki bug di mana lagu hasil pencarian tidak bisa langsung diputar. Sekarang setiap lagu dapat diketuk langsung dari layar pencarian tanpa perlu masuk ke layar artis terlebih dahulu.
  - **Multi-Stage Resilient Stream Resolution**: Sistem resolusi bertingkat otomatis (Local Cache -> Direct FLAC/MP3 -> Plugin Resolvers -> NewPipeExtractor -> Dynamic YouTube Fallback) dengan pemulihan mandiri (*self-healing error listener*) saat stream audio terputus.
  - **Monochrome Hi-Res Lossless Fix**: Memperbaiki endpoint direct byte-range FLAC dan mapping data JSON artis serta artwork cover.

- **Integrasi JavaScript Solver Engine (YouTube Cipher & Signature)**:
  - Mengadopsi arsitektur solver JavaScript dari Musicfy (`yt.solver.core.js`, `astring.js`, `meriyah.js`) yang dieksekusi secara instan via engine WebView tanpa overhead.
  - Mencegah error stream expired, error 403 Forbidden, dan menangani transformasi parameter `n` YouTube Music secara otomatis.

- **Rombak Total UI/UX (Clean, Familiar Spotify, Apple Music & YouTube Music)**:
  - **Apple Superellipse Squircles**: Menggunakan geometri lengkungan `SmoothCornerShape` (kurvatur eksponen 4.0) untuk semua artwork, kartu, dan miniplayer.
  - **Tactile Micro-Interactions**: Animasi pantulan pegas `pressBounce` (stiffness 1600f, damping ratio 0.42f) dan efek kemiringan perspektif 3D `press3D`.
  - **Animated Equalizer Bars**: Indikator animasi equalizer audio dinamis `PlayingIndicator` yang berdenyut saat lagu sedang aktif diputar.
  - **Apple Music & Spotify Top Result Card**: Kartu *Hasil Terbaik* teratas dengan latar belakang ambient artwork kabur, tombol *Play/Pause* melingkar, dan quick filter pills (Semua, Lagu, Artis, Album).
  - **Jelajahi Kategori / Genre Matrix**: Grid 2-kolom visual bergaya Spotify dengan gradien warna modern untuk eksplorasi genre musik cepat.
  - **Pembersihan Arsitektur & Deduplikasi**: Menghapus duplikasi *BackHandler*, menyederhanakan hierarki navigasi, dan menghilangkan elemen layout yang berantakan (*clean architecture*).

- **Desain & Layout Beranda Modern (Musicfy Style)**:
  - **Hero Carousel Editorial Banner**: Kartu sorotan lagu terpopuler dengan gradien vibran, label *Featured Spotlight*, badge fidelitas *Hi-Res FLAC*, dan tombol pemutaran instan.
  - **Quick Action Bar**: Tombol pengenal musik sekitar (*Shazam In-App*), tombol sinkronisasi akun, dan avatar profil dengan inisial dinamis.
  - **Tangga Lagu & Charts Resmi**: Tangga lagu resmi Indonesia dan Global dengan artwork bergradasi modern.

- **Profil Pengguna & Kartu VIP Member Pass**:
  - **Holographic VIP Member Card**: Kartu anggota eksklusif dengan nomor ID unik (`EUY-2026-XXXX`), status audiophile terverifikasi, dan efek kilau gradien holografik.
  - **Statistik Mendengarkan**: Menampilkan total lagu diputar, total menit durasi pemutaran, dan artis favorit.
  - **Dialog Edit Profil**: Kustomisasi nama tampilan, bio pribadi, dan foto profil.

- **Musicfy Audio Lab & Streaming Engine**:
  - **Audio Quality Tier**: Pilihan tingkat fidelitas murni *Hi-Res Lossless FLAC (24-bit)*, *Lossless Opus 320k*, *Balanced AAC 256k*, dan *Data Saver 128k*.
  - **AutoEQ Headphone Target**: Kalibrasi respon frekuensi *Harman Target* untuk AirPods Pro 2, AirPods Max, Sony WH-1000XM5/XM4, WF-1000XM5, Galaxy Buds 2 Pro, Moondrop Chu II/Aria, Sennheiser HD600/650, Audio-Technica ATH-M50x, Tangzu Wan'er, dan 7Hz Zero:2.
  - **Crossmix DJ Engine**: Transisi antar lagu tingkat studio (*Smooth Crossfade*, *Vinyl Spin-back*, *Tape-stop*, *Filter Sweep*).
  - **Interlude Dots & Terjemahan Lirik**: Animasi titik ritmik saat jeda vokal/instrumental dan auto-terjemahan lirik asing.
  - **PO Token & Bot-Detection Bypass**: Sakelar bypass otomatis deteksi bot YouTube Music tanpa memerlukan akun Google.

- **Fitur Ekosistem Tambahan**:
  - **Shazam Ambient Recognition**: Kenali judul lagu dari audio ruangan sekitar menggunakan mikrofon perangkat.
  - **Social Lyrics Story Generator**: Generator kartu kutipan lirik dengan cover art untuk Instagram Stories & WhatsApp.
  - **Apple Music Playlist Cover Generator**: Generator cover seni playlist dengan kuas warna gradien dinamis.
  - **Efek Partikel Like Burst**: Ledakan partikel halus saat menyukai lagu.
  - **Pembaruan Aplikasi OTA**: Pengecekan versi rilis langsung via GitHub Releases API.
  - **Dukungan Multi-Bahasa**: Pilihan bahasa Indonesia dan Inggris secara instan di dalam aplikasi.
  - **Plugin Streaming Lengkap**: Dukungan stream Monochrome Hi-Res FLAC, YouTube Music, JioSaavn, SoundCloud, Deezer, Bandcamp, Jamendo, Discord RPC, dan Last.fm Scrobbler.


---

## Pembaruan di Versi 1.4.0

- **Setup Pengguna Baru (First-Run Onboarding Flow)**:
  - **Layar Sambutan Modern**: Hero logo animasi dengan 3 kartu keunggulan MusicEuy (Bebas Iklan, Audio Caching Hemat Kuota, Lirik Realtime LRCLIB).
  - **Pilih Musisi Favorit (Pick 3+ Artists)**: Menampilkan grid lingkaran foto musisi top Indonesia & global (*Hindia, Tulus, Sheila On 7, Bernadya, Mahalini, Denny Caknan, NIKI, Taylor Swift, Coldplay*, dll).
  - **Pencarian Musisi di Onboarding**: Memudahkan pengguna mencari dan memilih musisi apa pun di luar daftar kurasi bawaan.
  - **Kalibrasi Instan Beranda**: Lagu-lagu top dari musisi pilihan pengguna langsung otomatis diambil dan mengisi Beranda saat pertama kali dibuka.
  - **Tombol Lewati (Skip)**: Pengguna yang ingin langsung menikmati lagu dapat melewati langkah setup kapan saja.
  - **Personalisasi Kapan Saja**: Tombol penala (*Tune*) di tab Koleksi memungkinkan pengguna mengubah kembali musisi favoritnya kapan pun.

---

## Pembaruan di Versi 1.3.0

- **Playback & Audio Quality**:
  - Transisi lagu halus **Crossfade & Gapless Playback** (opsi 0s/mati, 3s, 6s, 9s, 12s).
  - **Normalisasi Volume (Loudness Normalization)** otomatis mencegah lonjakan suara.
  - **Pemilih Kualitas Bitrate**: Hemat Kuota (96 kbps OPUS), Standar (160 kbps), dan High Fidelity (256–320 kbps).
  - **Offline Lyrics Caching**: Lirik realtime LRCLIB otomatis tersimpan di penyimpanan internal agar tetap dapat dibaca tanpa kuota.
- **Eksplorasi & Algoritma Rekomendasi**:
  - **Song Radio (Quick Picks)**: Buat antrean otomatis rekomendasi lagu serupa hanya dengan 1-klik di menu lagu.
  - **Mood & Category Chips**: Filter cepat di Beranda (Fokus, Santai, Olahraga, Pesta, Pop Indo, Indie, Rock, Dangdut).
  - **Tangga Lagu & Charts Resmi**: Carousel Top 50 Indonesia, Top 50 Global, Viral Hits, dan Rilisan Terbaru.
- **Manajemen Koleksi & Impor Playlist**:
  - **Impor Playlist Spotify & YouTube via URL**: Konversi link playlist publik menjadi playlist lokal tanpa login akun.
  - **Pencarian Cepat & Penyortiran di Dalam Playlist**: Filter pencarian instan dan sortir berdasarkan Judul (A-Z), Nama Artis, atau Durasi.
- **Ekosistem & Integrasi Perangkat**:
  - **Dukungan Android Auto**: Terintegrasi `MediaLibraryService` untuk penjelajahan musik langsung di dashboard mobil.
  - **Manajer Penyimpanan & Cache**: Pantau kapasitas cache streaming & berkas offline, serta tombol 1-klik pembersihan memori.
- **Personalisasi & Statistik**:
  - **MusicEuy Recap / Wrapped**: Statistik total menit mendengarkan, artis favorit teratas, dan lagu yang paling sering diputar.
