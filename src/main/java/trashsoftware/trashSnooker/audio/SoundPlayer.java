package trashsoftware.trashSnooker.audio;

public class SoundPlayer {
//    public static final SoundMixer POT_SOUND = new SoundMixer(
//            true,
//            new SoundMixer.Track("E6", 100, 0.2, false),
//            new SoundMixer.Track("A5", 100, 0.3, false),
//            new SoundMixer.Track("C5", 200, 0.6, false),
//            new SoundMixer.Track("D4", 200, 0.8, false),
//            new SoundMixer.Track("G3", 100, 1.0, false)
//    );
    public static final SoundMixer COLLISION_SOUND = new SoundMixer(
            true,
            new SoundMixer.Track("E6", 100, 0.2, false),
            new SoundMixer.Track("A5", 100, 0.3, false),
            new SoundMixer.Track("C5", 200, 0.6, false),
            new SoundMixer.Track("D4", 200, 0.8, false),
            new SoundMixer.Track("G3", 100, 1.0, false)
    );

    public static void main(String[] args) {
        COLLISION_SOUND.play(1.0);
    }
    
    static void playCushionSound(double vol) {
        System.out.println("Cushion " + vol);
    }
    
    static void playPotSound(double vol) {
        System.out.println("Pot " + vol);
    }
}
