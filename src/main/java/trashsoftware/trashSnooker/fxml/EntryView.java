package trashsoftware.trashSnooker.fxml;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import trashsoftware.trashSnooker.core.person.PlayerPerson;
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
    @FXML
    ProgressIndicator progressInd;

    private Stage selfStage;
    private ResourceBundle strings;
    private Scene thisScene;
    private Parent entryRoot;

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
            restoreScene();
            throw new RuntimeException(e);
        }

//            Scene scene = new Scene(root, -1, -1, false, SceneAntialiasing.BALANCED);
//            scene.getStylesheets().add(getClass().getResource("/trashsoftware/trashSnooker/css/font.css").toExternalForm());

        CareerView mainView = loader.getController();
        mainView.setParent(entryRoot);
        mainView.setup(this, stage);

        App.setRoot(root);

//        App.scaleWindow(stage);
        
        restoreScene();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;

        setupTable();
    }
    
    private void setupTable() {
        playerColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getPlayerName()));
        levelColumn.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getLevel()));
        careersTable.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            continueCareerBtn.setDisable(newValue == null);
            deleteCareerBtn.setDisable(newValue == null);
        }));
        
        careersTable.setRowFactory(tv -> {
            TableRow<CareerSave> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();

            // Create menu items
            MenuItem editItem = new MenuItem(strings.getString("renameCareer"));
            MenuItem deleteItem = new MenuItem(strings.getString("deleteCareer"));

            // Set action for the menu items
            editItem.setOnAction(event -> renameCareer(row.getItem()));

            deleteItem.setOnAction(event -> deleteCareer(row.getItem()));

            // Add items to the context menu
            contextMenu.getItems().addAll(editItem, deleteItem);

            // Show context menu on right-click
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty()) {
                    PlayerPerson person = DataLoader.getInstance().getPlayerPerson(row.getItem().getPlayerId());
                    if (person == null || !person.isCustom()) editItem.setDisable(true);
                    
                    contextMenu.show(row, event.getScreenX(), event.getScreenY());
                }
            });

            return row;
        });
    }

    public void refreshGui() {
        refreshTable();
    }

    public void setup(Stage selfStage) {
        this.selfStage = selfStage;
        this.thisScene = selfStage.getScene();
        this.entryRoot = (Parent) thisScene.getRoot().getChildrenUnmodifiable().get(0);
        
        refreshGui();

        this.selfStage.setOnCloseRequest(e -> {
            try {
                Node currentShowing = App.getAppRoot().getChildren().getFirst();
                if (currentShowing != entryRoot) {
                    e.consume();
                    AlertShower.askConfirmation(
                            selfStage,
                            strings.getString("confirmExitAppDes"),
                            strings.getString("confirmExitApp"),
                            selfStage::hide,
                            null
                    );
                }
            } catch (Exception err) {
                EventLogger.error(err);
            }
        });
        this.selfStage.setOnHidden(e -> {
//                Recorder.save();
//                ConfigLoader.stopLoader();
            App.setMainWindowShowing(false);
            DBAccess.closeDB();
        });

//        CareerManager careerManager = CareerManager.getInstance();
//        if (careerManager)
    }

    private void refreshTable() {
        careersTable.getItems().clear();
        careersTable.getItems().addAll(CareerManager.careerLists());
    }
    
    private void hangScene() {
        progressInd.setVisible(true);
        thisScene.getRoot().setDisable(true);
    }
    
    private void restoreScene() {
        progressInd.setVisible(false);
        thisScene.getRoot().setDisable(false);
    }

    @FXML
    void changelogAction() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("changelogView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage stage = new Stage();
            stage.setTitle(strings.getString("appName"));
            stage.initOwner(this.selfStage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.UTILITY);

            Scene scene = App.createScene(root);
            stage.setScene(scene);

            stage.show();
        } catch (IOException e) {
            EventLogger.error(e);
        }
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
            stage.setResizable(false);

            Scene scene = App.createScene(root);
            stage.setScene(scene);

            stage.show();
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }

    @FXML
    void cuePreviewerAction() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("cuePreviewerView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage stage = new Stage();
            stage.setTitle(strings.getString("appName"));
            stage.initOwner(this.selfStage);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.UTILITY);

            Scene scene = App.createScene(root);
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

            SettingsView view = loader.getController();
            view.setup(selfStage);
            view.setParent(thisScene);

            App.setRoot(root);
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
            
            App.setRoot(root);
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
            
            ReplayView view = loader.getController();
            view.setStage(selfStage);
            view.setParent(thisScene);
            view.naiveFill();

            App.setRoot(root);
        } catch (IOException e) {
            EventLogger.error(e);
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

//            Scene scene = new Scene(root, -1, -1, false, SceneAntialiasing.BALANCED);
//            scene.getStylesheets().add(getClass().getResource("/trashsoftware/trashSnooker/css/font.css").toExternalForm());
        App.setRoot(root);
    }

    @FXML
    void continueCareer() {
        CareerSave selected = careersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        hangScene();

        Thread thread = new Thread(() -> {
            try {
                CareerManager.setCurrentSave(selected);
                CareerManager.getInstance();  // 提前触发读取
                Platform.runLater(() -> startCareerView(selfStage));
            } catch (Exception e) {
                restoreScene();
                EventLogger.error(e);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
    
    private void renameCareer(CareerSave selected) {
        PlayerPerson person = DataLoader.getInstance().getPlayerPerson(selected.getPlayerId());
        if (person == null) {
            System.err.println("Person is null");
            return;
        } else {
            if (!person.isCustom()) System.err.println("Cannot delete non-custom player.");
        }
        
        TextField newNameField = new TextField();
        newNameField.setText(person.getName());
        
        AlertShower.askConfirmation(
                selfStage,
                "",
                strings.getString("renameCareerText"),
                strings.getString("confirm"),
                strings.getString("cancel"),
                false,
                () -> {
                    String newName = newNameField.getText();
                    if (newName.isBlank()) {
                        return;
                    }
                    boolean suc = person.rename(newName);
                    if (suc) {
                        DataLoader.getInstance().updatePlayer(person);
                        System.out.println("Rename successful!");
                    } else {
                        System.out.println("Rename failed!");
                    }

                    refreshGui();
                },
                null,
                newNameField
        );
    }
    
    private void deleteCareer(CareerSave selected) {
        CheckBox deletePerson = new CheckBox(strings.getString("deletePlayerWithCareer"));
        PlayerPerson person = DataLoader.getInstance().getPlayerPerson(selected.getPlayerId());
        if (person == null) {
            System.err.println("Person is null");
            return;
        } else {
            if (!person.isCustom()) deletePerson.setDisable(true);
        }

        AlertShower.askConfirmation(
                selfStage,
                String.format(strings.getString("confirmDeleteCareer"), selected.getPlayerName()),
                strings.getString("pleaseConfirm"),
                strings.getString("confirm"),
                strings.getString("cancel"),
                false,
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
    void deleteCareerAction() {
        CareerSave selected = careersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        deleteCareer(selected);
    }

    @FXML
    void fastGame() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("fastGameView.fxml"),
                strings
        );
        Parent root = loader.load();
        root.setStyle(App.FONT_STYLE);

        FastGameView fastGameView = loader.getController();
        fastGameView.setParent(thisScene);
        fastGameView.setStage(selfStage);

        App.setRoot(root);
        
//            scene.getStylesheets().add(getClass().getResource("/trashsoftware/trashSnooker/css/font.css").toExternalForm());
        
    }
}
