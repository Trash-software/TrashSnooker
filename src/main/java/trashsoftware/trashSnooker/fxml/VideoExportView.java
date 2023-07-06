package trashsoftware.trashSnooker.fxml;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import trashsoftware.trashSnooker.fxml.alert.AlertShower;
import trashsoftware.trashSnooker.recorder.GameReplay;
import trashsoftware.trashSnooker.recorder.VideoCapture;
import trashsoftware.trashSnooker.recorder.VideoConverter;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class VideoExportView implements Initializable {
    @FXML
    ComboBox<Integer> frameRateBox;
    @FXML
    ComboBox<VideoResolution> resolutionBox;
    @FXML
    ProgressBar progressBar;
    @FXML
    Label outFileLabel;
    @FXML
    Label progressLabel;
    @FXML
    Label timeUsedLabel, timeEstLabel;
    @FXML
    Button exportBtn, chooseFileBtn, cancelBtn;
    Timer eachSecondUpdate;

    File outFile;

    private GameReplay replay;
    private Stage selfStage;
    private ResourceBundle strings;
    VideoCapture videoCapture;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;

        resolutionBox.getSelectionModel().select(VideoResolution.RES_720P);
    }

    public void setup(GameReplay replay, Stage selfStage) {
        this.replay = replay;
        this.selfStage = selfStage;

        outFile = new File(replay.getItem().getFile().getAbsolutePath() + ".mp4");
        outFileLabel.setText(outFile.getAbsolutePath());

        resolutionBox.getItems().addAll(VideoResolution.values());

        selfStage.setOnHidden(e -> {
            if (videoCapture != null && !videoCapture.isFinished()) {
                interruptAction();
            }
        });
    }

    @FXML
    public void exportAction() {
        exportBtn.setDisable(true);
        chooseFileBtn.setDisable(true);
        resolutionBox.setDisable(true);
//        frameRateBox.setDisable(true);
        
        progressBar.setVisible(true);
        cancelBtn.setVisible(true);
        
        showStageAndExport();
    }

    @FXML
    public void selectFileAction() {
        FileChooser.ExtensionFilter mp4 = new FileChooser.ExtensionFilter(
                strings.getString("fileDesMp4"), "*.mp4");
        FileChooser outChooser = new FileChooser();
        outChooser.getExtensionFilters().add(mp4);
        outChooser.setInitialDirectory(outFile.getParentFile());
        outChooser.setInitialFileName(outFile.getName());
        File file = outChooser.showSaveDialog(selfStage);
        if (file != null) {
            outFile = file;
            outFileLabel.setText(outFile.getAbsolutePath());
            System.out.println(file.getAbsolutePath());
        }
    }

    @FXML
    public void interruptAction() {
        if (videoCapture != null && !videoCapture.isFinished()) {
            videoCapture.interrupt();
        }
    }

    private void createVideoCapture(GameView view) throws IOException {
        VideoResolution resolution = resolutionBox.getValue();

        videoCapture = new VideoCapture(outFile,
                replay,
                new VideoConverter.Params(30, resolution.width, resolution.height),
                1000,
                new ProgressUpdater(),
                view);

        eachSecondUpdate = new Timer();
        eachSecondUpdate.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                double progress = progressBar.getProgress();

                if (progress > 0) {
                    long msUsed = System.currentTimeMillis() - videoCapture.processStartTime;
                    long msEst = (long) (msUsed / progress - msUsed);

                    Platform.runLater(() -> {
                        timeUsedLabel.setText(String.format(strings.getString("timeUsed"),
                                Util.timeToReadable(msUsed)));
                        timeEstLabel.setText(String.format(strings.getString("timeRemEst"),
                                Util.timeToReadable(msEst)));
                    });
                }
            }
        }, 0, 1000);
    }

    private void showStageAndExport() {
        if (replay != null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("gameView.fxml"),
                        strings
                );
                Parent root = loader.load();
                root.setStyle(App.FONT_STYLE);

                Stage stage = new Stage();
//                stage.initOwner(this.selfStage);
                stage.initStyle(StageStyle.UTILITY);
                stage.initModality(Modality.WINDOW_MODAL);

                Scene scene = App.createScene(root);
                stage.setScene(scene);

                GameView gameView = loader.getController();
                gameView.setupReplay(stage, replay);

                createVideoCapture(gameView);

                stage.show();

                App.scaleGameStage(stage);

                gameView.startVideoCapture(videoCapture);
                stage.toBack();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class ProgressUpdater {

        public void end(boolean success) {
            eachSecondUpdate.cancel();
            Platform.runLater(() -> {
                timeEstLabel.setText(Util.timeToReadable(0));
                progressBar.setProgress(1.0);
                progressLabel.setText("100.0%");
                cancelBtn.setDisable(true);
                
                String header = strings.getString(success ? "exportSuccess" : "exportFailed");

                if (success) {
                    AlertShower.askConfirmation(
                            selfStage,
                            strings.getString("openTargetDirAsk"),
                            header,
                            () -> {
                                try {
                                    Desktop.getDesktop().open(outFile.getParentFile());
                                } catch (IOException e) {
                                    EventLogger.error(e);
                                }
                                selfStage.close();
                            },
                            () -> selfStage.close()
                    );
                } else {
                    AlertShower.showInfo(
                            selfStage,
                            "",
                            header
                    );
                    selfStage.close();
                }
            });

            System.out.println("Export success: " + success);
        }

        public void update(int processedFrames, int totalFrames) {
//            System.out.println(processedFrames + " " + totalFrames);
            double progress = (double) processedFrames / totalFrames;
            progress = Math.min(progress, 1.0);
            progressBar.setProgress(progress);
            progressLabel.setText(String.format("%.1f%%", progress * 100));
        }
    }

    public enum VideoResolution {
        RES_480P_LOW(720, 480),
        RES_480P(854, 480),
        RES_720P(1280, 720),
        RES_1080P(1920, 1080);

        public final int width;
        public final int height;

        VideoResolution(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return width + "x" + height;
        }
    }
}
