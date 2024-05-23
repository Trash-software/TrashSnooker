package trashsoftware.trashSnooker.util.db;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.career.achievement.CareerAchManager;
import trashsoftware.trashSnooker.core.career.championship.MetaMatchInfo;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.ChineseEightBallGame;
import trashsoftware.trashSnooker.core.numberedGames.nineBall.AmericanNineBallGame;
import trashsoftware.trashSnooker.core.numberedGames.nineBall.AmericanNineBallPlayer;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;
import trashsoftware.trashSnooker.core.snooker.MaximumType;
import trashsoftware.trashSnooker.core.snooker.SnookerPlayer;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;

public class DBAccess {
    public static final boolean SAVE = true;
    public static final boolean RECORD = true;

    private static final Map<GameRule, Map<String, Achievement>> ACHIEVEMENT_MAP = Map.of(
            GameRule.SNOOKER, Map.of(
                    "DEFEAT_UNIQUE_OPPONENTS", Achievement.DEFEAT_UNIQUE_OPPONENTS_SNOOKER,
                    "DEFEAT_SAME_CONTINUOUS", Achievement.DEFEAT_SAME_OPPONENT_CONTINUOUS_SNOOKER,
                    "DEFEAT_SAME_MULTI_1", Achievement.DEFEAT_SAME_OPPONENT_MULTI_SNOOKER_1,
                    "DEFEAT_SAME_MULTI_2", Achievement.DEFEAT_SAME_OPPONENT_MULTI_SNOOKER_2
            ),
            GameRule.CHINESE_EIGHT, Map.of(
                    "DEFEAT_UNIQUE_OPPONENTS", Achievement.DEFEAT_UNIQUE_OPPONENTS_CEB,
                    "DEFEAT_SAME_CONTINUOUS", Achievement.DEFEAT_SAME_OPPONENT_CONTINUOUS_CEB,
                    "DEFEAT_SAME_MULTI_1", Achievement.DEFEAT_SAME_OPPONENT_MULTI_CEB_1,
                    "DEFEAT_SAME_MULTI_2", Achievement.DEFEAT_SAME_OPPONENT_MULTI_CEB_2
            ),
            GameRule.AMERICAN_NINE, Map.of(
                    "DEFEAT_UNIQUE_OPPONENTS", Achievement.DEFEAT_UNIQUE_OPPONENTS_AMERICAN,
                    "DEFEAT_SAME_CONTINUOUS", Achievement.DEFEAT_SAME_OPPONENT_CONTINUOUS_AMERICAN,
                    "DEFEAT_SAME_MULTI_1", Achievement.DEFEAT_SAME_OPPONENT_MULTI_AMERICAN_1,
                    "DEFEAT_SAME_MULTI_2", Achievement.DEFEAT_SAME_OPPONENT_MULTI_AMERICAN_2
            )
    );

    private static DBAccess database;

    private Connection connection;

