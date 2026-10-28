import { ScanGrid, ScanPoint, DetectedAnomaly, TargetClassification } from '../types/scanner';

export function classifySignal(value: number): TargetClassification {
  if (value < -40) return 'void'; // Cavity / Room / Tunnel (Strong negative gradient)
  if (value > 65) return 'precious'; // Gold / Bronze / Highly conductive
  if (value > 25) return 'ferrous'; // Iron / Ferrous metal
  if (value > 10 || value < -15) return 'mineral'; // Mineralized rock / soil variation
  return 'neutral'; // Homogeneous soil
}

export function generateAntikMezarOdasi(): ScanGrid {
  const width = 14;
  const height = 14;
  const step = 0.5; // 0.5 meters between steps -> 7m x 7m area
  const points: ScanPoint[][] = [];

  for (let y = 0; y < height; y++) {
    const row: ScanPoint[] = [];
    for (let x = 0; x < width; x++) {
      // Background ground noise
      const noise = (Math.sin(x * 1.5) * Math.cos(y * 1.2) * 4) + ((Math.random() - 0.5) * 5);
      let signal = noise;
      let depth = 0;

      // Void: Room between x: 4..9 and y: 4..9 (2.5m x 2.5m room)
      const dxRoom = x - 6.5;
      const dyRoom = y - 6.5;
      const distRoom = Math.sqrt(dxRoom * dxRoom + dyRoom * dyRoom);

      if (distRoom < 3.2) {
        // Strong negative anomaly (cavity)
        const roomStrength = (1 - distRoom / 3.2);
        signal = -80 * roomStrength + noise;
        depth = 2.8 + roomStrength * 0.8; // Chamber depth 2.8m - 3.6m
      }

      // Metal Target: Bronze/Gold anomaly inside the room at x: 7, y: 6
      const dxMetal = x - 7;
      const dyMetal = y - 6;
      const distMetal = Math.sqrt(dxMetal * dxMetal + dyMetal * dyMetal);
      if (distMetal < 1.6) {
        const metalStrength = Math.exp(-distMetal * 2.2);
        signal = signal + (metalStrength * 165); // Overrides cavity to strong positive peak
        if (metalStrength > 0.4) {
          depth = 2.95;
        }
      }

      // Small secondary ferrous iron nail/hinge at x: 4, y: 5
      const dxIron = x - 4;
      const dyIron = y - 5;
      const distIron = Math.sqrt(dxIron * dxIron + dyIron * dyIron);
      if (distIron < 1.0) {
        signal += Math.exp(-distIron * 3) * 60;
      }

      // Clamp between -100 and +100
      const filtered = Math.max(-100, Math.min(100, signal));
      const raw = Math.round(filtered * 12.5 + 48200); // Earth magnetic field ~48,200 nT

      row.push({
        x,
        y,
        rawSignal: raw,
        filteredSignal: filtered,
        depth: depth > 0 ? depth : (Math.abs(filtered) > 15 ? 1.5 : 0),
        classification: classifySignal(filtered),
        confidence: Math.min(98, Math.round(50 + Math.abs(filtered) * 0.48)),
        timestamp: Date.now() - (width * height - (y * width + x)) * 1000
      });
    }
    points.push(row);
  }

  return {
    id: 'scan-lahit-01',
    title: 'İznik Nekropolü - Lahit & Mezar Odası',
    date: '2026-10-24 14:35',
    width,
    height,
    stepDistanceMeters: step,
    points,
    groundBalance: 48200,
    sensorType: 'Gradiometer 2-Sensör',
    locationName: 'İznik (Nicea) Doğu Nekropol Sektörü',
    notes: '2.8m derinlikte 2.5m x 2.5m mezar odası boşluğu ve oda içerisinde yüksek iletkenlikli metalik anomali.'
  };
}

