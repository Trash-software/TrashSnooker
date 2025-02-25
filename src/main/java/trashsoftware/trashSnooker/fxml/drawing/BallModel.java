package trashsoftware.trashSnooker.fxml.drawing;

import javafx.geometry.Point3D;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import org.jetbrains.annotations.NotNull;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.metrics.BallsGroupPreset;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.snooker.SnookerBall;
import trashsoftware.trashSnooker.fxml.widgets.GamePane;

import java.util.Random;

public abstract class BallModel {

    public final BallsGroupPreset preset;
    public final NonStretchSphere sphere;
    private NonStretchSphere staticSphere;
    protected boolean textured;

    protected BallModel(BallsGroupPreset preset) {
        this.preset = preset;
        sphere = new NonStretchSphere(64, 1.0);

        sphere.getTransforms().add(new Rotate());
    }

    public static BallModel createModel(Ball ball, BallsGroupPreset preset) {
        if (ball instanceof PoolBall) {
            return new PoolBallModel(preset, ball.getValue());
        } else if (ball instanceof SnookerBall) {
            return new SnookerBallModel(preset, ball.getValue());
        } else {
            throw new RuntimeException("No such ball type");
        }
    }
    
    protected PhongMaterial loadPresetMaterial(@NotNull BallsGroupPreset preset, int value) {
        PhongMaterial material = new PhongMaterial();
        Image image = preset.getImageByBallValue(value);
        if (image == null) {
            Color color = preset.getColorByBallValue(value);
//            System.err.println("bALL " + value + " is " + color);
            if (color == null) return loadDefaultMaterial(value);
            material.setDiffuseColor(color);
        } else {
            material.setDiffuseMap(image);
        }
        return material;
    }
    
    protected abstract PhongMaterial loadDefaultMaterial(int value);
    
    public NonStretchSphere getStaticSphere() {
        if (staticSphere == null) {
            staticSphere = sphere.copyNoTrans();
        }
        
        return staticSphere;
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
//        if (textured()) {
        Rotate nr = new Rotate(deg, new Point3D(axisX, axisY, axisZ));
        Transform cur = sphere.getTransforms().remove(0);
        Transform tr = nr.createConcatenation(cur);
        sphere.getTransforms().add(tr);
//        }
    }

    public void setX(GamePane container, double actualX) {
        sphere.setTranslateX(container.paneX(actualX));
    }

    public void setY(GamePane container, double actualY) {
        sphere.setTranslateY(container.paneY(actualY));
    }

    public boolean textured() {
        return textured;
    }
}
