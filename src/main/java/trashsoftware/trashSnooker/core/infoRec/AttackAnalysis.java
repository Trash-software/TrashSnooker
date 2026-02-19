package trashsoftware.trashSnooker.core.infoRec;

import trashsoftware.trashSnooker.core.attempt.PotAttempt;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.core.person.PlayerHand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unchecked")
public class AttackAnalysis {

    protected List<PotAttemptRec>[] playerAttempts = new List[2];

    AttackAnalysis() {
        for (int i = 0; i < playerAttempts.length; i++) playerAttempts[i] = new ArrayList<>();
    }

    protected void addAttempt(PotAttemptRec attemptRec, int playerNumFrom1) {
        playerAttempts[playerNumFrom1 - 1].add(attemptRec);
    }

    public boolean isEmpty() {
        return Arrays.stream(playerAttempts).allMatch(List::isEmpty);
    }

    public List<PotAttemptRec>[] filterBy(AttackSpecial special) {
        if (special == null) return Arrays.copyOf(playerAttempts, playerAttempts.length);
        else {
            List<PotAttemptRec>[] res = new List[playerAttempts.length];
            for (int i = 0; i < res.length; i++) {
                res[i] = new ArrayList<>();
                for (PotAttemptRec par : playerAttempts[i]) {
                    if (par.specials.contains(special)) res[i].add(par);
                }
            }
            return res;
        }
    }

    public static class PotAttemptRec {

        public final CueInfoRec cueInfoRec;
        public final CueInfoRec.PotInfo potInfo;
        public final List<AttackSpecial> specials = new ArrayList<>();
        public final int frameIndex;
        private boolean success;

        private PotAttemptRec(CueInfoRec cir, CueInfoRec.PotInfo potInfo, int frameIndex) {
            this.cueInfoRec = cir;
            this.potInfo = potInfo;
            this.frameIndex = frameIndex;
        }

        public static PotAttemptRec create(CueInfoRec cir, 
                                           TableMetrics tableMetrics,
                                           int frameIndex) {
            CueInfoRec.PotInfo potInfo = cir.potInfo;
            if (potInfo == null)
                throw new RuntimeException("PotInfo cannot be null here: should be checked prior to this.");
            PotAttemptRec par = new PotAttemptRec(cir, potInfo, frameIndex);
            par.success = cir.legallyPot();
            if (potInfo.isDouble()) par.specials.add(AttackSpecial.DOUBLE_ATTACK);
            if (PotAttempt.isLongPot(tableMetrics, potInfo.whiteTarDt() + potInfo.tarPocketDt())) {
                par.specials.add(AttackSpecial.LONG);
            }
            if (cir.hand.hand == PlayerHand.Hand.REST) {
                par.specials.add(AttackSpecial.REST);
            }
//            if (cir.hand.)
            return par;
        }

        public boolean isSuccess() {
            return success;
        }
    }

    public enum AttackSpecial {
        LONG,
        REST,
        ANTI_HAND,  // Not implemented yet
        DOUBLE_ATTACK,
        OFF_THE_CUSHION;  // Not implemented yet
    }
}
