package trashsoftware.trashSnooker.audio;

import trashsoftware.trashSnooker.core.TableMetrics;
import trashsoftware.trashSnooker.core.Values;

public class GameAudio {
    
    public static void hitCushion(TableMetrics values, double v) {
        double soundRatio = 0.3 + 0.7 * (v / Values.MAX_POWER_SPEED);
        SoundPlayer.playCushionSound(soundRatio);
    }

    public static void pot(TableMetrics values, double v) {
        double soundRatio = 0.1 + 0.9 * (v / Values.MAX_POWER_SPEED);
        SoundPlayer.playPotSound(soundRatio);
    }
}
