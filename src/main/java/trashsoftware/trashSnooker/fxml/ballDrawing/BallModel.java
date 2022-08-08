package trashsoftware.trashSnooker.fxml.ballDrawing;

import javafx.geometry.Point3D;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.GameValues;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.snooker.SnookerBall;
import trashsoftware.trashSnooker.fxml.GameView;

import java.util.Random;

public abstract class BallModel {
    
    public final Sphere sphere;
    public final Rotate rotation = new Rotate();
    
    protected BallModel(double physicalRadius) {
        sphere = new Sphere(physicalRadius * GameView.scale);
        
        sphere.getTransforms().add(rotation);
    }
    
    public static BallModel createModel(Ball ball) {
        if (ball instanceof PoolBall) {
            return new PoolBallModel(ball.getValue());
        } else if (ball instanceof SnookerBall) {
            return new SnookerBallModel(ball.getValue());
        } else {
            throw new RuntimeException("No such ball type");
        }
    }
    
    public void setX(double actualX) {
        sphere.setTranslateX(GameView.canvasX(actualX));
    }
    
    public void setY(double actualY) {
        sphere.setTranslateY(GameView.canvasY(actualY));
    }
}
