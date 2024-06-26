package trashsoftware.trashSnooker.audio;

import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueBrand;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.res.ResourcesLoader;
import trashsoftware.trashSnooker.res.SoundFile;
import trashsoftware.trashSnooker.util.EventLogger;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioPlayerManager {
    
    private static AudioPlayerManager instance;

    private ExecutorService executorService;
    
    AudioPlayerManager() {
        executorService = Executors.newCachedThreadPool();
    }

    public static AudioPlayerManager getInstance() {
        if (instance == null) {
            instance = new AudioPlayerManager();
        }
        return instance;
    }

    public void play(SoundInfo soundInfo,
                     GameValues gameValues,
                     Cue cue,
                     SoundRecorder soundRecorder,
                     int timeInMs) {
        SoundFile info = createSound(soundInfo, gameValues, cue);
        playSound(info, soundInfo, soundRecorder, timeInMs);
    }

    public SoundFile createSound(SoundInfo soundInfo,
                                 GameValues gameValues,
                                 Cue cue) {
        return switch (soundInfo.soundType) {
            case CUE_SOUND -> {
                // todo
                if (cue.getBrand().material == CueBrand.Material.HARD_WOOD) {
                    yield ResourcesLoader.getInstance().getCueSoundSmallMidPower();
                } else if (cue.getBrand().material == CueBrand.Material.SOFT_WOOD) {
                    yield ResourcesLoader.getInstance().getCueSoundSmallMidPower();
                } else if (cue.getBrand().material == CueBrand.Material.CARBON) {
                    yield ResourcesLoader.getInstance().getCueSoundCarbonMidPower();
                }
                yield null;
            }
            case MISCUE_SOUND -> {
                // todo
                yield ResourcesLoader.getInstance().getMiscueSoundGeneral();
            }
            case BALL_COLLISION -> {
                if (soundInfo.powerType == SoundInfo.PowerType.SMALL) {
                    yield ResourcesLoader.getInstance().getBallSoundSnookerLight();
                }
                yield ResourcesLoader.getInstance().getBallSoundSnookerLoud();
            }
            case POT -> {
                yield null;
            }
            case CUSHION -> {
                // todo
                yield ResourcesLoader.getInstance().getCushionSnooker();
            }
            case POCKET_BACK -> {
                if (soundInfo.powerType == SoundInfo.PowerType.SMALL) yield null;
                
                // todo
                yield ResourcesLoader.getInstance().getPocketBackGeneral();
            }
        };
    }

    public void playSound(SoundFile soundFile, 
                          SoundInfo soundInfo, 
                          SoundRecorder soundRecorder,
                          int timeInMs) {
        if (soundFile != null) {
//            long t0 = System.currentTimeMillis();
            AudioPlayer audioPlayer = new AudioPlayer(soundFile, soundInfo);
//            long t1 = System.currentTimeMillis();
//            System.out.println("Thread time: " + (t1 - t0));
            Thread thread = new Thread(() -> {
                try {
//                    System.out.println("Sound time: " + (System.currentTimeMillis() - t1));
                    if (soundRecorder == null) {
                        audioPlayer.play();
                    } else {
                        audioPlayer.playToStream(soundRecorder, timeInMs);
                    }
                } catch (IOException | UnsupportedAudioFileException e) {
                    EventLogger.error(e);
                }
            });
            thread.start();
        } else {
            EventLogger.warning("No wave info provided");
        }
    }
}
