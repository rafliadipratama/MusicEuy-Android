# MusicEuy Mobile (Android Native)

Aplikasi streaming musik *open-source* berbasis native **Kotlin** & **Jetpack Compose** untuk Android, dirancang sebagai pendamping mobile mandiri dari proyek **MusicEuy**.

---

## Fitur Utama

- **100% Bebas Biaya Langganan**: Menelusuri jutaan lagu, artis, dan album populer di seluruh dunia secara langsung tanpa perlu akun berbayar.
- **Background Playback & Lockscreen Controls**: Dibangun menggunakan **AndroidX Media3 (ExoPlayer)** dan `MediaSessionService`, lagu tetap berputar tanpa henti saat layar HP terkunci atau aplikasi ditutup.
- **Lirik Real-Time Tersinkronisasi**: Terintegrasi langsung dengan **LRCLIB API** untuk menampilkan lirik lagu yang bergulir otomatis sesuai detik pemutaran.
- **Tampilan Modern Khas MusicEuy**: Antarmuka gelap elegan dengan aksen Emerald & Purple khas MusicEuy Web, dilengkapi *Floating Mini Player* dan *Full Player Screen*.
- **3-Mode Repeat & Shuffle**: Mengadopsi logika pemutar modern: *Repeat Off ⟶ Repeat All ⟶ Repeat One*.

---

## Arsitektur & Teknologi

- **Bahasa**: Kotlin 2.1
- **UI Framework**: Jetpack Compose + Material 3 (Declarative UI)
- **Audio Engine**: AndroidX Media3 ExoPlayer (`1.5.0`) & MediaSessionService
- **Networking**: OkHttp 4.12
- **Image Loader**: Coil Compose 2.7
- **Arsitektur**: MVVM (Model-View-ViewModel) + StateFlow & Coroutines

---

## Menjalankan & Membangun APK

### Prasyarat
- JDK 21
- Android SDK (API 35)

### Kompilasi APK Debug
```bash
./gradlew assembleDebug
```
File APK yang dihasilkan akan berada di:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Lisensi & Kepemilikan
Dibuat dan dikembangkan oleh **Rafli Adi Pratama**.
