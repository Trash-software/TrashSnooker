package trashsoftware.trashSnooker.fxml;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.phy.TableCloth;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.GameSaver;
import trashsoftware.trashSnooker.util.Recorder;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Objects;
import java.util.ResourceBundle;

public class MainView implements Initializable {

    @FXML
    Button resumeButton;

    @FXML
    Button player1InfoButton, player2InfoButton;

    @FXML
    ComboBox<Integer> totalFramesBox;

    @FXML
    ComboBox<TableCloth.Smoothness> clothSmoothBox;
    
    @FXML
    ComboBox<TableCloth.Goodness> clothGoodBox;

    @FXML
    ComboBox<PlayerPerson> player1Box, player2Box;

    @FXML
    ComboBox<CueItem> player1CueBox, player2CueBox;

    @FXML
    ComboBox<PlayerType> player1Player, player2Player;

    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initTotalFramesBox();
        initClothBox();
        loadPlayerList();
        loadCueList();

        resumeButton.setDisable(!GameSaver.hasSavedGame());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void reloadPlayerList() {
        loadPlayerList();
    }

    private void initTotalFramesBox() {
        for (int i = 1; i <= 41; i += 2) {
            totalFramesBox.getItems().add(i);
        }
        totalFramesBox.getSelectionModel().select(0);
    }
    
    private void initClothBox() {
        clothSmoothBox.getItems().addAll(TableCloth.Smoothness.values());
        clothGoodBox.getItems().addAll(TableCloth.Goodness.values());
        clothSmoothBox.getSelectionModel().select(1);
        clothGoodBox.getSelectionModel().select(1);
    }

    private void loadCueList() {
        refreshCueList(player1CueBox);
        refreshCueList(player2CueBox);
    }

    private void refreshCueList(ComboBox<CueItem> box) {
        box.getItems().clear();
        for (Cue cue : Recorder.getCues().values()) {
            if (!cue.privacy) {
                box.getItems().add(new CueItem(cue, cue.getName()));
            }
        }
        box.getSelectionModel().select(0);
    }

    private void loadPlayerList() {
        Collection<PlayerPerson> playerPeople = Recorder.getPlayerPeople();
        player1Box.getItems().clear();
        player2Box.getItems().clear();
        player1Box.getItems().addAll(playerPeople);
        player2Box.getItems().addAll(playerPeople);

        addPlayerBoxProperty(player1Box, player1CueBox, player1InfoButton);
        addPlayerBoxProperty(player2Box, player2CueBox, player2InfoButton);

        player1Player.getItems().addAll(PlayerType.PLAYER, PlayerType.COMPUTER);
        player2Player.getItems().addAll(PlayerType.PLAYER, PlayerType.COMPUTER);
        player1Player.getSelectionModel().select(0);
        player2Player.getSelectionModel().select(0);
    }

    private void addPlayerBoxProperty(ComboBox<PlayerPerson> playerBox, 
                                      ComboBox<CueItem> cueBox, 
                                      Button infoButton) {
        playerBox.getSelectionModel().selectedItemProperty()
                .addListener(((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        infoButton.setDisable(false);
                        refreshCueList(cueBox);
                        boolean sel = false;
                        for (Cue cue : newValue.getPrivateCues()) {
                            cueBox.getItems().add(new CueItem(cue, cue.getName()));
                            if (!sel) {
                                // 有私杆的人默认选择第一根私杆
                                cueBox.getSelectionModel().select(cueBox.getItems().size() - 1);
                                sel = true;
                            }
                        }
                    } else {
                        infoButton.setDisable(true);
                    }
                }));
    }

    @FXML
    void addPlayerAction() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("addPlayerView.fxml")
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage stage = new Stage();
            stage.initOwner(this.stage);
            stage.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(root);
            stage.setScene(scene);

            AddPlayerView view = loader.getController();
            view.setStage(stage, this);

            stage.show();
        } catch (IOException e) {
            EventLogger.log(e);
        }
    }
    
    @FXML
    void playerInfoAction(ActionEvent event) {
        ComboBox<PlayerPerson> personBox;
        if (Objects.equals(event.getSource(), player1InfoButton)) {
            personBox = player1Box;
        } else {
            personBox = player2Box;
        }
        PlayerPerson person = personBox.getValue();
        if (person != null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("abilityView.fxml")
                );
                Parent root = loader.load();
                root.setStyle(App.FONT_STYLE);
                
                AbilityView controller = loader.getController();
                controller.setup(person);

                Stage stage = new Stage();
                stage.initOwner(this.stage);
                stage.initModality(Modality.WINDOW_MODAL);

                Scene scene = new Scene(root);
                stage.setScene(scene);

                stage.show();
            } catch (IOException e) {
                EventLogger.log(e);
            }
        }
    }

    @FXML
    void recordsAction() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("statsView.fxml")
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage stage = new Stage();
            stage.initOwner(this.stage);
            stage.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(root);
            stage.setScene(scene);

            stage.show();
        } catch (IOException e) {
            EventLogger.log(e);
        }
    }
    
    @FXML
    void replayAction() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("replayView.fxml")
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage stage = new Stage();
            stage.initOwner(this.stage);
            stage.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(root);
            stage.setScene(scene);

            stage.show();
        } catch (IOException e) {
            EventLogger.log(e);
        }
    }

    @FXML
    void resumeAction() {

    }

    @FXML
    void snookerAction() {
        showGame(GameType.SNOOKER);
    }

    @FXML
    void miniSnookerAction() {
        showGame(GameType.MINI_SNOOKER);
    }

    @FXML
    void chineseEightAction() {
        showGame(GameType.CHINESE_EIGHT);
    }

    @FXML
    void sidePocketAction() {
        showGame(GameType.SIDE_POCKET);
    }

    private void showGame(GameType gameType) {
        PlayerPerson p1 = player1Box.getValue();
        PlayerPerson p2 = player2Box.getValue();
        if (p1 == null || p2 == null) {
            System.out.println("没有足够的球员");
            return;
        }

        InGamePlayer igp1;
        InGamePlayer igp2;
        Cue stdBreakCue = Recorder.getStdBreakCue();
        if (stdBreakCue == null ||
                gameType == GameType.SNOOKER ||
                gameType == GameType.MINI_SNOOKER) {
            igp1 = new InGamePlayer(p1, player1CueBox.getValue().cue, player1Player.getValue(), 1);
            igp2 = new InGamePlayer(p2, player2CueBox.getValue().cue, player2Player.getValue(), 2);
        } else {
            igp1 = new InGamePlayer(p1, stdBreakCue, player1CueBox.getValue().cue, player1Player.getValue(), 1);
            igp2 = new InGamePlayer(p2, stdBreakCue, player2CueBox.getValue().cue, player2Player.getValue(), 2);
        }
        
        TableCloth cloth = new TableCloth(clothGoodBox.getValue(), clothSmoothBox.getValue());

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("gameView.fxml")
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage stage = new Stage();
            stage.initOwner(this.stage);
            stage.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(root);
            stage.setScene(scene);

            GameView gameView = loader.getController();
            gameView.setup(stage, gameType, totalFramesBox.getSelectionModel().getSelectedItem(),
                    igp1, igp2, cloth);

            stage.show();
        } catch (Exception e) {
            EventLogger.log(e);
        }
    }

    public static class CueItem {
        private final Cue cue;
        private final String string;

        CueItem(Cue cue, String string) {
            this.cue = cue;
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }
}
