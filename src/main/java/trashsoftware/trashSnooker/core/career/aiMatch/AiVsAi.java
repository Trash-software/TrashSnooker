package trashsoftware.trashSnooker.core.career.aiMatch;

import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.ai.AiPlayStyle;
import trashsoftware.trashSnooker.core.career.Career;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.ChampionshipData;

import java.util.Random;

public abstract class AiVsAi {

    protected final int totalFrames;
    protected final ChampionshipData data;
    protected final Career p1;
    protected final Career p2;
    protected final double potDifficulty;
    protected AiPlayStyle aps1;
    protected PlayerPerson.ReadableAbility ability1;
    protected AiPlayStyle aps2;
    protected PlayerPerson.ReadableAbility ability2;
    protected int p1WinFrames;
    protected int p2WinFrames;
    protected Career winner;
    protected Random random = new Random();

    public AiVsAi(Career p1, Career p2, ChampionshipData data, int totalFrames) {
        this.p1 = p1;
        this.p2 = p2;
        this.aps1 = p1.getPlayerPerson().getAiPlayStyle();
        this.ability1 = PlayerPerson.ReadableAbility.fromPlayerPerson(
                p1.getPlayerPerson(),
                p1.getHandFeelEffort(data.getType()));
        this.totalFrames = totalFrames;
        this.aps2 = p2.getPlayerPerson().getAiPlayStyle();
        this.ability2 = PlayerPerson.ReadableAbility.fromPlayerPerson(
                p2.getPlayerPerson(),
                p2.getHandFeelEffort(data.getType()));

        this.data = data;

        this.potDifficulty = calculatePotDifficulty(data);

        assert totalFrames % 2 == 1;
    }

    private static double calculatePotDifficulty(ChampionshipData data) {
        ChampionshipData.TableSpec tableSpec = data.getTableSpec();
        double ballSize = tableSpec.ballMetrics.ballDiameter;
        double pocketSize = tableSpec.tableMetrics.cornerHoleDiameter;
        double ratio = 1 - (pocketSize - ballSize) / ballSize;
        // 用平方
        return ratio * tableSpec.tableMetrics.maxLength * tableSpec.tableMetrics.maxLength / 
                6070364 / CareerManager.getInstance().getAiGoodness();
    }

    public static double playerSimpleWinningScore(PlayerPerson playerPerson,
                                                     AiPlayStyle aps,
                                                     PlayerPerson.ReadableAbility ability,
                                                     boolean isFinalFrame) {
        return ability.aiming * ability.cuePrecision / 100 +
                ability.normalPower * ability.powerControl / 10000 * 0.4 +
                ability.spin * ability.spinControl / 100 * 0.7 +
                aps.position / 100 +
                aps.defense / 100 * 0.5 +
                aps.stability / 100 +
                playerPerson.getSolving() / 100 * 0.2 +
                (isFinalFrame ? (playerPerson.psy / 20) : (playerPerson.psy / 200));
    }

    protected void gaussianRandom(boolean isFinalFrame) {
        double p1s = playerSimpleWinningScore(p1.getPlayerPerson(), aps1, ability1, isFinalFrame);
        double p2s = playerSimpleWinningScore(p2.getPlayerPerson(), aps2, ability2, isFinalFrame);
        double total = p1s + p2s;

//        System.out.println("p1 sc " + p1s + ", p2 sc " + p2s);

        double p1WinLimit = ((p1s / total) - 0.5) * 2.5 * 2;  // x2是因为正态分布标准差[-1,1]

        double gau = random.nextGaussian();

        if (gau < p1WinLimit) {
            p1WinFrames++;
        } else {
            p2WinFrames++;
        }
    }

    protected abstract void simulateOneFrame(boolean isFinalFrame);

    protected boolean randomAttackSuccess(PlayerPerson person,
                                          PlayerPerson.ReadableAbility ra,
                                          boolean goodPosition,
                                          boolean isFinalFrame,
                                          boolean isKeyBall) {

        double psyFactor = 1.0;
        if (isKeyBall) {
            psyFactor = person.psy / 100;
        }
        if (isFinalFrame) {
            psyFactor *= (200 + person.psy) / 300;
        }
        double difficulty = potDifficulty * (goodPosition ? 1 : 3);
        double failRatio = 10000 - ra.aiming * ra.cuePrecision * psyFactor;
        failRatio /= 10000;
        failRatio *= 0.25;
        failRatio *= difficulty;
        if (random.nextDouble() * 100 > person.getAiPlayStyle().stability) {
            // 失误
            failRatio *= 2;
        }
        failRatio = Math.min(0.97, failRatio);  // 乱打也能混进去吧
        return random.nextDouble() > failRatio;
    }

    public void simulate() {
        int half = totalFrames / 2 + 1;
        for (int i = 0; i < totalFrames; i++) {
            simulateOneFrame(p1WinFrames == half - 1 && p2WinFrames == half - 1);

            if (p1WinFrames >= half) {
                winner = p1;
                break;
            }
            if (p2WinFrames >= half) {
                winner = p2;
                break;
            }
        }
    }

    public int getP1WinFrames() {
        return p1WinFrames;
    }

    public int getP2WinFrames() {
        return p2WinFrames;
    }

    public Career getWinner() {
        return winner;
    }

    public Career getP1() {
        return p1;
    }

    public Career getP2() {
        return p2;
    }

    @Override
    public String toString() {
        return p1.getPlayerPerson().getPlayerId() +
                " " + p1WinFrames + " : " +
                p2WinFrames + " " +
                p2.getPlayerPerson().getPlayerId();
    }
}
