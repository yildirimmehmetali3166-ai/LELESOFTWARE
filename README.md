# Nicea Dedektör - 3D & 2D Yeraltı ve Alan Tarama Sistemi (Android)

Nicea Dedektör, jeofizik alan tarama, yeraltı boşluğu/mezar odası tespiti, değerli metal (altın/bronz) dipol analizi ve manyetik gradyan haritalandırması için geliştirilmiş Android Jetpack Compose uygulamasıdır.

## Mimari & Özellikler

- **3D Yeraltı ve Alan Görselleştirme:**
  - İzometrik ve perspektif kamera açıları, serbest dokunmatik 3D döndürme ve yakınlaştırma.
  - 4 farklı görünüm modu: Yüzey Ağı (Surface), 3D Voksel (Prism Columns), Tel Çerçeve (Wireframe), İzole Anomaliler.
  - 5 jeofizik renk paleti: Nicea Spektrum, Termal Jet, Magma, Zümrüt Boşluk, Altın Arama.
  - Dinamik derinlik kesiti (0-12m) ve sinyal eşiği filtreleme.
  - 3D anomali işaretleyicileri ve hedef derinlik sütunları.

- **2D Plan Matrisi & Jeofizik Kesit (Cross-Section):**
  - Bilinear düzgün enterpolasyonlu ısı matrisi veya piksel blok görünümü.
  - Kuzey yön pusulası ve etkileşimli hücre seçimi.
  - X ve Y aksı boyunca derinlik penetrasyon ve sinyal genlik eğrisi (Cross-Section).
  - Hücresel ses tonu sentezi (Boşluklar için kalın uğultu, metaller için tiz dedektör sinyali).

- **Canlı Saha Taraması (Walking Simulator):**
  - Donanım prob simülasyonu: Çift FGM fluxgate sensörleri, gradyan farkı ($\Delta nT$), eğim (pitch/roll), batarya ve BLE bağlantı göstergeleri.
  - Zikzak (Boustrophedon) ve Paralel adım tarama modları.
  - Adım adım veri toplama ve bitişte doğrudan 3D/2D analiz motoruna aktarma.

- **Raporlama & Dışa Aktarım:**
  - Boşluk, değerli metal, ferromanyetik ve mineral hedef sınıflandırması.
  - Hacim ($m^3$), derinlik ($m$), koordinat ($X, Y$) hesaplamaları.
  - CSV ve JSON formatında veri paylaşımı.

- **Sistem Mimarisi & Geliştirme Planı:**
  - STM32 ve nRF52840 BLE 50 Hz GATT protokol paket spesifikasyonları.
  - Android Kotlin, Room ve Jetpack Compose entegrasyonu.
  - Python SciPy ve Kriging enterpolasyon kütüphaneleri.

## Teknoloji Altyapısı
- **Dil:** Kotlin
- **UI:** Jetpack Compose (Material Design 3)
- **Minimum SDK:** 26 (Android 8.0+)
- **Target SDK:** 35
