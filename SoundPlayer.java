import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SoundPlayer programmatically synthesizes retro 8-bit sound effects.
 * It is refactored to use a dedicated single-threaded daemon ExecutorService
 * to play sounds asynchronously without the overhead and risks of spawning raw threads.
 */
public class SoundPlayer {
    private static final float SAMPLE_RATE = 8000f; // Low-sample rate for retro chiptune feel

    // Thread pool to play sounds sequentially on a background thread
    private static final ExecutorService soundExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("Audio-Effect-Thread");
        thread.setDaemon(true); // Ensures JVM can exit even if sound is queued/playing
        return thread;
    });

    /**
     * Synthesizes a frequency sweep tone and streams it to the audio hardware.
     * Queues and runs on the single-threaded sound executor.
     *
     * @param startFreq  starting frequency in Hz.
     * @param endFreq    ending frequency in Hz.
     * @param durationMs duration of the sound in milliseconds.
     */
    public static void playToneAsync(int startFreq, int endFreq, int durationMs) {
        soundExecutor.submit(() -> {
            try {
                // Audio Format: 8000Hz, 8-bit, Mono, Signed, Little-Endian
                AudioFormat af = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
                SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
                
                sdl.open(af);
                sdl.start();

                int samples = (int) (SAMPLE_RATE * durationMs / 1000f);
                byte[] buffer = new byte[samples];
                double phase = 0;

                for (int i = 0; i < samples; i++) {
                    double progressFraction = (double) i / samples;
                    double currentFreq = startFreq + progressFraction * (endFreq - startFreq);
                    
                    phase += 2.0 * Math.PI * currentFreq / SAMPLE_RATE;
                    if (phase > 2.0 * Math.PI) {
                        phase -= 2.0 * Math.PI;
                    }
                    
                    buffer[i] = (byte) (Math.sin(phase) * 127.0);
                }

                // Write sound buffer and play
                sdl.write(buffer, 0, buffer.length);
                sdl.drain(); // Blocks the background audio-thread temporarily, not the UI thread
                sdl.close();
            } catch (LineUnavailableException e) {
                // Fail silently if sound hardware is busy or unavailable
                System.err.println("Audio line unavailable: " + e.getMessage());
            } catch (Exception e) {
                // Catch any other unexpected exceptions to prevent thread leakage
                System.err.println("Error playing sound: " + e.getMessage());
            }
        });
    }

    /**
     * Plays a rising beep sound when the snake eats food.
     */
    public static void playEatSound() {
        playToneAsync(400, 800, 120);
    }

    /**
     * Plays a descending low buzz sound on death.
     */
    public static void playGameOverSound() {
        playToneAsync(250, 80, 450);
    }

    /**
     * Plays a brief click sound.
     */
    public static void playClickSound() {
        playToneAsync(600, 600, 40);
    }
}
