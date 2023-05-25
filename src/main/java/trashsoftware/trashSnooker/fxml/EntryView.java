package trashsoftware.trashSnooker.fxml;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.CareerSave;
import trashsoftware.trashSnooker.fxml.alert.AlertShower;
import trashsoftware.trashSnooker.fxml.statsViews.StatsView;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
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
    TableColumn<CareerSave, String> levelColumn;

    @FXML
    Button continueCareerBtn, deleteCareerBtn;

    private Stage selfStage;
    private ResourceBundle strings;
    private Scene thisScene;

    void startCareerView(Stage stage) {
//        stage.initOwner(owner);
//        stage.initModality(Modality.WINDOW_MODAL);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("careerView.fxml"),
                strings
        );
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        root.setStyle(App.FONT_STYLE);

//            Scene scene = new Scene(root, -1, -1, false, SceneAntialiasing.BALANCED);
//            scene.getStylesheets().add(getClass().getResource("/trashsoftware/trashSnooker/css/font.css").toExternalForm());

        CareerView mainView = loader.getController();
        mainView.setParent(thisScene);
        mainView.setSelfStage(stage);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.sizeToScene();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;

        playerColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getPlayerName()));
        levelColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getLevel()));
        careersTable.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            continueCareerBtn.setDisable(newValue == null);
            deleteCareerBtn.setDisable(newValue == null);
        }));
    }

    public void refreshGui() {
        refreshTable();
    }

    public void setup(Stage selfStage) {
        this.selfStage = selfStage;
        this.thisScene = selfStage.getScene();

        refreshGui();

        this.selfStage.setOnCloseRequest(e -> {
            if (selfStage.getScene() != thisScene) {
                e.consume();
                AlertShower.askConfirmation(
                        selfStage,
                        strings.getString("confirmExitAppDes"),
                        strings.getString("confirmExitApp"),
                        selfStage::hide,
                        null
                );
            }
        });
        this.selfStage.setOnHidden(e -> {
//                Recorder.save();
//                ConfigLoader.stopLoader();
            DBAccess.closeDB();
        });

//        CareerManager careerManager = CareerManager.getInstance();
//        if (careerManager)
    }

    private void refreshTable() {
        careersTable.getItems().clear();
        careersTable.getItems().addAll(CareerManager.careerLists());
    }

    @FXML
    void aboutAction() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("aboutView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage stage = new Stage();
            stage.setTitle(strings.getString("appName"));
            stage.initOwner(this.selfStage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.UTILITY);

            Scene scene = new Scene(root);
            stage.setScene(scene);

            stage.show();
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }
    
    @FXML
    void settingsAction() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("settingsView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Scene scene = new Scene(root);

            SettingsView view = loader.getController();
            view.setup(selfStage);
            view.setParent(thisScene);

            selfStage.setScene(scene);
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }

    @FXML
    void recordsAction() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("statsView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            StatsView view = loader.getController();
            view.setParent(thisScene);
            view.setStage(selfStage);

            Scene scene = new Scene(root);
            selfStage.setScene(scene);
            selfStage.sizeToScene();
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }

    @FXML
    void replayAction() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("replayView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage stage = new Stage();
            stage.initOwner(this.selfStage);
            stage.initModality(Modality.WINDOW_MODAL);

            ReplayView view = loader.getController();
            view.setStage(stage);
            view.fill();

            Scene scene = new Scene(root);
            stage.setScene(scene);

            Thread.sleep(100);  // 成熟的程序员应该一眼就能看出这种垃圾代码是为了什么

            stage.show();
        } catch (IOException e) {
            EventLogger.error(e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void newCareer() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("newCareerView.fxml"),
                strings
        );
        Parent root = loader.load();
        root.setStyle(App.FONT_STYLE);

        NewCareerView view = loader.getController();
        view.setParent(thisScene);
        view.setup(this, selfStage);

        Scene scene = new Scene(root);

//            Scene scene = new Scene(root, -1, -1, false, SceneAntialiasing.BALANCED);
//            scene.getStylesheets().add(getClass().getResource("/trashsoftware/trashSnooker/css/font.css").toExternalForm());
        selfStage.setScene(scene);
        selfStage.sizeToScene();
    }

    @FXML
    void continueCareer() {
        CareerSave selected = careersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        CareerManager.setCurrentSave(selected);

        startCareerView(selfStage);
    }

    @FXML
    void deleteCareer() {
        CareerSave selected = careersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        CheckBox deletePerson = new CheckBox(strings.getString("deletePlayerWithCareer"));
        PlayerPerson person = DataLoader.getInstance().getPlayerPerson(selected.getPlayerId());
        if (!person.isCustom()) deletePerson.setDisable(true);

        AlertShower.askConfirmation(
                selfStage,
                String.format(strings.getString("confirmDeleteCareer"), selected.getPlayerName()),
                strings.getString("pleaseConfirm"),
                strings.getString("confirm"),
                strings.getString("cancel"),
                () -> {
                    String playerId = selected.getPlayerId();
                    CareerManager.deleteCareer(selected);
                    if (deletePerson.isSelected()) {
                        DataLoader.getInstance().deletePlayer(playerId);
                    }

                    refreshGui();
                },
                null,
                deletePerson
        );
    }

    @FXML
    void fastGame() throws IOException {
//            ConfigLoader.startLoader(CONFIG);
//        Stage stage = new Stage();
//        stage.initOwner(selfStage);
//        stage.initModality(Modality.WINDOW_MODAL);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("fastGameView.fxml"),
                strings
        );
        Parent root = loader.load();
        root.setStyle(App.FONT_STYLE);

        FastGameView fastGameView = loader.getController();
        fastGameView.setParent(thisScene);
        fastGameView.setStage(selfStage);

        Scene scene = new Scene(root, -1, -1, false, SceneAntialiasing.BALANCED);
//            scene.getStylesheets().add(getClass().getResource("/trashsoftware/trashSnooker/css/font.css").toExternalForm());
        selfStage.setScene(scene);
        selfStage.sizeToScene();

//        stage.show();
    }
}
