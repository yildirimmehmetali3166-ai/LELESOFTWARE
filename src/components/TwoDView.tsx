import React, { useState, useRef, useEffect } from 'react';
import { ScanGrid, PaletteType, DetectedAnomaly, ScanPoint } from '../types/scanner';
import { getPaletteColor, rgbToHex } from '../utils/colorScales';
import { detectorAudio } from '../utils/audioSynth';
import { Crosshair, Layers, ZoomIn, ZoomOut, Compass, Info } from 'lucide-react';

interface TwoDViewProps {
  grid: ScanGrid;
  palette: PaletteType;
  anomalies: DetectedAnomaly[];
  selectedAnomalyId: string | null;
  onSelectAnomaly: (anomaly: DetectedAnomaly | null) => void;
}

export const TwoDView: React.FC<TwoDViewProps> = ({
  grid,
  palette,
  anomalies,
  selectedAnomalyId,
  onSelectAnomaly,
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [selectedCell, setSelectedCell] = useState<{ x: number; y: number; point: ScanPoint } | null>({
    x: Math.floor(grid.width / 2),
    y: Math.floor(grid.height / 2),
    point: grid.points[Math.floor(grid.height / 2)][Math.floor(grid.width / 2)],
  });
  const [showContours, setShowContours] = useState<boolean>(true);
  const [showValues, setShowValues] = useState<boolean>(false);
  const [smoothInterpolation, setSmoothInterpolation] = useState<boolean>(true);
  const [sliceAxis, setSliceAxis] = useState<'X' | 'Y'>('X');

  const { width, height, stepDistanceMeters, points } = grid;

  // Render 2D Heatmap to HTML5 Canvas
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const renderW = canvas.width;
    const renderH = canvas.height;
    ctx.clearRect(0, 0, renderW, renderH);

    const cellW = renderW / width;
    const cellH = renderH / height;

    if (smoothInterpolation) {
      // Sub-pixel bilinear interpolation for smooth geophysical heatmap
      const imgData = ctx.createImageData(renderW, renderH);
      const data = imgData.data;

      for (let py = 0; py < renderH; py++) {
        const gy = (py / renderH) * (height - 1);
        const y0 = Math.floor(gy);
        const y1 = Math.min(height - 1, y0 + 1);
        const wy = gy - y0;

        for (let px = 0; px < renderW; px++) {
          const gx = (px / renderW) * (width - 1);
          const x0 = Math.floor(gx);
          const x1 = Math.min(width - 1, x0 + 1);
          const wx = gx - x0;

          // 4-point bilinear interpolation
          const s00 = points[y0]?.[x0]?.filteredSignal ?? 0;
          const s10 = points[y0]?.[x1]?.filteredSignal ?? 0;
          const s01 = points[y1]?.[x0]?.filteredSignal ?? 0;
          const s11 = points[y1]?.[x1]?.filteredSignal ?? 0;

          const sTop = s00 * (1 - wx) + s10 * wx;
          const sBottom = s01 * (1 - wx) + s11 * wx;
          const finalSignal = sTop * (1 - wy) + sBottom * wy;

          const rgb = getPaletteColor(finalSignal, palette);
          const idx = (py * renderW + px) * 4;
          data[idx] = rgb.r;
          data[idx + 1] = rgb.g;
          data[idx + 2] = rgb.b;
          data[idx + 3] = 255;
        }
      }
      ctx.putImageData(imgData, 0, 0);
    } else {
      // Raw Block Grid
      for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
          const sig = points[y][x].filteredSignal;
          const rgb = getPaletteColor(sig, palette);
          ctx.fillStyle = rgbToHex(rgb);
          ctx.fillRect(x * cellW, y * cellH, cellW, cellH);

          ctx.strokeStyle = 'rgba(15, 23, 42, 0.4)';
          ctx.lineWidth = 1;
          ctx.strokeRect(x * cellW, y * cellH, cellW, cellH);
        }
      }
    }

    // Draw Grid Lines overlay
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.15)';
    ctx.lineWidth = 1;
    for (let x = 0; x <= width; x++) {
      ctx.beginPath();
      ctx.moveTo(x * cellW, 0);
      ctx.lineTo(x * cellW, renderH);
      ctx.stroke();
    }
    for (let y = 0; y <= height; y++) {
      ctx.beginPath();
      ctx.moveTo(0, y * cellH);
      ctx.lineTo(renderW, y * cellH);
      ctx.stroke();
    }

    // Optional Cell Values
    if (showValues) {
      ctx.fillStyle = '#ffffff';
      ctx.font = '10px monospace';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
          const sig = points[y][x].filteredSignal;
          ctx.fillText(Math.round(sig).toString(), (x + 0.5) * cellW, (y + 0.5) * cellH);
        }
      }
    }

    // Draw Detected Anomalies Target Markers
    anomalies.forEach((anom) => {
      const isSelected = selectedAnomalyId === anom.id;
      const ax = (anom.gridX + 0.5) * cellW;
      const ay = (anom.gridY + 0.5) * cellH;

      ctx.save();
      // Target reticle
      ctx.beginPath();
      ctx.arc(ax, ay, cellW * 0.7, 0, Math.PI * 2);
      ctx.strokeStyle = isSelected ? '#ffffff' : (anom.classification === 'void' ? '#38bdf8' : '#fbbf24');
      ctx.lineWidth = isSelected ? 3 : 2;
      ctx.setLineDash(isSelected ? [] : [4, 4]);
      ctx.stroke();

      // Crosshairs
      ctx.beginPath();
      ctx.moveTo(ax - cellW * 0.9, ay);
      ctx.lineTo(ax + cellW * 0.9, ay);
      ctx.moveTo(ax, ay - cellH * 0.9);
      ctx.lineTo(ax, ay + cellH * 0.9);
      ctx.strokeStyle = isSelected ? '#ffffff' : '#fbbf24';
      ctx.lineWidth = 1.5;
      ctx.setLineDash([]);
      ctx.stroke();

      // Target Label
      ctx.font = 'bold 11px sans-serif';
      ctx.fillStyle = '#0f172a';
      const label = `Z: -${anom.depthMeters}m`;
      const textW = ctx.measureText(label).width;
      ctx.fillRect(ax - textW / 2 - 4, ay - cellH * 1.1 - 12, textW + 8, 16);
      ctx.strokeStyle = anom.classification === 'void' ? '#38bdf8' : '#f59e0b';
      ctx.strokeRect(ax - textW / 2 - 4, ay - cellH * 1.1 - 12, textW + 8, 16);
      ctx.fillStyle = '#f8fafc';
      ctx.fillText(label, ax, ay - cellH * 1.1 - 3);

      ctx.restore();
    });

    // Draw Cross-Section Slice Guides
    if (selectedCell) {
      const sx = (selectedCell.x + 0.5) * cellW;
      const sy = (selectedCell.y + 0.5) * cellH;

      ctx.save();
      // Selected cell highlight
      ctx.strokeStyle = '#38bdf8';
      ctx.lineWidth = 2.5;
      ctx.strokeRect(selectedCell.x * cellW, selectedCell.y * cellH, cellW, cellH);

      // Active slice guideline
      ctx.strokeStyle = 'rgba(234, 179, 8, 0.7)';
      ctx.lineWidth = 1.5;
      ctx.setLineDash([6, 4]);
      ctx.beginPath();
      if (sliceAxis === 'X') {
        ctx.moveTo(0, sy);
        ctx.lineTo(renderW, sy);
      } else {
        ctx.moveTo(sx, 0);
        ctx.lineTo(sx, renderH);
      }
      ctx.stroke();
      ctx.restore();
    }
  }, [grid, palette, smoothInterpolation, showValues, showContours, selectedCell, sliceAxis, anomalies, selectedAnomalyId]);

  // Handle canvas clicks to inspect cells
  const handleCanvasClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const clickY = e.clientY - rect.top;

    const cellW = rect.width / width;
    const cellH = rect.height / height;
    const gx = Math.min(width - 1, Math.max(0, Math.floor(clickX / cellW)));
    const gy = Math.min(height - 1, Math.max(0, Math.floor(clickY / cellH)));

    const pt = points[gy][gx];
    setSelectedCell({ x: gx, y: gy, point: pt });
    detectorAudio.playSignalTone(pt.filteredSignal);

    // Check if clicked near an anomaly
    const matchingAnom = anomalies.find((a) => a.gridX === gx && a.gridY === gy);
    if (matchingAnom) {
      onSelectAnomaly(matchingAnom);
    }
  };

  // Compute profile slice data for cross-section chart
  const sliceData = React.useMemo(() => {
    if (!selectedCell) return [];
    if (sliceAxis === 'X') {
      // Y is constant, X varies along row
      const y = selectedCell.y;
      return points[y].map((p, x) => ({
        coord: `${(x * stepDistanceMeters).toFixed(1)}m`,
        signal: p.filteredSignal,
        depth: p.depth,
        raw: p.rawSignal,
        x,
        y,
      }));
    } else {
      // X is constant, Y varies along column
      const x = selectedCell.x;
      return points.map((row, y) => ({
        coord: `${(y * stepDistanceMeters).toFixed(1)}m`,
        signal: row[x].filteredSignal,
        depth: row[x].depth,
        raw: row[x].rawSignal,
        x,
        y,
      }));
    }
  }, [selectedCell, sliceAxis, points, stepDistanceMeters]);

  return (
    <div className="w-full h-full bg-slate-950 p-4 flex flex-col lg:flex-row gap-4 overflow-y-auto">
      {/* 2D Plan View Heatmap Section */}
      <div className="flex-1 flex flex-col bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow-xl">
        <div className="flex items-center justify-between pb-3 border-b border-slate-800 mb-3">
          <div className="flex items-center gap-2">
            <Layers className="w-5 h-5 text-amber-400" />
            <h2 className="font-bold text-white text-base">2D Plan Görünümü & Jeofizik Isı Matrisi</h2>
            <span className="text-xs text-slate-400">({width} x {height} Ölçüm Noktası)</span>
          </div>

          {/* Quick 2D Display Toggles */}
          <div className="flex items-center gap-2">
            <button
              onClick={() => setSmoothInterpolation(!smoothInterpolation)}
              className={`px-2.5 py-1 rounded-lg text-xs font-medium border transition-colors ${
                smoothInterpolation
                  ? 'bg-amber-500/20 text-amber-300 border-amber-500/40'
                  : 'bg-slate-800 text-slate-400 border-slate-700'
              }`}
            >
              Enterpolasyon: {smoothInterpolation ? 'Bilinear Düzgün' : 'Piksel/Blok'}
            </button>
            <button
              onClick={() => setShowValues(!showValues)}
              className={`px-2.5 py-1 rounded-lg text-xs font-medium border transition-colors ${
                showValues
                  ? 'bg-cyan-500/20 text-cyan-300 border-cyan-500/40'
                  : 'bg-slate-800 text-slate-400 border-slate-700'
              }`}
            >
              Sayısal Değerler
            </button>
          </div>
        </div>

        {/* Canvas Container */}
        <div className="relative flex-1 flex items-center justify-center min-h-[380px] bg-slate-950/60 rounded-xl border border-slate-800/80 p-2">
          {/* North Direction Marker */}
          <div className="absolute top-4 right-4 z-10 flex flex-col items-center bg-slate-900/80 border border-slate-700/60 rounded-lg px-2 py-1 backdrop-blur-sm">
            <Compass className="w-4 h-4 text-rose-400" />
            <span className="text-[10px] font-bold text-slate-200">KUZEY (N)</span>
          </div>

          <canvas
            ref={canvasRef}
            width={560}
            height={560}
            onClick={handleCanvasClick}
            className="w-full max-w-[540px] aspect-square rounded-lg shadow-2xl cursor-crosshair border border-slate-700/60"
          />
        </div>

        {/* Legend bar */}
        <div className="mt-3 pt-3 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400">
          <div className="flex items-center gap-2">
            <span className="text-[11px] font-semibold text-blue-400">-100 (Boşluk/Oda)</span>
            <div className="h-3 w-48 rounded-full bg-gradient-to-r from-blue-700 via-emerald-500 to-amber-500 border border-slate-700" />
            <span className="text-[11px] font-semibold text-amber-400">+100 (Altın/Metal)</span>
          </div>
          <span className="text-[11px] italic text-slate-500">Tıklanan hücre: sesli analiz ve derinlik profilini günceller.</span>
        </div>
      </div>

      {/* Cross-Section & Cell Detail Section */}
      <div className="w-full lg:w-[420px] flex flex-col gap-4">
        {/* Selected Point Inspector */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow-xl">
          <div className="flex items-center justify-between pb-2 border-b border-slate-800 mb-3">
            <div className="flex items-center gap-2">
              <Crosshair className="w-4 h-4 text-cyan-400" />
              <h3 className="font-bold text-white text-sm">Hücre Ölçüm Detayı</h3>
            </div>
            {selectedCell && (
              <span className="text-xs font-mono text-cyan-400 bg-cyan-950/60 px-2 py-0.5 rounded border border-cyan-800/50">
                X: {selectedCell.x} ({ (selectedCell.x * stepDistanceMeters).toFixed(1) }m) | Y: {selectedCell.y} ({ (selectedCell.y * stepDistanceMeters).toFixed(1) }m)
              </span>
            )}
          </div>

          {selectedCell ? (
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div className="bg-slate-950/80 p-2.5 rounded-xl border border-slate-800">
                <span className="text-slate-400 text-[10px] block font-medium">Filtrelenmiş Sinyal</span>
                <span
                  className={`text-lg font-bold font-mono ${
                    selectedCell.point.filteredSignal < -25
                      ? 'text-blue-400'
                      : selectedCell.point.filteredSignal > 40
                      ? 'text-amber-400'
                      : 'text-emerald-400'
                  }`}
                >
                  {selectedCell.point.filteredSignal > 0 ? `+${selectedCell.point.filteredSignal.toFixed(1)}` : selectedCell.point.filteredSignal.toFixed(1)} %
                </span>
              </div>

              <div className="bg-slate-950/80 p-2.5 rounded-xl border border-slate-800">
                <span className="text-slate-400 text-[10px] block font-medium">Hedef Derinliği</span>
                <span className="text-lg font-bold font-mono text-cyan-400">
                  {selectedCell.point.depth > 0 ? `-${selectedCell.point.depth.toFixed(2)} m` : 'Yüzey'}
                </span>
              </div>

              <div className="bg-slate-950/80 p-2.5 rounded-xl border border-slate-800">
                <span className="text-slate-400 text-[10px] block font-medium">Ham Manyetik Gradyan</span>
                <span className="text-sm font-mono text-slate-200">
                  {selectedCell.point.rawSignal.toLocaleString()} nT
                </span>
              </div>

              <div className="bg-slate-950/80 p-2.5 rounded-xl border border-slate-800">
                <span className="text-slate-400 text-[10px] block font-medium">Sınıflandırma</span>
                <span className="text-xs font-semibold uppercase text-amber-300">
                  {selectedCell.point.classification === 'void'
                    ? 'Boşluk / Tünel'
                    : selectedCell.point.classification === 'precious'
                    ? 'Değerli Metal'
                    : selectedCell.point.classification === 'ferrous'
                    ? 'Demir / Manyetik'
                    : 'Doğal Zemin'}
                </span>
              </div>
            </div>
          ) : (
            <p className="text-xs text-slate-500 py-3 text-center">Harita üzerinde bir noktaya tıklayın.</p>
          )}
        </div>

        {/* 1D Cross-Section Depth Profile Slice */}
        <div className="flex-1 bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow-xl flex flex-col">
          <div className="flex items-center justify-between pb-2 border-b border-slate-800 mb-3">
            <h3 className="font-bold text-white text-sm">Kesit Profili (Cross-Section)</h3>
            <div className="flex gap-1 bg-slate-950 p-1 rounded-lg border border-slate-800 text-xs">
              <button
                onClick={() => setSliceAxis('X')}
                className={`px-2 py-0.5 rounded font-medium transition-colors ${
                  sliceAxis === 'X' ? 'bg-amber-500 text-slate-950 font-bold' : 'text-slate-400'
                }`}
              >
                Yatay (X-Profili)
              </button>
              <button
                onClick={() => setSliceAxis('Y')}
                className={`px-2 py-0.5 rounded font-medium transition-colors ${
                  sliceAxis === 'Y' ? 'bg-amber-500 text-slate-950 font-bold' : 'text-slate-400'
                }`}
              >
                Dikey (Y-Profili)
              </button>
            </div>
          </div>

          {/* SVG Waveform Profile */}
          <div className="flex-1 min-h-[160px] bg-slate-950 rounded-xl p-3 border border-slate-800 relative flex flex-col justify-end">
            <div className="absolute top-2 left-2 text-[10px] text-slate-500">
              {sliceAxis === 'X' ? `Y = ${(selectedCell?.y ?? 0) * stepDistanceMeters}m hattı boyunca sinyal genliği & derinlik` : `X = ${(selectedCell?.x ?? 0) * stepDistanceMeters}m hattı boyunca sinyal genliği`}
            </div>

            <svg className="w-full h-32 overflow-visible">
              {/* Zero baseline */}
              <line x1="0" y1="50%" x2="100%" y2="50%" stroke="#334155" strokeDasharray="3 3" />

              {/* Signal curve */}
              {(() => {
                if (sliceData.length < 2) return null;
                const pointsSvg = sliceData
                  .map((d, i) => {
                    const x = (i / (sliceData.length - 1)) * 100;
                    // signal is -100 to +100 -> map to 0..100% height
                    const y = 50 - (d.signal / 100) * 45;
                    return `${x}%,${y}%`;
                  })
                  .join(' ');

                return (
                  <>
                    <polyline
                      fill="none"
                      stroke="#f59e0b"
                      strokeWidth="2.5"
                      points={pointsSvg}
                    />
                    {sliceData.map((d, i) => {
                      const cx = `${(i / (sliceData.length - 1)) * 100}%`;
                      const cy = `${50 - (d.signal / 100) * 45}%`;
                      const isVoid = d.signal < -30;
                      return (
                        <circle
                          key={i}
                          cx={cx}
                          cy={cy}
                          r="3"
                          fill={isVoid ? '#38bdf8' : d.signal > 40 ? '#f59e0b' : '#10b981'}
                        />
                      );
                    })}
                  </>
                );
              })()}
            </svg>

            <div className="flex justify-between text-[10px] font-mono text-slate-400 pt-2 border-t border-slate-800/80">
              <span>0.0m</span>
              <span>{((sliceData.length - 1) * stepDistanceMeters).toFixed(1)}m</span>
            </div>
          </div>

          {/* Anomaly list for quick jump */}
          <div className="mt-3">
            <span className="text-[11px] font-semibold text-slate-400 block mb-1.5">Tespit Edilen Anomaliler:</span>
            <div className="space-y-1.5 max-h-36 overflow-y-auto pr-1">
              {anomalies.map((anom) => (
                <div
                  key={anom.id}
                  onClick={() => {
                    onSelectAnomaly(anom);
                    setSelectedCell({
                      x: anom.gridX,
                      y: anom.gridY,
                      point: points[anom.gridY][anom.gridX],
                    });
                  }}
                  className={`p-2 rounded-lg border text-xs cursor-pointer flex items-center justify-between transition-colors ${
                    selectedAnomalyId === anom.id
                      ? 'bg-amber-500/20 border-amber-500 text-amber-200'
                      : 'bg-slate-950 hover:bg-slate-800/60 border-slate-800 text-slate-300'
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <span
                      className={`w-2.5 h-2.5 rounded-full ${
                        anom.classification === 'void' ? 'bg-blue-400' : 'bg-amber-400'
                      }`}
                    />
                    <span className="font-semibold">{anom.title}</span>
                  </div>
                  <span className="font-mono text-cyan-400 font-bold">-{anom.depthMeters} m</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