export function generateAltinKupu(): ScanGrid {
  const width = 12;
  const height = 12;
  const step = 0.5;
  const points: ScanPoint[][] = [];

  for (let y = 0; y < height; y++) {
    const row: ScanPoint[] = [];
    for (let x = 0; x < width; x++) {
      const noise = ((Math.random() - 0.5) * 6) + Math.sin(x * 0.8) * 3;
      let signal = noise;

      // High conductivity compact dipole anomaly at (6, 5)
      const dx = x - 6;
      const dy = y - 5;
      const dist = Math.sqrt(dx * dx + dy * dy);

      if (dist < 3.0) {
        // Classic magnetic dipole: positive core + weak negative fringe
        const peak = 94 * Math.exp(-dist * 1.5);
        const shadow = (dy > 0.8) ? -18 * Math.exp(-Math.pow(dist - 1.8, 2)) : 0;
        signal = peak + shadow + noise;
      }

      const filtered = Math.max(-100, Math.min(100, signal));
      const raw = Math.round(filtered * 11.8 + 48150);

      row.push({
        x,
        y,
        rawSignal: raw,
        filteredSignal: filtered,
        depth: dist < 1.8 ? 1.45 : 0,
        classification: classifySignal(filtered),
        confidence: dist < 1.8 ? 96 : 55,
        timestamp: Date.now()
      });
    }
    points.push(row);
  }

  return {
    id: 'scan-gold-02',
    title: 'Hedef Alan - Değerli Metal & Sikke Küpü',
    date: '2026-10-26 11:20',
    width,
    height,
    stepDistanceMeters: step,
    points,
    groundBalance: 48150,
    sensorType: 'Gradiometer 2-Sensör',
    locationName: 'Saha Parsel B-4 (Tarihi Yerleşim Sınırı)',
    notes: '1.45m derinlikte noktasal yüksek iletkenlikli altın/bronz hedef anomalisi. Tipik dipol tepkisi.'
  };
}

export function generateTunelVeMagara(): ScanGrid {
  const width = 16;
  const height = 12;
  const step = 0.6;
  const points: ScanPoint[][] = [];

  for (let y = 0; y < height; y++) {
    const row: ScanPoint[] = [];
    for (let x = 0; x < width; x++) {
      const noise = ((Math.random() - 0.5) * 7);
      let signal = noise;

      // Tunnel running diagonally from (2, 2) to (14, 9)
      // Line equation: y - 2 = (7 / 12) * (x - 2) => 7x - 12y + 10 = 0
      const distToLine = Math.abs(7 * x - 12 * y + 10) / Math.sqrt(49 + 144);
      if (distToLine < 1.4 && x >= 2 && x <= 14) {
        const tunnelFactor = (1 - distToLine / 1.4);
        signal = -75 * tunnelFactor + noise;
      }

      // Chamber at (13, 9)
      const distChamber = Math.sqrt(Math.pow(x - 13, 2) + Math.pow(y - 9, 2));
      if (distChamber < 2.5) {
        signal = Math.min(signal, -88 * (1 - distChamber / 2.5) + noise);
      }

      const filtered = Math.max(-100, Math.min(100, signal));
      const raw = Math.round(filtered * 10 + 48300);

      row.push({
        x,
        y,
        rawSignal: raw,
        filteredSignal: filtered,
        depth: distToLine < 1.4 ? 3.4 : (distChamber < 2.5 ? 4.2 : 0),
        classification: classifySignal(filtered),
        confidence: (distToLine < 1.4 || distChamber < 2.5) ? 92 : 60,
        timestamp: Date.now()
      });
    }
    points.push(row);
  }

  return {
    id: 'scan-tunel-03',
    title: 'Mahzen & Yeraltı Tüneli Hattı',
    date: '2026-10-27 16:50',
    width,
    height,
    stepDistanceMeters: step,
    points,
    groundBalance: 48300,
    sensorType: 'Gradiometer 2-Sensör',
    locationName: 'Eski Kale Etekleri - Galeri Girişi',
    notes: '3.4m derinlikte 12 metre uzunluğunda galeri/tünel ve tünelin bağlandığı mahzen boşluğu.'
  };
}

