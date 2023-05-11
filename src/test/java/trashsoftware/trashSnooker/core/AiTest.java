package trashsoftware.trashSnooker.core;

import org.junit.Test;
import trashsoftware.trashSnooker.core.ai.AiPlayStyle;
import trashsoftware.trashSnooker.core.metrics.*;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.phy.TableCloth;
import trashsoftware.trashSnooker.util.DataLoader;

public class AiTest {
    
    @Test
    public void testAiPlayerPrice() {
        PlayerPerson pp = null;
        for (PlayerPerson p : DataLoader.getInstance().getAllPlayers()) {
            if (p.getPlayerId().equals("Trump")) {
                pp = p;
                break;
            }
        }
        InGamePlayer inGamePlayer = new InGamePlayer(
                pp,
                DataLoader.getInstance().getCues().get("stdSnookerCue"),
                PlayerType.COMPUTER, 
                1,
                1.0
        );
        AiPlayStyle aps = inGamePlayer.getPlayerPerson().getAiPlayStyle();
        System.out.println(aps.priceOf(
                new double[]{0.0, 0.0}, 5.0, inGamePlayer, GamePlayStage.NORMAL
                ));
    }

    @Test
    public void testDistanceEstimation() {
        GameValues values = new GameValues(GameRule.SNOOKER, 
                TableMetrics.TableBuilderFactory.SNOOKER.create().holeSize(new PocketSize("mid", 85, 92)).build(), 
                BallMetrics.SNOOKER_BALL);
        Phy phy = Phy.Factory.createPlayPhy(new TableCloth(TableCloth.Goodness.GOOD, TableCloth.Smoothness.NORMAL));
        System.out.println(values.estimatedMoveDistance(phy, CuePlayParams.getSpeedOfPower(35, 0)));
    }
}
