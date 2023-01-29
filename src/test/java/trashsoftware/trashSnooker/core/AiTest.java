package trashsoftware.trashSnooker.core;

import org.junit.Test;
import trashsoftware.trashSnooker.core.ai.AiPlayStyle;
import trashsoftware.trashSnooker.util.DataLoader;

public class AiTest {
    
    @Test
    public void testAiPlayerPrice() {
        PlayerPerson pp = null;
        for (PlayerPerson p : DataLoader.getInstance().getAllPlayers()) {
            if (p.getName().equals("Trump")) {
                pp = p;
                break;
            }
        }
        InGamePlayer inGamePlayer = new InGamePlayer(
                pp,
                DataLoader.getInstance().getCues().get("stdSnookerCue"),
                PlayerType.COMPUTER, 
                1
        );
        AiPlayStyle aps = inGamePlayer.getPlayerPerson().getAiPlayStyle();
        System.out.println(aps.priceOf(
                new double[]{0.0, 0.0}, 5.0, inGamePlayer, GamePlayStage.NORMAL
                ));
    }
}
