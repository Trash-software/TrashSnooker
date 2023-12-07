package trashsoftware.trashSnooker.core.career.achievement;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.CareerSave;
import trashsoftware.trashSnooker.core.career.ChampionshipStage;
import trashsoftware.trashSnooker.core.metrics.Cushion;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallGame;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.JsonChecksum;
import trashsoftware.trashSnooker.util.db.DBAccess;

import java.io.*;
import java.util.*;

public class CareerAchManager extends AchManager {

    private final CareerSave careerSave;
    private final Map<Achievement, AchCompletion> recordedAchievements = new HashMap<>();  // 至少完成了一点点的
    private transient final Deque<AchCompletion> thisTimeComplete = new ArrayDeque<>();  // 记录这一杆完成的，在一次show之后清空
    private transient boolean popupShowing = false;

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
                if (ach.getType() == Achievement.Type.COLLECTIVE) {
                    // 千万不要嵌套检查，因为Achievement本身应该是直接复制的
                    AchCompletion.Collective collective =
                            AchCompletion.Collective.fromJson(ach, jsonObject.getJSONObject(key));
                    cam.recordedAchievements.put(ach, collective);
                } else {
                    AchCompletion completion = AchCompletion.fromJson(ach, jsonObject.getJSONObject(key));
                    cam.recordedAchievements.put(ach, completion);
                }
            } catch (IllegalArgumentException iae) {
                EventLogger.error("Unknown achievement: " + key);
            }
        }
        return cam;
    }

    @Override
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
        for (Map.Entry<Achievement, AchCompletion> entry : recordedAchievements.entrySet()) {
            object.put(entry.getKey().toKey(), entry.getValue().toJson());
        }
        return object;
    }

    public String getPlayerId() {
        return careerSave.getPlayerId();
    }

    @Override
    public void removePendingAch(Achievement achievement) {
        thisTimeComplete.removeIf(item -> item.achievement == achievement);
    }

    @Override
    public Map<Achievement, AchCompletion> getRecordedAchievements() {
        return recordedAchievements;
    }

