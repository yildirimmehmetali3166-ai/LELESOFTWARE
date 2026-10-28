export type PaletteType = 'nicea_spectrum' | 'thermal_jet' | 'magma' | 'emerald_void' | 'gold_rush';

export type ViewMode = 'surface' | 'voxels' | 'wireframe' | 'iso_anomalies';

export type TargetClassification = 'void' | 'precious' | 'ferrous' | 'mineral' | 'neutral';

export interface ScanPoint {
  x: number; // grid column (0 to width-1)
  y: number; // grid row (0 to height-1)
  rawSignal: number; // raw sensor reading (-1000 to +1000 or nT)
  filteredSignal: number; // normalized/filtered value (-100 to +100)
  depth: number; // estimated depth in meters (0 to 12m)
  classification: TargetClassification;
  confidence: number; // 0 to 100%
  timestamp: number;
}

export interface ScanGrid {
  id: string;
  title: string;
  date: string;
  width: number; // e.g. 10
  height: number; // e.g. 10
  stepDistanceMeters: number; // e.g. 0.5m
  points: ScanPoint[][];
  groundBalance: number; // baseline ground balance
  sensorType: 'Gradiometer 2-Sensör' | 'Proton Manyetometre' | 'Pulse Induction' | 'Direnç (Resistivity)';
  locationName: string;
  notes: string;
}

export interface DetectedAnomaly {
  id: string;
  title: string;
  classification: TargetClassification;
  xMeters: number;
  yMeters: number;
  gridX: number;
  gridY: number;
  depthMeters: number;
  volumeM3: number;
  signalStrength: number;
  description: string;
}

export interface DeviceStatus {
  connected: boolean;
  deviceName: string;
  batteryLevel: number;
  signalQuality: number; // RSSI or %
  currentPitch: number;
  currentRoll: number;
  currentSensorA: number;
  currentSensorB: number;
  gradientDelta: number;
}
