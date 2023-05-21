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
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.ai.AiCueResult;
import trashsoftware.trashSnooker.core.career.*;
import trashsoftware.trashSnooker.core.career.championship.*;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.fxml.widgets.LabelTable;
import trashsoftware.trashSnooker.fxml.widgets.LabelTableColumn;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;

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
    Label currentStageLabel, playerLabel, savedRoundLabel;
    //            matchResLabel;
    @FXML
    ComboBox<FastGameView.CueItem> cueBox;
    @FXML
    LabelTable<MatchResItem> matchResTable;
    @FXML
    Label extraInfoLabel;
    @FXML
    Button nextRoundButton;

    Championship championship;

    int totalRounds;

    GraphicsContext gc2d;

    double nodeHeight = 20;
    double nodeVGap = 24;
    double nodeWidth = 120.0;
    double leftBlockWidth = 20.0;
    double rightBlockWidth = 20.0;
    double nodeHGap = nodeWidth + leftBlockWidth + rightBlockWidth + 20.0;
    double width;

    CareerView parent;
    Stage selfStage;
    private ResourceBundle strings;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;
        
        championship = CareerManager.getInstance().getChampionshipInProgress();
        assert championship != null;
        
        leftBlockWidth = championship.getData().getTotalPlaces() >= 100 ? 28 : 20;
        nodeHGap = nodeWidth + leftBlockWidth + rightBlockWidth + 20.0;  // recalculate

        gc2d = treeCanvas.getGraphicsContext2D();
        gc2d.setFont(App.FONT);

        refreshCueBox();
        initTable();
        updateGui();
    }

    @Override
    public Stage getStage() {
        return selfStage;
    }

    private void refreshCueBox() {
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
        
        selectSuggestedCue(human);
    }
    
    private void selectSuggestedCue(PlayerPerson human) {
        Cue humanSuggestedCue = human.getPreferredCue(championship.getData().getType());
        selectCue(humanSuggestedCue);
    }
    
    private void selectCue(Cue cue) {
        for (FastGameView.CueItem cueItem : cueBox.getItems()) {
            if (cueItem.cue == cue) {
                cueBox.getSelectionModel().select(cueItem);
                return;
            }
        }

        System.err.println("Cue '" + cue.getCueId() + "' not in list");
        cueBox.getSelectionModel().select(0);
    }

    private void initTable() {
        LabelTableColumn<MatchResItem, String> rankCol =
                new LabelTableColumn<>(matchResTable, strings.getString("rankTitle"), param ->
                        new ReadOnlyStringWrapper(param.rank.getShown()));
        LabelTableColumn<MatchResItem, Integer> awardCol =
                new LabelTableColumn<>(matchResTable, strings.getString("awards"), param ->
                        new ReadOnlyObjectWrapper<>(championship.getData().getAwardByRank(param.rank)));
        LabelTableColumn<MatchResItem, String> peopleCol =
                new LabelTableColumn<>(matchResTable, "", param ->
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
        
        if (championship.isFinished() || !championship.isHumanAlive()) {
            showCongratulation();
        }
    }

    private void updateGui() {
        if (championship.isFinished()) {
            nextRoundButton.setText(strings.getString("close"));
            savedRoundLabel.setText("");
            savedRoundLabel.setManaged(false);
        } else {
            if (championship.hasSavedRound()) {
                nextRoundButton.setText(strings.getString("continueMatch"));
                EntireGame eg = championship.getSavedRound().getGame();
                savedRoundLabel.setText(eg.getP1Wins() + " (" + eg.getTotalFrames() + ") " + eg.getP2Wins());
                savedRoundLabel.setManaged(true);
                
                InGamePlayer humanIgp = eg.getPlayer1().getPlayerType() == PlayerType.PLAYER ?
                        eg.getPlayer1() : 
                        eg.getPlayer2();
                selectCue(humanIgp.getPlayCue());
                cueBox.setDisable(true);
            } else {
                if (championship.isHumanAlive()) {
                    cueBox.setDisable(false);
                    
                    nextRoundButton.setText(strings.getString("startNextRound"));
                } else {
                    nextRoundButton.setText(strings.getString("performAllMatches"));
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

                Scene scene = new Scene(root);
                stage.setScene(scene);

                CongratView view = loader.getController();
                view.setup(humanRank, humanCareer, championship);

                stage.show();
            } catch (Exception e) {
                EventLogger.error(e);
            }
        }
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
                    ChampionshipData.TableSpec tableSpec = match.getChampionship().getData().getTableSpec();

                    GameValues values = new GameValues(match.getChampionship().getData().getType(), tableSpec.tableMetrics, tableSpec.ballMetrics);

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
            }
            match.setGuiCallback(this::pveMatchFinish, this::pveMatchFinish);
            startGame(match);
        }
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

            Scene scene = new Scene(root);
            stage.setScene(scene);

            AiCueResult.setAiPrecisionFactor(CareerManager.getInstance().getAiGoodness());

            GameView gameView = loader.getController();
            gameView.setupCareerMatch(stage, match);

            stage.show();
        } catch (Exception e) {
            EventLogger.error(e);
        }
    }

    private void buildTreeGraph() {
        totalRounds = championship.getData().getStages().length;
//        int leafNodesCount = 1 << totalRounds;  // 最后一层是

        width = (totalRounds + 1) * nodeHGap;

//        double height = nodeHeight * leafNodesCount;
//        treeCanvas.setHeight(height);
        treeCanvas.setWidth(width);

        Node rootNode = buildNodeTree(championship.getMatchTree().getRoot(), 0);
        double maxY = assignYVal(rootNode, nodeVGap + nodeHeight);
//        System.out.println(rootNode);
        treeCanvas.setHeight(maxY + nodeVGap * 2);

        gc2d.setFill(Color.WHITE);
        gc2d.fillRect(0, 0, treeCanvas.getWidth(), treeCanvas.getHeight());

        drawTitles();
        drawTreeToGraph(rootNode, true, null);
    }

    private void drawTitles() {
        gc2d.setFill(Color.BLACK);
        gc2d.setStroke(Color.BLACK);
        for (int i = 0; i < totalRounds; i++) {
            double x = getX(i + 1) + nodeWidth + leftBlockWidth;
            ChampionshipStage stage = championship.getData().getStages()[i];
            int totalFrames = championship.getData().getNFramesOfStage(stage);
            int winFrames = totalFrames / 2 + 1;
            String text = stage.getShown() + " " + totalFrames + "/" + winFrames;
            gc2d.fillText(text, x, nodeHeight / 2);
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
        gc2d.strokeLine(leftX, y1, midX, y1);
        gc2d.strokeLine(midX, y1, midX, parentY);
        gc2d.strokeLine(midX, parentY, x2, parentY);
    }

    private void drawTreeToGraph(Node node, boolean isAlive, Node parent) {
        double x = node.x;
        double y = node.y;

        Color color = isAlive ? WINNER_COLOR : LOSER_COLOR;
        gc2d.setFill(color);
        gc2d.setStroke(color);

        double y1 = y - nodeHeight / 2;
        gc2d.strokeRect(x, y1, nodeWidth + leftBlockWidth + rightBlockWidth, nodeHeight);
        gc2d.strokeLine(x + leftBlockWidth, y1, x + leftBlockWidth, y1 + nodeHeight);
        gc2d.strokeLine(x + leftBlockWidth + nodeWidth, y1, x + leftBlockWidth + nodeWidth, y1 + nodeHeight);
        if (parent != null) {
//            gc2d.strokeLine(x + nodeWidth + nodeBlockWidth, y, parent.x, parent.y);
            connectLineTo(x, y, parent.x, parent.y);
            boolean completed = parent.node.isFinished();
            if (completed) {
                boolean isLeftChild = parent.left == node;
                int winFrames = isLeftChild ? parent.node.getP1Wins() : parent.node.getP2Wins();
                gc2d.fillText(String.valueOf(winFrames), x + leftBlockWidth + nodeWidth + 3, y + 3);
            }
        }

        if (node.node.isFinished()) {
            PlayerPerson person = node.node.getWinner().getPlayerPerson();
            gc2d.fillText(person.getName(), x + leftBlockWidth + 3, y + 3);
            Integer rankSeed = championship.getCareerSeedMap().get(person.getPlayerId());
            if (rankSeed != null) {
                gc2d.fillText(String.valueOf(rankSeed), x + 3, y + 3);
            }
        } else {
            gc2d.fillText(strings.getString("undetermined"), x + leftBlockWidth + 3, y + 3);
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

    private Node buildNodeTree(MatchTreeNode mtn, int depth) {
        if (mtn == null) return null;
        Node node = new Node();
        node.node = mtn;
        node.depth = depth;

        node.x = getX(depth);

        node.left = buildNodeTree(mtn.getPlayer1Position(), depth + 1);
        node.right = buildNodeTree(mtn.getPlayer2Position(), depth + 1);

        return node;
    }

    private double assignYVal(Node node, double currentMax) {
        if (node.left == null && node.right == null) {
            node.y = currentMax;
            return currentMax;
        } else if (node.left != null && node.right != null) {
            double leftY = assignYVal(node.left, currentMax);
            double rightY = assignYVal(node.right, leftY + nodeVGap);
            node.y = (currentMax + rightY) / 2;
            return rightY;
        } else {
            throw new RuntimeException("Unexpected: incomplete binary tree");
        }
    }

    public static class Node {
        private MatchTreeNode node;

        private Node left;
        private Node right;

        private int depth;
        private double y;
        private double x;

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
