package trashsoftware.trashSnooker.core;

import org.junit.Test;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.CareerSave;
import trashsoftware.trashSnooker.core.career.aiMatch.SnookerAiVsAi;
import trashsoftware.trashSnooker.util.DataLoader;

import java.io.IOException;
import java.util.Calendar;

public class AiVsAiTest {
    
    @Test
    public void testCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(1993, 2, 31);
        calendar.set(Calendar.MONTH, 3);
        System.out.println(calendar.get(Calendar.YEAR));
        System.out.println(calendar.get(Calendar.MONTH));
        System.out.println(calendar.get(Calendar.DAY_OF_MONTH));
    }
    
    @Test
    public void testCareerManager() throws IOException {
        CareerSave cs = CareerManager.createNew(DataLoader.getInstance().getPlayerPerson("Mou Moumou"));
        CareerManager.setCurrentSave(cs);
        CareerManager careerManager = CareerManager.getInstance();
        if (careerManager == null) {
            PlayerPerson player = DataLoader.getInstance().getPlayerPerson("Mou Moumou");
            CareerManager.createNew(player);
        }
        var top = CareerManager.getInstance().getSnookerTopN(16);
        System.out.println(top);
    }
    
    @Test
    public void testSnooker() {
        SnookerAiVsAi aiVsAi = new SnookerAiVsAi(
                CareerManager.getInstance().findCareerByPlayerId("Trump"),
                CareerManager.getInstance().findCareerByPlayerId("Mou Moumou"),
                9);
        aiVsAi.simulate();

        System.out.println(aiVsAi);
    }

    @Test
    public void testSnookerMultiple() {
        int p1Wins = 0;
        for (int i = 0; i < 100; i++) {
            SnookerAiVsAi aiVsAi = new SnookerAiVsAi(
                    CareerManager.getInstance().findCareerByPlayerId("Trump"),
                    CareerManager.getInstance().findCareerByPlayerId("Mou Moumou"),
                    9);
            aiVsAi.simulate();
            if (aiVsAi.getWinner() == aiVsAi.getP1()) p1Wins++;
        }

        System.out.println(p1Wins);
    }
}
