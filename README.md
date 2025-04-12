# Purrytify

## Tugas Besar 1 IF3210 - Pengembangan Aplikasi Piranti Bergerak

## Deskripsi Aplikasi

Purrytify merupakan sebuah aplikasi android native yang dikembangkan dengan bahasa pemrograman Kotlin. Aplikasi ini menyediakan berbagai fungsionalitas terkait music player dengan tampilan berbasis Jetpack Compose.

### Spesifikasi SDK

- Minimum SDK: Android 10 (API Level 29)
- Target SDK: Android 13 (API Level 34)
- Compile SDK: Android 13 (API Level 34)

### Fitur Utama

1. Log in ke sebuah akun yang sudah terdaftar
2. Melihat profil pengguna yang berisi identitas, banyak lagu yang dimiliki, banyak lagu yang disukai, dan banyak lagu yang sudah didengarkan
3. Memasukkan lagu ke library berdasarkan akun yang terautentikasi
4. Mengatur playlist lagu berdasarkan daftar lagu yang ada pada library, liked song, recently played, atau newly added
5. Pemutar lagu dengan fitur seek to, play/pause, next/previous, shuffle, repeat one/all, like, dan edit song
6. Mini player dengan fitur seek to, play/pause, dan like song
7. Background service untuk melakukan reautentikasi ketika JWT kadaluarsa
8. Network sensing untuk mendeteksi kehilangan sinyal pada saat menggunakan aplikasi

## Daftar Library

1. Hilt untuk dependency injection
2. Jetpack Compose untuk UI
3. MediaPlayer dari Android SDK untuk pemutar lagu
4. Room Database untuk penyimpanan aplikasi
5. Coil untuk image loading
6. Retrofit & OkHTTP untuk pemanggilan API
7. Datastore untuk menyimpan JWT
8. Android SDK + Kotlin Coroutine untuk network sensing
9. WorkManager untuk refresh JWT

## Screenshot Aplikasi

### Layar Log-In

![login](images/login.png)

### Layer Home

![home](images/home.png)

### Layar Library

![library](/images/library.png)

### Layar Profile

![profile](/images/profile.png)

### Layar Media Player

![media_player](/images/player.png)

### Mini Player

![library-mini-player](/images/library-mini-player.png)

### Layer Add Song

![add](/images/add.png)

### Tampilan No Internet

![internet](/images/internet.png)

## Pembagian Kerja

| NIM | Nama | Pekerjaan |
| --- | --- | --- |
| 13522122 | Maulvi Ziadinda Maulana | Idk |
| 13522144 | Nicholas Reymond Sihite | UI Library, UI Navigation Bar, Media Player Manager, UI Mini Player, Network Sensing (logic + UI) |
| 13522153 | Muhammad Fauzan Azhim | Idk |

## Durasi Persiapan dan Pengerjaan

| NIM       | Pekerjaan        | Durasi Persiapan | Durasi Pengerjaan |
|-----------|------------------|------------------|-------------------|
| 13522122  | Pekerjaan 1     | X jam            | Y jam            |
|           | Pekerjaan 2  | X jam            | Y jam            |
| 13522144  | UI Library      | 1 jam            | 5 jam            |
|           | UI Navigation Bar   | 1 jam            | 2 jam             |
|           | Media Player Manager   | 1 jam            | 10 jam             |
|           | UI Mini Player   | 1 jam            | 5 jam             |
|           | Network Sensing   | 1 jam            | 2 jam             |
| 13522153  | Pemutar Lagu     | X jam            | Y jam            |
|           | Mini Player      | X jam            | Y jam             |

