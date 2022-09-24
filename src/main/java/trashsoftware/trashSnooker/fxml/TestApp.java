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

import java.util.Arrays;

public class TestApp extends Application {
    private double x = 300, y = 300;
    double ballRadius = 100.0;
    private double rx, ry, rz;
    double vx = 1;
    double vy = 3;
    Rotate rotate1 = new Rotate();
    Rotate rotate2 = new Rotate();

    @Override
    public void start(Stage primaryStage) throws Exception {
        Sphere sphere = new Sphere(ballRadius);
        Image img = new Image(getClass().getResource("/trashsoftware/trashSnooker/img/pool/pool9.png").toExternalForm());
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(img);
        sphere.setMaterial(material);
        
        sphere.setTranslateX(300);
        sphere.setTranslateY(300);

        Sphere sphere2 = new Sphere(ballRadius);
        sphere2.setMaterial(material);
        
        sphere2.setTranslateX(600);
        sphere2.setTranslateY(300);
        
        Canvas canvas = new Canvas();

        canvas.setHeight(720);
        canvas.setWidth(1280);
        canvas.getGraphicsContext2D().setFill(Color.GREEN);
        canvas.getGraphicsContext2D().fillRect(0, 0, 1280, 720);

        Pane group = new Pane();
        group.getChildren().add(canvas);
        group.getChildren().add(sphere);
        group.getChildren().add(sphere2);

        Scene scene = new Scene(group);
        Rotate xRotate = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
        Rotate yRotate = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
        Rotate zRotate = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);
        sphere2.getTransforms().addAll(zRotate, yRotate, xRotate);

//        sphere.getTransforms().addAll(xRotate, yRotate, zRotate);
        sphere.getTransforms().addAll(rotate1, rotate2);

        primaryStage.setScene(scene);

        primaryStage.show();

//        xRotate.setAngle(15);
//        yRotate.setAngle(-15);
//        zRotate.setAngle(45);
        rotate1.setAxis(new Point3D(1, 1, 0));
        rotate1.setAngle(45);
//        rotate2.setAxis(new Point3D(0, 1, 1));
//        rotate2.setAngle(30);
        
        System.out.printf("%f %f %f\n", rotate1.getMxx(), rotate1.getMxy(), rotate1.getMxz());
        System.out.printf("%f %f %f\n", rotate1.getMyx(), rotate1.getMyy(), rotate1.getMyz());
        System.out.printf("%f %f %f\n", rotate1.getMzx(), rotate1.getMzy(), rotate1.getMzz());
        
        double[] r1 = getEulerAngles(rotate1);
//        double[] r2 = getEulerAngles(rotate2);
        double[] r2 = {0, 0, 0};
        
        zRotate.setAngle(-26.57);
//        yRotate.setAngle(30);
//        xRotate.setAngle(30);
//        xRotate.setAngle(Math.toDegrees(r1[0] + r2[0]));
//        yRotate.setAngle(Math.toDegrees(r1[1] + r2[1]));
//        zRotate.setAngle(Math.toDegrees(r1[2] + r2[2]));
        
////        sphere.getTransforms().clear();
//        Rotate r2 = new Rotate();
//        sphere.getTransforms().add(r2);
//        r2.setAxis(new Point3D(1, 0, 0));
//        r2.setAngle(20);
        
        double[] last = new double[3];

        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(new KeyFrame(new Duration(20), e -> {
            sphere.setTranslateX(x);
            sphere.setTranslateY(y);
            sphere2.setTranslateX(x + 300);
            sphere2.setTranslateY(y);
            x += vx;
            y += vy;
//            xRotate.set
            rotate1.setAxis(calculateAxis(vx, vy, 0));
            rotate1.setAngle(
                    rotate1.getAngle() + 
                    Math.hypot(vx, vy));
            double[] ea = getEulerAngles(rotate1);
            System.out.printf("%f, %f, %f\n", ea[0] - last[0], ea[1] - last[1], ea[2] - last[2]);
            
//            Point3D axis = calculateAxis(vx, vy, 0);
//            Math.sin()
            
//            System.out.println(Arrays.toString(ea));
            xRotate.setAngle(xRotate.getAngle() + 3);
//            xRotate.setAngle(Math.toDegrees(ea[0]));
//            yRotate.setAngle(Math.toDegrees(ea[1]));
//            zRotate.setAngle(Math.toDegrees(ea[2]));
            System.arraycopy(ea, 0, last, 0, 3);
            
            if (y + ballRadius >= canvas.getHeight() || y < ballRadius) {
//                rotate1 = new Rotate();
//                sphere.getTransforms().add(rotate1);
                vy = -vy;
            }
            
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
    
    private double[] getEulerAngles(Rotate r) {
        double theta = -Math.asin(r.getMzx());
        double cosTheta = Math.cos(theta);
        double psi = Math.atan2(r.getMzy() / cosTheta, r.getMzz() / cosTheta);
        double phi = Math.atan2(r.getMyx() / cosTheta, r.getMxx() / cosTheta);
        return new double[]{psi, theta, phi};
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
