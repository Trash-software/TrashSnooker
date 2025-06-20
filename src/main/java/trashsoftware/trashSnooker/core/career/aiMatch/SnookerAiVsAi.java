package trashsoftware.trashSnooker.core.career.aiMatch;

import trashsoftware.trashSnooker.core.Game;
import trashsoftware.trashSnooker.core.person.PlayerPerson;
import trashsoftware.trashSnooker.core.ai.AiCueResult;
import trashsoftware.trashSnooker.core.ai.AiPlayStyle;
import trashsoftware.trashSnooker.core.career.Career;
import trashsoftware.trashSnooker.core.career.championship.Championship;
import trashsoftware.trashSnooker.core.career.championship.SnookerChampionship;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.snooker.MaximumType;

import java.util.ArrayList;
import java.util.List;

public class SnookerAiVsAi extends AiVsAi {

    private static final int[] COLORED_BALL_POOL = {
            2, 3, 4, 5, 5, 5, 5, 6, 6, 6, 7, 7, 7, 7, 7, 7
    };
    private final int nReds;
    private final List<Integer> p1SinglePoles = new ArrayList<>();
    private final List<Integer> p2SinglePoles = new ArrayList<>();
    private final List<int[]> frameScores = new ArrayList<>();

    public SnookerAiVsAi(Career p1, Career p2, Championship championship, String matchId, int totalFrames, int nReds) {
        super(p1, p2, championship, matchId, totalFrames);
        
        this.nReds = nReds;
    }

    @Override
    protected void simulateOneFrame() {
        roughSimulateWholeGame();
    }

    private int randomColorBall() {
        return COLORED_BALL_POOL[random.nextInt(COLORED_BALL_POOL.length)];
    }

    private void roughSimulateWholeGame() {
        int redRem = nReds;
        int singlePoleScore = 0;

        double ballTypeBadness = (random.nextDouble() + 1) / 2;  // 球形好不好，越小越好

        SimPlayer sp1 = new SimPlayer(p1, ability1, 1, ballTypeBadness,
                AiCueResult.calculateFramePsyDivisor(
                        Game.frameImportance(1, totalFrames, getP1WinFrames(), getP2WinFrames(), GameRule.SNOOKER),
                        p1.getPlayerPerson().getPsyRua()
                ));
        SimPlayer sp2 = new SimPlayer(p2, ability2, 2, ballTypeBadness,
                AiCueResult.calculateFramePsyDivisor(
                        Game.frameImportance(2, totalFrames, getP1WinFrames(), getP2WinFrames(), GameRule.SNOOKER),
                        p2.getPlayerPerson().getPsyRua()
                ));

        SimPlayer playing = sp1;

        int target = 1;
//        boolean redTarget = true;
        boolean lastIsAttack = false;
        int cuesCount = 0;
        while (target < 8) {
            if (cuesCount > 1000) {
                break;  // 打累了，拜拜
            }
            SimPlayer oppo = playing == sp1 ? sp2 : sp1;
            boolean upHand = singlePoleScore == 0;  // 上手一球
            boolean attack = false;
            boolean goodPos = false;
            if (upHand) {
                if (redRem * 8 + 27 < Math.abs(sp1.score - sp2.score)) {
                    break;  // 超分了直接结束
                }
                if (lastIsAttack) {  // 上一杆是对手进攻失败，会漏球
                    if (random.nextDouble() * 105 < playing.aiPlayStyle.attackPrivilege) {
                        attack = true;
                    }
                } else {
                    boolean oppoDefSuc = random.nextDouble() * 105 < oppo.aiPlayStyle.defense;
                    if (!oppoDefSuc) {
                        if (random.nextDouble() * 90 < playing.aiPlayStyle.attackPrivilege) {
                            attack = true;
                        }
                    }
                }
            } else {
                // 连续进攻
                double position = random.nextDouble() * 100;
                double powerNeed = random.nextDouble() * 90;
                if (position < playing.goodPosition) {
                    goodPos = true;
                    attack = true;
                } else if (position < playing.position) {
                    goodPos = false;
                    attack = random.nextDouble() * 80 < playing.aiPlayStyle.attackPrivilege;
                } else {
                    attack = false;
                }
                if (attack) {
                    if (powerNeed > playing.career.getPlayerPerson().getPrimaryHand().getMaxPowerPercentage()) {
                        attack = false;
                    } else if (goodPos) {
                        if (powerNeed > playing.career.getPlayerPerson().getPrimaryHand().getControllablePowerPercentage()) {
                            goodPos = false;
                        }
                    }
                }
            }

            if (attack) {
//                System.out.println(goodPos);
                playing.totalAttacks++;
                boolean attackSuc = randomAttackSuccess(
                        playing.career.getPlayerPerson(),
                        playing.playerNum,
                        playing.ra,
                        goodPos,
                        playing.framePsyDivisor,
                        (redRem - 1) * 8 + 27 < playing.score - oppo.score &&
                                redRem * 8 + 27 >= playing.score - oppo.score);

//                System.out.println(playing.career.getPlayerPerson().getPlayerId() + " " + target + " " + redRem + " " + attackSuc);
                if (attackSuc) {
                    playing.sucAttacks++;
                    int newScore;
                    if (target == 1) {
                        newScore = 1;
                        redRem--;
                        target = 0;
                    } else if (target == 0) {
                        newScore = randomColorBall();
                        target = redRem == 0 ? 2 : 1;
                    } else {
                        newScore = target;
                        target++;
                    }
                    singlePoleScore += newScore;
                    playing.score += newScore;
                    playing.updateMaxSingle(singlePoleScore);
                } else {
                    playing = oppo;
                    target = redRem == 0 ? target : 1;
                    singlePoleScore = 0;
                }
            } else {
                playing = oppo;
                target = redRem == 0 ? target : 1;
                singlePoleScore = 0;
            }
            lastIsAttack = attack;
            cuesCount++;
        }
        if (sp1.score > sp2.score) {
            p1WinsAFrame();
        } else if (sp1.score < sp2.score) {
            p2WinsAFrame();
        } else {
            // 延分争黑
            double total = sp1.career.getPlayerPerson().psyNerve + sp2.career.getPlayerPerson().psyNerve;
            double p1psy = sp1.career.getPlayerPerson().psyNerve / total;
            if (random.nextDouble() < p1psy) {
                sp1.score += 7;
                p1WinsAFrame();
            } else {
                sp2.score += 7;
                p2WinsAFrame();
            }
        }
        frameScores.add(new int[]{sp1.score, sp2.score});
        if (printDebug) {
            System.out.println(sp1);
            System.out.println(sp2);
        }

        SnookerChampionship sc = (SnookerChampionship) championship;
        sc.updateBreakScore(sp1.career.getPlayerPerson().getPlayerId(),
                championship.getCurrentStage(),
                sp1.maxSinglePole,
                MaximumType.inferFromScore(nReds, sp1.maxSinglePole),
                true,
                matchId,
                currentFrameNumberFrom1() - 1);  // 因为上面已经更新了
        sc.updateBreakScore(sp2.career.getPlayerPerson().getPlayerId(),
                championship.getCurrentStage(),
                sp2.maxSinglePole,
                MaximumType.inferFromScore(nReds, sp2.maxSinglePole),
                true,
                matchId,
                currentFrameNumberFrom1() - 1);
    }

