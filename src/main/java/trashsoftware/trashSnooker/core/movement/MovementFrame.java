package trashsoftware.trashSnooker.core.movement;

public class MovementFrame {
    public static final int NORMAL = 0;
    public static final int CUSHION = 1;
    public static final int POT = 2;
    public static final int COLLISION = 3;
    
    public final double x;
    public final double y;
    public final double axisX;  // 都是以度为标准
    public final double axisY;
    public final double axisZ;
    public final double rotateDeg;
    public final boolean potted;
    public final int movementType;  // 记录这一帧是否有碰库等情况
    public final double movementValue;

//    public MovementFrame(double x, double y, boolean potted) {
//        this(x, y, potted, NORMAL, 0.0);
//    }

    public MovementFrame(double x, double y,
                         double axisX, double axisY, double axisZ, double rotateDeg,
                         boolean potted, int movementType, double movementValue) {
        this.x = x;
        this.y = y;
        this.axisX = axisX;
        this.axisY = axisY;
        this.axisZ = axisZ;
        this.rotateDeg = rotateDeg;
        this.potted = potted;
        this.movementType = movementType;
        this.movementValue = movementValue;
    }
}
