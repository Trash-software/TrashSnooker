package trashsoftware.trashSnooker.core.snooker;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.metrics.GameValues;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SnookerPlayer extends Player {

    private final AbstractSnookerGame game;
    private final List<Integer> singlePoleScores = new ArrayList<>();
    private boolean flushed = false;
    private MaximumType maximumType = MaximumType.NONE;

    public SnookerPlayer(InGamePlayer playerPerson, AbstractSnookerGame game) {
        super(playerPerson);

        this.game = game;
    }

    public void potFreeBall(int freeBallScore) {
        score += freeBallScore;
        AchManager.getInstance().cumulateAchievement(Achievement.SNOOKER_CUMULATE_SCORE,
                freeBallScore,
                getInGamePlayer());
        lastAddedScore = freeBallScore;
        Ball freeBall = game.getBallOfValue(freeBallScore);
        if (singlePole.containsKey(freeBall)) {
            singlePole.put(freeBall, singlePole.get(freeBall) + 1);
        } else {
            singlePole.put(freeBall, 1);
        }
    }

    @Override
    protected void addScoreOfPotted(Collection<? extends Ball> pottedBalls) {
        int add = 0;
        for (Ball ball : pottedBalls) {
            add += ball.getValue();
        }
        AchManager.getInstance().cumulateAchievement(Achievement.SNOOKER_CUMULATE_SCORE,
                add,
                getInGamePlayer());
        score += add;
    }

    public int getSinglePoleScore() {
        int singlePoleScore = 0;
        for (Map.Entry<Ball, Integer> entry : singlePole.entrySet()) {
            singlePoleScore += entry.getKey().getValue() * entry.getValue();
        }
        return singlePoleScore;
    }

    @Override
    public void clearSinglePole() {
        singlePoleScores.add(getSinglePoleScore());
        super.clearSinglePole();
    }

    @Override
    public void correctPotBalls(Game<?, ?> game, Collection<? extends Ball> pottedBalls) {
        super.correctPotBalls(game, pottedBalls);

        int singlePoleScore = getSinglePoleScore();
        if (singlePoleScore >= 50) {
            AchManager.getInstance().addAchievement(Achievement.SNOOKER_BREAK_50, getInGamePlayer());
            if (singlePoleScore >= 100) {
                AchManager.getInstance().addAchievement(Achievement.SNOOKER_BREAK_100, getInGamePlayer());
                if (singlePoleScore >= 135) {
                    AchManager.getInstance().addAchievement(Achievement.SNOOKER_BREAK_100_BIG, getInGamePlayer());
                }
            }
            MaximumType curMaxType = checkMaximum((AbstractSnookerGame) game);
            if (maximumType == null || maximumType == MaximumType.NONE) maximumType = curMaxType;
            else if (curMaxType == MaximumType.MAXIMUM_167) maximumType = curMaxType;
            
            if (maximumType != MaximumType.NONE) {
                switch (maximumType) {
                    case MAXIMUM_167 -> {
                        AchManager.getInstance().addAchievement(Achievement.SNOOKER_BREAK_147, getInGamePlayer());
                        AchManager.getInstance().addAchievement(Achievement.SNOOKER_BREAK_167, getInGamePlayer());
                    }
                    case MAXIMUM_147 ->
                            AchManager.getInstance().addAchievement(Achievement.SNOOKER_BREAK_147, getInGamePlayer());
                    case MAXIMUM_107 ->
                            AchManager.getInstance().addAchievement(Achievement.SNOOKER_TEN_BREAK_107, getInGamePlayer());
                    case MAXIMUM_75 ->
                            AchManager.getInstance().addAchievement(Achievement.MINI_SNOOKER_BREAK_75, getInGamePlayer());

                }
            }
        }
    }

    private MaximumType checkMaximum(AbstractSnookerGame game) {
        GameValues gameValues = game.getGameValues();
        int nReds = switch (gameValues.rule) {
            case SNOOKER -> 15;
            case SNOOKER_TEN -> 10;
            case MINI_SNOOKER -> 6;
            default -> throw new RuntimeException();
        };
        Map<Ball, Integer> singlePole = getSinglePole();
        int pottedReds = singlePole.get(game.getBallOfValue(1));
        if (pottedReds == nReds || pottedReds == nReds + 1) {
            for (int i = 2; i < 7; i++) {
                // 每个彩球各有一个
                if (singlePole.get(game.getBallOfValue(i)) != 1) {
                    return MaximumType.NONE;
                }
            }
            int blackCount = singlePole.get(game.getBallOfValue(7));
            if ((pottedReds == nReds && blackCount == nReds + 1) ||
                    (pottedReds == nReds + 1 && blackCount == nReds + 2)) {
                return switch (gameValues.rule) {
                    case SNOOKER -> {
                        if (gameValues.hasSubRule(SubRule.SNOOKER_GOLDEN)) {
                            if (singlePole.get(game.getBallOfValue(20)) == 1) {
                                yield MaximumType.MAXIMUM_167;
                            }
                        }
                        yield MaximumType.MAXIMUM_147;
                    }
                    case SNOOKER_TEN -> MaximumType.MAXIMUM_107;
                    case MINI_SNOOKER -> MaximumType.MAXIMUM_75;
                    default -> throw new RuntimeException();
                };
            }
        }
        return MaximumType.NONE;
    }

    public void flushSinglePoles() {
        System.out.println(getPlayerPerson().getName() +
                " Before flush " + singlePoleScores + ", current: " + getSinglePole());
        if (!singlePole.isEmpty() && !flushed) {
            singlePoleScores.add(getSinglePoleScore());
            flushed = true;
            System.out.println("Flushed!");
        }
        System.out.println("After flush " + singlePoleScores + ", current: " + getSinglePole());
    }

    public List<Integer> getSinglePolesInThisGame() {
        return singlePoleScores;
    }

    public boolean hasMaximumInThisGame() {
        return maximumType != null && maximumType != MaximumType.NONE;
    }

    public MaximumType getMaximumType() {
        return maximumType;
    }
}