export function generateEmptyLiveGrid(width = 10, height = 10, step = 0.5): ScanGrid {
  const points: ScanPoint[][] = [];
  for (let y = 0; y < height; y++) {
    const row: ScanPoint[] = [];
    for (let x = 0; x < width; x++) {
      row.push({
        x,
        y,
        rawSignal: 48000,
        filteredSignal: 0,
        depth: 0,
        classification: 'neutral',
        confidence: 0,
        timestamp: 0
      });
    }
    points.push(row);
  }

  return {
    id: `scan-live-${Date.now()}`,
    title: 'Canlı Saha Taraması (Yeni Oturum)',
    date: new Date().toISOString().replace('T', ' ').slice(0, 16),
    width,
    height,
    stepDistanceMeters: step,
    points,
    groundBalance: 48000,
    sensorType: 'Gradiometer 2-Sensör',
    locationName: 'Canlı Tarama Sahası',
    notes: 'Sensör probu ile adım adım veri toplama modu.'
  };
}

// Anomaly extraction algorithm
export function detectAnomalies(grid: ScanGrid): DetectedAnomaly[] {
  const anomalies: DetectedAnomaly[] = [];
  const visited = new Set<string>();
  const { width, height, stepDistanceMeters } = grid;

  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const pt = grid.points[y][x];
      const val = pt.filteredSignal;
      const key = `${x},${y}`;

      if (visited.has(key)) continue;

      // Check if local extremum
      if (Math.abs(val) > 40) {
        let isMax = true;
        let isMin = true;

        for (let dy = -1; dy <= 1; dy++) {
          for (let dx = -1; dx <= 1; dx++) {
            if (dx === 0 && dy === 0) continue;
            const nx = x + dx;
            const ny = y + dy;
            if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
              const nVal = grid.points[ny][nx].filteredSignal;
              if (nVal >= val) isMax = false;
              if (nVal <= val) isMin = false;
            }
          }
        }

        if ((val > 40 && isMax) || (val < -40 && isMin)) {
          // Found anomaly core. Mark neighbors
          for (let dy = -2; dy <= 2; dy++) {
            for (let dx = -2; dx <= 2; dx++) {
              visited.add(`${x + dx},${y + dy}`);
            }
          }

          const isCavity = val < 0;
          // Calculate half-width for depth estimation: d ~ FWHM * 0.55
          let halfWidthCells = 1.5;
          const depth = pt.depth > 0 ? pt.depth : (isCavity ? 2.5 + Math.random() * 0.8 : 1.2 + Math.random() * 0.6);
          const volume = isCavity
            ? Number((Math.PI * Math.pow(halfWidthCells * stepDistanceMeters, 2) * 1.8).toFixed(2))
            : Number((0.08 + Math.abs(val) * 0.003).toFixed(3));

          anomalies.push({
            id: `anom-${x}-${y}`,
            title: isCavity ? 'Yeraltı Boşluğu / Oda Anomalisı' : (val > 65 ? 'Değerli Metal / Yüksek İletkenlik' : 'Metalik Kütle'),
            classification: pt.classification,
            xMeters: Number((x * stepDistanceMeters).toFixed(1)),
            yMeters: Number((y * stepDistanceMeters).toFixed(1)),
            gridX: x,
            gridY: y,
            depthMeters: Number(depth.toFixed(2)),
            volumeM3: volume,
            signalStrength: Math.round(Math.abs(val)),
            description: isCavity
              ? `${depth.toFixed(2)}m derinlikte negatif manyetik gradyan boşluk alanı (~${volume} m³).`
              : `${depth.toFixed(2)}m derinlikte ${val > 65 ? 'Altın/Bronz/Gümüş' : 'Demir/Ferrous'} sınıfı yüksek dipol yanıtı.`
          });
        }
      }
    }
  }

  return anomalies;
}
