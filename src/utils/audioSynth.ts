class DetectorAudioSynthesizer {
  private ctx: AudioContext | null = null;
  private osc: OscillatorNode | null = null;
  private gainNode: GainNode | null = null;
  private isEnabled: boolean = false;

  public init() {
    if (this.ctx) return;
    try {
      const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      this.ctx = new AudioCtx();
    } catch {
      // Audio not supported
    }
  }

  public setEnabled(enabled: boolean) {
    this.isEnabled = enabled;
    if (enabled) {
      this.init();
      if (this.ctx && this.ctx.state === 'suspended') {
        this.ctx.resume();
      }
    } else {
      this.stop();
    }
  }

  public getEnabled(): boolean {
    return this.isEnabled;
  }

  public playSignalTone(signalVal: number) {
    if (!this.isEnabled) return;
    this.init();
    if (!this.ctx) return;

    // Normalizing signal (-100 to +100)
    // Cavity (-100..0): Low throaty hum (120Hz to 280Hz)
    // Neutral (0): Subtle pulse (300Hz)
    // Metal (0..100): High piercing detection tone (400Hz to 1800Hz)
    let freq = 350;
    if (signalVal < 0) {
      freq = Math.max(110, 320 + signalVal * 2.1); // drops down to 110Hz on deep void
    } else {
      freq = 350 + Math.pow(signalVal / 100, 1.4) * 1400; // climbs to 1750Hz on gold
    }

    try {
      const now = this.ctx.currentTime;
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      osc.type = signalVal < -20 ? 'sawtooth' : 'sine';
      osc.frequency.setValueAtTime(freq, now);

      const amp = Math.min(0.2, 0.03 + (Math.abs(signalVal) / 100) * 0.17);
      gain.gain.setValueAtTime(amp, now);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.12);

      osc.connect(gain);
      gain.connect(this.ctx.destination);

      osc.start(now);
      osc.stop(now + 0.13);
    } catch {
      // Ignore audio glitches
    }
  }

  public stop() {
    if (this.osc) {
      try {
        this.osc.stop();
        this.osc.disconnect();
      } catch {
        // already stopped
      }
      this.osc = null;
    }
  }
}

export const detectorAudio = new DetectorAudioSynthesizer();
