package trashsoftware.trashSnooker.fxml;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Point3D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

public class TestApp extends Application {
    private double x = 300, y = 300;
    double ballRadius = 100.0;
    private double rx, ry, rz;
    double vx = 1;
    double vy = 3;
    Rotate onlyRotate = new Rotate();

    @Override
    public void start(Stage primaryStage) throws Exception {
        Sphere sphere = new Sphere(ballRadius);
        sphere.setEffect(null);
        Image img = new Image(getClass().getResource("/trashsoftware/trashSnooker/img/pool/pool9.png").toExternalForm());
//        Image white = new Image(getClass().getResource("/trashsoftware/trashSnooker/img/white.png").toExternalForm());
        PhongMaterial material = new PhongMaterial();
//        material.setDiffuseColor(Color.RED);
        material.setDiffuseMap(img);
//        material.setSpecularColor(null);
//        material.setSelfIlluminationMap(white);
//        material.setSpecularPower(1);
        sphere.setMaterial(material);
        sphere.setLayoutX(300);
        sphere.setLayoutY(300);
        Canvas canvas = new Canvas();

        canvas.setHeight(720);
        canvas.setWidth(1280);
        canvas.getGraphicsContext2D().setFill(Color.GREEN);
        canvas.getGraphicsContext2D().fillRect(0, 0, 1280, 720);

//        GridPane gridPane = new GridPane();
//        gridPane.add(canvas, 0, 0);

        Pane group = new Pane();
        group.getChildren().add(canvas);
        group.getChildren().add(sphere);

//        gridPane.add(sphere, 0, 0);
//        gridPane.add(group, 0, 0);

        Scene scene = new Scene(group);
        Rotate xRotate = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
        Rotate yRotate = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
        Rotate zRotate = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);

//        sphere.getTransforms().addAll(xRotate, yRotate, zRotate);
        sphere.getTransforms().add(onlyRotate);

        primaryStage.setScene(scene);

        primaryStage.show();

//        xRotate.setAngle(15);
//        yRotate.setAngle(-15);
//        zRotate.setAngle(45);
        rx = 1;

        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(new Duration(20), e -> {
            sphere.setLayoutX(x);
            sphere.setLayoutY(y);
            x += vx;
            y += vy;
//            xRotate.set
            onlyRotate.setAxis(calculateAxis(vx, vy, 0));
            onlyRotate.setAngle(onlyRotate.getAngle() + Math.hypot(vx, vy));
            
            if (y + ballRadius >= canvas.getHeight() || y < ballRadius) {
                double ang = onlyRotate.getAngle() % 360.0;
                onlyRotate.setAxis(calculateAxis(vx, vy, 0));
                onlyRotate.setAngle(360.0 - ang);
                onlyRotate.getMxx();
                vy = -vy;
            }
            
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
    
    private Point3D calculateAxis(double xSpin, double ySpin, double sideSpin) {
        double ry = -xSpin;
        return new Point3D(ySpin, ry, -sideSpin);
    }

    private void matrixRotateNode(Sphere n, double alf, double bet, double gam) {
        double A11 = Math.cos(alf) * Math.cos(gam);
        double A12 = Math.cos(bet) * Math.sin(alf) + Math.cos(alf) * Math.sin(bet) * Math.sin(gam);
        double A13 = Math.sin(alf) * Math.sin(bet) - Math.cos(alf) * Math.cos(bet) * Math.sin(gam);
        double A21 = -Math.cos(gam) * Math.sin(alf);
        double A22 = Math.cos(alf) * Math.cos(bet) - Math.sin(alf) * Math.sin(bet) * Math.sin(gam);
        double A23 = Math.cos(alf) * Math.sin(bet) + Math.cos(bet) * Math.sin(alf) * Math.sin(gam);
        double A31 = Math.sin(gam);
        double A32 = -Math.cos(gam) * Math.sin(bet);
        double A33 = Math.cos(bet) * Math.cos(gam);

        double d = Math.acos((A11 + A22 + A33 - 1d) / 2d);
        if (d != 0d) {
            double den = 2d * Math.sin(d);
            Point3D p = new Point3D((A32 - A23) / den, (A13 - A31) / den, (A21 - A12) / den);
            n.setRotationAxis(p);
            n.setRotate(Math.toDegrees(d));
        }
    }
}
