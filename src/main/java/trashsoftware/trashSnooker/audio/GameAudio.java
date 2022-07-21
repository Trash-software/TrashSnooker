package trashsoftware.trashSnooker.audio;

import trashsoftware.trashSnooker.core.GameValues;
import trashsoftware.trashSnooker.core.Values;

public class GameAudio {
    
    public static void hitCushion(GameValues values, double v) {
        double soundRatio = 0.3 + 0.7 * (v / Values.MAX_POWER_SPEED);
        SoundPlayer.playCushionSound(soundRatio);
    }

    public static void pot(GameValues values, double v) {
        double soundRatio = 0.1 + 0.9 * (v / Values.MAX_POWER_SPEED);
        SoundPlayer.playPotSound(soundRatio);
    }
}
