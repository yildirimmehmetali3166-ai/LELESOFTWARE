/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useMemo } from 'react';
import { 
  ScanGrid, 
  PaletteType, 
  ViewMode, 
  DetectedAnomaly 
} from './types/scanner';
import { 
  generateAntikMezarOdasi, 
  generateAltinKupu, 
  generateTunelVeMagara, 
  detectAnomalies 
} from './utils/scanGenerators';
import { Navbar } from './components/Navbar';
import { ThreeCanvas } from './components/ThreeCanvas';
import { TwoDView } from './components/TwoDView';
import { LiveScanner } from './components/LiveScanner';
import { DevelopmentPlan } from './components/DevelopmentPlan';
import { BottomControls } from './components/BottomControls';
import { AnalysisModal } from './components/AnalysisModal';

export default function App() {
  // Navigation & Active Scan
  const [activeTab, setActiveTab] = useState<'3d' | '2d' | 'live' | 'plan'>('3d');
  const [currentGrid, setCurrentGrid] = useState<ScanGrid>(() => generateAntikMezarOdasi());

  // 3D Visualizer Parameters
  const [palette, setPalette] = useState<PaletteType>('nicea_spectrum');
  const [viewMode, setViewMode] = useState<ViewMode>('surface');
  const [depthCutMeters, setDepthCutMeters] = useState<number>(7.5);
  const [thresholdFilter, setThresholdFilter] = useState<number>(15);
  const [wireframe, setWireframe] = useState<boolean>(false);
  const [cameraPreset, setCameraPreset] = useState<'iso' | 'top' | 'side' | 'front'>('iso');
  const [selectedAnomalyId, setSelectedAnomalyId] = useState<string | null>(null);
  const [isControlsCollapsed, setIsControlsCollapsed] = useState<boolean>(false);
  const [isAnalysisOpen, setIsAnalysisOpen] = useState<boolean>(false);

  // Compute detected anomalies
  const anomalies = useMemo(() => {
    return detectAnomalies(currentGrid);
  }, [currentGrid]);

  const handleSelectPreset = (presetKey: string) => {
    setSelectedAnomalyId(null);
    if (presetKey === 'gold') {
      setCurrentGrid(generateAltinKupu());
    } else if (presetKey === 'tunel') {
      setCurrentGrid(generateTunelVeMagara());
    } else {
      setCurrentGrid(generateAntikMezarOdasi());
    }
  };

  const handleLiveScanCompleted = (newGrid: ScanGrid) => {
    setCurrentGrid(newGrid);
    setSelectedAnomalyId(null);
    setActiveTab('3d');
  };

  const handleSelectAnomaly = (anomaly: DetectedAnomaly | null) => {
    setSelectedAnomalyId(anomaly ? anomaly.id : null);
  };

  return (
    <div className="flex flex-col w-screen h-screen bg-slate-950 text-slate-100 overflow-hidden font-sans">
      {/* Top Tactical Navigation Bar */}
      <Navbar
        activeTab={activeTab}
        onSelectTab={setActiveTab}
        currentGrid={currentGrid}
        onSelectPreset={handleSelectPreset}
        onOpenAnalysis={() => setIsAnalysisOpen(true)}
      />

      {/* Main Content Viewport */}
      <main className="relative flex-1 w-full h-full overflow-hidden">
        {/* Tab 1: 3D Subsurface Visualizer */}
        {activeTab === '3d' && (
          <div className="relative w-full h-full">
            <ThreeCanvas
              grid={currentGrid}
              palette={palette}
              viewMode={viewMode}
              depthCutMeters={depthCutMeters}
              thresholdFilter={thresholdFilter}
              wireframe={wireframe}
              selectedAnomalyId={selectedAnomalyId}
              onSelectAnomaly={handleSelectAnomaly}
              anomalies={anomalies}
              cameraPreset={cameraPreset}
              isCinematic={isControlsCollapsed}
            />

            {/* Bottom Docked Floating Controls (Three.js skill directive) */}
            <BottomControls
              palette={palette}
              onSelectPalette={setPalette}
              viewMode={viewMode}
              onSelectViewMode={setViewMode}
              depthCutMeters={depthCutMeters}
              onChangeDepthCut={setDepthCutMeters}
              thresholdFilter={thresholdFilter}
              onChangeThreshold={setThresholdFilter}
              wireframe={wireframe}
              onToggleWireframe={() => setWireframe(!wireframe)}
              cameraPreset={cameraPreset}
              onSelectCamera={setCameraPreset}
              isCollapsed={isControlsCollapsed}
              onToggleCollapse={() => setIsControlsCollapsed(!isControlsCollapsed)}
            />
          </div>
        )}

        {/* Tab 2: 2D Plan & Cross-Section View */}
        {activeTab === '2d' && (
          <div className="w-full h-full">
            <TwoDView
              grid={currentGrid}
              palette={palette}
              anomalies={anomalies}
              selectedAnomalyId={selectedAnomalyId}
              onSelectAnomaly={handleSelectAnomaly}
            />
          </div>
        )}

        {/* Tab 3: Live Sensor Scan Walking Simulator */}
        {activeTab === 'live' && (
          <div className="w-full h-full">
            <LiveScanner
              onScanCompleted={handleLiveScanCompleted}
              currentGrid={currentGrid}
            />
          </div>
        )}

        {/* Tab 4: Python & Android Development Architecture & Plan */}
        {activeTab === 'plan' && (
          <div className="w-full h-full">
            <DevelopmentPlan />
          </div>
        )}
      </main>

      {/* Detailed Analysis & Report Modal */}
      <AnalysisModal
        isOpen={isAnalysisOpen}
        onClose={() => setIsAnalysisOpen(false)}
        grid={currentGrid}
        anomalies={anomalies}
      />
    </div>
  );
}
