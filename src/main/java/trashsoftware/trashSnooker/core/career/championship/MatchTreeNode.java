package trashsoftware.trashSnooker.core.career.championship;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.*;
import trashsoftware.trashSnooker.core.career.aiMatch.AiVsAi;
import trashsoftware.trashSnooker.core.career.aiMatch.ChineseAiVsAi;
import trashsoftware.trashSnooker.core.career.aiMatch.SnookerAiVsAi;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

public class MatchTreeNode {

    private static int matchIdCounter = 0;
    private MetaMatchInfo metaMatchInfo;
    private MatchTreeNode player1Position;
    private MatchTreeNode player2Position;
    private ChampionshipStage stage;
    private Career winner;
    private int p1Wins;
    private int p2Wins;

    public MatchTreeNode(Career winner) {
        this.winner = winner;
    }

    private MatchTreeNode(MatchTreeNode player1Position,
                          MatchTreeNode player2Position,
                          ChampionshipStage stage,
                          MetaMatchInfo metaMatchInfo) {
        this.player1Position = player1Position;
        this.player2Position = player2Position;
        this.stage = stage;
        this.metaMatchInfo = metaMatchInfo;
    }

    public MatchTreeNode(MatchTreeNode player1Position,
                         MatchTreeNode player2Position,
                         ChampionshipStage stage,
                         Championship championship) {
        this(player1Position, player2Position, stage, MetaMatchInfo.fromString(generateId(championship, stage)));
    }

    static void restoreIdCounter() {
        matchIdCounter = 0;
    }

    static String generateId(Championship championship, ChampionshipStage stage) {
        return championship.uniqueId() + "-" + stage.name() + "-" + (matchIdCounter++);
    }

    /**
     * @see Championship#uniqueId()
     * @see MatchTreeNode#generateId(Championship, ChampionshipStage)
     */
    public static MetaMatchInfo analyzeMatchId(String matchId) {
        return MetaMatchInfo.fromString(matchId);
    }

    public static MatchTreeNode fromJsonObject(JSONObject object) {
        if (object.has("stage")) {
            ChampionshipStage stage = ChampionshipStage.valueOf(object.getString("stage"));
            MatchTreeNode p1 = MatchTreeNode.fromJsonObject(object.getJSONObject("p1"));
            MatchTreeNode p2 = MatchTreeNode.fromJsonObject(object.getJSONObject("p2"));

            String matchId;
            if (object.has("matchId")) {
                // 这里用if是为了向下兼容，本来每个有stage的都是比赛，都应该有matchId
                matchId = object.getString("matchId");
            } else {
                matchId = "generated_" + (matchIdCounter++);
            }

            MetaMatchInfo metaMatchInfo = MetaMatchInfo.fromString(matchId, stage);

            MatchTreeNode rtn = new MatchTreeNode(p1, p2, stage, metaMatchInfo);

            if (object.has("winner")) {
                rtn.winner = CareerManager.getInstance().findCareerByPlayerId(object.getString("winner"));
                rtn.p1Wins = object.getInt("p1wins");
                rtn.p2Wins = object.getInt("p2wins");
            }
            return rtn;
        } else if (object.has("winner")) {
            return new MatchTreeNode(
                    CareerManager.getInstance().findCareerByPlayerId(object.getString("winner"))
            );
        } else {
            throw new RuntimeException("Unknown node " + object);
        }
    }

    public JSONObject saveToJson() {
        JSONObject object = new JSONObject();

        if (stage == null) {
            // 仅表示参赛选手
            object.put("winner", winner.getPlayerPerson().getPlayerId());
        } else {
            object.put("stage", stage.name());
            object.put("p1", player1Position.saveToJson());
            object.put("p2", player2Position.saveToJson());
            object.put("matchId", metaMatchInfo.toString());

            if (winner != null) {
                object.put("winner", winner.getPlayerPerson().getPlayerId());
                object.put("p1wins", p1Wins);
                object.put("p2wins", p2Wins);
            }
        }

        return object;
    }

    void slCheck(ChampionshipStage currentStage) {

    }

    public MetaMatchInfo getMetaMatchInfo() {
        return metaMatchInfo;
    }

//    public String getMatchId() {
//        return matchId;
//    }

    public MatchTreeNode findNodeByPlayers(String p1Id, String p2Id) {
        if (isLeaf()) return null;

        if (this.player1Position.winner != null &&
                this.player1Position.winner.getPlayerPerson().getPlayerId().equals(p1Id) &&
                this.player2Position.winner != null &&
                this.player2Position.winner.getPlayerPerson().getPlayerId().equals(p2Id)) {
            return this;
        }

        MatchTreeNode leftRes = player1Position.findNodeByPlayers(p1Id, p2Id);
        if (leftRes != null) return leftRes;
        return player2Position.findNodeByPlayers(p1Id, p2Id);
    }

