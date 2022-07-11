package trashsoftware.trashSnooker.util.db;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;
import trashsoftware.trashSnooker.core.snooker.SnookerPlayer;
import trashsoftware.trashSnooker.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;

public class DBAccess {
    private static final boolean SAVE = true;
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
//        db.printQuery("SELECT * FROM SnookerRecord;");
//        System.out.println("========");
//        db.printQuery("SELECT * FROM GeneralRecord WHERE PlayerName = 'Kid';");
//        System.out.println("========");
        db.printQuery("SELECT * FROM SnookerRecord;");

        db.printQuery("SELECT * FROM Game INNER JOIN GeneralRecord USING (EntireBeginTime, FrameIndex)" +
                " WHERE EntireBeginTime = '2021-11-06 17:20:25.53';");
//        db.insertPlayerIfNotExists("Ding");
//        System.out.println(db.playerExists("Ding"));
//        db.getPlayerSnookerScores("Ding");


        closeDB();
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

    private String getTypeKey(GameType gameType, String playerName) {
        return String.format("'%s_%s'", playerName, gameType.toSqlKey());
    }

    private void storeAttemptsForOnePlayer(EntireGame entireGame, Game frame, Player player) {
        // attempts, successes, long attempts, long successes, 
        // defenses, defense successes
        int[] data = new int[6];
        for (PotAttempt attempt : player.getAttempts()) {
            data[0]++;
            if (attempt.isSuccess()) data[1]++;
            if (attempt.isLongPot()) {
                data[2]++;
                if (attempt.isSuccess()) data[3]++;
            }
        }
        for (DefenseAttempt defenseAttempt : player.getDefenseAttempts()) {
            data[4]++;
            if (defenseAttempt.isSuccess()) {
                data[5]++;
            }
        }

        String queryWhere = getFrameQueryWhere(entireGame, frame,
                player.getPlayerPerson().getName(), true);
        String query =
                "UPDATE GeneralRecord SET " +
                        "Attempts = Attempts + " + data[0] + ", " +
                        "Successes = Successes + " + data[1] + ", " +
                        "LongAttempts = LongAttempts + " + data[2] + ", " +
                        "LongSuccesses = LongSuccesses + " + data[3] + ", " +
                        "Defenses = Defenses + " + data[4] + ", " +
                        "DefenseSuccesses = DefenseSuccesses + " + data[5] +
                        queryWhere;
        try {
            executeStatement(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void storeAttempts(EntireGame entireGame, Game frame) {
        storeAttemptsForOnePlayer(entireGame, frame,
                frame.getPlayer1());
        storeAttemptsForOnePlayer(entireGame, frame,
                frame.getPlayer2());
    }

    public List<EntireGameTitle> getAllMatches(GameType gameType, String playerName) {
        String typeStr = "'" + gameType.toSqlKey() + "'";
        String playerStr = "'" + playerName + "'";
        String query = "SELECT * FROM EntireGame " +
                "WHERE GameType = " + typeStr + " AND " +
                "(Player1Name = " + playerStr + " OR Player2Name = " + playerStr + ")" +
                "ORDER BY EntireBeginTime;";
        List<EntireGameTitle> rtn = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                EntireGameTitle egr = new EntireGameTitle(
                        resultSet.getTimestamp("EntireBeginTime"),
                        gameType,
                        resultSet.getString("Player1Name"),
                        resultSet.getString("Player2Name"),
                        resultSet.getInt("TotalFrames")
                );
                rtn.add(egr);
            }

            resultSet.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rtn;
    }

    /**
     * 返回{totalScore, highest, 50+，100+，147}
     */
    public int[] getSnookerBreaksTotal(GameType gameType, String playerName) {
        int[] rtn = new int[5];
        String pns = "'" + playerName + "'";
        String typeKey = "'" + gameType.toSqlKey() + "'";
        String highestQuery =
                "SELECT * FROM SnookerRecord " +
                        "WHERE (PlayerName = " + pns + " AND EntireBeginTime IN " +
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
                if (highInResult >= 147) {
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
     * 返回{炸请，接清}
     */
    public int[] getNumberedBallGamesTotal(GameType gameType, String playerName) {
        int[] rtn = new int[3];
        String pns = "'" + playerName + "'";
        String tableName = gameType.toSqlKey() + "Record";
        String typeKey = "'" + gameType.toSqlKey() + "'";
        String highestQuery =
                "SELECT * FROM " + tableName + " " +
                        "WHERE (PlayerName = " + pns + " AND EntireBeginTime IN " +
                        "(SELECT EntireBeginTime FROM EntireGame " +
                        "WHERE GameType = " + typeKey + "));";
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(highestQuery);
            while (resultSet.next()) {
                int highInResult = resultSet.getInt("Highest");
                rtn[0] += resultSet.getInt("BreakClear");
                rtn[1] += resultSet.getInt("ContinueClear");
                if (highInResult > rtn[2]) {
                    rtn[2] = highInResult;
                }
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rtn;
    }

    /**
     * 返回{进攻次数，进攻成功次数，长台进攻次数，长台成功次数，防守次数，防守成功次数}
     */
    public int[] getBasicPotStatusAll(GameType gameType, String playerName) {
        int[] array = new int[6];
        String pns = "'" + playerName + "'";
        String typeKey = "'" + gameType.toSqlKey() + "'";
        String cmd =
                "SELECT * FROM GeneralRecord " +
                        "WHERE (PlayerName = " + pns + " AND EntireBeginTime IN " +
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
                int[] array = new int[6];
                array[0] = result.getInt("Attempts");
                array[1] = result.getInt("Successes");
                array[2] = result.getInt("LongAttempts");
                array[3] = result.getInt("LongSuccesses");
                array[4] += result.getInt("Defenses");
                array[5] += result.getInt("DefenseSuccesses");
                durations.put(index, result.getInt("DurationSeconds"));
                String frameWinner = result.getString("WinnerName");
                if (frameWinner == null) {
                    System.err.println("Shit!");
                    continue;
                }
                int playerIndex = result.getString("PlayerName")
                        .equals(title.player1Name) ? 0 : 1;
                int[][] playerArray = framesPotMap.computeIfAbsent(index, k -> new int[2][]);
                playerArray[playerIndex] = array;
                framesWinnerMap.put(index, frameWinner);
            }
            general.close();

            if (title.gameType.snookerLike) {
                String snookerQuery = "SELECT * FROM SnookerRecord " +
                        "WHERE EntireBeginTime = " + Util.timeStampFmt(title.startTime) +
                        ";";
                Statement sn = connection.createStatement();
                ResultSet snRes = sn.executeQuery(snookerQuery);
                while (snRes.next()) {
                    int index = snRes.getInt("FrameIndex");
                    int playerIndex = snRes.getString("PlayerName")
                            .equals(title.player1Name) ? 0 : 1;
                    int[] scores = new int[5];  // total score, highest, breaks50, breaks100, 147
                    scores[0] = snRes.getInt("TotalScore");
                    scores[1] = snRes.getInt("Highest");
                    scores[2] = snRes.getInt("Breaks50");
                    if (scores[1] >= 100) {
                        scores[3]++;
                    }
                    if (scores[1] >= 147) {
                        scores[4]++;
                    }
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
            } else if (title.gameType == GameType.CHINESE_EIGHT ||
                    title.gameType == GameType.SIDE_POCKET) {
                String tableName = title.gameType.toSqlKey() + "Record";
                String numQuery = "SELECT * FROM " + tableName + " " +
                        "WHERE EntireBeginTime = " + Util.timeStampFmt(title.startTime) +
                        ";";
                Statement numStatement = connection.createStatement();
                ResultSet numRs = numStatement.executeQuery(numQuery);
                while (numRs.next()) {
                    int index = numRs.getInt("FrameIndex");
                    int playerIndex = numRs.getString("PlayerName")
                            .equals(title.player1Name) ? 0 : 1;
                    int[] scores = new int[3];  // break clear, continue clear, highest
                    scores[0] = numRs.getInt("BreakClear");
                    scores[1] = numRs.getInt("ContinueClear");
                    scores[2] = numRs.getInt("Highest");
                    PlayerFrameRecord.Numbered numbered = new PlayerFrameRecord.Numbered(
                            index, framesPotMap.get(index)[playerIndex],
                            framesWinnerMap.get(index), scores
                    );
                    PlayerFrameRecord[] thisFrame =
                            records.computeIfAbsent(index, k -> new PlayerFrameRecord[2]);
                    thisFrame[playerIndex] = numbered;
                }
                numStatement.close();

                if (title.gameType == GameType.CHINESE_EIGHT) {
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

    private String getFrameQueryWhere(EntireGame entireGame, Game game, String playerName,
                                      boolean endLine) {
        return " WHERE (EntireBeginTime = " + entireGame.getStartTimeSqlString() + " AND " +
                "FrameIndex = " + game.frameIndex + " AND " +
                "PlayerName = '" + playerName + "')" +
                (endLine ? ";" : "");
    }

    public void recordNumberedBallResult(EntireGame entireGame, Game frame,
                                         NumberedBallPlayer player, boolean wins,
                                         List<Integer> continuousPots) {
        String tableName = entireGame.gameType.toSqlKey() + "Record";
        int playTimes = player.getPlayTimes();
        boolean breaks = player.isBreakingPlayer();
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
        String query = "INSERT INTO " + tableName + " VALUES (" +
                entireGame.getStartTimeSqlString() + ", " +
                frame.frameIndex + ", " +
                "'" + player.getPlayerPerson().getName() + "', " +
                breakClear + ", " + continueClear + ", " + highest + ");";
        try {
            executeStatement(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void recordSnookerBreaks(EntireGame entireGame, Game frame,
                                    SnookerPlayer player, List<Integer> breakScores) {
        System.out.println("Snooker breaks: " + breakScores);
        int highBreak = 0;
        int breaks50 = 0;  // 一局最多两个50+，最多一个100+
        for (Integer b : breakScores) {
            if (b >= 50) breaks50++;
            if (b > highBreak) highBreak = b;
        }
        String query = "INSERT INTO SnookerRecord VALUES (" +
                entireGame.getStartTimeSqlString() + ", " +
                frame.frameIndex + ", " +
                "'" + player.getPlayerPerson().getName() + "', " +
                player.getScore() + ", " +
                breaks50 + ", " +
                highBreak + ");";

        try {
            executeStatement(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> listAllPlayerNames() {
        List<String> list = new ArrayList<>();
        String cmd = "SELECT Name FROM Player;";
        try {
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(cmd);
            while (result.next()) {
                list.add(result.getString("Name"));
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void executeStatement(String cmd) throws SQLException {
        Statement statement = connection.createStatement();
        statement.execute(cmd);
        statement.close();
    }

    private boolean playerExists(String playerName) {
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

    public void insertPlayerIfNotExists(String playerName) {
        if (playerExists(playerName)) return;
        String[] commands =
                new String[]{
                        "INSERT INTO Player VALUES ('" + playerName + "')"
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

    private void createRecordForFrame(EntireGame entireGame, Game game, String playerName)
            throws SQLException {
        String command1 = "INSERT INTO GeneralRecord VALUES (" +
                entireGame.getStartTimeSqlString() + ", " +
                game.frameIndex + ", " +
                "'" + playerName + "', " +
                "0, 0, 0, 0, 0, 0" +
                ");";
        executeStatement(command1);
    }

    public void recordAnEntireGameStarts(EntireGame entireGame) {
        String typeStr = "'" + entireGame.gameType.toSqlKey() + "'";
        int p1t = entireGame.getPlayer1().getPlayerType() == PlayerType.COMPUTER ? 1 : 0;
        int p2t = entireGame.getPlayer2().getPlayerType() == PlayerType.COMPUTER ? 1 : 0;
        String command =
                "INSERT INTO EntireGame VALUES (" +
                        entireGame.getStartTimeSqlString() + ", " +
                        typeStr + ", " +
                        "'" + entireGame.getPlayer1().getPlayerPerson().getName() + "', " +
                        "'" + entireGame.getPlayer2().getPlayerPerson().getName() + "', " +
                        p1t + ", " + 
                        p2t + ", " +
                        entireGame.getTotalFrames() + ");";
        try {
            executeStatement(command);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void recordAFrameStarts(EntireGame entireGame, Game game) {
        String generalCmd = "INSERT INTO Game VALUES (" +
                entireGame.getStartTimeSqlString() + ", " +
                game.frameIndex + ", 0, NULL);";
        try {
            executeStatement(generalCmd);
            createRecordForFrame(entireGame, game,
                    entireGame.getPlayer1().getPlayerPerson().getName());
            createRecordForFrame(entireGame, game,
                    entireGame.getPlayer2().getPlayerPerson().getName());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void recordAFrameEnds(EntireGame entireGame, Game game, PlayerPerson winner) {
        long duration = (System.currentTimeMillis() - game.frameStartTime) / 1000 + 1;
        String generalCmd = "UPDATE Game SET DurationSeconds = " + duration + ", " +
                "WinnerName = '" + winner.getName() + "' " +
                "WHERE (EntireBeginTime = " + entireGame.getStartTimeSqlString() + " AND " +
                "FrameIndex = " + game.frameIndex + ");";
        try {
            executeStatement(generalCmd);
            storeAttempts(entireGame, game);
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
