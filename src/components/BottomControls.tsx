import React from 'react';
import { PaletteType, ViewMode } from '../types/scanner';
import { 
  Eye, 
  Box, 
  Layers, 
  Sparkles, 
  Grid3X3, 
  Sliders, 
  ChevronDown, 
  ChevronUp, 
  Compass, 
  Palette
} from 'lucide-react';

interface BottomControlsProps {
  palette: PaletteType;
  onSelectPalette: (palette: PaletteType) => void;
  viewMode: ViewMode;
  onSelectViewMode: (mode: ViewMode) => void;
  depthCutMeters: number;
  onChangeDepthCut: (depth: number) => void;
  thresholdFilter: number;
  onChangeThreshold: (val: number) => void;
  wireframe: boolean;
  onToggleWireframe: () => void;
  cameraPreset: 'iso' | 'top' | 'side' | 'front';
  onSelectCamera: (cam: 'iso' | 'top' | 'side' | 'front') => void;
  isCollapsed: boolean;
  onToggleCollapse: () => void;
}

export const BottomControls: React.FC<BottomControlsProps> = ({
  palette,
  onSelectPalette,
  viewMode,
  onSelectViewMode,
  depthCutMeters,
  onChangeDepthCut,
  thresholdFilter,
  onChangeThreshold,
  wireframe,
  onToggleWireframe,
  cameraPreset,
  onSelectCamera,
  isCollapsed,
  onToggleCollapse,
}) => {
  return (
    <div className="fixed bottom-3 left-1/2 -translate-x-1/2 z-30 w-full max-w-[940px] px-3 select-none pointer-events-auto">
      {/* Collapse/Expand Toggle Tab */}
      <div className="flex justify-center -mb-px">
        <button
          onClick={onToggleCollapse}
          className="bg-slate-900/90 border border-slate-700/80 border-b-0 px-4 py-1 rounded-t-xl text-slate-300 hover:text-white text-xs font-semibold flex items-center gap-1.5 shadow-xl transition-all"
        >
          {isCollapsed ? (
            <>
              <ChevronUp className="w-3.5 h-3.5 text-amber-400" />
              <span>Kontrol Panelini Göster</span>
            </>
          ) : (
            <>
              <ChevronDown className="w-3.5 h-3.5 text-amber-400" />
              <span>Sinematik Mod (Gizle)</span>
            </>
          )}
        </button>
      </div>

      {/* Main Glassmorphic Control Dock */}
      {!isCollapsed && (
        <div className="bg-slate-900/95 border border-slate-700/80 rounded-2xl p-3 shadow-2xl shadow-black/60 text-xs text-slate-200">
          <div className="flex flex-wrap items-center justify-between gap-3">
            {/* 1. View Mode Pills */}
            <div className="flex items-center gap-1.5 bg-slate-950/80 p-1 rounded-xl border border-slate-800">
              {[
                { id: 'surface', label: 'Yüzey Mesh', icon: Layers },
                { id: 'voxels', label: '3D Voksel', icon: Box },
                { id: 'iso_anomalies', label: 'İzo-Anomali', icon: Sparkles },
                { id: 'wireframe', label: 'Tel Kafes', icon: Grid3X3 },
              ].map((vm) => {
                const Icon = vm.icon;
                const active = viewMode === vm.id;
                return (
                  <button
                    key={vm.id}
                    onClick={() => onSelectViewMode(vm.id as ViewMode)}
                    className={`px-2.5 py-1.5 rounded-lg font-medium flex items-center gap-1.5 transition-all ${
                      active
                        ? 'bg-amber-500 text-slate-950 font-bold shadow'
                        : 'text-slate-400 hover:text-white hover:bg-slate-800'
                    }`}
                  >
                    <Icon className="w-3.5 h-3.5" />
                    <span>{vm.label}</span>
                  </button>
                );
              })}
            </div>

            {/* 2. Camera Angles */}
            <div className="flex items-center gap-1 bg-slate-950/80 p-1 rounded-xl border border-slate-800">
              <span className="text-[10px] text-slate-500 px-1 font-semibold uppercase">Kamera:</span>
              {[
                { id: 'iso', label: 'İzometrik' },
                { id: 'top', label: 'Kuşbakışı' },
                { id: 'side', label: 'Yan Kesit' },
                { id: 'front', label: 'Ön' },
              ].map((cam) => (
                <button
                  key={cam.id}
                  onClick={() => onSelectCamera(cam.id as typeof cameraPreset)}
                  className={`px-2 py-1 rounded-lg text-xs transition-colors ${
                    cameraPreset === cam.id
                      ? 'bg-cyan-500/20 text-cyan-300 font-bold border border-cyan-500/40'
                      : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {cam.label}
                </button>
              ))}
            </div>

            {/* 3. Color Palettes */}
            <div className="flex items-center gap-1.5 bg-slate-950/80 p-1 rounded-xl border border-slate-800">
              <Palette className="w-3.5 h-3.5 text-amber-400 ml-1" />
              {[
                { id: 'nicea_spectrum', label: 'Nicea Spektrum', bg: 'from-blue-600 via-emerald-500 to-amber-500' },
                { id: 'thermal_jet', label: 'Jet Termal', bg: 'from-blue-500 via-green-400 to-red-500' },
                { id: 'magma', label: 'Magma', bg: 'from-purple-900 via-rose-500 to-yellow-300' },
                { id: 'gold_rush', label: 'Gold Rush', bg: 'from-slate-800 via-amber-600 to-amber-200' },
              ].map((p) => (
                <button
                  key={p.id}
                  onClick={() => onSelectPalette(p.id as PaletteType)}
                  className={`px-2 py-1 rounded-lg text-[11px] font-medium flex items-center gap-1.5 border transition-all ${
                    palette === p.id
                      ? 'bg-slate-800 text-white border-amber-500/70 shadow'
                      : 'border-transparent text-slate-400 hover:text-white'
                  }`}
                >
                  <span className={`w-2.5 h-2.5 rounded-full bg-gradient-to-r ${p.bg}`} />
                  <span>{p.label}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Slider Row: Depth Cut & Threshold */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-3 pt-2.5 border-t border-slate-800/80 text-xs">
            {/* Depth Cut Plane Slider */}
            <div className="flex items-center gap-3">
              <span className="text-slate-400 text-[11px] font-semibold whitespace-nowrap">
                Derinlik Kesiti (Z-Cut):
              </span>
              <input
                type="range"
                min={0.5}
                max={10.0}
                step={0.1}
                value={depthCutMeters}
                onChange={(e) => onChangeDepthCut(Number(e.target.value))}
                className="w-full accent-cyan-400 bg-slate-950 cursor-pointer h-1.5 rounded-lg"
              />
              <span className="font-mono text-cyan-400 font-bold min-w-[50px] text-right">
                -{depthCutMeters.toFixed(1)}m
              </span>
            </div>

            {/* Noise/Mineral Threshold Filter */}
            <div className="flex items-center gap-3">
              <span className="text-slate-400 text-[11px] font-semibold whitespace-nowrap">
                Zemin Gürültü Filtresi:
              </span>
              <input
                type="range"
                min={0}
                max={60}
                step={5}
                value={thresholdFilter}
                onChange={(e) => onChangeThreshold(Number(e.target.value))}
                className="w-full accent-amber-400 bg-slate-950 cursor-pointer h-1.5 rounded-lg"
              />
              <span className="font-mono text-amber-400 font-bold min-w-[45px] text-right">
                %{thresholdFilter}
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
