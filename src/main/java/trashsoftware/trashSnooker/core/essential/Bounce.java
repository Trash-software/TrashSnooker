package trashsoftware.trashSnooker.core.essential;

import trashsoftware.trashSnooker.core.phy.Phy;

public abstract class Bounce implements Cloneable {

    protected final BounceParams params;

    protected Bounce(BounceParams params) {
        this.params = params;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean apply(Ball ball, Phy phy) {
        Contact contact = computeContact(ball);
        if (contact == null) return false;

        double penetration = contact.penetration;
        if (penetration <= 0) return false;

        penetration = Math.min(penetration, params.maxPenetration);

        applyForces(ball, phy, contact.nx, contact.ny, penetration);
        return true;
    }

    protected abstract Contact computeContact(Ball ball);

    protected void applyForces(Ball ball, 
                               Phy phy,
                               double nx, double ny,
                               double penetration) {

        double dt = phy.tickSecond;

        // ---- Normal velocity ----
        double vN = ball.vx * nx + ball.vy * ny;

        // ---- Spring-damper ----
        double k = params.hardness;
        double zeta = 1.0 - params.bounciness;
        double c = 2.0 * Math.sqrt(k) * zeta;

        double Fn = k * penetration - c * vN;
        if (Fn < 0) Fn = 0;

        // ---- Apply normal acceleration ----
        double ax = Fn * nx;
        double ay = Fn * ny;

        ball.vx += ax * dt;
        ball.vy += ay * dt;

        // ---- Tangential interaction ----
        applyTangential(ball, nx, ny, Fn, dt);
    }

    protected void applyTangential(Ball ball,
                                   double nx, double ny,
                                   double normalForce,
                                   double dt) {

        // Tangential direction
        double tx = -ny;
        double ty = nx;

        // Contact point velocity including spin
        // surface velocity from xSpin/ySpin acts opposite direction of surface
        double contactVx = ball.vx - ball.xSpin;
        double contactVy = ball.vy - ball.ySpin;

        // Add side spin effect (perpendicular to normal)
        contactVx += -ball.sideSpin * ny;
        contactVy +=  ball.sideSpin * nx;

        // Tangential component
        double vT = contactVx * tx + contactVy * ty;

        if (Math.abs(vT) < 1e-9) return;

        double frictionMag = params.friction * normalForce;

        double sign = vT > 0 ? -1 : 1;

        double fx = frictionMag * sign * tx;
        double fy = frictionMag * sign * ty;

        // Apply to linear velocity
        ball.vx += fx * dt;
        ball.vy += fy * dt;

        // Apply to spins (torque)
        ball.xSpin -= fx * dt;
        ball.ySpin -= fy * dt;

        // Side spin damping
        ball.sideSpin -= vT * params.friction * dt;
    }

    protected static class Contact {
        public final double nx, ny;
        public final double penetration;

        public Contact(double nx, double ny, double penetration) {
            this.nx = nx;
            this.ny = ny;
            this.penetration = penetration;
        }
    }
}