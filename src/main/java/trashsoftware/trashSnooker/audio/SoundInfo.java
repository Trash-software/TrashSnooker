package trashsoftware.trashSnooker.audio;

public class SoundInfo {

    public final SoundType soundType;
    public final PowerType powerType;
    private double volume = 1.0;

    public SoundInfo(SoundType soundType, PowerType powerType) {
        this.soundType = soundType;
        this.powerType = powerType;
    }

    public static SoundInfo bySpeed(SoundType type, double speedRatio) {
        double vol = 1.0;
        PowerType pt = switch (type) {
            case CUE_SOUND -> {
                // todo: temp
                vol = Math.pow(speedRatio, 1.0);
                System.out.printf("Cue sound speed %.4f, vol %.4f\n", speedRatio, vol);
                if (speedRatio < 0.1) {
                    yield PowerType.SMALL;
                } else if (speedRatio < 0.3) {
                    yield PowerType.MID;
                } else {
                    yield PowerType.BIG;
                }
            }
            case CUSHION -> {
                vol = Math.pow(speedRatio, 0.5);
                yield PowerType.MID;
            }
            case BALL_COLLISION -> {
                if (speedRatio < 0.1) {
                    yield PowerType.SMALL;
                } else {
                    vol = Math.pow(speedRatio, 0.5);
                    yield PowerType.MID;
                }
            }
            case POCKET_BACK -> {
                if (speedRatio > 0.15) {
                    vol = Math.pow(speedRatio, 0.75);
                    yield PowerType.MID;
                } else {
                    yield PowerType.SMALL;
                }
            }

            default -> PowerType.MID;
        };
        SoundInfo soundInfo = new SoundInfo(type, pt);
        soundInfo.setVolume(vol);
        return soundInfo;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public double getVolume() {
        return volume;
    }

    public enum SoundType {
        BALL_COLLISION,
        CUE_SOUND,
        MISCUE_SOUND,
        POT,
        POCKET_BACK,
        CUSHION
    }

    public enum PowerType {
        BIG,
        MID,
        SMALL,
    }
}
