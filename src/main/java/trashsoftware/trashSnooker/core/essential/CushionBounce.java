package trashsoftware.trashSnooker.core.essential;

public class CushionBounce extends Bounce {

    private final double px, py;   // a point on cushion
    private final double nx, ny;   // outward unit normal

    public CushionBounce(double px, double py,
                         double nx, double ny,
                         BounceParams params) {
        super(params);
        this.px = px;
        this.py = py;

        double len = Math.hypot(nx, ny);
        this.nx = nx / len;
        this.ny = ny / len;
    }

    @Override
    protected Contact computeContact(Ball ball) {

        double dx = ball.x - px;
        double dy = ball.y - py;

        double dist = dx * nx + dy * ny;

        double penetration = ball.radius - dist;

        if (penetration <= 0) return null;

        return new Contact(nx, ny, penetration);
    }
}