    public ChampionshipStage getStage() {
        return stage;
    }

    public MatchTreeNode getPlayer1Position() {
        return player1Position;
    }

    public MatchTreeNode getPlayer2Position() {
        return player2Position;
    }

    public int getP1Wins() {
        return p1Wins;
    }

    public int getP2Wins() {
        return p2Wins;
    }

    PlayerVsAiMatch performMatches(Championship championship, ChampionshipStage stage) {
        if (stage == this.stage) {
            if (player1Position == null || player2Position == null ||
                    !player1Position.isFinished() || !player2Position.isFinished()) {
                throw new RuntimeException("Inconsistent stage of " + stage);
            }
            Career c1 = player1Position.winner;
            Career c2 = player2Position.winner;
            if (c1.isHumanPlayer() || c2.isHumanPlayer()) {
                return new PlayerVsAiMatch(c1, c2, championship, stage, this);
            }

            performAiVsAiMatch(championship, stage);
            return null;
        } else if (this.winner != null) {
            return null;  // 这属于守株待兔的选手
        } else if (this.stage == null) {
            throw new RuntimeException("Tree does not have stage " + stage);
        } else {
            var up = player1Position.performMatches(championship, stage);
            var down = player2Position.performMatches(championship, stage);
            return up == null ? down : up;
        }
    }

    private void performAiVsAiMatch(Championship championship, ChampionshipStage stage) {
        ChampionshipData data = championship.getData();
        AiVsAi aiVsAi;
        switch (championship.getData().getType()) {
            case SNOOKER:
                aiVsAi = new SnookerAiVsAi(
                        player1Position.winner,
                        player2Position.winner,
                        championship,
                        metaMatchInfo.toString(),
                        data.getNFramesOfStage(stage));
                break;
            case CHINESE_EIGHT:
                aiVsAi = new ChineseAiVsAi(
                        player1Position.winner,
                        player2Position.winner,
                        championship,
                        metaMatchInfo.toString(),
                        data.getNFramesOfStage(stage)
                );
                break;
            case LIS_EIGHT:
            case MINI_SNOOKER:
            case SIDE_POCKET:
            default:
                throw new UnsupportedOperationException();
        }
        aiVsAi.simulate();
        Career winner = aiVsAi.getWinner();
        setWinner(winner, aiVsAi.getP1WinFrames(), aiVsAi.getP2WinFrames());
        System.out.println(aiVsAi);
    }

    public void getResults(ChampionshipData data,
                           SortedMap<ChampionshipScore.Rank, List<Career>> results,
                           int depth) {
        if (winner != null) {
            if (depth == 0) {
                results.put(ChampionshipScore.Rank.CHAMPION, new ArrayList<>(List.of(winner)));
            }
            Career loser = getLoser();  // 除了冠军，每个人都只会输一次（目前没有双败赛制）
            ChampionshipScore.Rank rank = data.getRanksOfLosers()[depth];

            List<Career> careersOfThisRank = results.computeIfAbsent(rank, k -> new ArrayList<>());
            careersOfThisRank.add(loser);
        }

        if (!player1Position.isLeaf()) player1Position.getResults(data, results, depth + 1);
        if (!player2Position.isLeaf()) player2Position.getResults(data, results, depth + 1);
    }

    public Career getWinner() {
        return winner;
    }

    public void setWinner(Career winner, int p1Wins, int p2Wins) {
        if (winner == null) throw new RuntimeException("Why no winner");
        this.p1Wins = p1Wins;
        this.p2Wins = p2Wins;
        this.winner = winner;
    }

    public Career getLoser() {
        if (winner == null) return null;
        return winner == player1Position.winner ? player2Position.winner : player1Position.winner;
    }

    public boolean isFinished() {
        return winner != null;
    }

    public boolean isLeaf() {
        return player1Position == null && player2Position == null;  // 同时也意味着stage == null
    }

    public boolean isP1Win() {
        return winner == player1Position.winner;
    }

    public boolean isP2Win() {
        return winner == player2Position.winner;
    }

    public boolean isHumanAlive() {
        if (winner == null) {
            if (player1Position.isHumanAlive()) return true;
            if (player2Position.isHumanAlive()) return true;
            return false;
        } else {
            return winner.isHumanPlayer();
        }
    }

    int getWonRounds(Career career, boolean encountered) {
        if (isLeaf()) return 0;
        if (winner == career) {
            encountered = true;
        }

        if (encountered) {
            if (winner == player1Position.winner) {
                return player1Position.getWonRounds(career, true) + 1;
            }
            if (winner == player2Position.winner) {
                return player2Position.getWonRounds(career, true) + 1;
            }
            throw new RuntimeException("?");
        } else {
            return Math.max(player1Position.getWonRounds(career, false),
                    player2Position.getWonRounds(career, false));
        }
    }
}