    private DBAccess() {
        try {
            if (SAVE) {
                connection = DriverManager.getConnection("jdbc:sqlite:user/records.db");
            } else {
                connection = DriverManager.getConnection("jdbc:sqlite:user/empty.db");
            }
            connection.setAutoCommit(true);
            createTablesIfNotExists();
            updateDbStructure();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    public static DBAccess getInstance() {
        if (database == null) {
            database = new DBAccess();
        }
        return database;
    }

    public static void closeDB() {
        if (database != null) {
            database.close();
            database = null;
        }
    }

    public static void main(String[] args) {
        System.out.println(new Timestamp(System.currentTimeMillis()));
        DBAccess db = getInstance();
        System.out.println(db.listAllPlayerIds());
//        db.printQuery("SELECT * FROM SnookerRecord;");
//        System.out.println("========");
//        db.printQuery("SELECT * FROM GeneralRecord WHERE PlayerName = 'John Wizard';");
//        System.out.println("========");
//        db.printQuery("SELECT * FROM SnookerRecord;");

//        db.printQuery("SELECT * FROM Game INNER JOIN GeneralRecord USING (EntireBeginTime, FrameIndex)" +
//                " WHERE EntireBeginTime = '2021-11-06 17:20:25.53';");
//        db.insertPlayerIfNotExists("Ding");
//        System.out.println(db.playerExists("Ding"));
//        db.getPlayerSnookerScores("Ding");


        closeDB();
    }

    private void updateDbStructure() {
        alterTable("ALTER TABLE EntireGame ADD COLUMN MatchID TEXT DEFAULT null;");
        alterTable("ALTER TABLE SidePocketRecord ADD COLUMN GoldNine INTEGER DEFAULT 0;");

        alterTable("ALTER TABLE EntireGame ADD COLUMN SubRule TEXT DEFAULT NULL;");
        alterTable("ALTER TABLE SnookerRecord DROP COLUMN IsMaximum;");
        alterTable("ALTER TABLE SnookerRecord ADD COLUMN MaximumType TEXT DEFAULT NULL;",
                () -> {
                    // 推测可能的满分杆数量
                    MaximumType[] types = {
                            MaximumType.MAXIMUM_147,
                            MaximumType.MAXIMUM_107, 
                            MaximumType.MAXIMUM_75
                    };
                    String[] names = {"Snooker", "SnookerTen", "MiniSnooker"};
                    int[] values = {147, 107, 75};
                    for (int i = 0; i < 3; i++) {
                        String updater = String.format("""
                                UPDATE SnookerRecord
                                SET MaximumType = '%s'
                                WHERE (Highest >= %d
                                AND EntireBeginTime IN (
                                    SELECT EntireBeginTime
                                    FROM EntireGame
                                    WHERE GameType = '%s'
                                ))""", types[i], values[i], names[i]);
                        try (Statement stmt = connection.createStatement()) {
                            stmt.execute(updater);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void alterTable(String query) {
        alterTable(query, null);
    }

    private void alterTable(String query, Runnable successCallback) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(query);
            System.out.println("Executed: " + query);
            if (successCallback != null) {
                successCallback.run();
            }
        } catch (SQLException ex) {
            System.out.println("Not executed: " + query);
        }
    }

    public void printAllPlayers() {
        String cmd = "SELECT * FROM Player;";
        try {
            Statement stmt = connection.createStatement();
            ResultSet resultSet = stmt.executeQuery(cmd);

            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getTypeKey(GameRule gameRule, String playerName) {
        return String.format("'%s_%s'", playerName, gameRule.toSqlKey());
    }

    public void checkAchievements() {
        AchManager achManager = AchManager.getInstance();
        if (achManager instanceof CareerAchManager cam) {
            String playerId = cam.getPlayerId();

            Map<String, int[]> totalWinLosses = new HashMap<>();  // 对每个球员的胜负，不管什么规则
            for (GameRule gameRule : List.of(GameRule.SNOOKER, GameRule.CHINESE_EIGHT, GameRule.AMERICAN_NINE)) {
                Map<String, Achievement> ruleAchMap = ACHIEVEMENT_MAP.get(gameRule);
                List<EntireGameTitle> titles = getAllPveMatches(gameRule, playerId, false, true);
                List<EntireGameRecord> entireRecords = new ArrayList<>();  // 是有序的
                for (EntireGameTitle egt : titles) {
                    if (MetaMatchInfo.matchIdIsByCareer(egt.matchId, playerId)) {
                        entireRecords.add(DBAccess.getInstance().getMatchDetail(egt));
                    }
                }
                int uniqueWins = getUniqueWins(
                        cam,
                        ruleAchMap,
                        entireRecords,
                        playerId,
                        totalWinLosses);
                cam.addAchievement(ruleAchMap.get("DEFEAT_UNIQUE_OPPONENTS"), uniqueWins, null);
            }
            for (Map.Entry<String, int[]> oppoTotalWL : totalWinLosses.entrySet()) {
//                System.out.println(oppoTotalWL.getKey() + ": " + Arrays.toString(oppoTotalWL.getValue()));
                PlayerPerson person = DataLoader.getInstance().getPlayerPerson(oppoTotalWL.getKey());
                int wins = oppoTotalWL.getValue()[0];
                if (!person.isRandom && wins > 0) {
                    cam.setUniqueDefeats(oppoTotalWL.getKey(), wins);
                }
            }
        }
    }

    private static int[] winLossCount(List<Integer> opponentWinLoss) {
        int[] winLoss = new int[2];
        for (int result : opponentWinLoss) {
            if (result == 0) {
                winLoss[0]++;
            } else {
                winLoss[1]++;
            }
        }
        return winLoss;
    }

    private static int getUniqueWins(
            CareerAchManager cam,
            Map<String, Achievement> ruleAchMap,
            List<EntireGameRecord> entireRecords,
            String playerId,
            Map<String, int[]> totalWinLosses  // 这个不参与计算，只是fill这个表
    ) {
        Map<String, List<Integer>> oppoWinLostMap = new HashMap<>();  // 对于每个对手的胜负。0是玩家胜，1是对手胜
        for (EntireGameRecord egr : entireRecords) {
            if (!egr.isFinished()) continue;  // 没完成的比赛不算
            EntireGameTitle egt = egr.getTitle();
            String oppoId;
            boolean oppoWin;
            if (egt.getPlayer1Id().equals(playerId)) {
                oppoId = egt.getPlayer2Id();
                int[] winsCount = egr.getP1P2WinsCount();
                oppoWin = winsCount[1] > winsCount[0];
            } else {
                oppoId = egt.getPlayer1Id();
                int[] winsCount = egr.getP1P2WinsCount();
                oppoWin = winsCount[1] < winsCount[0];
            }
            List<Integer> cur = oppoWinLostMap.computeIfAbsent(oppoId, key -> new ArrayList<>());
            if (oppoWin) {
                cur.add(1);
            } else {
                cur.add(0);
            }

            int maxContinuousWin = 0;
            int continuousWin = 0;  // 最近的连胜
            for (int i = cur.size() - 1; i >= 0; i--) {
                if (cur.get(i) == 0) {
                    continuousWin++;
                    if (continuousWin > maxContinuousWin) maxContinuousWin = continuousWin;
                } else {
                    continuousWin = 0;
                }
            }
            if (maxContinuousWin >= 2) {
//                System.out.println("Continuous win " + oppoId + " " + maxContinuousWin + " times");
                cam.addAchievement(ruleAchMap.get("DEFEAT_SAME_CONTINUOUS"), null);
            }
        }

        int uniqueWins = 0;  // 赢过的不同球员
        for (var ent : oppoWinLostMap.entrySet()) {
            if (ent.getValue().contains(0)) {
                uniqueWins++;
            }
            int[] winLoss = winLossCount(ent.getValue());
            int[] totalWinLossAgainst = totalWinLosses.computeIfAbsent(ent.getKey(), key -> new int[2]);
            for (int i = 0; i < totalWinLossAgainst.length; i++) {
                // 应该就是2，保险起见，万一以后有平局呢
                totalWinLossAgainst[i] += winLoss[i];
            }

            if (winLoss[0] >= 3 && winLoss[1] == 0) {
                cam.addAchievement(ruleAchMap.get("DEFEAT_SAME_MULTI_1"), null);
            }
            int sum = winLoss[0] + winLoss[1];  // ent.getValue().size()
            double winRate = (double) winLoss[0] / sum;
            if (sum >= 10 && winRate >= 0.9) {
                cam.addAchievement(ruleAchMap.get("DEFEAT_SAME_MULTI_2"), null);
            }
        }
        return uniqueWins;
    }

    private void storeAttemptsForOnePlayer(EntireGame entireGame,
                                           Game<?, ?> frame,
                                           Player player,
                                           boolean isWinner) {
        // attempts, successes, 
        // long attempts, long successes, 
        // defenses, defense successes,
        // position, position success
        // rest attempts, rest success
        // solves, solve successes
        int[] data = new int[12];
        System.out.println(player.getPlayerPerson().getPlayerId() + ": " + player.getAttempts().size());
        for (CueAttempt attempt : player.getAttempts()) {
            if (attempt instanceof PotAttempt potAttempt) {
                data[0]++;
                if (potAttempt.isSuccess()) data[1]++;
                if (potAttempt.isLongPot()) {
                    data[2]++;
                    if (potAttempt.isSuccess()) data[3]++;
                }
                PotAttempt.Position position = potAttempt.getPositionSuccess();
                if (position != PotAttempt.Position.NOT_SET) {
                    data[6]++;
                    if (position == PotAttempt.Position.SUCCESS) {
                        data[7]++;
                    }
                }

                if (potAttempt.isRestPot()) {
                    data[8]++;
                    if (potAttempt.isSuccess()) {
                        data[9]++;
                    }
                }
            } else if (attempt instanceof DefenseAttempt defenseAttempt) {
                data[4]++;
                if (defenseAttempt.isSuccess()) {
                    data[5]++;
                }
                if (defenseAttempt.isSolvingSnooker()) {
                    data[10]++;
                    if (defenseAttempt.isSolveSuccess()) {
                        data[11]++;
                    }
                }
            }
        }

        // 添加成就
        if (data[0] == 0) {
            if (!isWinner) {
                AchManager.getInstance().addAchievement(Achievement.FRAME_NO_ATTACK, player.getInGamePlayer());
            }
        }
        if (data[1] == 0) {
            if (isWinner) {
                AchManager.getInstance().addAchievement(Achievement.LIE_DOWN_WIN, player.getInGamePlayer());
            }
        }

        if (player.getScore() == 0) {
            int threshold = 5;
            Achievement achievement = null;
            if (frame instanceof AbstractSnookerGame) {
                achievement = Achievement.SNOOKER_NO_POT;
            } else if (frame instanceof ChineseEightBallGame) {
                achievement = Achievement.CHINESE_EIGHT_NO_POT;
            } else if (frame instanceof AmericanNineBallGame) {
                achievement = Achievement.AMERICAN_NINE_NO_POT;
                threshold = 4;
            }
            if (data[0] + data[4] >= threshold) {  // 攻+防 应该就是总杆数
                AchManager.getInstance().addAchievement(achievement, player.getInGamePlayer());
            }
        }

        String queryWhere = getFrameQueryWhere(entireGame, frame,
                player.getPlayerPerson().getPlayerId(),
                player.getInGamePlayer().getPlayerType() == PlayerType.COMPUTER,
                true);
        String query =
                "UPDATE GeneralRecord SET " +
                        "Attempts = Attempts + " + data[0] + ", " +
                        "Successes = Successes + " + data[1] + ", " +
                        "LongAttempts = LongAttempts + " + data[2] + ", " +
                        "LongSuccesses = LongSuccesses + " + data[3] + ", " +
                        "Defenses = Defenses + " + data[4] + ", " +
                        "DefenseSuccesses = DefenseSuccesses + " + data[5] + ", " +
                        "Positions = Positions + " + data[6] + ", " +
                        "PositionSuccesses = PositionSuccesses + " + data[7] + ", " +
                        "RestAttempts = RestAttempts + " + data[8] + ", " +
                        "RestSuccesses = RestSuccesses + " + data[9] + ", " +
                        "Solves = Solves + " + data[10] + ", " +
                        "SolveSuccesses = SolveSuccesses + " + data[11] +
                        queryWhere;
        try {
            executeStatement(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void storeAttempts(EntireGame entireGame, Game<?, ?> frame, InGamePlayer frameWinner) {
        storeAttemptsForOnePlayer(entireGame,
                frame,
                frame.getPlayer1(),
                frameWinner == frame.getPlayer1().getInGamePlayer());
        storeAttemptsForOnePlayer(entireGame,
                frame,
                frame.getPlayer2(),
                frameWinner == frame.getPlayer2().getInGamePlayer());
    }

    public List<EntireGameTitle> getAllMatches(GameRule gameRule, boolean careerOnly) {
        String optional = "";
        if (careerOnly) {
            optional = " AND MatchID IS NOT NULL";
        }
        
        String query = "SELECT * FROM EntireGame WHERE (GameType = '" + gameRule.toSqlKey() + "'" +
                optional +
                ");";
        return getMatchesBy(gameRule, query);
    }

    public List<EntireGameTitle> getAllPveMatches(GameRule gameRule, boolean careerOnly) {
        String typeStr = "'" + gameRule.toSqlKey() + "'";
        String career = careerOnly ? " MatchID IS NOT NULL AND " : " ";
        String query = "SELECT * FROM EntireGame " +
                "WHERE GameType = " + typeStr + " AND " +
                career +
                "((Player1IsAI = 1 AND Player2IsAI = 0) OR (Player1IsAI = 0 AND Player2IsAI = 1))" +
                "ORDER BY EntireBeginTime;";
        return getMatchesBy(gameRule, query);
    }

    public List<EntireGameTitle> getAllPveMatches(GameRule gameRule, 
                                                  String playerName, 
                                                  boolean playerIsAi, 
                                                  boolean careerOnly) {
        String typeStr = "'" + gameRule.toSqlKey() + "'";
        String playerStr = "'" + playerName + "'";
        int aiRep = playerIsAi ? 1 : 0;
        String career = careerOnly ? " MatchID IS NOT NULL AND " : " ";
        String query = "SELECT * FROM EntireGame " +
                "WHERE GameType = " + typeStr + " AND " +
                career + 
                "((Player1Name = " + playerStr + " AND " +
                "Player1IsAI = " + aiRep +
                ") OR (Player2Name = " + playerStr + " AND " +
                "Player2IsAI = " + aiRep + "))" +
                "ORDER BY EntireBeginTime;";
        return getMatchesBy(gameRule, query);
    }

    private List<EntireGameTitle> getMatchesBy(GameRule gameRule, String query) {
        List<EntireGameTitle> rtn = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                EntireGameTitle egr = new EntireGameTitle(
                        resultSet.getTimestamp("EntireBeginTime"),
                        gameRule,
                        resultSet.getString("Player1Name"),
                        resultSet.getString("Player2Name"),
                        resultSet.getInt("Player1IsAI") == 1,
                        resultSet.getInt("Player2IsAI") == 1,
                        resultSet.getInt("TotalFrames"),
                        resultSet.getString("MatchID"),
                        SubRule.commaStringToSubRules(resultSet.getString("SubRule"))
                );
                if (isValidPlayer(egr.player1Id) && isValidPlayer(egr.player2Id)) {
                    rtn.add(egr);
                }
            }

            resultSet.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Collections.reverse(rtn);
        return rtn;
    }

    private boolean isValidPlayer(String playerId) {
        return DataLoader.getInstance().hasPlayer(playerId);
    }

    /**
     * 返回{totalScore, highest, 50+，100+，maximum}
     */
    public int[] getSnookerBreaksTotal(GameRule gameRule, String playerName, boolean playerIsAi) {
        int[] rtn = new int[5];
        String pns = "'" + playerName + "'";
        String typeKey = "'" + gameRule.toSqlKey() + "'";
        int aiRep = playerIsAi ? 1 : 0;
        String highestQuery =
                "SELECT * FROM SnookerRecord " +
                        "WHERE (PlayerName = " + pns + " AND " +
                        "PlayerIsAI = " + aiRep + " AND " +
                        "EntireBeginTime IN " +
                        "(SELECT EntireBeginTime FROM EntireGame " +
                        "WHERE GameType = " + typeKey + "));";
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(highestQuery);
            while (resultSet.next()) {
                int highInResult = resultSet.getInt("Highest");
                rtn[0] += resultSet.getInt("TotalScore");
                if (highInResult > rtn[1]) {
                    rtn[1] = highInResult;
                }
                rtn[2] += resultSet.getInt("Breaks50");
                if (highInResult >= 100) {
                    rtn[3]++;
                }
                if ((gameRule == GameRule.SNOOKER && highInResult >= 147) ||
                        (gameRule == GameRule.MINI_SNOOKER && highInResult >= 75) ||
                        (gameRule == GameRule.SNOOKER_TEN && highInResult >= 107)) {
                    rtn[4]++;
                }
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rtn;
    }

    /**
     * 返回{开球次数，开球进球次数，炸请，接清，单杆最高球数，黄金九（仅九球）}
     */
    public int[] getNumberedBallGamesTotal(GameRule gameRule, String playerName, boolean playerIsAi) {
        int[] rtn = new int[6];
        String pns = "'" + playerName + "'";
        String tableName = gameRule.toSqlKey() + "Record";
        String typeKey = "'" + gameRule.toSqlKey() + "'";
        int aiRep = playerIsAi ? 1 : 0;
        String highestQuery =
                "SELECT * FROM " + tableName + " " +
                        "WHERE (PlayerName = " + pns + " AND " +
                        "PlayerIsAI = " + aiRep + " AND " +
                        "EntireBeginTime IN " +
                        "(SELECT EntireBeginTime FROM EntireGame " +
                        "WHERE GameType = " + typeKey + "));";
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(highestQuery);
            while (resultSet.next()) {
                rtn[0] += resultSet.getInt("Breaks");
                rtn[1] += resultSet.getInt("BreakPots");
                int highInResult = resultSet.getInt("Highest");
                rtn[2] += resultSet.getInt("BreakClear");
                rtn[3] += resultSet.getInt("ContinueClear");
                if (highInResult > rtn[4]) {
                    rtn[4] = highInResult;
                }
                if (gameRule == GameRule.AMERICAN_NINE) {
                    rtn[5] += resultSet.getInt("GoldNine");
                }
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rtn;
    }

    /**
     * 返回{进攻次数，进攻成功次数，长台进攻次数，长台成功次数，防守次数，防守成功次数，走位次数，走位成功次数，架杆进攻次数，架杆进攻成功次数，解球次数，解球成功次数}
     */
    public int[] getBasicPotStatusAll(GameRule gameRule, String playerName, boolean isAi) {
        int[] array = new int[12];
        String pns = "'" + playerName + "'";
        String typeKey = "'" + gameRule.toSqlKey() + "'";
        int aiRep = isAi ? 1 : 0;
        String cmd =
                "SELECT * FROM GeneralRecord " +
                        "WHERE (PlayerName = " + pns + " AND " +
                        "PlayerIsAI = " + aiRep + " AND " +
                        "EntireBeginTime IN " +
                        "(SELECT EntireBeginTime FROM EntireGame " +
                        "WHERE GameType = " + typeKey + "));";
        try {
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(cmd);
            while (result.next()) {
                array[0] += result.getInt("Attempts");
                array[1] += result.getInt("Successes");
                array[2] += result.getInt("LongAttempts");
                array[3] += result.getInt("LongSuccesses");
                array[4] += result.getInt("Defenses");
                array[5] += result.getInt("DefenseSuccesses");
                array[6] += result.getInt("Positions");
                array[7] += result.getInt("PositionSuccesses");
                array[8] += result.getInt("RestAttempts");
                array[9] += result.getInt("RestSuccesses");
                array[10] += result.getInt("Solves");
                array[11] += result.getInt("SolveSuccesses");
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return array;
    }

    public EntireGameRecord getMatchDetail(EntireGameTitle title) {
        Map<Integer, int[][]> framesPotMap = new HashMap<>();
        Map<Integer, String> framesWinnerMap = new HashMap<>();
        String query = "SELECT * FROM Game " +
                "INNER JOIN GeneralRecord USING (EntireBeginTime, FrameIndex) " +
                "WHERE EntireBeginTime = " + Util.timeStampFmt(title.startTime) +
                ";";
        SortedMap<Integer, PlayerFrameRecord[]> records = new TreeMap<>();  // Sortable treemap
        SortedMap<Integer, Integer> durations = new TreeMap<>();
        try {
            Statement general = connection.createStatement();
            ResultSet result = general.executeQuery(query);

            while (result.next()) {
                int index = result.getInt("FrameIndex");
                int[] array = new int[12];
                array[0] = result.getInt("Attempts");
                array[1] = result.getInt("Successes");
                array[2] = result.getInt("LongAttempts");
                array[3] = result.getInt("LongSuccesses");
                array[4] += result.getInt("Defenses");
                array[5] += result.getInt("DefenseSuccesses");
                array[6] += result.getInt("Positions");
                array[7] += result.getInt("PositionSuccesses");
                array[8] += result.getInt("RestAttempts");
                array[9] += result.getInt("RestSuccesses");
                array[10] += result.getInt("Solves");
                array[11] += result.getInt("SolveSuccesses");
                durations.put(index, result.getInt("DurationSeconds"));
                String frameWinner = result.getString("WinnerName");
                if (frameWinner == null) {
                    System.err.println("Shit!");
                    continue;
                }
                int playerIndex = result.getString("PlayerName")
                        .equals(title.player1Id) ? 0 : 1;
                int[][] playerArray = framesPotMap.computeIfAbsent(index, k -> new int[2][]);
                playerArray[playerIndex] = array;
                framesWinnerMap.put(index, frameWinner);
            }
            general.close();

            if (title.gameRule.snookerLike()) {
                String snookerQuery = "SELECT * FROM SnookerRecord " +
                        "WHERE EntireBeginTime = " + Util.timeStampFmt(title.startTime) +
                        ";";
                Statement sn = connection.createStatement();
                ResultSet snRes = sn.executeQuery(snookerQuery);
                while (snRes.next()) {
                    int index = snRes.getInt("FrameIndex");
                    int playerIndex = snRes.getString("PlayerName")
                            .equals(title.player1Id) ? 0 : 1;
                    int[] scores = new int[5];  // total score, highest, breaks50, breaks100, maximum
                    scores[0] = snRes.getInt("TotalScore");
                    scores[1] = snRes.getInt("Highest");
                    scores[2] = snRes.getInt("Breaks50");
                    if (scores[1] >= 100) {
                        scores[3]++;
                    }
                    String mtStr = snRes.getString("MaximumType");
                    MaximumType mt = mtStr == null ? MaximumType.NONE : MaximumType.valueOf(mtStr);
                    if (mt != MaximumType.NONE) {
                        scores[4]++;
                    }
//                    if ((title.gameRule == GameRule.SNOOKER && scores[1] >= 147) || 
//                            (title.gameRule == GameRule.MINI_SNOOKER && scores[1] >= 75) || 
//                            (title.gameRule == GameRule.SNOOKER_TEN && scores[1] >= 107)) {
//                        scores[4]++;
//                    }
                    PlayerFrameRecord.Snooker snooker = new PlayerFrameRecord.Snooker(
                            index, framesPotMap.get(index)[playerIndex],
                            framesWinnerMap.get(index), scores
                    );
                    PlayerFrameRecord[] thisFrame =
                            records.computeIfAbsent(index, k -> new PlayerFrameRecord[2]);
                    thisFrame[playerIndex] = snooker;
                }

                sn.close();

                return new EntireGameRecord.Snooker(title, records, durations);
            } else if (title.gameRule == GameRule.CHINESE_EIGHT ||
                    title.gameRule == GameRule.AMERICAN_NINE ||
                    title.gameRule == GameRule.LIS_EIGHT) {
                String tableName = title.gameRule.toSqlKey() + "Record";
                String numQuery = "SELECT * FROM " + tableName + " " +
                        "WHERE EntireBeginTime = " + Util.timeStampFmt(title.startTime) +
                        ";";
                Statement numStatement = connection.createStatement();
                ResultSet numRs = numStatement.executeQuery(numQuery);
                while (numRs.next()) {
                    int index = numRs.getInt("FrameIndex");
                    int playerIndex = numRs.getString("PlayerName")
                            .equals(title.player1Id) ? 0 : 1;
                    int[] scores = new int[6];  // breaks, break successes, break clear, continue clear, highest, gold nines (nine ball only)
                    scores[0] = numRs.getInt("Breaks");
                    scores[1] = numRs.getInt("BreakPots");
                    scores[2] = numRs.getInt("BreakClear");
                    scores[3] = numRs.getInt("ContinueClear");
                    scores[4] = numRs.getInt("Highest");
                    if (title.gameRule == GameRule.AMERICAN_NINE) {
                        scores[5] = numRs.getInt("GoldNine");
                    }
                    PlayerFrameRecord.Numbered numbered = new PlayerFrameRecord.Numbered(
                            index, framesPotMap.get(index)[playerIndex],
                            framesWinnerMap.get(index), scores
                    );
                    PlayerFrameRecord[] thisFrame =
                            records.computeIfAbsent(index, k -> new PlayerFrameRecord[2]);
                    thisFrame[playerIndex] = numbered;
                }
                numStatement.close();

                if (title.gameRule == GameRule.CHINESE_EIGHT || title.gameRule == GameRule.LIS_EIGHT) {
                    return new EntireGameRecord.ChineseEight(title, records, durations);
                } else {
                    return new EntireGameRecord.SidePocket(title, records, durations);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getEntireGameQueryWhere(EntireGame entireGame, String playerName,
                                           boolean endLine) {
        return " WHERE (EntireBeginTime = " + entireGame.getStartTimeSqlString() + " AND " +
                "PlayerName = '" + playerName + "')" + (endLine ? ";" : "");
    }

    private String getFrameQueryWhere(EntireGame entireGame, Game<?, ?> game, String playerName,
                                      boolean playerIsAi,
                                      boolean endLine) {
        int aiRep = playerIsAi ? 1 : 0;
        return " WHERE (EntireBeginTime = " + entireGame.getStartTimeSqlString() + " AND " +
                "FrameIndex = " + game.frameIndex + " AND " +
                "PlayerName = '" + playerName + "' AND " +
                "PlayerIsAI = " + aiRep + ")" +
                (endLine ? ";" : "");
    }

    public void recordNumberedBallResult(EntireGame entireGame,
                                         Game<?, ?> frame,
                                         NumberedBallPlayer player,
                                         boolean wins,
                                         List<Integer> continuousPots) {
        GameRule gameRule = entireGame.gameValues.rule;
        String tableName = entireGame.gameValues.rule.toSqlKey() + "Record";
        int playTimes = player.getPlayTimes();
        boolean breaks = player.isBreakingPlayer();
        boolean breakPot = player.isBreakSuccess();
        int breakClear = 0;
        int continueClear = 0;
        int highest = 0;
        // 输的人不可能清台
        if (wins) {
            if (playTimes == 1) {
                if (breaks) {  // 炸清
                    breakClear = 1;
                } else {  // 接清
                    continueClear = 1;
                }
            }
        }
        for (Integer b : continuousPots) {
            if (b > highest) highest = b;
        }
        int goldNine = 0;
        if (player instanceof AmericanNineBallPlayer) {
            if (((AmericanNineBallPlayer) player).isGoldNine()) {
                // 黄金九。由于目前的检测方式有可能将黄金九识别为炸清，所以清空breakClear
                goldNine = 1;
                breakClear = 0;
                continueClear = 0;
                AchManager.getInstance().addAchievement(Achievement.GOLDEN_NINE, player.getInGamePlayer());
            }
        }
        if (breakClear > 0 || continueClear > 0) {
            AchManager.getInstance().addAchievement(Achievement.POOL_CLEAR, player.getInGamePlayer());
            if (gameRule == GameRule.CHINESE_EIGHT) {
                AchManager.getInstance().addAchievement(Achievement.CEB_CUMULATIVE_CLEAR, player.getInGamePlayer());
            }

            if (breakClear > 0) {
                AchManager.getInstance().addAchievement(Achievement.POOL_BREAK_CLEAR, player.getInGamePlayer());
            }
        }

        String query = "INSERT INTO " + tableName + " VALUES (" +
                entireGame.getStartTimeSqlString() + ", " +
                frame.frameIndex + ", " +
                "'" + player.getPlayerPerson().getPlayerId() + "', " +
                (player.getInGamePlayer().getPlayerType() == PlayerType.COMPUTER) + ", " +
                (breaks ? 1 : 0) + ", " + (breakPot ? 1 : 0) + ", " +
                breakClear + ", " +
                continueClear + ", " +
                highest +
                (gameRule == GameRule.AMERICAN_NINE ? (", " + goldNine) : "") +
                ");";
        try {
            executeStatement(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void recordSnookerBreaks(EntireGame entireGame, Game<?, ?> frame,
                                    SnookerPlayer player, List<Integer> breakScores,
                                    MaximumType maximumType) {
        System.out.println("Snooker breaks: " + breakScores);
        int highBreak = 0;
        int breaks50 = 0;  // 一局最多两个50+，最多一个100+
        for (Integer b : breakScores) {
            if (b >= 50) {
                breaks50++;
            }
            if (b > highBreak) {
                highBreak = b;
            }
        }
//        int maximum = isMaximum ? 1 : 0;
        String query = "INSERT INTO SnookerRecord VALUES (" +
                entireGame.getStartTimeSqlString() + ", " +
                frame.frameIndex + ", " +
                "'" + player.getPlayerPerson().getPlayerId() + "', " +
                (player.getInGamePlayer().getPlayerType() == PlayerType.COMPUTER) + ", " +
                player.getScore() + ", " +
                breaks50 + ", " +
                highBreak + ", " +
                "'" + maximumType.name() + "'" +
                ");";
        try {
            executeStatement(query);
        } catch (SQLException e) {
            EventLogger.error(e);
        }
    }

    public List<String>[] listPlayerIdsHumanComputer(boolean careerOnly) {
        Set<String> humanIds = new TreeSet<>();
        Set<String> computerIds = new TreeSet<>();

        String extra = "";
        if (careerOnly) {
            extra += " WHERE MatchID IS NOT NULL";
        }
        String query = "SELECT * FROM EntireGame" + extra + ";";
        try {
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(query);
            while (result.next()) {
                boolean p1Ai = result.getInt("Player1IsAI") != 0;
                boolean p2Ai = result.getInt("Player2IsAI") != 0;
                String p1 = result.getString("Player1Name");
                String p2 = result.getString("Player2Name");

                if (p1Ai) {
                    if (isValidPlayer(p1)) computerIds.add(p1);
                } else {
                    if (isValidPlayer(p1)) humanIds.add(p1);
                }
                if (p2Ai) {
                    if (isValidPlayer(p2)) computerIds.add(p2);
                } else {
                    if (isValidPlayer(p2)) humanIds.add(p2);
                }
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new List[]{new ArrayList<>(humanIds), new ArrayList<>(computerIds)};
    }

    public List<String> listAllPlayerIds() {
        List<String> list = new ArrayList<>();
        String cmd = "SELECT Name FROM Player;";
        try {
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(cmd);
            while (result.next()) {
                String name = result.getString("Name");
                if (isValidPlayer(name)) {
                    list.add(name);
                }
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void executeStatement(String cmd) throws SQLException {
        System.out.println(cmd);
        Statement statement = connection.createStatement();
        statement.execute(cmd);
        statement.close();
    }

    /**
     * 只返回sqlite里有没有这个player，和player.json与custom_player.json里有没有无关
     */
    private boolean playerExistsInSql(String playerName) {
        String cmd = "SELECT Name FROM Player WHERE Player.Name = '" + playerName + "';";
        try {
            Statement stmt = connection.createStatement();
            ResultSet resultSet = stmt.executeQuery(cmd);
            boolean hasItem = resultSet.next();

            stmt.close();

            return hasItem;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void insertPlayerIfNotExists(String playerId) {
        if (playerExistsInSql(playerId)) return;
        String[] commands =
                new String[]{
                        "INSERT INTO Player VALUES ('" + playerId + "')"
                };
        try {
            for (String cmd : commands) {
                Statement stmt = connection.createStatement();
                stmt.execute(cmd);
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createRecordForFrame(EntireGame entireGame, Game<?, ?> game, String playerId, boolean playerIsAi)
            throws SQLException {
        int aiRep = playerIsAi ? 1 : 0;
        String command1 = "INSERT INTO GeneralRecord VALUES (" +
                entireGame.getStartTimeSqlString() + ", " +
                game.frameIndex + ", " +
                "'" + playerId + "', " +
                aiRep + ", " +
                "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0" +
                ");";
        executeStatement(command1);
    }

    public void recordAnEntireGameStarts(EntireGame entireGame, MetaMatchInfo metaMatchInfo) {
        String typeStr = "'" + entireGame.gameValues.rule.toSqlKey() + "'";
        int p1t = entireGame.getPlayer1().getPlayerType() == PlayerType.COMPUTER ? 1 : 0;
        int p2t = entireGame.getPlayer2().getPlayerType() == PlayerType.COMPUTER ? 1 : 0;

        insertPlayerIfNotExists(entireGame.getPlayer1().getPlayerPerson().getPlayerId());
        insertPlayerIfNotExists(entireGame.getPlayer2().getPlayerPerson().getPlayerId());

        String command =
                "INSERT INTO EntireGame VALUES (" +
                        entireGame.getStartTimeSqlString() + ", " +
                        typeStr + ", " +
                        "'" + entireGame.getPlayer1().getPlayerPerson().getPlayerId() + "', " +
                        "'" + entireGame.getPlayer2().getPlayerPerson().getPlayerId() + "', " +
                        p1t + ", " +
                        p2t + ", " +
                        entireGame.getTotalFrames() + ", " +
                        (metaMatchInfo == null ? null : ("'" + metaMatchInfo + "'")) + ", " +
                        "'" + SubRule.subRulesToCommaString(entireGame.gameValues.subRules) + "'" +
                        ");";
        try {
            executeStatement(command);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void recordAFrameStarts(EntireGame entireGame, Game<?, ?> game) {
        String generalCmd = "INSERT INTO Game VALUES (" +
                entireGame.getStartTimeSqlString() + ", " +
                game.frameIndex + ", 0, NULL);";
        System.out.println(generalCmd);
        try {
            executeStatement(generalCmd);
            createRecordForFrame(entireGame, game,
                    entireGame.getPlayer1().getPlayerPerson().getPlayerId(),
                    entireGame.getPlayer1().getPlayerType() == PlayerType.COMPUTER);
            createRecordForFrame(entireGame, game,
                    entireGame.getPlayer2().getPlayerPerson().getPlayerId(),
                    entireGame.getPlayer2().getPlayerType() == PlayerType.COMPUTER);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void recordAFrameEnds(EntireGame entireGame, Game<?, ?> game, InGamePlayer winner) {
        long duration = (System.currentTimeMillis() - game.frameStartTime) / 1000 + 1;
        String generalCmd = "UPDATE Game SET DurationSeconds = " + duration + ", " +
                "WinnerName = '" + winner.getPlayerPerson().getPlayerId() + "' " +
                "WHERE (EntireBeginTime = " + entireGame.getStartTimeSqlString() + " AND " +
                "FrameIndex = " + game.frameIndex + ");";
        try {
            executeStatement(generalCmd);
            storeAttempts(entireGame, game, winner);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void abortEntireGame(EntireGame entireGame) {
        String cmd = "DELETE FROM EntireGame " +
                "WHERE EntireBeginTime = " + entireGame.getStartTimeSqlString() + ";";
        try {
            executeStatement(cmd);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void printQuery(String query) {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            for (int c = 1; c <= colCount; c++) {
                System.out.print(meta.getColumnName(c) + ", ");
            }
            System.out.println();
            while (rs.next()) {
                for (int c = 1; c <= colCount; c++) {
                    System.out.print(rs.getString(c) + ", ");
                }
                System.out.println();
            }

            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTablesIfNotExists() throws SQLException, IOException {
        InputStream is = getClass().getResourceAsStream("DbCreator.sql");
        if (is == null) {
            System.err.println(getClass().getPackageName());
            throw new IOException(getClass().toString());
        }
        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(is));
        StringBuilder command = new StringBuilder();
        String line;
        int lineNum = 0;
        while ((line = reader.readLine()) != null) {
            lineNum++;
            line = line.trim();
            if (!line.isEmpty()) {
                int commentIndex = line.indexOf("--");
                if (commentIndex != -1) {
                    line = line.substring(0, commentIndex);
                }
                if (line.endsWith(";")) {
                    command.append(line);
//                    System.out.println(command);
                    try {
                        Statement stmt = connection.createStatement();
                        stmt.execute(command.toString());
                        stmt.close();
                    } catch (SQLException e) {
                        System.err.println("Error at line " + lineNum);
                        throw e;
                    }
                    command.setLength(0);
                } else if (!line.startsWith("--")) {
                    command.append(line);
                }
            }
        }
        reader.close();
    }
}
