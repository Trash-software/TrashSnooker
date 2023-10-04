package trashsoftware.trashSnooker.fxml;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.ai.AiCueResult;
import trashsoftware.trashSnooker.core.career.Career;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.ChampionshipScore;
import trashsoftware.trashSnooker.core.career.ChampionshipStage;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.career.championship.*;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.metrics.BallMetrics;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.TableSpec;
import trashsoftware.trashSnooker.fxml.alert.AlertShower;
import trashsoftware.trashSnooker.fxml.widgets.LabelTable;
import trashsoftware.trashSnooker.fxml.widgets.LabelTableColumn;
import trashsoftware.trashSnooker.res.ResourcesLoader;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class ChampDrawView extends ChildInitializable {

    private static final Color WINNER_COLOR = Color.BLACK;
    private static final Color LOSER_COLOR = Color.GRAY;

    @FXML
    Label champNameLabel;
    @FXML
    Canvas treeCanvas;
    @FXML
    Label currentStageLabel, humanOpponentLabel, savedRoundLabel;
    @FXML
    Button opponentInfoBtn;
    @FXML
    ComboBox<FastGameView.CueItem> cueBox;
    @FXML
    LabelTable<MatchResItem> matchResTable;
    @FXML
    Label extraInfoLabel;
    @FXML
    Button nextRoundButton, quitTournamentBtn;
    @FXML
    Button zoomInBtn, zoomOutBtn;
    @FXML
    ComboBox<TreeShowing> treeShowingBox;
    @FXML
    ScrollPane scrollPane;

    Championship championship;
    MatchTreeNode.PvAiSnapshot nextMatchSnapshot;

    int showingRounds;

    GraphicsContext gc2d;
    Font font;
    double zoomRatio = 1.0;
    int zoomIndex = 6;
    static final double[] allowedZooms = {
            0.25, 0.35, 0.5, 0.625, 0.75, 0.9, 1.0, 1.1, 1.25, 1.5, 1.75, 2.0, 2.5
    };

    double nodeHeight = 20;
    double nodeVGap = 24;
    double nodeWidth = 120.0;
    double leftBlockWidth = 20.0;
    double rightBlockWidth = 20.0;
    double nodeHGap = nodeWidth + leftBlockWidth + rightBlockWidth + 20.0;
    double width, height;

    CareerView parent;
    Stage selfStage;
    private ResourceBundle strings;

    public static void refreshCueBox(ComboBox<FastGameView.CueItem> cueBox) {
        cueBox.getItems().clear();
        PlayerPerson human = CareerManager.getInstance().getHumanPlayerCareer().getPlayerPerson();
        for (Cue cue : human.getPrivateCues()) {
            cueBox.getItems().add(new FastGameView.CueItem(cue, cue.getName()));
        }
        for (Cue cue : DataLoader.getInstance().getCues().values()) {
            if (!cue.privacy) {
                cueBox.getItems().add(new FastGameView.CueItem(cue, cue.getName()));
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;

        championship = CareerManager.getInstance().getChampionshipInProgress();
        assert championship != null;

        champNameLabel.setText(championship.fullName());
        leftBlockWidth = championship.getData().getTotalPlaces() >= 100 ? 28 : 20;
        nodeHGap = nodeWidth + leftBlockWidth + rightBlockWidth + 20.0;  // recalculate

        font = App.FONT;
        gc2d = treeCanvas.getGraphicsContext2D();
        gc2d.setFont(font);
//        gc2d.setTextAlign(TextAlignment.JUSTIFY);

        setupCheckbox();
        refreshCueBox();
        initTable();
        updateGui();

        treeShowingBox.getSelectionModel().select(0);
    }

    @Override
    public Stage getStage() {
        return selfStage;
    }

    private void setupCheckbox() {
//        showAllMatchesBox.selectedProperty().addListener((observable, oldValue, newValue) ->
//                buildTreeGraph());
        treeShowingBox.getItems().addAll(TreeShowing.values());
        treeShowingBox.getSelectionModel().selectedIndexProperty().addListener(((observable, oldValue, newValue) ->
                buildTreeGraph()));
    }

    private void refreshCueBox() {
        refreshCueBox(cueBox);
        PlayerPerson human = CareerManager.getInstance().getHumanPlayerCareer().getPlayerPerson();
        FastGameView.selectSuggestedCue(cueBox, championship.getData().getType(), human);
    }

    private void initTable() {
        LabelTableColumn<MatchResItem, String> rankCol =
                new LabelTableColumn<>(matchResTable,
                        strings.getString("rankTitle"),
                        param ->
                                new ReadOnlyStringWrapper(param.rank.getShown()));
        LabelTableColumn<MatchResItem, Integer> awardCol =
                new LabelTableColumn<>(matchResTable,
                        ResourcesLoader.getInstance().createMoneyIcon(),
                        param ->
                                new ReadOnlyObjectWrapper<>(championship.getData().getAwardByRank(param.rank)));
        LabelTableColumn<MatchResItem, String> peopleCol =
                new LabelTableColumn<>(matchResTable,
                        param ->
                                new ReadOnlyStringWrapper(param.getPeopleString()));

        matchResTable.addColumns(rankCol, awardCol, peopleCol);
    }

    public void setup(CareerView parent, Stage selfStage) {
        this.parent = parent;
        this.selfStage = selfStage;

        this.selfStage.setOnHidden(event -> parent.refreshGui());
    }

    private void pveMatchFinish() {
        updateGui();

        if (championship.isFinished() || !championship.isHumanAlive()) {  // 这里是or，看错两次了
            int humanWinRounds = championship.getWonRoundsCount(CareerManager.getInstance().getHumanPlayerCareer());
            System.out.println("Player wins count: " + humanWinRounds);
            if (humanWinRounds == 0) {
                AchManager.getInstance().addAchievement(Achievement.ONE_ROUND_TOUR, null);
            }

            checkAfterAllMatchesFinish();
            showCongratulation();
        }
    }

    private void checkAfterAllMatchesFinish() {
        if (championship.isFinished()) {
            Career champion = championship.getChampion();
            if (champion == null) {
                EventLogger.error("Champion should not be null after finished");
                return;
            }
            if (!champion.isHumanPlayer()) {
                MatchTreeNode humanLost = championship.getHumanLostMatch();
                if (humanLost != null &&
                        humanLost.getStage() != ChampionshipStage.FINAL &&
                        humanLost.getWinner().getPlayerPerson().getPlayerId().equals(champion.getPlayerPerson().getPlayerId())) {
                    AchManager.getInstance().addAchievement(Achievement.DEFEAT_BY_CHAMPION, null);
                }
            }
        }
    }

    private void setOpponentText(MatchTreeNode.PvAiSnapshot snapshot) {
        nextMatchSnapshot = snapshot;
        opponentInfoBtn.setDisable(snapshot == null);
        if (snapshot == null) {
            humanOpponentLabel.setText("");
        } else {
            humanOpponentLabel.setText(String.format("%s vs %s",
                    snapshot.p1().getPlayerPerson().getName(),
                    snapshot.p2().getPlayerPerson().getName()));
        }
    }

    private void updateGui() {
        if (championship.isFinished()) {
            nextRoundButton.setText(strings.getString("close"));
            savedRoundLabel.setText("");
            savedRoundLabel.setManaged(false);
            quitTournamentBtn.setDisable(true);
            setOpponentText(null);
            checkAfterAllMatchesFinish();
            AchManager.getInstance().showAchievementPopup();
        } else {
            if (championship.hasSavedRound()) {
                nextRoundButton.setText(strings.getString("continueMatch"));
                EntireGame eg = championship.getSavedRound().getGame();
                savedRoundLabel.setText(eg.getP1Wins() + " (" + eg.getTotalFrames() + ") " + eg.getP2Wins());
                savedRoundLabel.setManaged(true);

                InGamePlayer humanIgp = eg.getPlayer1().getPlayerType() == PlayerType.PLAYER ?
                        eg.getPlayer1() :
                        eg.getPlayer2();
                FastGameView.selectCue(cueBox, humanIgp.getPlayCue());
                cueBox.setDisable(true);
                setOpponentText(championship.findHumanNextOpponent());
            } else {
                if (championship.isHumanAlive()) {
                    cueBox.setDisable(false);
                    nextRoundButton.setText(strings.getString("startNextRound"));
                    setOpponentText(championship.findHumanNextOpponent());
                } else {
                    nextRoundButton.setText(strings.getString("performAllMatches"));
                    quitTournamentBtn.setDisable(true);
                    setOpponentText(null);
                }
                savedRoundLabel.setText("");
                savedRoundLabel.setManaged(false);
            }
        }

        nextRoundButton.setDisable(false);
        if (championship.isFinished()) {
            currentStageLabel.setText("");
        } else {
            currentStageLabel.setText(championship.getCurrentStage().getShown());
        }
        showResults();
        buildTreeGraph();
    }

    private void showResults() {
        SortedMap<ChampionshipScore.Rank, List<Career>> matchRes = championship.getResults();

        matchResTable.clearItems();
        matchResTable.addItem(new MatchResItem(ChampionshipScore.Rank.CHAMPION)
                .ranks(matchRes.get(ChampionshipScore.Rank.CHAMPION)));
        for (ChampionshipScore.Rank rank : championship.getData().getRanksOfLosers()) {
            matchResTable.addItem(new MatchResItem(rank).ranks(matchRes.get(rank)));
        }

        if (championship instanceof SnookerChampionship) {
            showSnookerBreakScores((SnookerChampionship) championship);
        }
    }

    private void showSnookerBreakScores(SnookerChampionship sc) {
        List<SnookerBreakScore> breakScores = sc.getTopNBreaks(5);

        StringBuilder builder = new StringBuilder()
                .append(strings.getString("highestBreaks"))
                .append(" ");

        int ranking = 1;
        SnookerBreakScore last = null;
        for (int i = 0; i < breakScores.size(); i++) {
            SnookerBreakScore sbs = breakScores.get(i);
            if (last == null || last.score != sbs.score) {
                ranking = i + 1;
            }
            builder.append(ranking)
                    .append(". ")
                    .append(DataLoader.getInstance().getPlayerPerson(sbs.playerId).getName())
                    .append(" (")
                    .append(sbs.score)
                    .append(')')
                    .append("  ");

            last = sbs;
        }
        extraInfoLabel.setText(builder.toString());
    }

    private void showCongratulation() {
        AchManager.getInstance().showAchievementPopup();  // 展示比如获得冠军这些成就

        SortedMap<ChampionshipScore.Rank, List<Career>> matchRes = championship.getResults();

        ChampionshipScore.Rank humanRank = null;
        Career humanCareer = null;

        OUT_LOOP:
        for (Map.Entry<ChampionshipScore.Rank, List<Career>> entry : matchRes.entrySet()) {
            for (Career career : entry.getValue()) {
                if (career.isHumanPlayer()) {
                    humanRank = entry.getKey();
                    humanCareer = career;
                    break OUT_LOOP;
                }
            }
        }

        if (humanCareer != null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("congratView.fxml"),
                        strings
                );
                Parent root = loader.load();
                root.setStyle(App.FONT_STYLE);

                Stage stage = new Stage();

                stage.initOwner(this.selfStage);
                stage.initModality(Modality.WINDOW_MODAL);
                stage.initStyle(StageStyle.UTILITY);

                Scene scene = App.createScene(root);
                stage.setScene(scene);

                CongratView view = loader.getController();
                view.setup(humanRank, humanCareer, championship);

                stage.show();
            } catch (Exception e) {
                EventLogger.error(e);
            }
        }
    }

    private void quitTournament() {
        if (!championship.isHumanAlive()) return;
        if (championship.isFinished()) return;
        if (championship.hasSavedRound()) {
            PlayerVsAiMatch match = championship.continueSavedRound();
            quitCurrentMatch(match);
        } else {
            PlayerVsAiMatch match;
            while ((match = championship.startNextRound()) == null) {
                // do nothing
            }
            startGameInNewRound(match);
            quitCurrentMatch(match);
        }
        AchManager.getInstance().showAchievementPopup();  // 展示比如获得冠军这些成就。当然这里不会是冠军
        updateGui();
    }

    private void quitCurrentMatch(PlayerVsAiMatch match) {
        EntireGame eg = match.getGame();
        eg.quitMatch(match.getHumanCareer().getPlayerPerson());
        PlayerPerson winner = match.getAiCareer().getPlayerPerson();
        match.finish(winner,
                eg.getP1Wins(),
                eg.getP2Wins());
    }

    @Override
    public void backAction() {
        parent.refreshGui();

        super.backAction();
    }

    @FXML
    public void zoomInAction() {
        zoomRatio = allowedZooms[++zoomIndex];
        zoomOutBtn.setDisable(zoomIndex == 0);
        zoomInBtn.setDisable(zoomIndex == allowedZooms.length - 1);
        
        font = new Font(App.FONT.getName(), App.FONT.getSize() * zoomRatio);
        gc2d.setFont(font);
        gc2d.setLineWidth(zoomRatio);
        buildTreeGraph();

        System.out.println(zoomRatio + " " + font.getSize());
    }

    @FXML
    public void zoomOutAction() {
        zoomRatio = allowedZooms[--zoomIndex];
        zoomOutBtn.setDisable(zoomIndex == 0);
        zoomInBtn.setDisable(zoomIndex == allowedZooms.length - 1);
        
        font = new Font(App.FONT.getName(), App.FONT.getSize() * zoomRatio);
        gc2d.setFont(font);
        gc2d.setLineWidth(zoomRatio);
        buildTreeGraph();
    }

    @FXML
    public void opponentInfoAction() {
        if (nextMatchSnapshot != null) {
            PlayerPerson person = nextMatchSnapshot.p1().isHumanPlayer() ?
                    nextMatchSnapshot.p2().getPlayerPerson() :
                    nextMatchSnapshot.p1().getPlayerPerson();
            if (person != null) {
                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("abilityView.fxml"),
                            strings
                    );
                    Parent root = loader.load();
                    root.setStyle(App.FONT_STYLE);

                    Stage stage = new Stage();
                    stage.initOwner(selfStage);
                    stage.initModality(Modality.WINDOW_MODAL);

                    Scene scene = App.createScene(root);
                    stage.setScene(scene);

                    stage.show();

                    AbilityView controller = loader.getController();
                    controller.setup(scene, person);
                } catch (IOException e) {
                    EventLogger.error(e);
                }
            }
        }
    }

    @FXML
    public void quitTournamentAction() {
        AlertShower.askConfirmation(selfStage,
                strings.getString("quitTournamentDes"),
                strings.getString("quitTournamentConfirmation"),
                this::quitTournament,
                null
        );
    }

    @FXML
    public void nextRound() {
        if (championship.isFinished()) {
            backAction();
        } else {
            nextRoundButton.setDisable(true);
            PlayerVsAiMatch match;
            if (championship.hasSavedRound()) {
                match = championship.continueSavedRound();
            } else {
                match = championship.startNextRound();
                if (match == null) {
                    if (!championship.isFinished()) {
                        nextRound();
                    }
                    updateGui();
                    return;
                } else {
                    startGameInNewRound(match);
                }
            }
            match.setGuiCallback(this::pveMatchFinish, this::pveMatchFinish);
            startGame(match);
        }
    }

    private void startGameInNewRound(PlayerVsAiMatch match) {
        TableSpec tableSpec = match.getChampionship().getData().getTableSpec();
        BallMetrics ballMetrics = match.getChampionship().getData().getBallMetrics();

        GameValues values = new GameValues(match.getChampionship().getData().getType(), tableSpec.tableMetrics, ballMetrics);
        values.setTablePreset(match.getChampionship().getData().getTablePreset());

        InGamePlayer igp1;
        InGamePlayer igp2;

        PlayerType p1t = match.p1.isHumanPlayer() ? PlayerType.PLAYER : PlayerType.COMPUTER;
        PlayerType p2t = p1t == PlayerType.PLAYER ? PlayerType.COMPUTER : PlayerType.PLAYER;

        PlayerPerson aiPerson = p1t == PlayerType.COMPUTER ?
                match.p1.getPlayerPerson() : match.p2.getPlayerPerson();

        double p1HandFeelEffort = match.p1.isHumanPlayer() ?
                1.0 : match.p1.getHandFeelEffort(championship.getData().getType());
        double p2HandFeelEffort = match.p2.isHumanPlayer() ?
                1.0 : match.p2.getHandFeelEffort(championship.getData().getType());

        Cue aiCue = aiPerson.getPreferredCue(championship.getData().getType());
        Cue playerCue = cueBox.getValue().cue;

        Cue p1Cue;
        Cue p2Cue;

        if (match.p1.isHumanPlayer()) {
            p1Cue = playerCue;
            p2Cue = aiCue;
        } else {
            p1Cue = aiCue;
            p2Cue = playerCue;
        }

        Cue stdBreakCue = DataLoader.getInstance().getStdBreakCue();
        if (stdBreakCue == null ||
                values.rule == GameRule.SNOOKER ||
                values.rule == GameRule.MINI_SNOOKER) {
            igp1 = new InGamePlayer(match.p1.getPlayerPerson(), p1Cue,
                    p1t, 1, p1HandFeelEffort);
            igp2 = new InGamePlayer(match.p2.getPlayerPerson(), p2Cue,
                    p2t, 2, p2HandFeelEffort);
        } else {
            igp1 = new InGamePlayer(match.p1.getPlayerPerson(), stdBreakCue, p1Cue,
                    p1t, 1, p1HandFeelEffort);
            igp2 = new InGamePlayer(match.p2.getPlayerPerson(), stdBreakCue, p2Cue,
                    p2t, 2, p2HandFeelEffort);
        }

        EntireGame newGame = new EntireGame(
                igp1,
                igp2,
                values,
                match.getChampionship().getData().getNFramesOfStage(match.getStage()),
                tableSpec.tableCloth,
                match.metaMatchInfo
        );

        match.setGame(newGame);
    }

    private void startGame(PlayerVsAiMatch match) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("gameView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage stage = new Stage();

            stage.setTitle(championship.fullName() + " " + championship.getCurrentStage().getShown());

            stage.initOwner(this.selfStage);
            stage.initModality(Modality.WINDOW_MODAL);

            Scene scene = App.createScene(root);
            stage.setScene(scene);

            AiCueResult.setAiPrecisionFactor(CareerManager.getInstance().getAiGoodness());

            GameView gameView = loader.getController();
            gameView.setupCareerMatch(stage, match);

            stage.show();

            App.scaleGameStage(stage, gameView);
        } catch (Exception e) {
            EventLogger.error(e);
        }
    }

    private void buildTreeGraph() {
        TreeShowing treeShowing = treeShowingBox.getValue();
        if (treeShowing == null) treeShowing = TreeShowing.FULL;

        Node rootNode;
        MatchTreeNode rootMatch = championship.getMatchTree().getRoot();

        switch (treeShowing) {
            case ALIVE -> {
                rootNode = onlyBuildAlive(rootMatch, 0, true);
                showingRounds = rootNode.height() - 1;
            }
            case REMAINING -> {
                ChampionshipStage limit;
                if (championship.isFinished()) {
                    limit = ChampionshipStage.FINAL;
                } else {
                    limit = championship.getCurrentStage();
                }
                rootNode = buildNodeTree(rootMatch, 0, limit, true);
                showingRounds = Util.indexOf(limit, championship.getData().getStages()) + 1;
            }
            default -> {
                rootNode = buildNodeTree(rootMatch, 0, null, true);
                showingRounds = championship.getData().getStages().length;
            }
        }
//        int leafNodesCount = 1 << totalRounds;  // 最后一层是

//        System.out.println(rootNode.height() + " " + showingRounds);

        width = (showingRounds + 1) * nodeHGap;

        double maxY = assignPosition(rootNode, nodeVGap + nodeHeight);
//        System.out.println(rootNode);
        height = maxY + nodeVGap * 2;
        treeCanvas.setWidth(width * zoomRatio);
        treeCanvas.setHeight(height * zoomRatio);

        // wipe
        gc2d.setFill(GameView.GLOBAL_BACKGROUND);
        gc2d.fillRect(0, 0,
                treeCanvas.getWidth(),
                treeCanvas.getHeight());

        drawTitles();
        drawTreeToGraph(rootNode, true, null);
    }

    private void drawTitles() {
        gc2d.setFill(Color.BLACK);
        gc2d.setStroke(Color.BLACK);
        for (int i = 0; i < showingRounds; i++) {
            ChampionshipStage stage = championship.getData().getStages()[i];
            int totalFrames = championship.getData().getNFramesOfStage(stage);
            int winFrames = totalFrames / 2 + 1;
            String text = stage.getShown() + " " + totalFrames + "/" + winFrames;
            double x = getX(i + 1) + nodeWidth + leftBlockWidth;
            fillText(text, x, nodeHeight / 2);
        }
    }

    private double getX(int depth) {
        return width - (depth + 1) * nodeHGap;
    }

    private void connectLineTo(double x1, double y1, double x2, double y2) {
        if (x1 > x2) {  // 让线都从左到右连
            double tempX = x1;
            double tempY = y1;
            x1 = x2;
            x2 = tempX;
            y1 = y2;
            y2 = tempY;
        }
        double parentY;
        if (y1 > y2) {
            parentY = y2 + 2.0;
        } else {
            parentY = y2 - 2.0;
        }
        double leftX = x1 + nodeWidth + leftBlockWidth + rightBlockWidth;
        double midX = (leftX + x2) / 2;
        strokeLine(leftX,
                y1,
                midX,
                y1);
        strokeLine(midX,
                y1,
                midX,
                parentY);
        strokeLine(midX,
                parentY,
                x2,
                parentY);
    }

    private void drawTreeToGraph(Node node, boolean isAlive, Node parent) {
        double x = node.x;
        double y = node.y;

        Color color = isAlive ? WINNER_COLOR : LOSER_COLOR;
        gc2d.setFill(color);
        gc2d.setStroke(color);

        double y1 = y - nodeHeight / 2;
        strokeRect(x, 
                y1,
                nodeWidth  + leftBlockWidth + rightBlockWidth, 
                nodeHeight);
        strokeLine(x + leftBlockWidth, y1, x + leftBlockWidth, y1 + nodeHeight);
        strokeLine(x + leftBlockWidth + nodeWidth, y1, x + leftBlockWidth + nodeWidth, y1 + nodeHeight);
        if (parent != null) {
//            gc2d.strokeLine(x + nodeWidth + nodeBlockWidth, y, parent.x, parent.y);
            connectLineTo(x, y, parent.x, parent.y);
            boolean completed = parent.node.isFinished();
            if (completed) {
                boolean isLeftChild = parent.left == node;
                int winFrames = isLeftChild ? parent.node.getP1Wins() : parent.node.getP2Wins();
                fillText(String.valueOf(winFrames), x + leftBlockWidth + nodeWidth + 3, y + 3);
            }
        }
        
        if (node.node.isFinished()) {
            PlayerPerson person = node.node.getWinner().getPlayerPerson();
            fillText(person.getName(), x + leftBlockWidth + 3, y + 3);
            Integer rankSeed = championship.getCareerSeedMap().get(person.getPlayerId());
            if (rankSeed != null) {
                fillText(String.valueOf(rankSeed), x + 3, y + 3);
            }
        } else {
            fillText(strings.getString("undetermined"), x + leftBlockWidth + 3, y + 3);
        }

        if (node.left != null && node.right != null) {
            boolean leftAlive = isAlive;
            boolean rightAlive = isAlive;

            if (node.node.getWinner() != null) {
                if (node.node.getWinner() == node.left.node.getWinner()) {
                    // 这里都有winner了，上一场肯定也有
                    rightAlive = false;
                } else {
                    leftAlive = false;
                }
            }

            drawTreeToGraph(node.left, leftAlive, node);
            drawTreeToGraph(node.right, rightAlive, node);
        }
    }
    
    private void strokeLine(double x1, double y1, double x2, double y2) {
        gc2d.strokeLine(x1 * zoomRatio,
                y1 * zoomRatio,
                x2 * zoomRatio,
                y2 * zoomRatio);
    }

    private void strokeRect(double x, double y, double w, double h) {
        gc2d.strokeRect(x * zoomRatio,
                y * zoomRatio,
                w * zoomRatio,
                h * zoomRatio);
    }
    
    private void fillText(String text, double x, double y) {
        gc2d.fillText(text, x * zoomRatio, y * zoomRatio);
    }

    private Node onlyBuildAlive(MatchTreeNode mtn, int depth, boolean recursive) {
        if (mtn == null) return null;

        Node node = new Node();
        node.node = mtn;
        node.depth = depth;

        node.x = getX(depth);

        if (recursive) {
            if (mtn.getWinner() == null) {
                node.left = onlyBuildAlive(mtn.getPlayer1Position(), depth + 1, true);
                node.right = onlyBuildAlive(mtn.getPlayer2Position(), depth + 1, true);
            } else if (!mtn.isLeaf()) {
                node.left = onlyBuildAlive(mtn.getPlayer1Position(), depth + 1,
                        mtn.isP1Win());
                node.right = onlyBuildAlive(mtn.getPlayer2Position(), depth + 1,
                        mtn.isP2Win());
            }
        }

        return node;
    }

    private Node buildNodeTree(MatchTreeNode mtn, int depth, ChampionshipStage limit, boolean recursive) {
        if (mtn == null) return null;

        Node node = new Node();
        node.node = mtn;
        node.depth = depth;

        if (recursive) {
            boolean nextRecursive = limit != mtn.getStage();
            node.left = buildNodeTree(mtn.getPlayer1Position(), depth + 1, limit, nextRecursive);
            node.right = buildNodeTree(mtn.getPlayer2Position(), depth + 1, limit, nextRecursive);
        }
        return node;
    }

    private double assignPosition(Node node, double currentMax) {
        node.x = getX(node.depth);
        if (node.left == null && node.right == null) {
            node.y = currentMax;
            return currentMax;
        } else if (node.left != null && node.right != null) {
            double leftY = assignPosition(node.left, currentMax);
            double rightY = assignPosition(node.right, leftY + nodeVGap);
            node.y = (currentMax + rightY) / 2;
            return rightY;
        } else {
            throw new RuntimeException("Unexpected: incomplete binary tree");
        }
    }

    public enum TreeShowing {
        FULL,
        ALIVE,
        REMAINING;

        @Override
        public String toString() {
            return App.getStrings().getString(Util.toLowerCamelCase("TREE_SHOWING_" + name()));
        }
    }

    public static class Node {
        private MatchTreeNode node;

        private Node left;
        private Node right;

        private int depth;
        private double y;
        private double x;

        int height() {
            int leftH = left == null ? 0 : left.height();
            int rightH = right == null ? 0 : right.height();
            return Math.max(leftH, rightH) + 1;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "node=" + (node.getWinner() == null ? "und" : node.getWinner().getPlayerPerson().getPlayerId()) +
                    ", left=" + left +
                    ", right=" + right +
                    ", depth=" + depth +
                    ", y=" + y +
                    '}';
        }
    }

    public static class MatchResItem {
        ChampionshipScore.Rank rank;
        List<Career> people = new ArrayList<>();

        MatchResItem(ChampionshipScore.Rank rank) {
            this.rank = rank;
        }

        MatchResItem ranks(List<Career> careers) {
            if (careers != null) {
                people.addAll(careers);
            }
            return this;
        }

        String getPeopleString() {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < people.size(); i++) {
                Career career = people.get(i);
                builder.append(career.getPlayerPerson().getName());
                if (i != people.size() - 1) {
                    builder.append(" / ");
                }
            }
            return builder.toString();
        }
    }
}
