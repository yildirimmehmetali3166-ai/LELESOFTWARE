import { PaletteType } from '../types/scanner';

export interface RGB {
  r: number;
  g: number;
  b: number;
}

// Maps normalized value [-100, 100] to RGB (0-255)
export function getPaletteColor(value: number, palette: PaletteType): RGB {
  // Normalize to 0..1
  const t = Math.max(0, Math.min(1, (value + 100) / 200));

  switch (palette) {
    case 'nicea_spectrum':
      // Blue (void -100) -> Cyan -> Green (neutral 0) -> Yellow -> Orange -> Red/Gold (metal +100)
      if (t < 0.2) {
        // Deep purple to royal blue (Chamber/Void)
        const sub = t / 0.2;
        return {
          r: Math.round(30 + sub * 10),
          g: Math.round(20 + sub * 60),
          b: Math.round(140 + sub * 115)
        };
      } else if (t < 0.4) {
        // Blue to cyan
        const sub = (t - 0.2) / 0.2;
        return {
          r: Math.round(40 - sub * 40),
          g: Math.round(80 + sub * 140),
          b: Math.round(255 - sub * 40)
        };
      } else if (t < 0.6) {
        // Cyan to green (neutral ground)
        const sub = (t - 0.4) / 0.2;
        return {
          r: Math.round(0 + sub * 60),
          g: Math.round(220 - sub * 20),
          b: Math.round(215 - sub * 195)
        };
      } else if (t < 0.8) {
        // Green to yellow-orange (ferrous / bronze)
        const sub = (t - 0.6) / 0.2;
        return {
          r: Math.round(60 + sub * 190),
          g: Math.round(200 - sub * 40),
          b: Math.round(20 - sub * 20)
        };
      } else {
        // Orange to radiant red/gold (precious / gold)
        const sub = (t - 0.8) / 0.2;
        return {
          r: 255,
          g: Math.round(160 - sub * 130),
          b: Math.round(sub * 30)
        };
      }

    case 'thermal_jet':
      // Classic Jet: Blue -> Cyan -> Green -> Yellow -> Red
      if (t < 0.25) {
        const sub = t / 0.25;
        return { r: 0, g: Math.round(sub * 255), b: 255 };
      } else if (t < 0.5) {
        const sub = (t - 0.25) / 0.25;
        return { r: 0, g: 255, b: Math.round(255 * (1 - sub)) };
      } else if (t < 0.75) {
        const sub = (t - 0.5) / 0.25;
        return { r: Math.round(sub * 255), g: 255, b: 0 };
      } else {
        const sub = (t - 0.75) / 0.25;
        return { r: 255, g: Math.round(255 * (1 - sub)), b: 0 };
      }

    case 'magma':
      // Black -> Purple -> Orange -> Bright Pale Yellow
      return {
        r: Math.round(Math.min(255, 300 * Math.pow(t, 1.2))),
        g: Math.round(Math.min(255, 270 * Math.pow(t, 2.2))),
        b: Math.round(Math.min(255, 120 + 135 * Math.sin(t * Math.PI)))
      };

    case 'gold_rush':
      // Dark slate -> Charcoal -> Amber -> Pure Gold -> Bright White-Gold
      if (t < 0.5) {
        const sub = t / 0.5;
        return {
          r: Math.round(20 + sub * 180),
          g: Math.round(25 + sub * 140),
          b: Math.round(35 - sub * 20)
        };
      } else {
        const sub = (t - 0.5) / 0.5;
        return {
          r: Math.round(200 + sub * 55),
          g: Math.round(165 + sub * 75),
          b: Math.round(15 + sub * 200)
        };
      }

    case 'emerald_void':
      // Focus on void detection (Deep blue/violet for cavities, dark emerald for earth, gold for metals)
      if (t < 0.4) {
        const sub = t / 0.4;
        return { r: Math.round(100 * (1 - sub)), g: Math.round(30 * (1 - sub)), b: Math.round(240 * (1 - sub * 0.4)) };
      } else if (t < 0.7) {
        const sub = (t - 0.4) / 0.3;
        return { r: Math.round(20 + sub * 30), g: Math.round(120 + sub * 60), b: Math.round(60 - sub * 30) };
      } else {
        const sub = (t - 0.7) / 0.3;
        return { r: Math.round(50 + sub * 205), g: Math.round(180 + sub * 30), b: Math.round(30 + sub * 20) };
      }

    default:
      return { r: 120, g: 120, b: 120 };
  }
}

export function rgbToHex(rgb: RGB): string {
  const r = rgb.r.toString(16).padStart(2, '0');
  const g = rgb.g.toString(16).padStart(2, '0');
  const b = rgb.b.toString(16).padStart(2, '0');
  return `#${r}${g}${b}`;
}

export function rgbToThreeColor(rgb: RGB): number {
  return (rgb.r << 16) + (rgb.g << 8) + rgb.b;
}
