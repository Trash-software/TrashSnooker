package trashsoftware.trashSnooker.core.career.aiMatch;

import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.ai.AiPlayStyle;
import trashsoftware.trashSnooker.core.career.Career;
import trashsoftware.trashSnooker.core.career.ChampionshipData;

public class ChineseAiVsAi extends AiVsAi {
    boolean p1break = true;
    
    public ChineseAiVsAi(Career p1, Career p2, ChampionshipData data, int totalFrames) {
        super(p1, p2, data, totalFrames);
    }

    @Override
    protected void simulateOneFrame(boolean isFinalFrame) {
//        gaussianRandom(isFinalFrame);
        roughSimulateWholeGame(isFinalFrame);
    }

    private void roughSimulateWholeGame(boolean isFinalFrame) {
        double ballTypeBadness = (random.nextDouble() + 1) / 2;  // 球形好不好，越小越好

        SimPlayer sp1 = new SimPlayer(p1, ability1, ballTypeBadness);
        SimPlayer sp2 = new SimPlayer(p2, ability2, ballTypeBadness);

        SimPlayer playing = p1break ? sp1 : sp2;
        
        // 开球
        boolean breakSuc = random.nextDouble() * 75.0 < playing.ra.maxPower;
        if (breakSuc) {
            if (random.nextDouble() < 0.5) sp1.remCount--;
            else sp2.remCount--;
        } else {
            playing = playing == sp1 ? sp2 : sp1;
        }
        
        SimPlayer lastCued = playing;

        int cuesCount = 0;
        boolean lastIsAttack = false;
        while (sp1.remCount > 0 && sp2.remCount > 0) {
            if (cuesCount > 1000) {
                break;  // 打累了，拜拜
            }

            SimPlayer oppo = playing == sp1 ? sp2 : sp1;
            boolean upHand = lastCued == oppo;
            lastCued = playing;
            
            boolean attack = false;
            boolean goodPos = false;
            
            if (upHand) {
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
                    if (powerNeed > playing.career.getPlayerPerson().getMaxPowerPercentage()) {
                        attack = false;
                    } else if (goodPos) {
                        if (powerNeed > playing.career.getPlayerPerson().getControllablePowerPercentage()) {
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
                        playing.ra,
                        goodPos,
                        isFinalFrame,
                        playing.remCount == 1);

//                System.out.println(playing.career.getPlayerPerson().getPlayerId() + " " + target + " " + redRem + " " + attackSuc);
                if (attackSuc) {
                    playing.sucAttacks++;
                    playing.remCount--;
                } else {
                    playing = oppo;
                }
            } else {
                playing = oppo;
            }
            lastIsAttack = attack;
            cuesCount++;
        }
        
        if (sp1.remCount == 0) {
            p1WinFrames++;
        } else if (sp2.remCount == 0) {
            p2WinFrames++;
        } else {
            throw new RuntimeException("Why two people finished");
        }
    }

    private static class SimPlayer {
        final Career career;
        final PlayerPerson.ReadableAbility ra;
        final AiPlayStyle aiPlayStyle;
        final double goodPosition;
        final double position;
        int remCount = 8;
        int totalAttacks;
        int sucAttacks;

        SimPlayer(Career career, PlayerPerson.ReadableAbility ra, double ballBadness) {
            this.career = career;
            this.ra = ra;
            this.aiPlayStyle = career.getPlayerPerson().getAiPlayStyle();
            double posDif = 100 - (aiPlayStyle.position * (ra.spinControl + ra.powerControl) / 200);
            this.goodPosition = 100 - posDif * ballBadness;
            this.position = 100 - (100 - goodPosition) / 3;
        }

        @Override
        public String toString() {
            return "SimPlayer{" +
                    career.getPlayerPerson().getName() +
                    ", goodPosition=" + goodPosition +
                    ", position=" + position +
                    ", remCount=" + remCount +
                    ", totalAttacks=" + totalAttacks +
                    ", sucAttacks=" + sucAttacks +
                    '}';
        }
    }
}
