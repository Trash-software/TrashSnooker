package trashsoftware.trashSnooker.fxml.drawing;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.util.DataLoader;

import java.io.File;

public class ConeTest extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {

        VBox base = new VBox();
        base.setPrefWidth(1000);
        base.setPrefHeight(600);

        Cone cone = new Cone();
        
        base.getChildren().add(cone);
        
        cone.getTransforms().add(new Rotate(45, new Point3D(1, 1, 1)));
        
        Scene scene = new Scene(base);
        primaryStage.setScene(scene);
        
        primaryStage.show();
    }
}
