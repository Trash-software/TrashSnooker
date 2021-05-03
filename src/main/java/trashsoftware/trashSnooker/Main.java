package trashsoftware.trashSnooker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.fxml.GameView;

import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("fxml/gameView.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        GameView gameView = loader.getController();
        gameView.setStage(primaryStage);

        primaryStage.show();
    }
}
