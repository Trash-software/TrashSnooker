package trashsoftware.trashSnooker.core.numberedGames.chineseEightBall;

import trashsoftware.trashSnooker.core.PlayerPerson;

import java.util.Map;

/**
 * 让球，不知道怎么翻译
 */
public enum LetBall {
    FRONT,  // todo: 让前和让中均未实装
    MID,
    BACK;
    
    public static int magicScore(PlayerPerson person) {
        int magicScore = 0;
        if (person.getSex() == PlayerPerson.Sex.F) magicScore += 1;
        if (person.isUnderage()) magicScore += 1;
        
        return magicScore;
    }
    
    public static void chineseEightLetBall(PlayerPerson p1, Map<LetBall, Integer> p1Let,
                                           PlayerPerson p2, Map<LetBall, Integer> p2Let) {
        int p1Magic = magicScore(p1);
        int p2Magic = magicScore(p2);
        
        int diff = p1Magic - p2Magic;
        if (diff < -2) {
            p2Let.put(BACK, 2);
        } else if (diff > 2) {
            p1Let.put(BACK, 2);
        }
        
        switch (diff) {
            case -2:
                p2Let.put(FRONT, 1);
                p2Let.put(BACK, 1);
                break;
            case -1:
                p2Let.put(BACK, 1);
                break;
            case 0:
                break;
            case 1:
                p1Let.put(BACK, 1);
                break;
            case 2:
                p1Let.put(FRONT, 1);
                p1Let.put(BACK, 1);
                break;
        }
    }
}
