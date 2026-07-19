package com.tetris;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public final class SoundUtil {

    private SoundUtil() {
    }

    public static void playLineClearSound() {
        new Thread(() -> playTone(880, 120)).start();
    }

    private static void playTone(double frequencyHz, int durationMs) {
        try {
            float sampleRate = 44100;
            AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();

            int numSamples = (int) (durationMs / 1000.0 * sampleRate);
            byte[] buffer = new byte[numSamples];
            for (int i = 0; i < numSamples; i++) {
                double angle = 2.0 * Math.PI * i * frequencyHz / sampleRate;
                buffer[i] = (byte) (Math.sin(angle) * 100);
            }

            line.write(buffer, 0, buffer.length);
            line.drain();
            line.close();
        } catch (Exception e) {
            // No audio device available; fail silently.
        }
    }
}
