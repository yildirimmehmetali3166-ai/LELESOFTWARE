import React, { useState, useEffect, useRef } from 'react';
import { ScanGrid, ScanPoint, DeviceStatus } from '../types/scanner';
import { classifySignal } from '../utils/scanGenerators';
import { detectorAudio } from '../utils/audioSynth';
import { Play, Pause, RotateCcw, Bluetooth, Zap, Gauge, Compass, Footprints, CheckCircle2 } from 'lucide-react';

interface LiveScannerProps {
  onScanCompleted: (grid: ScanGrid) => void;
  currentGrid: ScanGrid;
}

export const LiveScanner: React.FC<LiveScannerProps> = ({ onScanCompleted, currentGrid }) => {
  const [gridSize, setGridSize] = useState<{ width: number; height: number; step: number }>({
    width: 10,
    height: 10,
    step: 0.5,
  });
  const [walkPattern, setWalkPattern] = useState<'zigzag' | 'parallel'>('zigzag');
  const [isScanning, setIsScanning] = useState<boolean>(false);
  const [currentStep, setCurrentStep] = useState<{ x: number; y: number }>({ x: 0, y: 0 });
  const [points, setPoints] = useState<ScanPoint[][]>([]);
  const [groundBalanceVal, setGroundBalanceVal] = useState<number>(48100);
  const [isCalibrating, setIsCalibrating] = useState<boolean>(false);
  const [walkSpeedMs, setWalkSpeedMs] = useState<number>(450);

  // Simulated Hardware Probe State
  const [deviceStatus, setDeviceStatus] = useState<DeviceStatus>({
    connected: true,
    deviceName: 'Nicea Geo-Sensor Pro BLE (V3.2)',
    batteryLevel: 94,
    signalQuality: 98,
    currentPitch: 1.2,
    currentRoll: -0.4,
    currentSensorA: 48120,
    currentSensorB: 48095,
    gradientDelta: 25,
  });

  // Hidden underground target generator for the simulation
  const targetMapRef = useRef<{ [key: string]: number }>({});

  // Initialize live session grid
  useEffect(() => {
    resetSession();
  }, [gridSize]);

  const resetSession = () => {
    setIsScanning(false);
    setCurrentStep({ x: 0, y: 0 });

    // Seed hidden anomaly targets to uncover during walking
    const targets: { [key: string]: number } = {};
    // Center target (Metal hoard at (6, 6))
    const cx = Math.floor(gridSize.width * 0.6);
    const cy = Math.floor(gridSize.height * 0.6);
    // Void chamber at (3, 4)
    const vx = Math.floor(gridSize.width * 0.3);
    const vy = Math.floor(gridSize.height * 0.4);

    for (let y = 0; y < gridSize.height; y++) {
      for (let x = 0; x < gridSize.width; x++) {
        const dMetal = Math.sqrt(Math.pow(x - cx, 2) + Math.pow(y - cy, 2));
        const dVoid = Math.sqrt(Math.pow(x - vx, 2) + Math.pow(y - vy, 2));

        let s = (Math.random() - 0.5) * 6; // noise
        if (dMetal < 2.0) {
          s += 90 * Math.exp(-dMetal * 1.6);
        } else if (dVoid < 2.5) {
          s -= 78 * Math.exp(-dVoid * 1.2);
        }
        targets[`${x},${y}`] = Math.max(-100, Math.min(100, s));
      }
    }
    targetMapRef.current = targets;

    const initialPoints: ScanPoint[][] = [];
    for (let y = 0; y < gridSize.height; y++) {
      const row: ScanPoint[] = [];
      for (let x = 0; x < gridSize.width; x++) {
        row.push({
          x,
          y,
          rawSignal: 48000,
          filteredSignal: 0,
          depth: 0,
          classification: 'neutral',
          confidence: 0,
          timestamp: 0,
        });
      }
      initialPoints.push(row);
    }
    setPoints(initialPoints);
  };

  // Perform single step acquisition
  const takeStep = () => {
    const { x, y } = currentStep;
    if (y >= gridSize.height) return;

    // Read simulated sensor value
    const baseSignal = targetMapRef.current[`${x},${y}`] ?? ((Math.random() - 0.5) * 5);
    const rawVal = groundBalanceVal + Math.round(baseSignal * 12);
    const depthVal = baseSignal < -40 ? 2.8 : baseSignal > 50 ? 1.4 : 0;

    detectorAudio.playSignalTone(baseSignal);

    // Update Probe Live Telemetry
    setDeviceStatus((prev) => ({
      ...prev,
      currentSensorA: rawVal,
      currentSensorB: groundBalanceVal,
      gradientDelta: Math.round(baseSignal * 12),
      currentPitch: Number(((Math.random() - 0.5) * 2).toFixed(1)),
      currentRoll: Number(((Math.random() - 0.5) * 2).toFixed(1)),
    }));

    // Update Points Grid
    setPoints((prev) => {
      const next = prev.map((row) => [...row]);
      next[y][x] = {
        x,
        y,
        rawSignal: rawVal,
        filteredSignal: baseSignal,
        depth: depthVal,
        classification: classifySignal(baseSignal),
        confidence: Math.round(60 + Math.abs(baseSignal) * 0.38),
        timestamp: Date.now(),
      };
      return next;
    });

    // Advance to next step based on walk pattern
    let nextX = x;
    let nextY = y;

    if (walkPattern === 'zigzag') {
      const isEvenRow = y % 2 === 0;
      if (isEvenRow) {
        if (x < gridSize.width - 1) {
          nextX = x + 1;
        } else {
          nextY = y + 1;
        }
      } else {
        if (x > 0) {
          nextX = x - 1;
        } else {
          nextY = y + 1;
        }
      }
    } else {
      // Parallel
      if (x < gridSize.width - 1) {
        nextX = x + 1;
      } else {
        nextX = 0;
        nextY = y + 1;
      }
    }

    if (nextY >= gridSize.height) {
      // Finished scan
      setIsScanning(false);
      setCurrentStep({ x: gridSize.width - 1, y: gridSize.height - 1 });
      // Finalize and deliver to parent
      finishScan(points);
    } else {
      setCurrentStep({ x: nextX, y: nextY });
    }
  };

  // Auto-walk step timer loop
  useEffect(() => {
    if (!isScanning) return;
    const interval = setInterval(() => {
      takeStep();
    }, walkSpeedMs);
    return () => clearInterval(interval);
  }, [isScanning, currentStep, walkPattern, walkSpeedMs, points]);

  const finishScan = (scannedPoints: ScanPoint[][]) => {
    const finishedGrid: ScanGrid = {
      id: `scan-live-${Date.now()}`,
      title: `Nicea Canlı Tarama - ${new Date().toLocaleDateString('tr-TR')} ${new Date().toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })}`,
      date: new Date().toISOString().replace('T', ' ').slice(0, 16),
      width: gridSize.width,
      height: gridSize.height,
      stepDistanceMeters: gridSize.step,
      points: scannedPoints,
      groundBalance: groundBalanceVal,
      sensorType: 'Gradiometer 2-Sensör',
      locationName: 'Saha Canlı Ölçüm Alanı',
      notes: `${gridSize.width}x${gridSize.height} grid, ${walkPattern} adım deseniyle gerçek zamanlı tamamlandı.`,
    };
    onScanCompleted(finishedGrid);
  };

  const handleCalibrateGroundBalance = () => {
    setIsCalibrating(true);
    setTimeout(() => {
      setGroundBalanceVal(48150 + Math.round((Math.random() - 0.5) * 50));
      setIsCalibrating(false);
    }, 800);
  };

  const totalSteps = gridSize.width * gridSize.height;
  const completedSteps = points.flat().filter((p) => p.timestamp > 0).length;
  const progressPercent = Math.round((completedSteps / totalSteps) * 100);

  return (
    <div className="w-full h-full bg-slate-950 p-4 flex flex-col lg:flex-row gap-4 overflow-y-auto">
      {/* Walking Grid Simulator Area */}
      <div className="flex-1 bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow-xl flex flex-col">
        <div className="flex items-center justify-between pb-3 border-b border-slate-800 mb-3">
          <div className="flex items-center gap-2">
            <Footprints className="w-5 h-5 text-amber-400" />
            <h2 className="font-bold text-white text-base">Canlı Adım & Saha Taraması</h2>
            <span className="text-xs text-slate-400">
              ({completedSteps} / {totalSteps} Adım - %{progressPercent})
            </span>
          </div>

          <div className="flex items-center gap-2">
            {progressPercent === 100 ? (
              <button
                onClick={() => finishScan(points)}
                className="px-3 py-1.5 rounded-lg bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs flex items-center gap-1.5 shadow-lg shadow-emerald-500/20"
              >
                <CheckCircle2 className="w-4 h-4" />
                3D Görüntüle
              </button>
            ) : (
              <button
                onClick={() => setIsScanning(!isScanning)}
                className={`px-4 py-1.5 rounded-lg text-xs font-bold flex items-center gap-1.5 transition-colors shadow-lg ${
                  isScanning
                    ? 'bg-rose-500 hover:bg-rose-400 text-white shadow-rose-500/20'
                    : 'bg-amber-500 hover:bg-amber-400 text-slate-950 shadow-amber-500/20'
                }`}
              >
                {isScanning ? (
                  <>
                    <Pause className="w-4 h-4" /> Duraklat
                  </>
                ) : (
                  <>
                    <Play className="w-4 h-4 fill-current" /> Taramayı Başlat
                  </>
                )}
              </button>
            )}

            <button
              onClick={takeStep}
              disabled={isScanning || progressPercent >= 100}
              className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 disabled:opacity-50 text-slate-200 text-xs font-medium border border-slate-700"
            >
              Manuel Adım (+1)
            </button>

            <button
              onClick={resetSession}
              className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white border border-slate-700"
              title="Sıfırla"
            >
              <RotateCcw className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Progress Bar */}
        <div className="w-full bg-slate-950 rounded-full h-2 mb-4 overflow-hidden border border-slate-800">
          <div
            className="bg-gradient-to-r from-amber-500 to-emerald-400 h-full transition-all duration-200"
            style={{ width: `${progressPercent}%` }}
          />
        </div>

        {/* Grid Canvas Representation */}
        <div className="flex-1 flex items-center justify-center p-2 bg-slate-950/80 rounded-xl border border-slate-800">
          <div
            className="grid gap-1 max-w-[500px] w-full aspect-square"
            style={{
              gridTemplateColumns: `repeat(${gridSize.width}, minmax(0, 1fr))`,
            }}
          >
            {points.map((row, y) =>
              row.map((pt, x) => {
                const isCurrent = currentStep.x === x && currentStep.y === y && isScanning;
                const isRecorded = pt.timestamp > 0;
                const val = pt.filteredSignal;

                let cellBg = 'bg-slate-900 border-slate-800';
                if (isRecorded) {
                  if (val < -30) cellBg = 'bg-blue-600 border-blue-400';
                  else if (val > 50) cellBg = 'bg-amber-500 border-amber-300';
                  else if (val > 20) cellBg = 'bg-yellow-600 border-yellow-400';
                  else cellBg = 'bg-emerald-700/80 border-emerald-500/50';
                }

                return (
                  <div
                    key={`${x}-${y}`}
                    className={`aspect-square rounded border flex items-center justify-center text-[10px] font-mono transition-all duration-150 ${cellBg} ${
                      isCurrent ? 'ring-2 ring-white scale-110 z-10 animate-bounce' : ''
                    }`}
                  >
                    {isCurrent ? (
                      <Footprints className="w-3.5 h-3.5 text-slate-950" />
                    ) : isRecorded ? (
                      Math.round(val)
                    ) : (
                      <span className="text-slate-700">·</span>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Scan Parameters Settings Bar */}
        <div className="mt-4 pt-3 border-t border-slate-800 flex flex-wrap items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-3">
            <span className="text-slate-400 font-semibold">Grid Boyutu:</span>
            {[
              { w: 8, h: 8 },
              { w: 10, h: 10 },
              { w: 12, h: 12 },
              { w: 14, h: 14 },
            ].map((size) => (
              <button
                key={`${size.w}x${size.h}`}
                onClick={() => setGridSize({ width: size.w, height: size.h, step: 0.5 })}
                className={`px-2 py-1 rounded border font-mono transition-colors ${
                  gridSize.width === size.w
                    ? 'bg-amber-500/20 text-amber-300 border-amber-500'
                    : 'bg-slate-800 text-slate-400 border-slate-700'
                }`}
              >
                {size.w}x{size.h}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-3">
            <span className="text-slate-400 font-semibold">Yürüme Deseni:</span>
            <button
              onClick={() => setWalkPattern('zigzag')}
              className={`px-2.5 py-1 rounded border font-medium ${
                walkPattern === 'zigzag'
                  ? 'bg-cyan-500/20 text-cyan-300 border-cyan-500'
                  : 'bg-slate-800 text-slate-400 border-slate-700'
              }`}
            >
              Zigzag (S-Pattern)
            </button>
            <button
              onClick={() => setWalkPattern('parallel')}
              className={`px-2.5 py-1 rounded border font-medium ${
                walkPattern === 'parallel'
                  ? 'bg-cyan-500/20 text-cyan-300 border-cyan-500'
                  : 'bg-slate-800 text-slate-400 border-slate-700'
              }`}
            >
              Paralel (Tek Yönlü)
            </button>
          </div>
        </div>
      </div>

      {/* Hardware Telemetry & Probe Connection Panel */}
      <div className="w-full lg:w-[360px] flex flex-col gap-4">
        {/* Hardware Status Card */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow-xl">
          <div className="flex items-center justify-between pb-3 border-b border-slate-800 mb-3">
            <div className="flex items-center gap-2">
              <Bluetooth className="w-4 h-4 text-blue-400" />
              <h3 className="font-bold text-white text-sm">Nicea Geo-Sensor BLE</h3>
            </div>
            <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-950 text-emerald-400 border border-emerald-800">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" /> Bağlı
            </span>
          </div>

          {/* Sensor Telemetry Gauges */}
          <div className="space-y-3 text-xs">
            <div className="bg-slate-950 p-3 rounded-xl border border-slate-800">
              <div className="flex justify-between text-slate-400 text-[11px] mb-1">
                <span>Manyetik Gradyan (ΔB):</span>
                <span className="font-mono text-amber-400 font-bold">{deviceStatus.gradientDelta} nT</span>
              </div>
              <div className="w-full bg-slate-900 h-2 rounded-full overflow-hidden">
                <div
                  className="bg-amber-400 h-full transition-all duration-150"
                  style={{ width: `${Math.min(100, (Math.abs(deviceStatus.gradientDelta) / 800) * 100)}%` }}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div className="bg-slate-950 p-2.5 rounded-xl border border-slate-800">
                <span className="text-[10px] text-slate-400 block">Sensör 1 (Alt Prob):</span>
                <span className="font-mono text-slate-200 font-bold text-sm">
                  {deviceStatus.currentSensorA.toLocaleString()} nT
                </span>
              </div>
              <div className="bg-slate-950 p-2.5 rounded-xl border border-slate-800">
                <span className="text-[10px] text-slate-400 block">Sensör 2 (Üst Prob):</span>
                <span className="font-mono text-slate-200 font-bold text-sm">
                  {deviceStatus.currentSensorB.toLocaleString()} nT
                </span>
              </div>
            </div>

            <div className="grid grid-cols-3 gap-2">
              <div className="bg-slate-950 p-2 rounded-xl border border-slate-800 text-center">
                <span className="text-[9px] text-slate-400 block">Eğim (Pitch)</span>
                <span className="font-mono text-slate-200 text-xs font-semibold">{deviceStatus.currentPitch}°</span>
              </div>
              <div className="bg-slate-950 p-2 rounded-xl border border-slate-800 text-center">
                <span className="text-[9px] text-slate-400 block">Yatma (Roll)</span>
                <span className="font-mono text-slate-200 text-xs font-semibold">{deviceStatus.currentRoll}°</span>
              </div>
              <div className="bg-slate-950 p-2 rounded-xl border border-slate-800 text-center">
                <span className="text-[9px] text-slate-400 block">Batarya</span>
                <span className="font-mono text-emerald-400 text-xs font-semibold">%{deviceStatus.batteryLevel}</span>
              </div>
            </div>
          </div>

          {/* Ground Balance Calibration */}
          <div className="mt-4 pt-3 border-t border-slate-800">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs text-slate-400">Zemin Referansı:</span>
              <span className="text-xs font-mono font-bold text-cyan-400">{groundBalanceVal.toLocaleString()} nT</span>
            </div>
            <button
              onClick={handleCalibrateGroundBalance}
              disabled={isCalibrating}
              className="w-full py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-medium text-xs border border-slate-700 flex items-center justify-center gap-2 transition-colors"
            >
              <Zap className={`w-3.5 h-3.5 text-amber-400 ${isCalibrating ? 'animate-spin' : ''}`} />
              {isCalibrating ? 'Zemin Dengeleniyor...' : 'Zemin Balansı Sıfırla (Ground Balance)'}
            </button>
          </div>
        </div>

        {/* Walking Pace / Simulation Speed Slider */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow-xl">
          <div className="flex justify-between items-center mb-2">
            <span className="text-xs font-bold text-white">Adım Hızı (Simülatör):</span>
            <span className="text-xs font-mono text-amber-400">{walkSpeedMs} ms/adım</span>
          </div>
          <input
            type="range"
            min={150}
            max={1000}
            step={50}
            value={walkSpeedMs}
            onChange={(e) => setWalkSpeedMs(Number(e.target.value))}
            className="w-full accent-amber-500 bg-slate-950 cursor-pointer"
          />
        </div>
      </div>
    </div>
  );
};
