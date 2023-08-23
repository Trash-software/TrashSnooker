package trashsoftware.trashSnooker.fxml.drawing;

import javafx.geometry.Point3D;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.snooker.SnookerBall;
import trashsoftware.trashSnooker.fxml.widgets.GamePane;

import java.util.Random;

public abstract class BallModel {

    public final Sphere sphere;

    protected BallModel() {
        sphere = new Sphere();

        sphere.getTransforms().add(new Rotate());
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

    public void setVisualRadius(double visualRadius) {
        sphere.setRadius(visualRadius);
    }

    public void initRotation(boolean random) {
        if (random) {
            Random randomGen = new Random();
            rotateBy(randomGen.nextDouble(), randomGen.nextDouble(), randomGen.nextDouble(),
                    randomGen.nextDouble() * 360);
        }
    }

    public void rotateBy(double axisX, double axisY, double axisZ, double deg) {
        Rotate nr = new Rotate(deg, new Point3D(axisX, axisY, axisZ));
        Transform cur = sphere.getTransforms().remove(0);
        Transform tr = nr.createConcatenation(cur);
        sphere.getTransforms().add(tr);
    }

    public void setX(GamePane container, double actualX) {
        sphere.setTranslateX(container.paneX(actualX));
    }

    public void setY(GamePane container, double actualY) {
        sphere.setTranslateY(container.paneY(actualY));
    }
}
