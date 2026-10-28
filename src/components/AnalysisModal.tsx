import React, { useState } from 'react';
import { ScanGrid, DetectedAnomaly } from '../types/scanner';
import { 
  X, 
  Download, 
  FileSpreadsheet, 
  FileCode, 
  Box, 
  Check, 
  Printer, 
  AlertCircle, 
  Compass,
  Layers
} from 'lucide-react';

interface AnalysisModalProps {
  isOpen: boolean;
  onClose: () => void;
  grid: ScanGrid;
  anomalies: DetectedAnomaly[];
}

export const AnalysisModal: React.FC<AnalysisModalProps> = ({
  isOpen,
  onClose,
  grid,
  anomalies,
}) => {
  const [downloadSuccess, setDownloadSuccess] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleExportJSON = () => {
    const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(grid, null, 2));
    const dlAnchor = document.createElement('a');
    dlAnchor.setAttribute('href', dataStr);
    dlAnchor.setAttribute('download', `${grid.id}_scan_data.json`);
    dlAnchor.click();
    showToast('JSON Verisi İndirildi');
  };

  const handleExportCSV = () => {
    let csv = `Nicea Dedektor - Saha Tarama Raporu\n`;
    csv += `Baslik,${grid.title}\nTarih,${grid.date}\nSensor,${grid.sensorType}\nZemin Referansi,${grid.groundBalance} nT\n\n`;
    csv += `Hucresel Sinyal Matrisi (-100 ile +100):\n`;

    for (let y = 0; y < grid.height; y++) {
      const row = grid.points[y].map((p) => p.filteredSignal.toFixed(1)).join(',');
      csv += `${row}\n`;
    }

    csv += `\nTespit Edilen Anomaliler:\n`;
    csv += `ID,Baslik,Siniflandirma,Derinlik (m),Hacim (m3),X (m),Y (m),Sinyal\n`;
    anomalies.forEach((a) => {
      csv += `${a.id},${a.title},${a.classification},${a.depthMeters},${a.volumeM3},${a.xMeters},${a.yMeters},${a.signalStrength}%\n`;
    });

    const dataStr = 'data:text/csv;charset=utf-8,' + encodeURIComponent(csv);
    const dlAnchor = document.createElement('a');
    dlAnchor.setAttribute('href', dataStr);
    dlAnchor.setAttribute('download', `${grid.id}_matrix.csv`);
    dlAnchor.click();
    showToast('CSV Matrisi İndirildi');
  };

  const handleExportOBJ = () => {
    // Generate basic Wavefront OBJ 3D terrain mesh
    let obj = `# Nicea Dedektor 3D Terrain OBJ\n`;
    obj += `o GroundSubsurface\n`;
    const { width, height, stepDistanceMeters, points } = grid;

    // Vertices
    for (let y = 0; y < height; y++) {
      for (let x = 0; x < width; x++) {
        const sig = points[y][x].filteredSignal;
        const elevation = (sig / 100) * 0.8;
        obj += `v ${(x * stepDistanceMeters).toFixed(3)} ${elevation.toFixed(3)} ${(y * stepDistanceMeters).toFixed(3)}\n`;
      }
    }

    // Faces
    for (let y = 0; y < height - 1; y++) {
      for (let x = 0; x < width - 1; x++) {
        const i1 = y * width + x + 1;
        const i2 = y * width + (x + 1) + 1;
        const i3 = (y + 1) * width + x + 1;
        const i4 = (y + 1) * width + (x + 1) + 1;
        // Two triangles per quad
        obj += `f ${i1} ${i3} ${i2}\n`;
        obj += `f ${i2} ${i3} ${i4}\n`;
      }
    }

    const dataStr = 'data:text/plain;charset=utf-8,' + encodeURIComponent(obj);
    const dlAnchor = document.createElement('a');
    dlAnchor.setAttribute('href', dataStr);
    dlAnchor.setAttribute('download', `${grid.id}_mesh.obj`);
    dlAnchor.click();
    showToast('3D OBJ Modeli İndirildi');
  };

  const showToast = (msg: string) => {
    setDownloadSuccess(msg);
    setTimeout(() => setDownloadSuccess(null), 2500);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm select-none">
      <div className="relative w-full max-w-3xl max-h-[90vh] bg-slate-900 border border-slate-700 rounded-2xl shadow-2xl overflow-hidden flex flex-col text-slate-200">
        {/* Header */}
        <div className="px-6 py-4 bg-slate-950 border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-amber-500/20 border border-amber-500/40 flex items-center justify-center text-amber-400 font-bold">
              N
            </div>
            <div>
              <h2 className="text-base font-bold text-white">Nicea Dedektör Jeofizik Saha Raporu</h2>
              <span className="text-xs text-slate-400">{grid.title}</span>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6 text-xs">
          {/* Scan Metadata Cards */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="bg-slate-950 p-3 rounded-xl border border-slate-800">
              <span className="text-slate-400 text-[10px] block font-semibold">TARAMA ALANI</span>
              <span className="font-mono text-sm text-slate-100 font-bold">
                {(grid.width * grid.stepDistanceMeters).toFixed(1)}m × {(grid.height * grid.stepDistanceMeters).toFixed(1)}m
              </span>
            </div>

            <div className="bg-slate-950 p-3 rounded-xl border border-slate-800">
              <span className="text-slate-400 text-[10px] block font-semibold">ÖLÇÜM ADIMI</span>
              <span className="font-mono text-sm text-slate-100 font-bold">
                {grid.stepDistanceMeters}m ({grid.width * grid.height} Nokta)
              </span>
            </div>

            <div className="bg-slate-950 p-3 rounded-xl border border-slate-800">
              <span className="text-slate-400 text-[10px] block font-semibold">ZEMİN BALANSI</span>
              <span className="font-mono text-sm text-emerald-400 font-bold">
                {grid.groundBalance.toLocaleString()} nT
              </span>
            </div>

            <div className="bg-slate-950 p-3 rounded-xl border border-slate-800">
              <span className="text-slate-400 text-[10px] block font-semibold">SENSÖR TİPİ</span>
              <span className="text-xs text-amber-400 font-bold truncate block">
                {grid.sensorType}
              </span>
            </div>
          </div>

          {/* Location & Operator Notes */}
          <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-2">
            <div className="flex items-center gap-2 text-slate-400">
              <Compass className="w-4 h-4 text-amber-400" />
              <span className="font-semibold text-white">Saha Konum Bilgisi:</span>
              <span>{grid.locationName}</span>
            </div>
            <p className="text-slate-300 italic text-[11px] bg-slate-900/60 p-2.5 rounded-lg border border-slate-850">
              "{grid.notes}"
            </p>
          </div>

          {/* Detected Anomalies Table */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <h3 className="font-bold text-white text-sm flex items-center gap-2">
                <Layers className="w-4 h-4 text-cyan-400" />
                Tespit Edilen Yeraltı Anomalileri ({anomalies.length} Hedef)
              </h3>
            </div>

            {anomalies.length === 0 ? (
              <div className="p-4 bg-slate-950 rounded-xl border border-slate-800 text-center text-slate-500">
                Seçili filtre parametrelerinde belirgin bir anomali saptanmadı.
              </div>
            ) : (
              <div className="border border-slate-800 rounded-xl overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-950 text-slate-400 font-mono text-[11px] border-b border-slate-800">
                    <tr>
                      <th className="p-3">Hedef / Tanım</th>
                      <th className="p-3">Sınıf</th>
                      <th className="p-3">Derinlik (Z)</th>
                      <th className="p-3">Hacim</th>
                      <th className="p-3">Koordinat (X, Y)</th>
                      <th className="p-3">Sinyal</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800 bg-slate-950/40 text-slate-300">
                    {anomalies.map((anom) => (
                      <tr key={anom.id} className="hover:bg-slate-800/40 transition-colors">
                        <td className="p-3 font-semibold text-white">{anom.title}</td>
                        <td className="p-3">
                          <span
                            className={`px-2 py-0.5 rounded text-[10px] font-semibold uppercase ${
                              anom.classification === 'void'
                                ? 'bg-blue-900/70 text-blue-300'
                                : anom.classification === 'precious'
                                ? 'bg-amber-900/70 text-amber-300'
                                : 'bg-yellow-900/70 text-yellow-300'
                            }`}
                          >
                            {anom.classification === 'void' ? 'Boşluk' : anom.classification === 'precious' ? 'Değerli Metal' : 'Metalik'}
                          </span>
                        </td>
                        <td className="p-3 font-mono font-bold text-cyan-400">-{anom.depthMeters} m</td>
                        <td className="p-3 font-mono text-slate-300">{anom.volumeM3} m³</td>
                        <td className="p-3 font-mono text-slate-400">{anom.xMeters}m, {anom.yMeters}m</td>
                        <td className="p-3 font-mono text-amber-400 font-bold">{anom.signalStrength}%</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>

        {/* Footer Actions: Export formats */}
        <div className="px-6 py-4 bg-slate-950 border-t border-slate-800 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <span className="text-xs text-slate-400 font-semibold">Dışa Aktar (Export):</span>
            <button
              onClick={handleExportCSV}
              className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium border border-slate-700 flex items-center gap-1.5 transition-colors"
            >
              <FileSpreadsheet className="w-3.5 h-3.5 text-emerald-400" />
              CSV Matris
            </button>
            <button
              onClick={handleExportJSON}
              className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium border border-slate-700 flex items-center gap-1.5 transition-colors"
            >
              <FileCode className="w-3.5 h-3.5 text-cyan-400" />
              JSON Log
            </button>
            <button
              onClick={handleExportOBJ}
              className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium border border-slate-700 flex items-center gap-1.5 transition-colors"
            >
              <Box className="w-3.5 h-3.5 text-amber-400" />
              3D OBJ Mesh
            </button>
          </div>

          <button
            onClick={() => window.print()}
            className="px-4 py-1.5 rounded-lg bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-xs flex items-center gap-1.5 shadow-lg shadow-amber-500/20 transition-all"
          >
            <Printer className="w-3.5 h-3.5" />
            Raporu Yazdır
          </button>
        </div>

        {downloadSuccess && (
          <div className="absolute bottom-16 left-1/2 -translate-x-1/2 bg-emerald-600 text-white px-4 py-1.5 rounded-full text-xs font-semibold shadow-xl flex items-center gap-2 animate-fade-in">
            <Check className="w-3.5 h-3.5" />
            {downloadSuccess}
          </div>
        )}
      </div>
    </div>
  );
};
