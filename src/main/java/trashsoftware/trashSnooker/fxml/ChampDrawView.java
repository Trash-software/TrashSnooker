package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.Cue;
import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.PlayerType;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.ChampionshipData;
import trashsoftware.trashSnooker.core.career.championship.Championship;
import trashsoftware.trashSnooker.core.career.championship.MatchTreeNode;
import trashsoftware.trashSnooker.core.career.championship.PlayerVsAiMatch;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;

import java.net.URL;
import java.util.ResourceBundle;

public class ChampDrawView implements Initializable {
    
    private static final Color WINNER_COLOR = Color.BLACK;
    private static final Color LOSER_COLOR = Color.GRAY;

    @FXML
    Label champNameLabel;
    @FXML
    Canvas treeCanvas;
    @FXML
    Label currentStageLabel, playerLabel;

    @FXML
    Button nextRoundButton;

    Championship championship;

    int totalRounds;

    GraphicsContext gc2d;

    double nodeHeight = 20;
    double nodeVGap = 24;
    double nodeWidth = 120.0;
    double nodeHGap = 200.0;
    double width;

    CareerView parent;
    Stage selfStage;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        championship = CareerManager.getInstance().getChampionshipInProgress();
        assert championship != null;

        gc2d = treeCanvas.getGraphicsContext2D();

        updateGui();
    }

    public void setup(CareerView parent, Stage selfStage) {
        this.parent = parent;
        this.selfStage = selfStage;
    }

    private void updateGui() {
        buildTreeGraph();
    }
    
    @FXML
    public void nextRound() {
        if (championship.isFinished()) {
            parent.refreshGui();
            selfStage.close();
        } else {
            PlayerVsAiMatch match = championship.startNextRound();
            if (match == null) {
                updateGui();
            } else {
                match.setGuiCallback(this::updateGui);

                ChampionshipData.TableSpec tableSpec = match.data.getTableSpec();

                GameValues values = new GameValues(match.data.getType(), tableSpec.tableMetrics, tableSpec.ballMetrics);

                InGamePlayer igp1;
                InGamePlayer igp2;

                PlayerType p1t = match.p1.isHumanPlayer() ? PlayerType.PLAYER : PlayerType.COMPUTER;
                PlayerType p2t = p1t == PlayerType.PLAYER ? PlayerType.COMPUTER : PlayerType.PLAYER;

                Cue aiCue;
                switch (values.rule) {
                    case SNOOKER:
                    case MINI_SNOOKER:
                        aiCue = DataLoader.getInstance().getCues().get("stdSnookerCue");
                        break;
                    case CHINESE_EIGHT:
                        aiCue = DataLoader.getInstance().getCues().get("stdPottsCue");
                        break;
                    default:
                        aiCue = DataLoader.getInstance().getCues().get("stdPoolCue");
                        break;
                }
                Cue playerCue = DataLoader.getInstance().getCues().get("stdSnookerCue");  // todo

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
                            p1t, 1);
                    igp2 = new InGamePlayer(match.p2.getPlayerPerson(), p2Cue,
                            p2t, 2);
                } else {
                    igp1 = new InGamePlayer(match.p1.getPlayerPerson(), stdBreakCue, p1Cue,
                            p1t, 1);
                    igp2 = new InGamePlayer(match.p2.getPlayerPerson(), stdBreakCue, p2Cue,
                            p2t, 2);
                }

                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("gameView.fxml")
                    );
                    Parent root = loader.load();
                    root.setStyle(App.FONT_STYLE);

                    Stage stage = new Stage();
                    stage.initOwner(this.selfStage);
                    stage.initModality(Modality.WINDOW_MODAL);

                    Scene scene = new Scene(root);
                    stage.setScene(scene);

                    GameView gameView = loader.getController();
                    gameView.setup(stage, values, match.data.getNFramesOfStage(match.getStage()),
                            igp1, igp2, tableSpec.tableCloth);
                    gameView.setCareerMatch(match);

                    stage.show();
                } catch (Exception e) {
                    EventLogger.log(e);
                }
            }
        }
    }

    private void buildTreeGraph() {
        totalRounds = championship.getData().getStages().size();
//        int leafNodesCount = 1 << totalRounds;  // 最后一层是

        width = (totalRounds + 1) * nodeHGap;

//        double height = nodeHeight * leafNodesCount;
//        treeCanvas.setHeight(height);
        treeCanvas.setWidth(width);

        Node rootNode = buildNodeTree(championship.getMatchTree().getRoot(), 0);
        double maxY = assignYValToLeaves(rootNode, nodeVGap + nodeHeight);
        System.out.println(rootNode);
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
            double x = getX(i + 1) + nodeWidth;
            gc2d.fillText(championship.getData().getStages().get(i).shown, x, nodeHeight / 2);
        }
    }

    private double getX(int depth) {
        return width - (depth + 1) * nodeHGap;
    }

    private void drawTreeToGraph(Node node, boolean isAlive, Node parent) {
        double x = node.x;
        double y = node.y;
        
        Color color = isAlive ? WINNER_COLOR : LOSER_COLOR;
        gc2d.setFill(color);
        gc2d.setStroke(color);

        gc2d.strokeRect(x, y - nodeHeight / 2, nodeWidth, nodeHeight);
        if (parent != null) {
            gc2d.strokeLine(x + nodeWidth, y, parent.x, parent.y);
        }

        if (node.node.isFinished()) {
            gc2d.fillText(node.node.getWinner().getPlayerPerson().getName(), x + 2, y + 2);
            
            if (!node.node.isLeaf()) {
                gc2d.fillText(String.valueOf(node.node.getP1Wins()), x - 10, y - 6);
                gc2d.fillText(String.valueOf(node.node.getP2Wins()), x - 10, y + 12);
            }
        } else {
            gc2d.fillText("待定", x + 2, y + 2);
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

    private double assignYValToLeaves(Node node, double currentMax) {
        if (node.left == null && node.right == null) {
            node.y = currentMax;
            return currentMax;
        } else if (node.left != null && node.right != null) {
            double leftMax = assignYValToLeaves(node.left, currentMax);
            double rightMax = assignYValToLeaves(node.right, leftMax + nodeVGap);
            node.y = (currentMax + rightMax) / 2;
            return rightMax;
        } else {
            throw new RuntimeException("Incomplete binary tree");
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
}
