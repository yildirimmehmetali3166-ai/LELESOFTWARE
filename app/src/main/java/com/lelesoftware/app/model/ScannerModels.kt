package com.lelesoftware.app.model

enum class PaletteType(val label: String) {
    LELE_SPECTRUM("LELE Spektrum (Jeofizik)"),
    THERMAL_JET("Termal Jet (Klasik)"),
    MAGMA("Magma Radyant"),
    EMERALD_VOID("Zümrüt Boşluk Odaklı"),
    GOLD_RUSH("Altın & Değerli Metal")
}

enum class ViewMode(val label: String) {
    SURFACE("Yüzey Ağı"),
    VOXELS("3D Voksel"),
    WIREFRAME("Tel Çerçeve"),
    ISO_ANOMALIES("İzole Anomaliler")
}

enum class TargetClassification(val label: String, val colorHex: Long) {
    VOID("Boşluk / Mezar Odası", 0xFF3B82F6),
    PRECIOUS("Değerli Metal / Altın", 0xFFEAB308),
    FERROUS("Demir / Manyetik Metal", 0xFFEF4444),
    MINERAL("Mineral / Kayaç Yapısı", 0xFFA855F7),
    NEUTRAL("Homojen Zemin", 0xFF22C55E)
}

enum class CameraPreset(val label: String) {
    ISO("İzometrik"),
    TOP("Kuşbakışı"),
    SIDE("Yan Kesit"),
    FRONT("Ön Kesit")
}

data class ScanPoint(
    val x: Int,
    val y: Int,
    val rawSignal: Int,
    val filteredSignal: Float,
    val depth: Float,
    val classification: TargetClassification,
    val confidence: Int,
    val timestamp: Long
)

data class ScanGrid(
    val id: String,
    val title: String,
    val date: String,
    val width: Int,
    val height: Int,
    val stepDistanceMeters: Float,
    val points: List<List<ScanPoint>>,
    val groundBalance: Int,
    val sensorType: String,
    val locationName: String,
    val notes: String
)

enum class SoilType(
    val id: String,
    val label: String,
    val attenuationFactor: Float,
    val description: String
) {
    STANDARD("std", "Standart Toprak / Alüvyon", 1.0f, "Standart nemli tarım toprağı ve alüvyon tabaka"),
    DRY_SAND("sand", "Kuru Kum / Çakıllı Zemin", 0.88f, "Düşük sönümleme, yüksek penetrasyon derinliği"),
    WET_CLAY("clay", "Islak Kil / Yoğun Zemin", 1.28f, "Yüksek sinyal sönümleme ve iletken killi zemin"),
    ROCKY_MINERAL("rock", "Mineralize / Bazaltik Kayaç", 1.15f, "Manyetik gürültülü ve kireçtaşlı zemin")
}

data class DepthCalculationResult(
    val estimatedDepthMeters: Float,
    val minDepthMeters: Float,
    val maxDepthMeters: Float,
    val peakAmplitudePercent: Float,
    val peakAmplitudeNanoTesla: Float,
    val baseNoisePercent: Float,
    val netDeltaAmplitude: Float,
    val fwhmMeters: Float,
    val soilType: SoilType,
    val confidencePercent: Int,
    val method: String,
    val profileCurve: List<Pair<Float, Float>> = emptyList()
)

data class DetectedAnomaly(
    val id: String,
    val title: String,
    val classification: TargetClassification,
    val xMeters: Float,
    val yMeters: Float,
    val gridX: Int,
    val gridY: Int,
    val depthMeters: Float,
    val volumeM3: Float,
    val signalStrength: Int,
    val description: String,
    val peakAmplitude: Float = 0f,
    val fwhmMeters: Float = 0f,
    val minDepthMeters: Float = depthMeters * 0.85f,
    val maxDepthMeters: Float = depthMeters * 1.15f,
    val depthConfidence: Int = 88,
    val calculationFormula: String = "Peters Half-Slope & Dipol Genlik Sönümleme"
)

data class DeviceStatus(
    val connected: Boolean = true,
    val deviceName: String = "LELE SOFTWARE Geo-Probe Pro V4.0 (Quantum Core)",
    val batteryLevel: Int = 94,
    val signalQuality: Int = 98,
    val currentPitch: Float = 1.2f,
    val currentRoll: Float = -0.4f,
    val currentSensorA: Int = 48120,
    val currentSensorB: Int = 48095,
    val gradientDelta: Int = 25
)
