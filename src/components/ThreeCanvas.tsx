import React, { useEffect, useRef, useState, useCallback } from 'react';
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { RoomEnvironment } from 'three/addons/environments/RoomEnvironment.js';
import { ScanGrid, PaletteType, ViewMode, DetectedAnomaly } from '../types/scanner';
import { getPaletteColor, rgbToThreeColor } from '../utils/colorScales';

interface ThreeCanvasProps {
  grid: ScanGrid;
  palette: PaletteType;
  viewMode: ViewMode;
  depthCutMeters: number; // 0 to 12
  thresholdFilter: number; // 0 to 100
  wireframe: boolean;
  selectedAnomalyId: string | null;
  onSelectAnomaly: (anomaly: DetectedAnomaly | null) => void;
  anomalies: DetectedAnomaly[];
  cameraPreset: 'iso' | 'top' | 'side' | 'front';
  isCinematic: boolean;
}

export const ThreeCanvas: React.FC<ThreeCanvasProps> = ({
  grid,
  palette,
  viewMode,
  depthCutMeters,
  thresholdFilter,
  wireframe,
  selectedAnomalyId,
  onSelectAnomaly,
  anomalies,
  cameraPreset,
  isCinematic,
}) => {
  const mountRef = useRef<HTMLDivElement>(null);
  const sceneRef = useRef<THREE.Scene | null>(null);
  const rendererRef = useRef<THREE.WebGLRenderer | null>(null);
  const cameraRef = useRef<THREE.PerspectiveCamera | null>(null);
  const controlsRef = useRef<OrbitControls | null>(null);
  const groupRef = useRef<THREE.Group | null>(null);
  const anomalyMarkersRef = useRef<THREE.Group | null>(null);
  const reqIdRef = useRef<number>(0);
  const [hoveredCell, setHoveredCell] = useState<{ x: number; y: number; val: number; depth: number } | null>(null);

  // Setup Three.js scene once
  useEffect(() => {
    if (!mountRef.current) return;
    const container = mountRef.current;
    const width = container.clientWidth || 800;
    const height = container.clientHeight || 500;

    // Detect software WebGL
    const isSoftwareGl = (() => {
      try {
        const gl = document.createElement('canvas').getContext('webgl2');
        if (!gl) return true;
        const ext = gl.getExtension('WEBGL_debug_renderer_info');
        const param = ext ? ext.UNMASKED_RENDERER_WEBGL : gl.RENDERER;
        const gpu = String(gl.getParameter(param));
        gl.getExtension('WEBGL_lose_context')?.loseContext();
        return /SwiftShader|llvmpipe|softpipe|Software|Basic Render/i.test(gpu);
      } catch {
        return false;
      }
    })();

    const scene = new THREE.Scene();
    scene.background = new THREE.Color(0x080c14); // Deep geological dark slate
    scene.fog = new THREE.FogExp2(0x080c14, 0.025);
    sceneRef.current = scene;

    const camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 150);
    camera.position.set(12, 10, 16);
    cameraRef.current = camera;

    const renderer = new THREE.WebGLRenderer({
      antialias: !isSoftwareGl,
      powerPreference: 'high-performance',
      alpha: false,
    });
    renderer.setSize(width, height);
    renderer.setPixelRatio(isSoftwareGl ? 1 : Math.min(window.devicePixelRatio, 2));
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.15;
    renderer.shadowMap.enabled = !isSoftwareGl;
    rendererRef.current = renderer;

    container.appendChild(renderer.domElement);

    // Studio Environment
    const pmremGenerator = new THREE.PMREMGenerator(renderer);
    scene.environment = pmremGenerator.fromScene(new RoomEnvironment(), 0.04).texture;
    pmremGenerator.dispose();

    // Lighting
    const hemiLight = new THREE.HemisphereLight(0x94b4d6, 0x1f2937, 0.85);
    scene.add(hemiLight);

    const dirLight = new THREE.DirectionalLight(0xfff7ed, 1.4);
    dirLight.position.set(15, 20, 12);
    dirLight.castShadow = !isSoftwareGl;
    scene.add(dirLight);

    const fillLight = new THREE.DirectionalLight(0x38bdf8, 0.5);
    fillLight.position.set(-15, 8, -12);
    scene.add(fillLight);

    // Controls
    const controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.06;
    controls.maxDistance = 50;
    controls.minDistance = 2;
    controls.target.set(0, -1.5, 0);
    controlsRef.current = controls;

    // Scan Root Group
    const scanGroup = new THREE.Group();
    scene.add(scanGroup);
    groupRef.current = scanGroup;

    // Anomaly Markers Group
    const markerGroup = new THREE.Group();
    scene.add(markerGroup);
    anomalyMarkersRef.current = markerGroup;

    // Resize Observer
    const resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width: w, height: h } = entry.contentRect;
        if (w > 0 && h > 0 && cameraRef.current && rendererRef.current) {
          cameraRef.current.aspect = w / h;
          cameraRef.current.updateProjectionMatrix();
          rendererRef.current.setSize(w, h);
        }
      }
    });
    resizeObserver.observe(container);

    // Animation Loop
    const animate = () => {
      reqIdRef.current = requestAnimationFrame(animate);
      controls.update();
      renderer.render(scene, camera);
    };
    animate();

    return () => {
      cancelAnimationFrame(reqIdRef.current);
      resizeObserver.disconnect();
      if (renderer.domElement && container.contains(renderer.domElement)) {
        container.removeChild(renderer.domElement);
      }
      renderer.dispose();
    };
  }, []);

  // Update Camera Presets smoothly
  useEffect(() => {
    if (!cameraRef.current || !controlsRef.current) return;
    const cam = cameraRef.current;
    const ctrl = controlsRef.current;
    const center = new THREE.Vector3(0, -1.5, 0);
    ctrl.target.copy(center);

    switch (cameraPreset) {
      case 'top':
        cam.position.set(0, 18, 0.001);
        break;
      case 'side':
        cam.position.set(18, -1.0, 0);
        break;
      case 'front':
        cam.position.set(0, -1.0, 18);
        break;
      case 'iso':
      default:
        cam.position.set(11, 9, 14);
        break;
    }
    ctrl.update();
  }, [cameraPreset]);

  // Rebuild 3D Mesh / Voxels when grid, palette, viewMode, or filters change
  const buildSceneObjects = useCallback(() => {
    if (!groupRef.current || !sceneRef.current) return;
    const group = groupRef.current;

    // Clear previous mesh objects
    while (group.children.length > 0) {
      const child = group.children[0];
      group.remove(child);
      if ((child as THREE.Mesh).geometry) {
        (child as THREE.Mesh).geometry.dispose();
      }
      if ((child as THREE.Mesh).material) {
        const mat = (child as THREE.Mesh).material;
        if (Array.isArray(mat)) mat.forEach((m) => m.dispose());
        else mat.dispose();
      }
    }

    const { width, height, stepDistanceMeters, points } = grid;
    const totalW = width * stepDistanceMeters;
    const totalH = height * stepDistanceMeters;
    const offsetX = -totalW / 2;
    const offsetZ = -totalH / 2;

    // 1. Base Grid Plane / Coordinate Boundary
    const gridHelper = new THREE.GridHelper(Math.max(totalW, totalH) * 1.3, Math.max(width, height), 0x0ea5e9, 0x1e293b);
    gridHelper.position.y = 0.01;
    group.add(gridHelper);

    // 2. Subsurface Bounding Box Cage (representing excavation cube 0 to -10m)
    const boxDepth = 7; // 7 meters depth visualization
    const boxGeo = new THREE.BoxGeometry(totalW, boxDepth, totalH);
    const boxWireMat = new THREE.MeshBasicMaterial({
      color: 0x334155,
      wireframe: true,
      transparent: true,
      opacity: 0.35,
    });
    const boxCage = new THREE.Mesh(boxGeo, boxWireMat);
    boxCage.position.set(0, -boxDepth / 2, 0);
    group.add(boxCage);

    // Depth Ruler Markers (-1m, -2m, -3m, -5m)
    for (let d = 1; d <= 6; d++) {
      const rulerGeo = new THREE.BufferGeometry().setFromPoints([
        new THREE.Vector3(offsetX - 0.3, -d, offsetZ - 0.3),
        new THREE.Vector3(offsetX + 0.1, -d, offsetZ - 0.3),
      ]);
      const rulerLine = new THREE.Line(
        rulerGeo,
        new THREE.LineBasicMaterial({ color: d % 2 === 0 ? 0x38bdf8 : 0x64748b })
      );
      group.add(rulerLine);
    }

    // 3. Render Mode: Surface Topography (Mesh)
    if (viewMode === 'surface' || viewMode === 'wireframe') {
      const geom = new THREE.PlaneGeometry(
        totalW,
        totalH,
        Math.max(width - 1, 1),
        Math.max(height - 1, 1)
      );
      geom.rotateX(-Math.PI / 2);

      const posAttr = geom.attributes.position;
      const count = posAttr.count;
      const colors = new Float32Array(count * 3);

      for (let i = 0; i < count; i++) {
        const gx = i % width;
        const gy = Math.floor(i / width);
        const pt = points[gy] ? points[gy][gx] : null;
        const sig = pt ? pt.filteredSignal : 0;

        // Displace Y subtly based on signal
        const yElevation = (sig / 100) * 0.9;
        posAttr.setY(i, yElevation);

        // Color based on palette
        const rgb = getPaletteColor(sig, palette);
        colors[i * 3] = rgb.r / 255;
        colors[i * 3 + 1] = rgb.g / 255;
        colors[i * 3 + 2] = rgb.b / 255;
      }

      geom.setAttribute('color', new THREE.BufferAttribute(colors, 3));
      geom.computeVertexNormals();

      const mat = new THREE.MeshStandardMaterial({
        vertexColors: true,
        wireframe: wireframe || viewMode === 'wireframe',
        roughness: 0.35,
        metalness: 0.2,
        side: THREE.DoubleSide,
      });

      const surfaceMesh = new THREE.Mesh(geom, mat);
      surfaceMesh.castShadow = true;
      surfaceMesh.receiveShadow = true;
      group.add(surfaceMesh);

      // Bottom skirt walls to give solid block look
      const skirtMat = new THREE.MeshStandardMaterial({
        color: 0x0f172a,
        roughness: 0.8,
        transparent: true,
        opacity: 0.85,
        side: THREE.DoubleSide,
      });
      const skirtGeo = new THREE.BoxGeometry(totalW, boxDepth * 0.4, totalH);
      const skirtMesh = new THREE.Mesh(skirtGeo, skirtMat);
      skirtMesh.position.set(0, -boxDepth * 0.2, 0);
      group.add(skirtMesh);
    }

    // 4. Render Mode: Subsurface Voxels / Volumetric Columns
    if (viewMode === 'voxels' || viewMode === 'iso_anomalies') {
      // Create instanced voxels
      const voxelSize = stepDistanceMeters * 0.85;
      const boxGeo = new THREE.BoxGeometry(voxelSize, voxelSize * 0.8, voxelSize);

      // Collect qualifying points
      const validPoints: { x: number; y: number; z: number; signal: number; depth: number }[] = [];

      for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
          const pt = points[y][x];
          const sig = pt.filteredSignal;
          const absSig = Math.abs(sig);

          // Threshold filtering
          if (absSig < thresholdFilter) continue;

          // Target actual depth
          const targetDepth = pt.depth > 0 ? pt.depth : 0.8 + (absSig / 100) * 3.5;

          // Depth slice cut plane
          if (targetDepth > depthCutMeters) continue;

          if (viewMode === 'iso_anomalies') {
            // Only render prominent void/metal anomalies
            if (absSig >= 35) {
              validPoints.push({
                x: offsetX + (x + 0.5) * stepDistanceMeters,
                z: offsetZ + (y + 0.5) * stepDistanceMeters,
                y: -targetDepth,
                signal: sig,
                depth: targetDepth,
              });
            }
          } else {
            // Full voxel columns from surface down to depth
            const layers = Math.min(6, Math.max(1, Math.round(targetDepth / 0.6)));
            for (let l = 0; l <= layers; l++) {
              const curD = (l / layers) * targetDepth;
              if (curD <= depthCutMeters) {
                // Attenuate signal with depth
                const attSig = sig * (1 - (curD / (targetDepth + 0.5)) * 0.4);
                validPoints.push({
                  x: offsetX + (x + 0.5) * stepDistanceMeters,
                  z: offsetZ + (y + 0.5) * stepDistanceMeters,
                  y: -curD,
                  signal: attSig,
                  depth: curD,
                });
              }
            }
          }
        }
      }

      if (validPoints.length > 0) {
        const instancedMesh = new THREE.InstancedMesh(
          boxGeo,
          new THREE.MeshStandardMaterial({
            roughness: 0.25,
            metalness: 0.4,
            transparent: true,
            opacity: viewMode === 'iso_anomalies' ? 0.92 : 0.8,
            wireframe: wireframe,
          }),
          validPoints.length
        );

        const dummy = new THREE.Object3D();
        const color = new THREE.Color();

        validPoints.forEach((p, idx) => {
          dummy.position.set(p.x, p.y, p.z);
          // Scale anomaly voxels slightly larger for emphasis
          const scale = Math.abs(p.signal) > 60 ? 1.15 : 1.0;
          dummy.scale.set(scale, scale, scale);
          dummy.updateMatrix();
          instancedMesh.setMatrixAt(idx, dummy.matrix);

          const rgb = getPaletteColor(p.signal, palette);
          color.setRGB(rgb.r / 255, rgb.g / 255, rgb.b / 255);
          instancedMesh.setColorAt(idx, color);
        });

        instancedMesh.instanceMatrix.needsUpdate = true;
        if (instancedMesh.instanceColor) instancedMesh.instanceColor.needsUpdate = true;
        instancedMesh.computeBoundingSphere();
        group.add(instancedMesh);
      }
    }

    // 5. Build 3D Anomaly Pinpoint Indicators
    if (anomalyMarkersRef.current) {
      const markers = anomalyMarkersRef.current;
      while (markers.children.length > 0) {
        const child = markers.children[0];
        markers.remove(child);
      }

      anomalies.forEach((anom) => {
        const anomX = offsetX + (anom.gridX + 0.5) * stepDistanceMeters;
        const anomZ = offsetZ + (anom.gridY + 0.5) * stepDistanceMeters;
        const anomY = -anom.depthMeters;

        const isSelected = selectedAnomalyId === anom.id;

        const markerGroup = new THREE.Group();
        markerGroup.position.set(anomX, anomY, anomZ);

        // Vertical laser plumb line from ground to target
        const lineGeo = new THREE.BufferGeometry().setFromPoints([
          new THREE.Vector3(0, anom.depthMeters, 0),
          new THREE.Vector3(0, 0, 0),
        ]);
        const lineMat = new THREE.LineDashedMaterial({
          color: anom.classification === 'void' ? 0x38bdf8 : 0xf59e0b,
          dashSize: 0.2,
          gapSize: 0.1,
          linewidth: 2,
        });
        const line = new THREE.Line(lineGeo, lineMat);
        line.computeLineDistances();
        markerGroup.add(line);

        // Anomaly Bounding Box
        const isVoid = anom.classification === 'void';
        const boxSize = isVoid ? 1.2 : 0.6;
        const targetGeo = new THREE.BoxGeometry(boxSize, boxSize * 0.8, boxSize);
        const targetMat = new THREE.MeshStandardMaterial({
          color: isVoid ? 0x2563eb : 0xd97706,
          emissive: isSelected ? (isVoid ? 0x1d4ed8 : 0xb45309) : 0x000000,
          emissiveIntensity: 0.6,
          roughness: 0.2,
          metalness: isVoid ? 0.1 : 0.85,
          wireframe: false,
          transparent: true,
          opacity: 0.85,
        });
        const targetMesh = new THREE.Mesh(targetGeo, targetMat);
        markerGroup.add(targetMesh);

        // Halo Ring around target
        const ringGeo = new THREE.RingGeometry(boxSize * 0.6, boxSize * 0.85, 24);
        ringGeo.rotateX(-Math.PI / 2);
        const ringMat = new THREE.MeshBasicMaterial({
          color: isSelected ? 0xffffff : (isVoid ? 0x60a5fa : 0xfbbf24),
          side: THREE.DoubleSide,
          transparent: true,
          opacity: 0.7,
        });
        const ringMesh = new THREE.Mesh(ringGeo, ringMat);
        ringMesh.position.y = boxSize * 0.5;
        markerGroup.add(ringMesh);

        markers.add(markerGroup);
      });
    }
  }, [grid, palette, viewMode, depthCutMeters, thresholdFilter, wireframe, anomalies, selectedAnomalyId]);

  useEffect(() => {
    buildSceneObjects();
  }, [buildSceneObjects]);

  // Handle Raycasting on Canvas click to select anomaly or inspect cell
  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!mountRef.current || !cameraRef.current || !sceneRef.current) return;
    const rect = mountRef.current.getBoundingClientRect();
    const mouseX = ((e.clientX - rect.left) / rect.width) * 2 - 1;
    const mouseY = -((e.clientY - rect.top) / rect.height) * 2 + 1;

    // Raycast on anomaly positions
    const raycaster = new THREE.Raycaster();
    raycaster.setFromCamera(new THREE.Vector2(mouseX, mouseY), cameraRef.current);

    const { width, stepDistanceMeters } = grid;
    const totalW = width * stepDistanceMeters;
    const offsetX = -totalW / 2;
    const offsetZ = -(grid.height * stepDistanceMeters) / 2;

    let closestAnom: DetectedAnomaly | null = null;
    let minDist = 1.2;

    anomalies.forEach((anom) => {
      const pos = new THREE.Vector3(
        offsetX + (anom.gridX + 0.5) * stepDistanceMeters,
        -anom.depthMeters,
        offsetZ + (anom.gridY + 0.5) * stepDistanceMeters
      );
      const dist = raycaster.ray.distanceToPoint(pos);
      if (dist < minDist) {
        minDist = dist;
        closestAnom = anom;
      }
    });

    if (closestAnom) {
      onSelectAnomaly(closestAnom);
    }
  };

  return (
    <div className="relative w-full h-full select-none overflow-hidden bg-slate-950">
      {/* Three.js Canvas Mount */}
      <div
        ref={mountRef}
        onPointerDown={handlePointerDown}
        className="w-full h-full cursor-grab active:cursor-grabbing"
      />

      {/* Top Overlay Legend and Coordinate Display */}
      {!isCinematic && (
        <div className="absolute top-3 left-3 pointer-events-none flex flex-col gap-2">
          <div className="bg-slate-900/85 border border-slate-700/60 rounded-lg p-2.5 backdrop-blur-md shadow-xl text-xs text-slate-300 flex items-center gap-4">
            <div>
              <span className="text-slate-400 block text-[10px] uppercase tracking-wider font-semibold">Aktif Tarama</span>
              <span className="font-bold text-amber-400 text-sm">{grid.title}</span>
            </div>
            <div className="h-6 w-px bg-slate-700" />
            <div>
              <span className="text-slate-400 block text-[10px] uppercase tracking-wider font-semibold">Grid Boyutu</span>
              <span className="font-mono text-slate-200">
                {grid.width}x{grid.height} ({ (grid.width * grid.stepDistanceMeters).toFixed(1) }m x { (grid.height * grid.stepDistanceMeters).toFixed(1) }m)
              </span>
            </div>
            <div className="h-6 w-px bg-slate-700" />
            <div>
              <span className="text-slate-400 block text-[10px] uppercase tracking-wider font-semibold">Sensör</span>
              <span className="text-emerald-400 font-semibold">{grid.sensorType}</span>
            </div>
          </div>
        </div>
      )}

      {/* Selected Anomaly HUD Tooltip */}
      {selectedAnomalyId && (
        <div className="absolute top-3 right-3 max-w-xs bg-slate-900/90 border border-amber-500/50 rounded-xl p-3 backdrop-blur-md shadow-2xl text-xs">
          {(() => {
            const anom = anomalies.find((a) => a.id === selectedAnomalyId);
            if (!anom) return null;
            return (
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-amber-400 text-sm">{anom.title}</span>
                  <span
                    className={`px-1.5 py-0.5 rounded text-[10px] font-semibold uppercase ${
                      anom.classification === 'void'
                        ? 'bg-blue-900/80 text-blue-300 border border-blue-500/40'
                        : anom.classification === 'precious'
                        ? 'bg-amber-900/80 text-amber-300 border border-amber-500/40'
                        : 'bg-emerald-900/80 text-emerald-300 border border-emerald-500/40'
                    }`}
                  >
                    {anom.classification === 'void' ? 'Boşluk' : anom.classification === 'precious' ? 'Değerli Metal' : 'Metalik'}
                  </span>
                </div>
                <div className="grid grid-cols-2 gap-2 py-1 text-slate-300">
                  <div className="bg-slate-800/80 rounded p-1.5 border border-slate-700/50">
                    <span className="text-[10px] text-slate-400 block">Derinlik (Z):</span>
                    <span className="font-mono text-base font-bold text-cyan-400">-{anom.depthMeters} m</span>
                  </div>
                  <div className="bg-slate-800/80 rounded p-1.5 border border-slate-700/50">
                    <span className="text-[10px] text-slate-400 block">Sinyal Genliği:</span>
                    <span className="font-mono text-base font-bold text-amber-400">{anom.signalStrength} %</span>
                  </div>
                  <div className="bg-slate-800/80 rounded p-1.5 border border-slate-700/50">
                    <span className="text-[10px] text-slate-400 block">Tahmini Hacim:</span>
                    <span className="font-mono text-slate-200">{anom.volumeM3} m³</span>
                  </div>
                  <div className="bg-slate-800/80 rounded p-1.5 border border-slate-700/50">
                    <span className="text-[10px] text-slate-400 block">Konum (X, Y):</span>
                    <span className="font-mono text-slate-200">{anom.xMeters}m, {anom.yMeters}m</span>
                  </div>
                </div>
                <p className="text-[11px] text-slate-400 italic pt-0.5">{anom.description}</p>
                <button
                  onClick={() => onSelectAnomaly(null)}
                  className="w-full mt-1 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white text-[10px] transition-colors"
                >
                  Seçimi Kapat
                </button>
              </div>
            );
          })()}
        </div>
      )}
    </div>
  );
};
