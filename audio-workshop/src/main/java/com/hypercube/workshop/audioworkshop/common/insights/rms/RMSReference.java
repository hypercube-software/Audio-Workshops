package com.hypercube.workshop.audioworkshop.common.insights.rms;

public enum RMSReference {
    /**
     * Sine wave (AES-17 standard)
     * <a href="https://masteringelectronicsdesign.com/how-to-derive-the-rms-value-of-a-sine-wave-with-a-dc-offset/">See calculation</a>
     */
    SINE_WAVE_AES_17,
    /**
     * Square wave (scientific standard)
     * <a href="https://masteringelectronicsdesign.com/how-to-derive-the-rms-value-of-pulse-and-square-waveforms/">See calculation</a>
     */
    SQUARE_WAVE,
    /**
     * Triangle wave <a href="https://masteringelectronicsdesign.com/how-to-derive-the-rms-value-of-a-triangle-waveform/">See calculation</a>
     */
    TRIANGLE_WAVE
}
