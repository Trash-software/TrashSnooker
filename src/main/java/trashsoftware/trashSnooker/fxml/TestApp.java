package trashsoftware.trashSnooker.fxml;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;
import javafx.util.Duration;
import trashsoftware.trashSnooker.core.cue.CueTip;
import trashsoftware.trashSnooker.fxml.drawing.CueModel3D;
import trashsoftware.trashSnooker.fxml.drawing.CueTipModel;
import trashsoftware.trashSnooker.fxml.drawing.NonStretchSphere;
import trashsoftware.trashSnooker.fxml.drawing.TipModel;

import java.util.Objects;

public class TestApp extends Application {
    private double x = 300, y = 300;
    double ballRadius = 100.0;
    private double rx, ry, rz;
    double vx = 1;
    double vy = 3;
    Rotate rotate1 = new Rotate();
    Rotate rotate2 = new Rotate();

    Rotate xRotate = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
    Rotate yRotate = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
    Rotate zRotate = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);

    private void testHemisphere(Pane group) {
//        Group sub = new Group();
//        Hemisphere hs = Hemisphere.createByBaseRadius(48, 100, 90);
//
//        PhongMaterial material = new PhongMaterial();
//        material.setDiffuseColor(Color.BLUE);
//        hs.setMaterial(material);
//
//        sub.getChildren().add(hs);
//        sub.setTranslateX(200);
//        sub.setTranslateY(200);

        TipModel sub = CueTipModel.create(CueTip.createDefault(50, 30));
        sub.setTranslateX(200);
        sub.setTranslateY(200);

        sub.getTransforms().addAll(xRotate, yRotate, zRotate);

        group.getChildren().add(sub);
    }
    
    private void testCustomSphere(Pane group) {
        NonStretchSphere nss = new NonStretchSphere(128, 57.15);

        String fileName = "/trashsoftware/trashSnooker/res/img/"
                + 256 + "/pool/pool" + "0" + ".png";
        Image img = new Image(Objects.requireNonNull(getClass().getResource(fileName)).toExternalForm());
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(img);
        nss.setMaterial(material);
        
        nss.setTranslateX(200);
        nss.setTranslateY(200);
        xRotate.setPivotX(200);
//        yRotate.setPivotZ(200);
        
        double s = 0.45;
        Scale scale = new Scale(s, s, s);
        
        nss.getTransforms().addAll(xRotate, yRotate, zRotate);
        nss.setPolarLimit(45);
        group.getChildren().add(nss);
    }

    private void test3DCueModel(Pane group) {
        CueModel3D cueModel3D = CueModel3D.makeDefault("trashCue1", 48);
        cueModel3D.setScale(0.8);

        cueModel3D.setTranslateX(100);
        cueModel3D.setTranslateY(100);

//        xRotate.setAngle(45);
//        zRotate.setAngle(90);

        cueModel3D.getTransforms().addAll(xRotate, yRotate, zRotate);

        group.getChildren().add(cueModel3D);

//        Timeline timeline = new Timeline();
//        timeline.getKeyFrames().add(new KeyFrame(new Duration(20), e -> {
//            yRotate.setAngle(yRotate.getAngle() + 1);
//        }));
//        timeline.setCycleCount(500);
//        timeline.play();
    }

    private void testBallRotate(Pane group) {
        Sphere sphere = new Sphere(ballRadius);
        Image img = new Image(getClass().getResource(
                "/trashsoftware/trashSnooker/res/img/256/ball/pool/std/pool0.png").toExternalForm());
        PhongMaterial material = new PhongMaterial();
//        PhongMaterial material = new PhongMaterial(Color.RED, img, null, null, null);
        material.setDiffuseMap(img);
        sphere.setMaterial(material);

        sphere.setTranslateX(300);
        sphere.setTranslateY(300);

        Sphere sphere2 = new Sphere(ballRadius);
        sphere2.setMaterial(material);

        sphere2.setTranslateX(600);
        sphere2.setTranslateY(300);

        group.getChildren().add(sphere);
        group.getChildren().add(sphere2);

        Rotate xRotate = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
        Rotate yRotate = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
        Rotate zRotate = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);
        sphere2.getTransforms().addAll(zRotate, yRotate, xRotate);

