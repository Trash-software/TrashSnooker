package trashsoftware.trashSnooker.fxml;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.CareerSave;
import trashsoftware.trashSnooker.util.db.DBAccess;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class EntryView implements Initializable {

    @FXML
    TableView<CareerSave> careersTable;
    @FXML
    TableColumn<CareerSave, String> playerColumn;

    @FXML
    Button continueCareerBtn;

    private Stage selfStage;

    void startCareerView(Stage owner, Stage stage) {
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("careerView.fxml")
        );
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        root.setStyle(App.FONT_STYLE);

        CareerView mainView = loader.getController();
        mainView.setSelfStage(stage);

        Scene scene = new Scene(root);

//            Scene scene = new Scene(root, -1, -1, false, SceneAntialiasing.BALANCED);
//            scene.getStylesheets().add(getClass().getResource("/trashsoftware/trashSnooker/css/font.css").toExternalForm());
        stage.setScene(scene);

        stage.show();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        playerColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getPlayerName()));
        careersTable.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            continueCareerBtn.setDisable(newValue == null);
        }));
    }
    
    public void refreshGui() {
        refreshTable();
    }

    public void setup(Stage selfStage) {
        this.selfStage = selfStage;

        refreshGui();

//        CareerManager careerManager = CareerManager.getInstance();
//        if (careerManager)
    }

    private void refreshTable() {
        careersTable.getItems().clear();
        careersTable.getItems().addAll(CareerManager.careerLists());
    }

    @FXML
    void newCareer() throws IOException {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(selfStage);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("newCareerView.fxml")
        );
        Parent root = loader.load();
        root.setStyle(App.FONT_STYLE);

        NewCareerView mainView = loader.getController();
        mainView.setup(this, selfStage, stage);

        Scene scene = new Scene(root);

//            Scene scene = new Scene(root, -1, -1, false, SceneAntialiasing.BALANCED);
//            scene.getStylesheets().add(getClass().getResource("/trashsoftware/trashSnooker/css/font.css").toExternalForm());
        stage.setScene(scene);

        stage.show();
    }

    @FXML
    void continueCareer() throws IOException {
        CareerSave selected = careersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        CareerManager.setCurrentSave(selected);

        Stage stage = new Stage();
        startCareerView(selfStage, stage);
    }

    @FXML
    void fastGame() throws IOException {
//            ConfigLoader.startLoader(CONFIG);
        Stage stage = new Stage();
        stage.initOwner(selfStage);
        stage.initModality(Modality.WINDOW_MODAL);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("mainView.fxml")
        );
        Parent root = loader.load();
        root.setStyle(App.FONT_STYLE);

        MainView mainView = loader.getController();
        mainView.setStage(stage);

        Scene scene = new Scene(root, -1, -1, false, SceneAntialiasing.BALANCED);
//            scene.getStylesheets().add(getClass().getResource("/trashsoftware/trashSnooker/css/font.css").toExternalForm());
        stage.setScene(scene);

        stage.setOnHidden(e -> {
//                Recorder.save();
//                ConfigLoader.stopLoader();
            DBAccess.closeDB();
        });

        stage.show();
    }
}