//    @Override
//    public boolean completed(Achievement achievement) {
//        return achievement.isComplete(completedAchievements.get(achievement));
//    }

    /**
     * 理论上来说，最好最后再调用该方法。
     * 注意，该方法会在switchPlayer之后被调用
     */
    @Override
    public void updateAfterCueFinish(Pane owner,
                                     Game<?, ?> game,
                                     ScoreResult scoreResult,
                                     PotAttempt potAttempt,
                                     DefenseAttempt defenseAttempt,
                                     GamePlayStage playStage) {
        Player justCuedP = game.getLastCuedPlayer();
        InGamePlayer justCuedPlayer = justCuedP.getInGamePlayer();

        if (justCuedPlayer.isHuman()) {

            if (game.isEnded() && game.getWiningPlayer() != null) {
                humanContinuousPotFail = 0;
                // 一局结束后的更新
            }

            if (game.isThisCueFoul()) {
                // 犯规分支
                if (game instanceof AbstractSnookerGame asg) {
                    if (asg.getRepositionCount() >= 2) {
                        if (justCuedPlayer.isHuman()) {
                            addAchievement(Achievement.HARD_SNOOKER_BY_OPPONENT, justCuedPlayer);
                        }
                    }
                    if (game.getGameType().snookerLike()) {
                        if (defenseAttempt != null) {
                            if (defenseAttempt.getPlayParams().cueParams.selectedPower() >= 70.0) {
                                // 这个成就无关解球成功还是失败
                                addAchievement(Achievement.NOT_RESPECT, justCuedPlayer);
                            }
                        }
                    }
                }
                return;
            }

            // 没犯规了
            if (potAttempt != null) {
                if (potAttempt.isSuccess()) {
                    humanContinuousPotFail = 0;
                    addAchievement(Achievement.POT_A_BALL, justCuedPlayer);
                    addAchievement(Achievement.POT_BALLS, justCuedPlayer);

                    PlayerPerson.HandSkill handSkill = potAttempt.getHandSkill();
                    if (handSkill.hand == PlayerPerson.Hand.REST) {
                        addAchievement(Achievement.POT_BALLS_REST, justCuedPlayer);
                    } else if (handSkill.hand == justCuedPlayer.getPlayerPerson().handBody.getAntiHand().hand) {
                        addAchievement(Achievement.POT_BALLS_ANTI, justCuedPlayer);
                    }

                    // 走位相关
                    PotAttempt positionPot = potAttempt.getPositionToThis();
                    if (positionPot != null) {
                        // 有上一杆
                        Movement.Trace whiteTrace = positionPot.getWhiteTrace();
                        List<Cushion> whiteCushionAfter = whiteTrace.getCushionAfter();
                        int endCount = 0;
                        int topBotCount = 0;
                        int arcCount = 0;
                        for (Cushion cushion : whiteCushionAfter) {
                            if (cushion instanceof Cushion.EdgeCushion edge) {
                                if (edge.isEndCushion()) endCount++;
                                else topBotCount++;
                            } else if (cushion instanceof Cushion.CushionArc) {
                                arcCount++;
                            }
                        }
                        if (topBotCount >= 2 && endCount >= 1) {
//                            System.out.println("around table");
                            addAchievement(Achievement.AROUND_TABLE_POSITION, justCuedPlayer);
                        }
                        // 袋内直线不算，袋角最多算两次
                        int validCount = topBotCount + endCount + Math.min(arcCount, 2);
                        if (validCount >= 4) {
                            addAchievement(Achievement.MULTI_CUSHION_POSITION_1, justCuedPlayer);
                            if (validCount >= 7) {
                                addAchievement(Achievement.MULTI_CUSHION_POSITION_2, justCuedPlayer);
                            }
                        }
//                        System.out.printf("Cushion: %d, top bot: %d, end: %d\n",
//                                whiteCushionAfter.size(), topBotCount, endCount);
                    }

                } else {
                    humanContinuousPotFail += 1;
                    if (humanContinuousPotFail >= 3) {
                        addAchievement(Achievement.POT_FAIL_THREE, justCuedPlayer);
                    }
                    if (playStage == GamePlayStage.THIS_BALL_WIN) {
                        addAchievement(Achievement.KEY_BALL_FAIL, justCuedPlayer);
                    }
                    if (game instanceof NumberedBallGame<?>) {
                        if (game.isFirstCueAfterHandBall()) {
                            addAchievement(Achievement.FREE_BALL_FAIL, justCuedPlayer);
                        }
                    }
                }
                if (potAttempt.isLongPot()) {
                    AchManager.getInstance().addAchievement(Achievement.CUMULATIVE_LONG_POTS_1, justCuedPlayer);
                }

                List<PotAttempt> singlePole = justCuedP.getRecentSinglePoleAttempts();
                int longCount = 0;
                for (PotAttempt pa : singlePole) {
                    if (pa.isLongPot()) {
                        longCount++;
                    }
                }
                System.out.println("Single pole attack count: " + singlePole.size() + ", long: " + longCount);
                if (longCount >= 3) {
                    addAchievement(Achievement.CONTINUOUS_LONG_POT, justCuedPlayer);
                }
            }
            if (defenseAttempt != null) {
                // 下一杆还是我打，说明我进了
                // 至少斯诺克、中八、九球都是这样
                // 开球不算
                boolean defensePot = defenseAttempt.isPotLegalBall();

                if (defensePot) {
                    if (defenseAttempt.isDoublePot()) {
                        addAchievement(Achievement.DOUBLE_POT, justCuedPlayer);

                        if (game instanceof NumberedBallGame) {
                            if (game.isEnded() && game.getWiningPlayer() != null) {
                                if (game.getWiningPlayer().getInGamePlayer() == justCuedPlayer) {
                                    addAchievement(Achievement.DOUBLE_POT_WIN, justCuedPlayer);
                                }
                            }
                        }
                    }
                    if (defenseAttempt.isPassPot()) {
                        addAchievement(Achievement.PASS_POT, justCuedPlayer);
                    }
                }

                if (defenseAttempt.isSolvingSnooker()) {
                    if (defenseAttempt.isSolveSuccess()) {  // 基本可认为一定是true，否则就犯规了，不会进这个分支
                        addAchievement(Achievement.SOLVE_SNOOKER_SUCCESS, justCuedPlayer);

                        Movement.Trace whiteTrace = defenseAttempt.getWhiteTrace();
                        if (whiteTrace.getCushionBefore().size() >= 4) {
                            addAchievement(Achievement.MULTI_CUSHION_ESCAPE, justCuedPlayer);
                        }

                        if (defensePot) {
                            addAchievement(Achievement.SOLVE_SNOOKER_SUCCESS_POT, justCuedPlayer);
                        }
                    }

                    if (game.getGameType().snookerLike()) {
                        if (defenseAttempt.getPlayParams().cueParams.selectedPower() >= 70.0) {
                            // 这个成就无关解球成功还是失败
                            addAchievement(Achievement.NOT_RESPECT, justCuedPlayer);
                        }
                    }
                }
            }

            if (game.getGameType().poolLike()) {
                NumberedBallGame<?> nbg = (NumberedBallGame<?>) game;
            }
        } else {
            if (game.isEnded() && game.getWiningPlayer() != null) {
                humanContinuousPotFail = 0;
                // 一局结束后的更新
            }
            
            InGamePlayer opponent = game.getAnotherIgp(justCuedPlayer);
            if (opponent.isHuman()) {  // 保险措施，实际上一定是true
                if (defenseAttempt != null) {
                    if (defenseAttempt.isSolvingSnooker()) {
                        if (!defenseAttempt.isSolveSuccess()) {
                            addAchievement(Achievement.GAIN_BY_SNOOKER, opponent);
                            if (game instanceof AbstractSnookerGame asg) {
                                if (asg.getRepositionCount() >= 2) {
                                    addAchievement(Achievement.HARD_SNOOKER_BY_HUMAN, opponent);
                                }
                            }
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
            addAchievement(Achievement.WIN_MATCHES, human);
            humanWin = true;
        }

        if (entireGame.totalFrames >= 5) {
            if (humanWin) {
                if ((p1Human && entireGame.getP2Wins() == entireGame.totalFrames / 2) ||
                        (!p1Human && entireGame.getP1Wins() == entireGame.totalFrames / 2)) {
                    addAchievement(Achievement.BIG_HEART, human);
                    if (entireGame.getMetaMatchInfo() != null) {
                        // 必须!=null
                        if (entireGame.getMetaMatchInfo().stage == ChampionshipStage.FINAL) {
                            addAchievement(Achievement.FINAL_STAGE_FINAL_FRAME_WIN, human);
                        }
                    }
                }
            }

//            if (entireGame.totalFrames >= 5) {
            if ((p1Human && entireGame.getP1Wins() == 0) || (!p1Human && entireGame.getP2Wins() == 0)) {
                addAchievement(Achievement.LOST_ALL_MATCHES, human);
            }
            if ((p1Human && entireGame.getP2Wins() == 0) || (!p1Human && entireGame.getP1Wins() == 0)) {
                addAchievement(Achievement.WIN_ALL_MATCHES, human);
            }
//            }

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

        DBAccess.getInstance().checkAchievements();
    }

    public void cumulateAchievement(Achievement achievement, int newAdd, InGamePlayer igp) {
        if (achievement == null || (igp != null && !igp.isHuman())) return;

        AchCompletion ac = recordedAchievements.get(achievement);
        if (ac != null) {
            if (achievement.getType() == Achievement.Type.CUMULATIVE) {
                boolean changed = ac.setNewRecord(ac.getTimes() + newAdd);
                if (changed) {
                    thisTimeComplete.addLast(ac);
                }
                saveToDisk();  // 不加到thisTimeComplete里，所以现在就存。暂且认为这个save不是很花时间
            } else {
                EventLogger.error("Achievement '" + achievement + "' is not record like, Should not call this method.");
            }
            return;
        }
        AchCompletion newCompletion = new AchCompletion(achievement);
        boolean changed = newCompletion.setNewRecord(newAdd);
        recordedAchievements.put(achievement, newCompletion);
        if (changed) {
            thisTimeComplete.addLast(newCompletion);
        }
        saveToDisk();
    }

    public void addAchievement(Achievement achievement, int newRecord, InGamePlayer igp) {
        if (achievement == null || (igp != null && !igp.isHuman())) return;
        AchCompletion ac = recordedAchievements.get(achievement);
        if (ac != null) {
            if (achievement.getType() == Achievement.Type.HIGH_RECORD ||
                    achievement.getType() == Achievement.Type.CUMULATIVE) {
                boolean changed = ac.setNewRecord(newRecord);
                if (changed) {
                    thisTimeComplete.addLast(ac);
                }
                saveToDisk();  // 不加到thisTimeComplete里，所以现在就存。暂且认为这个save不是很花时间
            } else {
                EventLogger.error("Achievement '" + achievement + "' is not record like, Should not call this method.");
            }
            return;
        }
        AchCompletion newCompletion = new AchCompletion(achievement);
        boolean changed = newCompletion.setNewRecord(newRecord);
        recordedAchievements.put(achievement, newCompletion);
        if (changed) {
            thisTimeComplete.addLast(newCompletion);
        }
        saveToDisk();
    }

    @Override
    public void addAchievement(Achievement achievement, @Nullable InGamePlayer igp) {
        if (achievement == null || (igp != null && !igp.isHuman())) return;
        AchCompletion ac = recordedAchievements.get(achievement);
        if (ac != null) {
            if (achievement.getType() == Achievement.Type.CUMULATIVE) {
                boolean newComplete = ac.addOneTime();
                if (newComplete) {
                    thisTimeComplete.addLast(ac);
                }
                saveToDisk();  // 不加到thisTimeComplete里，所以现在就存。暂且认为这个save不是很花时间
            } else if (achievement.getType() == Achievement.Type.HIGH_RECORD) {
                EventLogger.error("Achievement '" + achievement + "' is record like, Should not call this method.");
            }
            return;
        }
        AchCompletion newCompletion = new AchCompletion(achievement);
        boolean newComplete = newCompletion.addOneTime();
        recordedAchievements.put(achievement, newCompletion);
        if (newComplete) {
            thisTimeComplete.addLast(newCompletion);
        }
        saveToDisk();
    }

    public void setUniqueDefeats(String opponentId, int totalWins) {
        AchCompletion.Collective defeatsCollection =
                (AchCompletion.Collective) recordedAchievements.computeIfAbsent(
                        Achievement.UNIQUE_DEFEAT,
                        k -> new AchCompletion.Collective(Achievement.UNIQUE_DEFEAT));
        boolean newComplete = defeatsCollection.setIndividual(opponentId, totalWins);
        if (newComplete) {
            AchCompletion indComp = defeatsCollection.getIndividual(opponentId);
            thisTimeComplete.addLast(indComp);
        }
    }

    @Override
    public void showAchievementPopup() {
        showAchievement(null);
    }

    protected final synchronized void showAchievement(Pane owner) {
        if (!thisTimeComplete.isEmpty()) {
            saveToDisk();
            showAchievement(owner, thisTimeComplete);
        }
    }

    /**
     * 不准从其他地方call这个！！！
     * 只允许：
     * 1. 递归调用
     * 2. 从上面这个synchronized method里调用
     */
    private void showAchievement(Pane owner, Deque<AchCompletion> achievements) {
        if (achievements.isEmpty()) return;
        if (!App.isMainWindowShowing()) return;

        AchCompletion ac = achievements.removeFirst();
        Image icon = ac.getImage();
        if (icon == null) {
            // 直接下一个
            EventLogger.error("Finished award icon is null: " + ac.achievement);
            if (!achievements.isEmpty()) {
                showAchievement(owner, achievements);
            }
            return;
        }
        popupShowing = true;

        Stage stage = new Stage();

        VBox baseRoot = new VBox();
        baseRoot.setAlignment(Pos.CENTER);
        VBox vbox = new VBox();

        Insets insetsOut = new Insets(2.0);
        Insets insets = new Insets(4.0);
        CornerRadii cornerRadii = new CornerRadii(5.0);
        baseRoot.setBackground(new Background(new BackgroundFill(Color.valueOf("#FFFCF7"),
                cornerRadii, null)));
        vbox.setBorder(new Border(new BorderStroke(Color.BURLYWOOD,
                BorderStrokeStyle.SOLID, cornerRadii, BorderWidths.DEFAULT)));
        baseRoot.setPadding(insetsOut);
        baseRoot.setStyle(App.FONT_STYLE);

        vbox.setSpacing(5.0);
        vbox.setPadding(insets);

        HBox root = new HBox();
        root.setSpacing(20.0);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(48.0);
        imageView.setFitHeight(48.0);
        imageView.setImage(icon);

        root.getChildren().add(imageView);

        VBox rightBox = new VBox();
        rightBox.setSpacing(10.0);

        Label title = new Label(ac.getTitle());

        title.setFont(titleFont);
        rightBox.getChildren().add(title);
        rightBox.getChildren().add(new Label(ac.getDescriptionOfCompleted()));  // 刚刚完成了

        root.getChildren().addAll(rightBox);

        vbox.getChildren().addAll(new Label(App.getStrings().getString("achievementComplete")), root);
        baseRoot.getChildren().add(vbox);

        Scene scene = new Scene(baseRoot);
        scene.setFill(Color.TRANSPARENT);

        Stage gameStage = App.getFullScreenStage();
        if (gameStage != null) {
            stage.initOwner(gameStage);
        }

        stage.setScene(scene);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setAlwaysOnTop(true);

        baseRoot.setOpacity(0.0);

        Timeline shower = new Timeline();
        KeyFrame showing = new KeyFrame(Duration.millis(500),
                new KeyValue(baseRoot.opacityProperty(), 0.8));
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
            popupShowing = false;
            if (!achievements.isEmpty()) {
                showAchievement(owner, achievements);
            }
        });

        stage.show();
        showToPosition(stage);

//        App.focusFullScreenStage();

        st.play();
    }

    public static void showToPosition(Stage stage) {
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
