import React, { useState } from 'react';
import { 
  Code2, 
  Smartphone, 
  Terminal, 
  Cpu, 
  Layers, 
  Copy, 
  Check, 
  Download, 
  Calendar, 
  BookOpen, 
  FileCode2, 
  ExternalLink,
  ChevronRight,
  ShieldCheck
} from 'lucide-react';

export const DevelopmentPlan: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'roadmap' | 'python' | 'android' | 'protocol' | 'code_python' | 'code_android'>('roadmap');
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const pythonRequirements = `# Nicea Dedektör - Python 3D/2D Alan Tarama Bağımlılıkları
numpy>=1.26.0
scipy>=1.12.0
pyvista>=0.43.0
pyvistaqt>=0.11.0
PySide6>=6.6.0
bleak>=0.21.0
pyserial>=3.5
matplotlib>=3.8.0
pandas>=2.2.0`;

  const androidGradle = `// build.gradle.kts (Module :app)
dependencies {
    // Jetpack Compose & UI
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Nordic BLE Library (Stabil Sensör İletişimi)
    implementation("no.nordicsemi.android:ble:2.7.2")
    implementation("no.nordicsemi.android:ble-ktx:2.7.2")

    // 3D Render Motoru (Google Filament / Sceneview)
    implementation("io.github.sceneview:sceneview:2.2.1")
    // veya saf OpenGL ES 3.0 (android.opengl.GLES30)

    // 2D Manyetik Kesit & Sinyal Grafikleri
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Veri Tabanı (Tarama Oturumlarını Saklama)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Coroutines & Asenkron Veri Akışı
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
}`;

  const pythonScript = `"""
Nicea Dedektör - 3D Yeraltı ve Alan Tarama Sistemi (Python Prototipi)
Özellikler: BLE / Seri Sensör Verisi Okuma, Kriging/Spline Enterpolasyon ve PyVista 3D Render.
"""

import sys
import numpy as np
import scipy.interpolate as interp
import pyvista as pv
from pyvistaqt import BackgroundPlotter
from PySide6.QtWidgets import QApplication, QMainWindow, QVBoxLayout, QWidget, QPushButton, QLabel

def create_geophysical_colormap():
    """Mavi (Boşluk) -> Yeşil (Zemin) -> Kırmızı/Altın (Metal) renk haritası"""
    colors = [
        [0.1, 0.1, 0.8],  # Derin Boşluk (-100)
        [0.0, 0.7, 0.9],  # Hafif Boşluk (-50)
        [0.0, 0.8, 0.3],  # Nötr Zemin (0)
        [0.9, 0.8, 0.1],  # Mineral / Demir (+40)
        [1.0, 0.1, 0.1]   # Değerli Metal / Altın (+100)
    ]
    return pv.LookupTable(cmap=colors)

def interpolate_grid(raw_data, grid_w=10, grid_h=10, upscale=4):
    """
    Saha adımlarını jeofizik kriging/spline yöntemiyle yüksek çözünürlüğe enterpole eder.
    """
    x = np.arange(grid_w)
    y = np.arange(grid_h)
    f = interp.RectBivariateSpline(y, x, raw_data)
    
    x_dense = np.linspace(0, grid_w - 1, grid_w * upscale)
    y_dense = np.linspace(0, grid_h - 1, grid_h * upscale)
    z_dense = f(y_dense, x_dense)
    return x_dense, y_dense, z_dense

def show_3d_subsurface(grid_data):
    """PyVista ile 3D Yeraltı Voksel ve Yüzey Anomalisi Renderı"""
    x_dense, y_dense, z_dense = interpolate_grid(grid_data)
    xx, yy = np.meshgrid(x_dense, y_dense)
    
    # Z yüksekliğini sinyal genliğine göre kabartma
    elevation = z_dense * 0.05
    mesh = pv.StructuredGrid(xx, yy, elevation)
    mesh["Manyetik Gradyan (nT)"] = z_dense.flatten(order="F")
    
    plotter = pv.Plotter(title="Nicea Dedektör - 3D Yeraltı Görüntüleme")
    plotter.set_background("#080c14")
    plotter.add_mesh(mesh, cmap="jet", show_edges=False, smooth_shading=True)
    plotter.add_bounding_box(color="#64748b")
    plotter.show_grid(color="#334155")
    plotter.camera_position = 'iso'
    plotter.show()

if __name__ == "__main__":
    # Örnek 10x10 Sensör Matrisi (Oda boşluğu ve Altın küpü)
    test_grid = np.zeros((10, 10))
    # Boşluk Anomalisı (Negatif dipol)
    test_grid[3:6, 3:6] = -75.0
    # Altın Anomalisı (Pozitif pik)
    test_grid[4, 4] = 95.0
    
    print("Nicea Dedektör 3D Görüntüleme Başlatılıyor...")
    show_3d_subsurface(test_grid)
`;

  const androidKotlinCode = `package com.niceadedektor.geoscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Nicea Dedektör - Android Bluetooth BLE ve 2D/3D Tarama Mimarisi
 */
class ScanViewModel : ViewModel() {
    private val _gridPoints = MutableStateFlow<Array<FloatArray>>(Array(10) { FloatArray(10) })
    val gridPoints: StateFlow<Array<FloatArray>> = _gridPoints

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    fun onSensorPacketReceived(x: Int, y: Int, gradientDelta: Float) {
        viewModelScope.launch {
            val current = _gridPoints.value.map { it.clone() }.toTypedArray()
            current[y][x] = gradientDelta
            _gridPoints.value = current
        }
    }
}

@Composable
fun NiceaScanScreen(viewModel: ScanViewModel) {
    val grid by viewModel.gridPoints.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Nicea Dedektör - Mobil Alan Tarama", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        
        // 2D Gerçek Zamanlı Isı Matrisi
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            val cellW = size.width / 10f
            val cellH = size.height / 10f
            
            for (y in 0 until 10) {
                for (x in 0 until 10) {
                    val value = grid[y][x]
                    val color = when {
                        value < -30f -> Color(0xFF2563EB) // Boşluk (Mavi)
                        value > 50f -> Color(0xFFEAB308)  // Altın / Değerli Metal (Sarı)
                        value > 20f -> Color(0xFFD97706)  // Metalik / Demir (Turuncu)
                        else -> Color(0xFF10B981)         // Doğal Zemin (Yeşil)
                    }
                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(x * cellW, y * cellH),
                        size = androidx.compose.ui.geometry.Size(cellW, cellH)
                    )
                }
            }
        }
    }
}
`;

  return (
    <div className="w-full h-full bg-slate-950 p-4 md:p-6 text-slate-200 overflow-y-auto">
      {/* Header Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-amber-950/40 to-slate-900 border border-amber-500/30 rounded-2xl p-6 shadow-2xl mb-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider bg-amber-500/20 text-amber-300 border border-amber-500/40">
                Nicea Dedektör R&D Mühendislik Dokümantasyonu
              </span>
              <span className="text-xs text-slate-400 font-mono">v4.2 PRO</span>
            </div>
            <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
              Python & Android Tabanlı 3D/2D Alan Tarama Geliştirme Çalışma Planı
            </h1>
            <p className="text-slate-400 text-sm mt-1 max-w-3xl">
              Nicea Dedektör donanımları için gerçek zamanlı sensör veri akışı, jeofizik sinyal filtreleme (Kriging & Spline), 
              masaüstü Python (PyVista/PySide6) ve mobil Android (Kotlin/Jetpack Compose/OpenGL ES) geliştirme yol haritası.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => {
                const blob = new Blob([pythonScript], { type: 'text/plain' });
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'nicea_ground_scanner.py';
                a.click();
              }}
              className="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-xs flex items-center gap-2 shadow-lg shadow-amber-500/20 transition-all"
            >
              <Download className="w-4 h-4" />
              Python Şablonunu İndir (.py)
            </button>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex flex-wrap gap-2 mt-6 pt-4 border-t border-slate-800">
          {[
            { id: 'roadmap', label: '1. Aşama Aşama Çalışma Planı (Gantt)', icon: Calendar },
            { id: 'python', label: '2. Python Temel Kütüphaneleri & Mimari', icon: Terminal },
            { id: 'android', label: '3. Android Temel Kütüphaneleri & Mimari', icon: Smartphone },
            { id: 'protocol', label: '4. Sensör & Donanım Protokolü', icon: Cpu },
            { id: 'code_python', label: '5. Python 3D Kod Şablonu', icon: FileCode2 },
            { id: 'code_android', label: '6. Android Kotlin Kod Şablonu', icon: Code2 },
          ].map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id as typeof activeTab)}
                className={`px-3.5 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 border transition-all ${
                  activeTab === tab.id
                    ? 'bg-amber-500 text-slate-950 border-amber-400 shadow-md font-bold'
                    : 'bg-slate-900/80 text-slate-400 border-slate-800 hover:text-white hover:bg-slate-850'
                }`}
              >
                <Icon className="w-4 h-4" />
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* TAB CONTENT 1: ROADMAP */}
      {activeTab === 'roadmap' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {/* Phase 1 */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg relative overflow-hidden">
              <div className="absolute top-0 left-0 right-0 h-1 bg-blue-500" />
              <div className="flex justify-between items-start mb-3">
                <span className="text-xs font-bold font-mono text-blue-400 bg-blue-950 px-2.5 py-0.5 rounded-full border border-blue-800">
                  FAZ 1 (Hafta 1 - 2)
                </span>
                <Cpu className="w-5 h-5 text-blue-400" />
              </div>
              <h3 className="font-bold text-white text-base mb-2">Sensör & Firmware Entegrasyonu</h3>
              <p className="text-xs text-slate-400 mb-4 leading-relaxed">
                Manyetometre/Gradiometre sensörlerinin (FGM-3, Bartington veya PNI RM3100) ESP32/STM32 mikrokontrolcüye bağlanması.
              </p>
              <ul className="text-xs space-y-1.5 text-slate-300">
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> 24-bit ADC okuma (ADS1256 / STM32 SPI)</li>
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> İki sensör farkı gradyan hesaplama (ΔB = S1 - S2)</li>
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> BLE GATT bildirimi (Notify Characteristic)</li>
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> IMU (MPU6050) eğim kompanzasyonu</li>
              </ul>
            </div>

            {/* Phase 2 */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg relative overflow-hidden">
              <div className="absolute top-0 left-0 right-0 h-1 bg-amber-500" />
              <div className="flex justify-between items-start mb-3">
                <span className="text-xs font-bold font-mono text-amber-400 bg-amber-950 px-2.5 py-0.5 rounded-full border border-amber-800">
                  FAZ 2 (Hafta 3 - 4)
                </span>
                <Terminal className="w-5 h-5 text-amber-400" />
              </div>
              <h3 className="font-bold text-white text-base mb-2">Python Masaüstü Prototipi</h3>
              <p className="text-xs text-slate-400 mb-4 leading-relaxed">
                Bilgisayarda Bleak ile canlı BLE verisi toplama, NumPy/SciPy ile gürültü filtreleme ve PyVista ile 3D görselleştirme.
              </p>
              <ul className="text-xs space-y-1.5 text-slate-300">
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> Bleak asenkron BLE dinleyici</li>
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> Kriging / Bivariate Spline enterpolasyonu</li>
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> PyVista VTK interaktif 3D yüzey & voksel</li>
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> PySide6 Dark-Theme modern kontrol paneli</li>
              </ul>
            </div>

            {/* Phase 3 */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg relative overflow-hidden">
              <div className="absolute top-0 left-0 right-0 h-1 bg-emerald-500" />
              <div className="flex justify-between items-start mb-3">
                <span className="text-xs font-bold font-mono text-emerald-400 bg-emerald-950 px-2.5 py-0.5 rounded-full border border-emerald-800">
                  FAZ 3 (Hafta 5 - 6)
                </span>
                <Smartphone className="w-5 h-5 text-emerald-400" />
              </div>
              <h3 className="font-bold text-white text-base mb-2">Android BLE & 2D Saha Arayüzü</h3>
              <p className="text-xs text-slate-400 mb-4 leading-relaxed">
                Kotlin ve Jetpack Compose ile mobil arayüz, Nordic BLE kütüphanesi ile kesintisiz prob eşleşmesi ve 2D ısı haritası.
              </p>
              <ul className="text-xs space-y-1.5 text-slate-300">
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> Nordic Ble-Library Bluetooth entegrasyonu</li>
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> Zigzag ve Paralel adım tarama motoru</li>
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> Jetpack Compose Canvas ile 2D ısı haritası</li>
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> Zemin balansı kalibrasyon butonları</li>
              </ul>
            </div>

            {/* Phase 4 */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg relative overflow-hidden">
              <div className="absolute top-0 left-0 right-0 h-1 bg-purple-500" />
              <div className="flex justify-between items-start mb-3">
                <span className="text-xs font-bold font-mono text-purple-400 bg-purple-950 px-2.5 py-0.5 rounded-full border border-purple-800">
                  FAZ 4 (Hafta 7 - 8)
                </span>
                <Layers className="w-5 h-5 text-purple-400" />
              </div>
              <h3 className="font-bold text-white text-base mb-2">Android OpenGL / Filament 3D Motoru</h3>
              <p className="text-xs text-slate-400 mb-4 leading-relaxed">
                Mobil cihaz GPU'su üzerinde donanım hızlandırmalı 3D yeraltı derinlik kesitleri ve voksel blokları renderı.
              </p>
              <ul className="text-xs space-y-1.5 text-slate-300">
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> OpenGL ES 3.0 veya Google Filament</li>
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> Derinlik kesit düzlemi (Z-Cut Slicing)</li>
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> Dokunmatik orbit, zoom ve pan kontrolleri</li>
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> Boşluk ve metal anomali pinleme</li>
              </ul>
            </div>

            {/* Phase 5 */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg relative overflow-hidden">
              <div className="absolute top-0 left-0 right-0 h-1 bg-rose-500" />
              <div className="flex justify-between items-start mb-3">
                <span className="text-xs font-bold font-mono text-rose-400 bg-rose-950 px-2.5 py-0.5 rounded-full border border-rose-800">
                  FAZ 5 (Hafta 9 - 10)
                </span>
                <ShieldCheck className="w-5 h-5 text-rose-400" />
              </div>
              <h3 className="font-bold text-white text-base mb-2">Saha Testleri & Derinlik Kalibrasyonu</h3>
              <p className="text-xs text-slate-400 mb-4 leading-relaxed">
                Farklı toprak tiplerinde (killi, kireçli, mineralli) bilinen hedeflerle derinlik hesabı doğrulaması ve son sürüm.
              </p>
              <ul className="text-xs space-y-1.5 text-slate-300">
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> Manyetik dipol yarı-genişlik derinlik formülü</li>
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> Room DB ile tarama kayıtlarını yerel saklama</li>
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> PDF ve CSV formatında saha raporu çıktısı</li>
                <li className="flex items-center gap-2"><Check className="w-3.5 h-3.5 text-emerald-400" /> Nicea Dedektör kurumsal logosu ve lisanslama</li>
              </ul>
            </div>
          </div>
        </div>
      )}

      {/* TAB CONTENT 2: PYTHON LIBRARIES */}
      {activeTab === 'python' && (
        <div className="space-y-6">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-xl">
            <h2 className="text-lg font-bold text-white mb-2 flex items-center gap-2">
              <Terminal className="w-5 h-5 text-amber-400" />
              Python Ekosistemi: Gerekli Temel Kütüphaneler & Rolleri
            </h2>
            <p className="text-xs text-slate-400 mb-6">
              Jeofizik alan tarama, manyetometre ve yeraltı görüntüleme yazılımlarında en yüksek performans ve hassasiyet için kullanılan temel kütüphaneler:
            </p>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {[
                {
                  name: 'PyVista (pyvista & pyvistaqt)',
                  cat: '3D Görselleştirme & Hacimsel Render',
                  desc: 'VTK (Visualization Toolkit) üzerine inşa edilmiş en güçlü 3D jeofizik görselleştirici. Izgara verilerini saniyeler içinde 3D yüzeye, voksel bloklarına, izo-yüzeylere (isosurface) ve derinlik kesitlerine dönüştürür.',
                  cmd: 'pip install pyvista pyvistaqt',
                  link: 'https://docs.pyvista.org'
                },
                {
                  name: 'SciPy (scipy.interpolate & signal)',
                  cat: 'Sinyal İşleme & Enterpolasyon',
                  desc: 'Adım adım toplanan kaba ızgarayı (örn. 10x10) pürüzsüz yüksek çözünürlüklü 100x100 ısı matrisine dönüştüren 2D Bivariate Spline, Kriging ve RBF (Radial Basis Function) enterpolasyon algoritmalarını sunar. Ayrıca Kalman ve Savitzky-Golay filtreleri içerir.',
                  cmd: 'pip install scipy numpy',
                  link: 'https://scipy.org'
                },
                {
                  name: 'PySide6 (Qt6 for Python)',
                  cat: 'Masaüstü Kullanıcı Arayüzü (GUI)',
                  desc: 'Nicea Dedektör için profesyonel koyu temalı (dark mode), dokunmatik uyumlu masaüstü arayüzü geliştirmek için resmi Qt6 frameworkü. PyVista 3D canvasını doğrudan QWidget içerisine gömebilir.',
                  cmd: 'pip install PySide6',
                  link: 'https://doc.qt.io/qtforpython-6'
                },
                {
                  name: 'Bleak (Bluetooth Low Energy)',
                  cat: 'Kablosuz Sensör İletişimi',
                  desc: 'Windows, Linux ve macOS üzerinde çalışan çapraz platform asenkron (asyncio) BLE kütüphanesi. Nicea probundan yayınlanan GATT sensör paketlerini sıfır gecikmeyle arka planda dinler.',
                  cmd: 'pip install bleak',
                  link: 'https://bleak.readthedocs.io'
                },
                {
                  name: 'NumPy',
                  cat: 'Matris Hesaplamaları & Dipol Analizi',
                  desc: 'Tüm jeofizik matris işlemlerini C hızında çalıştırır. Zemin balansı çıkarma, gradyan farkı hesabı ve manyetik dipol ters çözümleme formüllerini yürütür.',
                  cmd: 'pip install numpy',
                  link: 'https://numpy.org'
                },
                {
                  name: 'PySerial',
                  cat: 'Kablolu / USB Seri İletişim',
                  desc: 'USB veya FTDI kablosu ile bağlanan problar veya RS485/UART portları üzerinden ham sensör baytlarını okumak için endüstri standardı.',
                  cmd: 'pip install pyserial',
                  link: 'https://pyserial.readthedocs.io'
                }
              ].map((lib, i) => (
                <div key={i} className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                  <div className="flex justify-between items-start mb-1">
                    <span className="font-bold text-amber-400 text-sm">{lib.name}</span>
                    <span className="text-[10px] font-mono text-cyan-400 bg-cyan-950/80 px-2 py-0.5 rounded border border-cyan-800/60">
                      {lib.cat}
                    </span>
                  </div>
                  <p className="text-xs text-slate-300 my-2 leading-relaxed">{lib.desc}</p>
                  <div className="bg-slate-900 p-2 rounded-lg border border-slate-800 font-mono text-xs text-slate-400 flex justify-between items-center">
                    <span>{lib.cmd}</span>
                    <button
                      onClick={() => copyToClipboard(lib.cmd, `pip-${i}`)}
                      className="text-slate-400 hover:text-white"
                      title="Kopyala"
                    >
                      {copiedId === `pip-${i}` ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                </div>
              ))}
            </div>

            {/* Quick requirements file block */}
            <div className="mt-6 pt-4 border-t border-slate-800">
              <div className="flex justify-between items-center mb-2">
                <span className="text-xs font-bold text-white">Hazır requirements.txt Dosyası:</span>
                <button
                  onClick={() => copyToClipboard(pythonRequirements, 'req-txt')}
                  className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs flex items-center gap-1.5 border border-slate-700"
                >
                  {copiedId === 'req-txt' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  Metni Kopyala
                </button>
              </div>
              <pre className="bg-slate-950 p-3 rounded-xl border border-slate-800 text-xs font-mono text-emerald-400 overflow-x-auto">
                {pythonRequirements}
              </pre>
            </div>
          </div>
        </div>
      )}

      {/* TAB CONTENT 3: ANDROID LIBRARIES */}
      {activeTab === 'android' && (
        <div className="space-y-6">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-xl">
            <h2 className="text-lg font-bold text-white mb-2 flex items-center gap-2">
              <Smartphone className="w-5 h-5 text-emerald-400" />
              Android Mobil Ekosistemi: Gerekli Temel Kütüphaneler & Mimari
            </h2>
            <p className="text-xs text-slate-400 mb-6">
              Arazide tablet ve telefonlarda çalışan modern, stabil ve yüksek FPS sunan Kotlin tabanlı yeraltı tarama mimarisi:
            </p>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {[
                {
                  name: 'Nordic Semiconductor Android-BLE-Library',
                  dep: 'no.nordicsemi.android:ble-ktx:2.7.2',
                  cat: 'Kopmayan BLE Bağlantısı',
                  desc: 'Android’in yerel BluetoothGatt API’sındaki bilinen bağlantı kopma ve kuyruk tıkanma sorunlarını tamamen çözen endüstri standardı BLE kütüphanesi. Otomatik yeniden bağlanma (Auto-reconnect) ve yüksek MTU (512 byte) desteği sunar.'
                },
                {
                  name: 'Google Filament / Sceneview / OpenGL ES 3.0',
                  dep: 'io.github.sceneview:sceneview:2.2.1',
                  cat: 'Mobil 3D Yeraltı Render',
                  desc: 'Android cihazlarda 60 FPS donanım hızlandırmalı 3D yüzey ve voksel blokları renderı. Sceneview, Filament motorunu Jetpack Compose içine tek bir @Composable bileşen olarak entegre etmeyi sağlar.'
                },
                {
                  name: 'Jetpack Compose & Compose Canvas',
                  dep: 'androidx.compose.ui:ui',
                  cat: '2D Reaktif Arayüz & Isı Matrisi',
                  desc: 'Deklaratif modern Android UI mimarisi. Compose Canvas ile dokunmatik 2D jeofizik ısı haritası, adım kılavuz çizgileri ve anomali retikülleri 120Hz yenileme hızında pürüzsüzce çizilir.'
                },
                {
                  name: 'MPAndroidChart',
                  dep: 'com.github.PhilJay:MPAndroidChart:v3.1.0',
                  cat: 'Derinlik & Manyetik Kesit Grafikleri',
                  desc: 'Saha taraması sırasında X veya Y ekseni boyunca alınan manyetik sinyal eğrilerini (sinyal tepe/çukur profilleri) ve derinlik eğrilerini çizmek için en popüler grafik motoru.'
                },
                {
                  name: 'Android Room Persistence DB',
                  dep: 'androidx.room:room-runtime:2.6.1',
                  cat: 'Tarama Oturumları & SQLite Depolama',
                  desc: 'Kullanıcının sahada yaptığı taramaları, GPS koordinatlarını, müşteri/parsel notlarını ve ham ızgara matrislerini yerel SQLite veritabanında güvenle saklar ve çevrimdışı çalışır.'
                },
                {
                  name: 'Kotlinx Coroutines & StateFlow',
                  dep: 'org.jetbrains.kotlinx:kotlinx-coroutines-android',
                  cat: 'Asenkron Sensör Veri Akışı',
                  desc: 'Saniyede 20-50 paket hızında gelen sensör verilerini arka plan thread’lerinde filtreleyip ana arayüzü (UI) kesinlikle dondurmadan ekrana aktaran asenkron veri boru hattı.'
                }
              ].map((lib, i) => (
                <div key={i} className="bg-slate-950 p-4 rounded-xl border border-slate-800">
                  <div className="flex justify-between items-start mb-1">
                    <span className="font-bold text-emerald-400 text-sm">{lib.name}</span>
                    <span className="text-[10px] font-mono text-amber-400 bg-amber-950/80 px-2 py-0.5 rounded border border-amber-800/60">
                      {lib.cat}
                    </span>
                  </div>
                  <p className="text-xs text-slate-300 my-2 leading-relaxed">{lib.desc}</p>
                  <div className="bg-slate-900 p-2 rounded-lg border border-slate-800 font-mono text-[11px] text-slate-400 flex justify-between items-center">
                    <span className="truncate pr-2">{lib.dep}</span>
                    <button
                      onClick={() => copyToClipboard(lib.dep, `grad-${i}`)}
                      className="text-slate-400 hover:text-white shrink-0"
                    >
                      {copiedId === `grad-${i}` ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                </div>
              ))}
            </div>

            {/* Gradle Dependencies Block */}
            <div className="mt-6 pt-4 border-t border-slate-800">
              <div className="flex justify-between items-center mb-2">
                <span className="text-xs font-bold text-white">build.gradle.kts Bağımlılıkları:</span>
                <button
                  onClick={() => copyToClipboard(androidGradle, 'gradle-txt')}
                  className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs flex items-center gap-1.5 border border-slate-700"
                >
                  {copiedId === 'gradle-txt' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  Metni Kopyala
                </button>
              </div>
              <pre className="bg-slate-950 p-3 rounded-xl border border-slate-800 text-xs font-mono text-cyan-300 overflow-x-auto">
                {androidGradle}
              </pre>
            </div>
          </div>
        </div>
      )}

      {/* TAB CONTENT 4: PROTOCOL */}
      {activeTab === 'protocol' && (
        <div className="space-y-6">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-xl">
            <h2 className="text-lg font-bold text-white mb-2 flex items-center gap-2">
              <Cpu className="w-5 h-5 text-amber-400" />
              Donanım & Firmware İletişim Protokolü (Nicea Geo-Packet Spec)
            </h2>
            <p className="text-xs text-slate-400 mb-6">
              ESP32 veya STM32 tabanlı Manyetik Gradiometre probundan Python veya Android uygulamasına BLE Notify veya USB Seri üzerinden iletilen 16-Bayt ikili (binary) paket formatı:
            </p>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="bg-slate-950 border-b border-slate-800 text-slate-400 font-mono">
                    <th className="p-3">Bayt Aralığı</th>
                    <th className="p-3">Alan Adı</th>
                    <th className="p-3">Veri Tipi</th>
                    <th className="p-3">Açıklama & Örnek Değer</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800 text-slate-300">
                  <tr>
                    <td className="p-3 font-mono text-amber-400">Byte 0 - 1</td>
                    <td className="p-3 font-semibold text-white">SYNC HEADER</td>
                    <td className="p-3 font-mono">uint16</td>
                    <td className="p-3">0xAA 0x55 (Paket başlangıç kontrol baytları)</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-mono text-amber-400">Byte 2 - 3</td>
                    <td className="p-3 font-semibold text-white">STEP X / Y</td>
                    <td className="p-3 font-mono">uint8[2]</td>
                    <td className="p-3">Adım koordinatları (X: 0..width-1, Y: 0..height-1)</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-mono text-amber-400">Byte 4 - 7</td>
                    <td className="p-3 font-semibold text-white">SENSOR 1 (Alt Prob)</td>
                    <td className="p-3 font-mono">int32 (LE)</td>
                    <td className="p-3">Alt sensör manyetik akısı (nT cinsinden, örn: 48210)</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-mono text-amber-400">Byte 8 - 11</td>
                    <td className="p-3 font-semibold text-white">SENSOR 2 (Üst Prob)</td>
                    <td className="p-3 font-mono">int32 (LE)</td>
                    <td className="p-3">Üst sensör manyetik akısı (nT cinsinden, örn: 48180)</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-mono text-amber-400">Byte 12</td>
                    <td className="p-3 font-semibold text-white">IMU PITCH</td>
                    <td className="p-3 font-mono">int8</td>
                    <td className="p-3">Prob ileri/geri eğim açısı (-90° ile +90°)</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-mono text-amber-400">Byte 13</td>
                    <td className="p-3 font-semibold text-white">IMU ROLL</td>
                    <td className="p-3 font-mono">int8</td>
                    <td className="p-3">Prob sağa/sola yatma açısı (-90° ile +90°)</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-mono text-amber-400">Byte 14</td>
                    <td className="p-3 font-semibold text-white">BATTERY & STATUS</td>
                    <td className="p-3 font-mono">uint8</td>
                    <td className="p-3">Batarya yüzdesi (0 - 100%) ve kalibrasyon bayrağı</td>
                  </tr>
                  <tr>
                    <td className="p-3 font-mono text-amber-400">Byte 15</td>
                    <td className="p-3 font-semibold text-white">CHECKSUM / CRC8</td>
                    <td className="p-3 font-mono">uint8</td>
                    <td className="p-3">İlk 15 baytın XOR veya CRC8 toplamı (Hata koruması)</td>
                  </tr>
                </tbody>
              </table>
            </div>

            {/* Magnetic Dipole Math Box */}
            <div className="mt-6 bg-slate-950 p-4 rounded-xl border border-slate-800">
              <h3 className="text-sm font-bold text-amber-400 mb-2">Jeofizik Derinlik Hesabı Algoritması</h3>
              <p className="text-xs text-slate-300 leading-relaxed">
                Manyetik gradiometrede derinlik hesabı, dipolün mesafeyle $1/r^3$ oranında sönümlenmesi prensibine dayanır. 
                Saha uygulamasında en pratik ve güvenilir formül <b>Yarı Genişlik (Full Width at Half Maximum - FWHM)</b> kuralıdır:
              </p>
              <div className="bg-slate-900 p-3 rounded-lg border border-slate-800 font-mono text-xs text-cyan-300 my-2">
                Derinlik (Z) ≈ 0.55 × Anomali_Tepe_Yarı_Genişliği (metre)
              </div>
              <p className="text-xs text-slate-400">
                Örnek: 100 nT'lik bir altın küpü anomalisi zemin seviyesinde 2.6 metre çapında bir alanda tepe yapıyorsa, hedefin merkez derinliği yaklaşık <b>1.43 metredir</b>.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* TAB CONTENT 5: PYTHON CODE TEMPLATE */}
      {activeTab === 'code_python' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <span className="text-xs text-slate-400">
              Bu script bağımsız bir Python dosyasıdır (`nicea_ground_scanner.py`). PyVista ve SciPy ile doğrudan çalıştırılabilir.
            </span>
            <button
              onClick={() => copyToClipboard(pythonScript, 'py-code')}
              className="px-3 py-1.5 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 text-xs font-bold flex items-center gap-1.5 shadow-md"
            >
              {copiedId === 'py-code' ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
              Kodu Kopyala
            </button>
          </div>
          <pre className="bg-slate-900 border border-slate-800 rounded-2xl p-4 text-xs font-mono text-emerald-300 overflow-x-auto max-h-[580px] shadow-2xl leading-relaxed">
            {pythonScript}
          </pre>
        </div>
      )}

      {/* TAB CONTENT 6: ANDROID CODE TEMPLATE */}
      {activeTab === 'code_android' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <span className="text-xs text-slate-400">
              Bu dosya Android Studio projenize ekleyebileceğiniz Jetpack Compose tabanlı mobil arayüz ve StateFlow veri yönetim kodudur.
            </span>
            <button
              onClick={() => copyToClipboard(androidKotlinCode, 'kt-code')}
              className="px-3 py-1.5 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 text-xs font-bold flex items-center gap-1.5 shadow-md"
            >
              {copiedId === 'kt-code' ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
              Kotlin Kodunu Kopyala
            </button>
          </div>
          <pre className="bg-slate-900 border border-slate-800 rounded-2xl p-4 text-xs font-mono text-cyan-300 overflow-x-auto max-h-[580px] shadow-2xl leading-relaxed">
            {androidKotlinCode}
          </pre>
        </div>
      )}
    </div>
  );
};
