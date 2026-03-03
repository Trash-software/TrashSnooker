package trashsoftware.trashSnooker.core.essential;

import trashsoftware.trashSnooker.core.metrics.TableMetrics;

public class BounceParams {
    public static BounceParams DEFAULT = new BounceParams(60, 0.85, 0.12, 10);
    
    //    public final double tickSeconds;
    public final double hardness;       // spring stiffness (k)

    /**
     * American pool: 0.78 – 0.88 (start 0.83)
     * Snooker: 0.80 – 0.90 (start 0.86)
     * Chinese 8-ball: 0.80 – 0.90 (start 0.85)
     * Russian pyramid: 0.78 – 0.90 (start 0.85)
     */
    public final double bounciness;     // 0..1

    /**
     * Pool (American): 0.10 – 0.20 (start 0.14)
     * Snooker: 0.08 – 0.16 (start 0.12)
     * Chinese 8-ball (plays closer to snooker generally): 0.08 – 0.16 (start 0.12–0.14)
     * Russian pyramid: 0.10 – 0.20 (start 0.14)
     */
    public final double friction;       // tangential friction coefficient

    /**
     * American pool (K-66 style, “crisper” feel):
     * maxPenetration ≈ 0.02R – 0.05R
     * <p>
     * Snooker (lively but not mushy):
     * maxPenetration ≈ 0.03R – 0.07R
     * <p>
     * Chinese 8-ball:
     * maxPenetration ≈ 0.03R – 0.07R
     * <p>
     * Russian pyramid:
     * maxPenetration ≈ 0.02R – 0.06R
     */
    public final double maxPenetration; // safety clamp

    public BounceParams(double hardness,
                        double bounciness,
                        double friction,
                        double maxPenetration) {
        this.hardness = hardness;
        this.bounciness = bounciness;
        this.friction = friction;
        this.maxPenetration = maxPenetration;
    }
}