    private int getP1Score() {
        return p1SinglePoles.stream().reduce(0, Integer::sum);
    }

    private int getP2Score() {
        return p2SinglePoles.stream().reduce(0, Integer::sum);
    }

    private static class SimPlayer {
        final Career career;
        final PlayerPerson.ReadableAbility ra;
        final PlayerPerson.ReadableAbilityHand raPrimary;
        final AiPlayStyle aiPlayStyle;
        final double goodPosition;
        final double position;
        final int playerNum;
        final double framePsyDivisor;
        int score;
        int totalAttacks;
        int sucAttacks;
        int maxSinglePole;

        SimPlayer(Career career, PlayerPerson.ReadableAbility ra, int playerNum, double ballBadness,
                  double framePsyDivisor) {
            this.career = career;
            this.playerNum = playerNum;
            this.ra = ra;
            this.raPrimary = ra.primary();
            this.aiPlayStyle = career.getPlayerPerson().getAiPlayStyle();
            double posDif = 100 - (aiPlayStyle.position * (raPrimary.spinControl + raPrimary.powerControl) / 200);
            this.goodPosition = 100 - posDif * ballBadness;
            this.position = 100 - (100 - goodPosition) / 3;
            this.framePsyDivisor = framePsyDivisor;
        }

        void updateMaxSingle(int single) {
            if (single > maxSinglePole) {
                maxSinglePole = single;
            }
        }

        @Override
        public String toString() {
            return "SimPlayer{" +
                    career.getPlayerPerson().getPlayerId() +
                    ", goodPosition=" + goodPosition +
                    ", position=" + position +
                    ", score=" + score +
                    ", totalAttacks=" + totalAttacks +
                    ", sucAttacks=" + sucAttacks +
                    ", maxSinglePole=" + maxSinglePole +
                    '}';
        }
    }
}