//        sphere.getTransforms().addAll(xRotate, yRotate, zRotate);
        sphere.getTransforms().addAll(rotate1);


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

//        zRotate.setAngle(-26.57);
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
//            sphere2.setTranslateX(x + 300);
//            sphere2.setTranslateY(y);
            x += vx;
            y += vy;
//            xRotate.set
//            rotate1.setAxis(calculateAxis(vx, vy, 0));
            Rotate nr = new Rotate(Math.hypot(vx, vy), calculateAxis(vx, vy, 0));
            Transform cur = sphere.getTransforms().remove(0);
            Transform tr = nr.createConcatenation(cur);
            sphere.getTransforms().add(tr);

//            rotate1.setAngle(
//                    rotate1.getAngle() +
//                            Math.hypot(vx, vy));
//            double[] ea = getEulerAngles(rotate1);
//            System.out.printf("%f, %f, %f\n", ea[0] - last[0], ea[1] - last[1], ea[2] - last[2]);

//            Point3D axis = calculateAxis(vx, vy, 0);
//            Math.sin()

//            System.out.println(Arrays.toString(ea));
//            xRotate.setAngle(xRotate.getAngle() + 3);
//            yRotate.setAngle(yRotate.getAngle() + 3);
//            zRotate.setAngle(zRotate.getAngle() + 3);
//            yRotate.setAngle(yRotate.getAngle() + vy);

//            xRotate.setAngle(Math.toDegrees(ea[0]));
//            yRotate.setAngle(Math.toDegrees(ea[1]));
//            zRotate.setAngle(Math.toDegrees(ea[2]));
//            System.arraycopy(ea, 0, last, 0, 3);

            if (y + ballRadius >= 720 || y < ballRadius) {
//                rotate1 = new Rotate();
//                sphere.getTransforms().add(rotate1);
//                Transform cur = sphere.getTransforms().get(0);
//                Rotate tempRotate = new Rotate();
//                cur = tempRotate.createConcatenation(cur);
//                rotate1 = tempRotate;
                vy = -vy;
            }

        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

//    private void rotateBy()

    private void testGradient(Pane group, Canvas canvas) {
        Stop[] stops = new Stop[]{new Stop(0, Color.RED), new Stop(1, Color.BLUE)};
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 0.4,
                true,
                CycleMethod.NO_CYCLE,
                stops
        );
        canvas.getGraphicsContext2D().setFill(gradient);
        canvas.getGraphicsContext2D().fillRect(0, 200, 200, 200);
    }

    boolean dragging;
    double lastDragX, lastDragY;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Pane group = new Pane();
        Canvas canvas = new Canvas();

        canvas.setHeight(500);
        canvas.setWidth(1440);
        canvas.getGraphicsContext2D().setFill(Color.GREEN);
        canvas.getGraphicsContext2D().fillRect(0, 0, 1440, 500);
        
//        xRotate.setPivotY(canvas.getWidth() / 2);

        canvas.setOnMouseDragged(e -> {
            if (dragging) {
                double dx = e.getX() - lastDragX;
                double dy = e.getY() - lastDragY;
//                System.out.println(dx);

                xRotate.setAngle(xRotate.getAngle() - dx * 0.2);
                yRotate.setAngle(yRotate.getAngle() + dy * 1);
            } else {
                dragging = true;
            }
            lastDragX = e.getX();
            lastDragY = e.getY();
        });
        
        canvas.setOnMouseReleased(e -> {
            dragging = false;
            System.out.println("Release");
        });

        canvas.setOnMouseDragReleased(e -> {
            dragging = false;
            System.out.println("Drag Release");
        });

        group.getChildren().add(canvas);

//        AmbientLight light = new AmbientLight();
//        group.getChildren().add(light);

//        testHemisphere(group);
//        test3DCueModel(group);
        testCustomSphere(group);
//        testBallRotate(group);
//        testCue(group);
//        testGradient(group, canvas);

        DirectionalLight light = new DirectionalLight();
        light.setTranslateZ(2000);
        group.getChildren().add(light);

        Camera camera = new ParallelCamera();
//        camera.setTranslateZ(2000);

        Scene scene = new Scene(group, -1, -1, false, SceneAntialiasing.BALANCED);
        scene.setCamera(camera);

        primaryStage.setScene(scene);

        primaryStage.show();
        primaryStage.setY(50);
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
