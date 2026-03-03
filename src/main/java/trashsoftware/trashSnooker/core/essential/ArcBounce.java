package trashsoftware.trashSnooker.core.essential;

public class ArcBounce extends Bounce {

    private final double cx, cy;
    private final double arcRadius;

    public ArcBounce(double cx, double cy,
                     double arcRadius,
                     BounceParams params) {
        super(params);
        this.cx = cx;
        this.cy = cy;
        this.arcRadius = arcRadius;
    }

    @Override
    protected Contact computeContact(Ball ball) {

        double dx = ball.x - cx;
        double dy = ball.y - cy;

        double dist = Math.hypot(dx, dy);
        if (dist < 1e-6) return null;

        double nx = dx / dist;
        double ny = dy / dist;

        double penetration = ball.radius - (dist - arcRadius);
        if (penetration <= 0) return null;

        return new Contact(nx, ny, penetration);
    }
}
