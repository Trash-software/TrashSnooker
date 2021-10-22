package trashsoftware.trashSnooker.core;

public class SavedPrediction {

    public final double whiteX;
    public final double whiteY;

    public final Ball predictHitBall;
    public final double ballX;
    public final double ballY;

    public final double whiteHitX;
    public final double whiteHitY;

    public SavedPrediction(double whiteX, double whiteY,
                           Ball hitBall,
                           double ballX, double ballY,
                           double whiteHitX, double whiteHitY) {
        this.whiteX = whiteX;
        this.whiteY = whiteY;
        this.predictHitBall = hitBall;
        this.ballX = ballX;
        this.ballY = ballY;
        this.whiteHitX = whiteHitX;
        this.whiteHitY = whiteHitY;
    }

    public SavedPrediction(double whiteX, double whiteY, double directionX, double directionY) {
        this(
                whiteX, whiteY, null,
                0.0, 0.0,
                whiteX + directionX * 3000, whiteY + directionY * 3000
        );
    }
}
