package trashsoftware.trashSnooker.core.career.achievement;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.career.CareerSave;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallGame;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.JsonChecksum;

import java.io.*;
import java.util.*;

public class CareerAchManager extends AchManager {
//    private final List<Achievement> snookerNormal = List.of(
//            Achievement.POT_A_BALL,
//            Achievement.SNOOKER_BREAK_100
//    );
//    private final List<Achievement> chineseEightNormal = List.of(
//            Achievement.POT_A_BALL,
//            Achievement.POOL_BREAK_POT
//    );
    private final CareerSave careerSave;
    private final Map<Achievement, AchCompletion> completedAchievements = new HashMap<>();
    private transient final List<Achievement> thisTimeComplete = new ArrayList<>();  // 记录这一杆完成的，在一次show之后清空

    private final Font titleFont = Font.font(App.FONT.getFamily(), FontWeight.BLACK, 16.0);
    
    private int humanContinuousPotFail;

    CareerAchManager(CareerSave careerSave) {
        this.careerSave = careerSave;
    }

    private static File storageFile(CareerSave careerSave) {
        return new File(careerSave.getDir(), "achievements.json");
    }

    static CareerAchManager loadFromDisk(CareerSave careerSave) {
        CareerAchManager cam = new CareerAchManager(careerSave);

        File file = storageFile(careerSave);
        JSONObject jsonObject;
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                StringBuilder builder = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    builder.append(line);
                }
                JSONObject outer = new JSONObject(builder.toString());

