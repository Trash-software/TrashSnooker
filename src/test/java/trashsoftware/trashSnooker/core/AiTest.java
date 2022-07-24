package trashsoftware.trashSnooker.core;

import org.junit.Test;
import trashsoftware.trashSnooker.core.ai.AiPlayStyle;
import trashsoftware.trashSnooker.util.Recorder;

public class AiTest {
    
    @Test
    public void testAiPlayerPrice() {
        Recorder.loadAll();
        PlayerPerson pp = null;
        for (PlayerPerson p : Recorder.getPlayerPeople()) {
            if (p.getName().equals("Trump")) {
                pp = p;
                break;
            }
        }
        InGamePlayer inGamePlayer = new InGamePlayer(
                pp,
                Recorder.getCues().get("stdSnookerCue"),
                PlayerType.COMPUTER, 
                1
        );
        AiPlayStyle aps = inGamePlayer.getPlayerPerson().getAiPlayStyle();
        System.out.println(aps.priceOf(
                new double[]{0.0, 0.0}, 5.0, inGamePlayer, GamePlayStage.NORMAL
                ));
    }
}
