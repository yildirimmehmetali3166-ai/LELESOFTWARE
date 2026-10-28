import React, { useState } from 'react';
import { 
  Box, 
  Layers, 
  Footprints, 
  BookOpen, 
  Volume2, 
  VolumeX, 
  FileText, 
  ChevronDown, 
  Radio, 
  Sparkles,
  Download
} from 'lucide-react';
import { ScanGrid } from '../types/scanner';
import { detectorAudio } from '../utils/audioSynth';

interface NavbarProps {
  activeTab: '3d' | '2d' | 'live' | 'plan';
  onSelectTab: (tab: '3d' | '2d' | 'live' | 'plan') => void;
  currentGrid: ScanGrid;
  onSelectPreset: (presetKey: string) => void;
  onOpenAnalysis: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  onSelectTab,
  currentGrid,
  onSelectPreset,
  onOpenAnalysis,
}) => {
  const [audioEnabled, setAudioEnabled] = useState<boolean>(false);
  const [presetDropdownOpen, setPresetDropdownOpen] = useState<boolean>(false);

  const toggleAudio = () => {
    const next = !audioEnabled;
    setAudioEnabled(next);
    detectorAudio.setEnabled(next);
  };

  return (
    <header className="w-full bg-slate-950 border-b border-slate-800/80 px-4 py-2.5 flex flex-wrap items-center justify-between gap-3 text-slate-200 select-none z-20">
      {/* Brand & Emblem */}
      <div className="flex items-center gap-3">
        <div className="flex items-center justify-center w-9 h-9 rounded-xl bg-gradient-to-br from-amber-500 to-amber-700 shadow-lg shadow-amber-600/20 text-slate-950 font-black text-lg tracking-wider">
          N
        </div>
        <div>
          <div className="flex items-center gap-1.5">
            <span className="font-extrabold text-base tracking-wider text-white">NICEA</span>
            <span className="font-semibold text-xs tracking-widest text-amber-400">DEDEKTÖR</span>
            <span className="bg-amber-500/20 text-amber-300 text-[10px] px-1.5 py-0.2 rounded font-mono font-bold border border-amber-500/30">
              3D/2D PRO
            </span>
          </div>
          <span className="text-[10px] text-slate-400 block tracking-tight">
            Jeofizik Alan Tarama & Yeraltı Görüntüleme Sistemi
          </span>
        </div>
      </div>

      {/* Main Mode Navigation Switcher */}
      <nav className="flex items-center gap-1 bg-slate-900/90 p-1 rounded-xl border border-slate-800">
        <button
          onClick={() => onSelectTab('3d')}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all ${
            activeTab === '3d'
              ? 'bg-amber-500 text-slate-950 shadow-md font-bold'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <Box className="w-3.5 h-3.5" />
          <span>3D Yeraltı Görünümü</span>
        </button>

        <button
          onClick={() => onSelectTab('2d')}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all ${
            activeTab === '2d'
              ? 'bg-amber-500 text-slate-950 shadow-md font-bold'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <Layers className="w-3.5 h-3.5" />
          <span>2D Isı & Kesit Analizi</span>
        </button>

        <button
          onClick={() => onSelectTab('live')}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all ${
            activeTab === 'live'
              ? 'bg-amber-500 text-slate-950 shadow-md font-bold'
              : 'text-slate-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <Footprints className="w-3.5 h-3.5" />
          <span>Canlı Saha Taraması</span>
        </button>

        <button
          onClick={() => onSelectTab('plan')}
          className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all ${
            activeTab === 'plan'
              ? 'bg-emerald-500 text-slate-950 shadow-md font-bold'
              : 'text-emerald-400 hover:text-white hover:bg-slate-800'
          }`}
        >
          <BookOpen className="w-3.5 h-3.5" />
          <span>Python & Android Çalışma Planı</span>
        </button>
      </nav>

      {/* Right Actions: Preset Selector, Audio, Report */}
      <div className="flex items-center gap-2">
        {/* Preset Selector Dropdown */}
        <div className="relative">
          <button
            onClick={() => setPresetDropdownOpen(!presetDropdownOpen)}
            className="px-2.5 py-1.5 rounded-xl bg-slate-900 hover:bg-slate-850 text-slate-300 text-xs font-medium border border-slate-800 flex items-center gap-1.5 transition-colors"
          >
            <Sparkles className="w-3.5 h-3.5 text-amber-400" />
            <span className="hidden sm:inline max-w-[130px] truncate">{currentGrid.title}</span>
            <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
          </button>

          {presetDropdownOpen && (
            <div className="absolute right-0 top-full mt-1.5 w-64 bg-slate-900 border border-slate-700/80 rounded-xl shadow-2xl py-1.5 z-50 text-xs">
              <span className="px-3 py-1 text-[10px] uppercase font-bold text-slate-400 block tracking-wider">
                Hazır Saha Taramaları
              </span>
              {[
                { key: 'mezar', title: 'İznik Lahit & Mezar Odası', sub: '2.8m derinlikte oda & bronz/altın' },
                { key: 'gold', title: 'Altın Sikke Küpü', sub: '1.45m dipol manyetik tepe' },
                { key: 'tunel', title: 'Yeraltı Tüneli & Mahzen', sub: '3.4m derinlikte galeri hattı' },
              ].map((p) => (
                <button
                  key={p.key}
                  onClick={() => {
                    onSelectPreset(p.key);
                    setPresetDropdownOpen(false);
                  }}
                  className="w-full text-left px-3 py-2 hover:bg-slate-800 flex flex-col transition-colors border-b border-slate-800/50 last:border-0"
                >
                  <span className="font-semibold text-slate-200">{p.title}</span>
                  <span className="text-[10px] text-slate-400">{p.sub}</span>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Audio Pitch Toggle */}
        <button
          onClick={toggleAudio}
          className={`p-2 rounded-xl border text-xs transition-colors ${
            audioEnabled
              ? 'bg-amber-500/20 text-amber-300 border-amber-500/60'
              : 'bg-slate-900 text-slate-400 border-slate-800 hover:text-white'
          }`}
          title={audioEnabled ? 'Dedektör Sesi Açık' : 'Dedektör Sesi Kapalı'}
        >
          {audioEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
        </button>

        {/* Detailed Anomaly Report & Export Modal Button */}
        <button
          onClick={onOpenAnalysis}
          className="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-750 text-slate-200 text-xs font-semibold border border-slate-700 flex items-center gap-1.5 transition-colors"
        >
          <FileText className="w-3.5 h-3.5 text-cyan-400" />
          <span>Saha Raporu</span>
        </button>
      </div>
    </header>
  );
};