                // todo: checksum
                jsonObject = outer.getJSONObject("achievements");
            } catch (IOException | JSONException e) {
                EventLogger.error(e);
                jsonObject = new JSONObject();
            }
        } else {
            jsonObject = new JSONObject();
        }

        for (String key : jsonObject.keySet()) {
            try {
                Achievement ach = Achievement.fromKey(key);
                AchCompletion completion = AchCompletion.fromJson(jsonObject.getJSONObject(key));
                cam.completedAchievements.put(ach, completion);
            } catch (IllegalArgumentException iae) {
                System.err.println("Unknown achievement: " + key);
            }
        }
        return cam;
    }

    private Set<Achievement> notCompleted(List<Achievement> check) {
        Set<Achievement> result = new HashSet<>();
        for (Achievement ach : check) {
            if (!completed(ach)) result.add(ach);
        }
        return result;
    }

    public void saveToDisk() {
        JSONObject jsonObject = new JSONObject();
        JSONObject root = toJson();

        jsonObject.put("achievements", root);
        jsonObject.put("checksum", JsonChecksum.checksum(root));

        String string = jsonObject.toString(2);

        File file = storageFile(careerSave);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(string);
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }

    @Override
    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        for (Map.Entry<Achievement, AchCompletion> entry : completedAchievements.entrySet()) {
            object.put(entry.getKey().toKey(), entry.getValue().toJson());
        }
        return object;
    }

    @Override
    public void removePendingAch(Achievement achievement) {
        thisTimeComplete.remove(achievement);
    }

    @Override
    public Map<Achievement, AchCompletion> getCompletedAchievements() {
        return completedAchievements;
    }

    @Override
    public boolean completed(Achievement achievement) {
        return achievement.isComplete(completedAchievements.get(achievement));
    }

    /**
     * 理论上来说，最好最后再调用该方法
     */
    @Override
    public void updateAfterCueFinish(Pane owner, Game<?, ?> game, ScoreResult scoreResult,
                                     PotAttempt potAttempt, DefenseAttempt defenseAttempt, 
                                     GamePlayStage playStage) {
//        List<Achievement> all = switch (game.getGameType()) {
//            case SNOOKER -> snookerNormal;
//            case CHINESE_EIGHT -> chineseEightNormal;
//            default -> new ArrayList<>();
//        };
        
        InGamePlayer justCuedPlayer = game.getCuingIgp();
        
        if (justCuedPlayer.isHuman()) {

//        Set<Achievement> notCompleted = notCompleted(all);

            if (game.isEnded() && game.getWiningPlayer() != null) {
                humanContinuousPotFail = 0;
                // 一局结束后的更新
                if (!game.getGameValues().isTraining() && game.getWiningPlayer().getInGamePlayer().isHuman()) {
                    addAchievement(Achievement.WIN_A_FRAME, justCuedPlayer);
                    
                    if (game instanceof AbstractSnookerGame asg) {
                        int maxAhead = asg.getMaxScoreDiff(justCuedPlayer.getPlayerNumber());
                        if (maxAhead <= -65) {
                            addAchievement(Achievement.COME_BACK_BEHIND_65, justCuedPlayer);
                        }
                        if ((justCuedPlayer.getPlayerNumber() == 1 && asg.isP2EverOver()) || 
                                (justCuedPlayer.getPlayerNumber() == 2 && asg.isP1EverOver())) {
                            // 从被超分逆转胜利
                            addAchievement(Achievement.COME_BACK_BEHIND_OVER_SCORE, justCuedPlayer);
                        }
                    }
                }
            }

            if (game.isThisCueFoul()) {
                // 犯规分支
                showAchievement(owner);
                return;
            }

            // 没犯规了
            if (potAttempt != null) {
                if (potAttempt.isSuccess()) {
                    humanContinuousPotFail = 0;
                    addAchievement(Achievement.POT_A_BALL, justCuedPlayer);
                    addAchievement(Achievement.POT_EIGHT_BALLS, justCuedPlayer);
                } else {
                    humanContinuousPotFail += 1;
                    if (humanContinuousPotFail >= 3) {
                        addAchievement(Achievement.POT_FAIL_THREE, justCuedPlayer);
                    }
                    if (playStage == GamePlayStage.THIS_BALL_WIN) {
                        addAchievement(Achievement.KEY_BALL_FAIL, justCuedPlayer);
                    }
                }
            }
            if (defenseAttempt != null) {
                if (defenseAttempt.isSolvingSnooker()) {
                    if (defenseAttempt.isSolveSuccess()) {
                        addAchievement(Achievement.SOLVE_SNOOKER_SUCCESS, justCuedPlayer);
                    }
                }
            }

            if (game.getGameType().poolLike()) {
                NumberedBallGame<?> nbg = (NumberedBallGame<?>) game;
            }

            showAchievement(owner);
        } else {
            InGamePlayer opponent = game.getAnotherIgp(justCuedPlayer);
            if (opponent.isHuman()) {
                if (defenseAttempt != null) {
                    if (defenseAttempt.isSolvingSnooker()) {
                        if (!defenseAttempt.isSolveSuccess()) {
                            addAchievement(Achievement.GAIN_BY_SNOOKER, opponent);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void updateAfterMatchEnds(EntireGame entireGame) {
//        .....检查
        boolean p1Human = entireGame.getPlayer1().isHuman();
        InGamePlayer human = p1Human ? entireGame.getPlayer1() : entireGame.getPlayer2();
        boolean humanWin = false;

        if ((p1Human && entireGame.getP1Wins() > entireGame.getP2Wins()) ||
                (!p1Human && entireGame.getP2Wins() > entireGame.getP1Wins())) {
            addAchievement(Achievement.WIN_A_MATCH, human);
            humanWin = true;
        }

        if (entireGame.totalFrames >= 3) {
            if (humanWin) {
                if ((p1Human && entireGame.getP2Wins() == entireGame.totalFrames / 2) || 
                        (!p1Human && entireGame.getP1Wins() == entireGame.totalFrames / 2)) {
                    addAchievement(Achievement.BIG_HEART, human);
                }
            }
            
            if (entireGame.totalFrames >= 5) {
                if ((p1Human && entireGame.getP1Wins() == 0) || (!p1Human && entireGame.getP2Wins() == 0)) {
                    addAchievement(Achievement.LOST_ALL_MATCHES, human);
                }
                if ((p1Human && entireGame.getP2Wins() == 0) || (!p1Human && entireGame.getP1Wins() == 0)) {
                    addAchievement(Achievement.WIN_ALL_MATCHES, human);
                }
            }

            if (entireGame.totalFrames >= 9) {
//                int frameNeed = entireGame.totalFrames / 2 + 1;
                int achJudge = 5;
                if (entireGame.getP1Wins() + entireGame.getP2Wins() == entireGame.totalFrames) {
                    // 打满了
                    if (p1Human) {
                        if (entireGame.playerContinuousLoses(2) >= achJudge) {
                            // 玩家连胜翻盘
                            addAchievement(Achievement.LEGENDARY_REVENGE, human);
                        } else if (entireGame.playerContinuousLoses(1) >= achJudge) {
                            // 玩家连败被翻盘
                            addAchievement(Achievement.LEGENDARY_REVENGED, human);
                        }
                    } else {
                        if (entireGame.playerContinuousLoses(1) >= achJudge) {
                            // 玩家连胜翻盘
                            addAchievement(Achievement.LEGENDARY_REVENGE, human);
                        } else if (entireGame.playerContinuousLoses(2) >= achJudge) {
                            // 玩家连败被翻盘
                            addAchievement(Achievement.LEGENDARY_REVENGED, human);
                        }
                    }
                }
            }
        }

        Platform.runLater(this::showAchievementPopup);
    }

    public void addAchievement(Achievement achievement, int newRecord, InGamePlayer igp) {
        if (!igp.isHuman()) return;
        AchCompletion ac = completedAchievements.get(achievement);
        if (ac != null) {
            if (achievement.isRecordLike()) {
                boolean newComplete = ac.setNewRecord(achievement, newRecord);
                if (newComplete) {
                    thisTimeComplete.add(achievement);
                }
                saveToDisk();  // 不加到thisTimeComplete里，所以现在就存。暂且认为这个save不是很花时间
            } else {
                System.err.println("Achievement '" + achievement + "' is not record like, Should not call this method.");
            }
        }
        AchCompletion newCompletion = new AchCompletion(newRecord);
        if (achievement.isComplete(newCompletion)) {
            newCompletion.setFirstCompletion(new Date(System.currentTimeMillis()));

            completedAchievements.put(achievement, newCompletion);
            thisTimeComplete.add(achievement);
        } else {
            completedAchievements.put(achievement, newCompletion);
        }
        saveToDisk();
    }

    @Override
    public void addAchievement(Achievement achievement, @Nullable InGamePlayer igp) {
        if (igp != null && !igp.isHuman()) return;
        AchCompletion ac = completedAchievements.get(achievement);
        if (ac != null) {
            if (achievement.countLikeRepeatable()) {
                boolean newComplete = ac.addOneTime(achievement);
                if (newComplete) {
                    thisTimeComplete.add(achievement);
                }
                saveToDisk();  // 不加到thisTimeComplete里，所以现在就存。暂且认为这个save不是很花时间
            }
            return;
        }
        AchCompletion newCompletion = new AchCompletion();
        if (achievement.isComplete(newCompletion)) {
            newCompletion.setFirstCompletion(new Date(System.currentTimeMillis()));

            completedAchievements.put(achievement, newCompletion);
            thisTimeComplete.add(achievement);
        } else {
            completedAchievements.put(achievement, newCompletion);
        }
        saveToDisk();
    }

    @Override
    public void showAchievementPopup() {
        showAchievement(null);
    }

    private void showAchievement(Pane owner) {
        if (!thisTimeComplete.isEmpty()) {
            saveToDisk();
            showAchievement(owner, thisTimeComplete, 0);
        }
    }

    private void showAchievement(Pane owner, List<Achievement> achievements, int index) {
        Achievement achievement = achievements.get(index);
        Stage stage = new Stage();

        VBox baseRoot = new VBox();
//        baseRoot.setStyle(".vbox { -fx-background-color: red; }");
        baseRoot.setSpacing(5.0);
        baseRoot.setStyle(App.FONT_STYLE);
        
        HBox root = new HBox();
        root.setSpacing(20.0);

        Insets padding = new Insets(5.0);
        root.setPadding(padding);

        ImageView imageView = new ImageView();
        
        root.getChildren().add(imageView);

        VBox rightBox = new VBox();
        rightBox.setSpacing(10.0);

        Label title = new Label(achievement.title());

        title.setFont(titleFont);
        rightBox.getChildren().add(title);
        rightBox.getChildren().add(new Label(achievement.description()));

        root.getChildren().addAll(rightBox);
        
        baseRoot.getChildren().addAll(new Label(App.getStrings().getString("achievementComplete")), root);

        Scene scene = new Scene(baseRoot);
        scene.setFill(Color.TRANSPARENT);

        stage.setScene(scene);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        baseRoot.setOpacity(0.0);

        Timeline shower = new Timeline();
        KeyFrame showing = new KeyFrame(Duration.millis(500),
                new KeyValue(baseRoot.opacityProperty(), 0.75));
        shower.getKeyFrames().add(showing);

        Timeline keeper = new Timeline();
        keeper.getKeyFrames().add(new KeyFrame(Duration.millis(2000)));

        Timeline fader = new Timeline();
        KeyFrame fading = new KeyFrame(Duration.millis(500),
                new KeyValue(baseRoot.opacityProperty(), 0.0));
        fader.getKeyFrames().add(fading);

        SequentialTransition st = new SequentialTransition(shower, keeper, fader);
        st.setOnFinished(event -> {
            stage.hide();
            if (index < achievements.size() - 1) {
                showAchievement(owner, achievements, index + 1);
            } else {
                thisTimeComplete.clear();  // 显完了，清空
            }
        });
        
        stage.show();
        showToPosition(stage);
//        stage.setX(0);
//        stage.setY(0);
        
        st.play();
    }
    
    private void showToPosition(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double centerX =
                bounds.getMinX() + (bounds.getWidth() - stage.getWidth())
                        * 0.5;
        double centerY =
                bounds.getMinY() + (bounds.getHeight() - stage.getHeight())
                        * 0.7;

        stage.setX(centerX);
        stage.setY(centerY);
    }
}
