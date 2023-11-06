package trashsoftware.trashSnooker.core;

import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.ChampionshipStage;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.career.achievement.CareerAchManager;
import trashsoftware.trashSnooker.core.career.championship.MetaMatchInfo;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.phy.TableCloth;
import trashsoftware.trashSnooker.core.snooker.SnookerPlayer;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.GeneralSaveManager;
import trashsoftware.trashSnooker.util.Util;
import trashsoftware.trashSnooker.util.db.DBAccess;
import trashsoftware.trashSnooker.util.db.EntireGameRecord;
import trashsoftware.trashSnooker.util.db.EntireGameTitle;

import java.io.File;
import java.sql.Timestamp;
import java.util.SortedMap;
import java.util.TreeMap;

public class EntireGame {

    public final int totalFrames;
    public final GameValues gameValues;
    public final TableCloth cloth;
    public final Phy playPhy;
    public final Phy predictPhy;
    public final Phy whitePhy;
    final InGamePlayer p1;
    final InGamePlayer p2;
    private final SortedMap<Integer, Integer> winRecords = new TreeMap<>();
    private final MetaMatchInfo metaMatchInfo;  // nullable
    Game<? extends Ball, ? extends Player> game;
    private Timestamp startTime = new Timestamp(System.currentTimeMillis());
    private int p1Wins;
    private int p2Wins;
    private boolean p1Breaks;
    
    private int p1BreakLoseChance;
    private int p2BreakLoseChance;

    public EntireGame(InGamePlayer p1, 
                      InGamePlayer p2, 
                      GameValues gameValues,
                      int totalFrames, 
                      TableCloth cloth,
                      MetaMatchInfo metaMatchInfo) {
        this(p1, p2, gameValues, totalFrames, cloth, true, metaMatchInfo);
    }

    private EntireGame(InGamePlayer p1, 
                       InGamePlayer p2, 
                       GameValues gameValues,
                       int totalFrames, 
                       TableCloth cloth, 
                       boolean isNewCreate,
                       MetaMatchInfo metaMatchInfo) {
        this.p1 = p1;
        this.p2 = p2;
        if (totalFrames % 2 != 1) {
            throw new RuntimeException("Total frames must be odd.");
        }
        this.gameValues = gameValues;
        this.totalFrames = totalFrames;
        this.cloth = cloth;
        this.playPhy = Phy.Factory.createPlayPhy(cloth);
        this.predictPhy = Phy.Factory.createAiPredictPhy(cloth);
        this.whitePhy = Phy.Factory.createWhitePredictPhy(cloth);
        this.metaMatchInfo = metaMatchInfo;
        
        if (metaMatchInfo != null) {
            if (metaMatchInfo.stage == ChampionshipStage.SEMI_FINAL) {
                AchManager.getInstance().addAchievement(Achievement.SEMIFINAL_STAGE,
                        p1.isHuman() ? p1 : p2);
            } else if (metaMatchInfo.stage == ChampionshipStage.FINAL) {
                AchManager.getInstance().addAchievement(Achievement.FINAL_STAGE, 
                        p1.isHuman() ? p1 : p2);
            }
        }

        if (isNewCreate && gameValues.isStandard())
            DBAccess.getInstance().recordAnEntireGameStarts(this, metaMatchInfo);

//        createNextFrame();
    }

    public static EntireGame fromJson(JSONObject jsonObject) {
        int frames = jsonObject.getInt("totalFrames");
        JSONObject clothObj = jsonObject.getJSONObject("cloth");
        TableCloth cloth = TableCloth.fromJson(clothObj);

        GameValues gameValues = GameValues.fromJson(jsonObject.getJSONObject("gameValues"), cloth);

        InGamePlayer p1 = InGamePlayer.fromJson(jsonObject.getJSONObject("p1"));
        InGamePlayer p2 = InGamePlayer.fromJson(jsonObject.getJSONObject("p2"));

        EntireGame entireGame = new EntireGame(
                p1,
                p2,
                gameValues,
                frames,
                cloth,
                false,
                jsonObject.has("matchId") ?
                        MetaMatchInfo.fromString(jsonObject.getString("matchId")) :
                        null
        );

        entireGame.p1Wins = jsonObject.getInt("p1Wins");
        entireGame.p2Wins = jsonObject.getInt("p2Wins");
        entireGame.p1Breaks = jsonObject.getBoolean("p1Breaks");
        entireGame.startTime = new Timestamp(jsonObject.getLong("startTime"));
        
        if (jsonObject.has("p1BreakLoseChance") && jsonObject.has("p2BreakLoseChance")) {
            entireGame.p1BreakLoseChance = jsonObject.getInt("p1BreakLoseChance");
            entireGame.p2BreakLoseChance = jsonObject.getInt("p2BreakLoseChance");
        }

        JSONObject wr;
        if (jsonObject.has("winRecords")) {
            wr = jsonObject.getJSONObject("winRecords");
        } else {
            wr = new JSONObject();
            for (int i = 1; i < entireGame.p1Wins + entireGame.p2Wins + 1; i++) {
                if (i <= entireGame.p1Wins) {
                    wr.put(String.valueOf(i), 1);
                } else {
                    wr.put(String.valueOf(i), 2);
                }
            }
        }
        for (String frameIndex : wr.keySet()) {
            entireGame.winRecords.put(Integer.parseInt(frameIndex), wr.getInt(frameIndex));
        }

        return entireGame;
    }

    public static EntireGame loadFrom(File file) {
        try {
            if (!file.exists()) return null;
            JSONObject json = Util.readJson(file);
            assert json != null;
            EntireGame eg = fromJson(json);
            if (eg.isFinished()) return null;
            return eg;
        } catch (JSONException e) {
            EventLogger.error(e);
            return null;
        }
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();

        object.put("totalFrames", totalFrames);
        object.put("gameValues", gameValues.toJson());

        object.put("cloth", cloth.toJson());

        object.put("startTime", startTime.getTime());
        object.put("p1Wins", p1Wins);
        object.put("p2Wins", p2Wins);
        object.put("p1Breaks", p1Breaks);

        object.put("p1", p1.toJson());
        object.put("p2", p2.toJson());

        object.put("winRecords", winRecords);
        object.put("matchId", metaMatchInfo == null ? null : metaMatchInfo.toString());
        
        object.put("p1BreakLoseChance", p1BreakLoseChance);
        object.put("p2BreakLoseChance", p2BreakLoseChance);

        return object;
    }

    public void generalSave() {
        GeneralSaveManager.getInstance().save(this);
    }

    public Game<? extends Ball, ? extends Player> getGame() {
        return game;
    }

    public int playerContinuousLoses(int playerNum) {
        if (p1Wins + p2Wins == 0) return 0;
        int count = 0;
        for (int i = p1Wins + p2Wins; i >= 1; i--) {
            int frameWinner = winRecords.get(i);
            if (frameWinner == playerNum) {
                break;
            }
            count++;
        }
        return count;
    }

    /**
     * 这个球员是否被打rua了
     */
    public boolean rua(InGamePlayer igp) {
        int playerNum = igp == p1 ? 1 : 2;
        return playerContinuousLoses(playerNum) >= 3;  // 连输3局以上，rua了
    }
    
    public void quitMatch(PlayerPerson quitPerson) {
        InGamePlayer winner;
        if (quitPerson.getPlayerId().equals(p1.getPlayerPerson().getPlayerId())) {
            winner = p2;
        } else {
            winner = p1;
        }
        while (!playerWinsAframe(winner, false)) {
            // do nothing
        }
    }

    public MetaMatchInfo getMetaMatchInfo() {
        return metaMatchInfo;
    }

    public int getP1Wins() {
        return p1Wins;
    }

    public int getP2Wins() {
        return p2Wins;
    }

    public InGamePlayer getPlayer1() {
        return p1;
    }

    public InGamePlayer getPlayer2() {
        return p2;
    }

    /**
     * 返回赢了这一局之后，比赛是不是就结束了
     */
    public boolean playerWinsAframe(InGamePlayer player) {
        return playerWinsAframe(player, true);
    }
    
    private boolean playerWinsAframe(InGamePlayer player, boolean record) {
        if (game != null && record) {
            if (gameValues.isStandard()) {
                DBAccess.getInstance().recordAFrameEnds(
                        this, game, player);
            }
            updateFrameRecords(game.getPlayer1(), player);
            updateFrameRecords(game.getPlayer2(), player);
        }

        boolean end;
        InGamePlayer winner = null;
        int frameIndex = p1Wins + p2Wins + 1;
        if (player.getPlayerPerson().equals(p1.getPlayerPerson())) {
            winRecords.put(frameIndex, 1);
            end = p1WinsAFrame();
            if (end) winner = p1;
        } else {
            winRecords.put(frameIndex, 2);
            end = p2WinsAFrame();
            if (end) winner = p2;
        }
        if (end && record && gameValues.isStandard()) {
            EntireGameRecord egr = DBAccess.getInstance().getMatchDetail(toEgt());
            int winnerIndex = winner.getPlayerNumber() == 1 ? 0 : 1;
            if (winner.isHuman()) {
                int[][] totalBasic = egr.totalBasicStats();
                if (totalBasic[winnerIndex][0] >= 50) {
                    double potSuccessRate = (double) totalBasic[winnerIndex][1] / totalBasic[winnerIndex][0];
                    if (potSuccessRate >= 0.9) {
                        CareerAchManager.getInstance().addAchievement(Achievement.ACCURACY_WIN, winner);
                    }
                }
                if (totalBasic[winnerIndex][2] >= 8) {
                    double longPotSuccessRate = (double) totalBasic[winnerIndex][3] / totalBasic[winnerIndex][2];
                    if (longPotSuccessRate >= 0.75) {
                        CareerAchManager.getInstance().addAchievement(Achievement.ACCURACY_WIN_LONG, winner);
                    }
                }
            }
        }
        
        return end;
    }
    
    public EntireGameTitle toEgt() {
        return new EntireGameTitle(
                startTime,
                gameValues.rule,
                p1.getPlayerPerson().getPlayerId(),
                p2.getPlayerPerson().getPlayerId(),
                !p1.isHuman(),
                !p2.isHuman(),
                totalFrames,
                metaMatchInfo == null ? null : metaMatchInfo.toString()
        );
    }

    public int getTotalFrames() {
        return totalFrames;
    }

    public boolean isFinished() {
        return p1Wins > totalFrames / 2 || p2Wins > totalFrames / 2;
    }

    private boolean p1WinsAFrame() {
        p1Wins++;
        return p1Wins > totalFrames / 2;
    }

    private boolean p2WinsAFrame() {
        p2Wins++;
        return p2Wins > totalFrames / 2;
    }

    private void updateFrameRecords(Player framePlayer, InGamePlayer winingPlayer) {
        if (framePlayer instanceof SnookerPlayer snookerPlayer) {
            snookerPlayer.flushSinglePoles();
            if (gameValues.isStandard() && game.isStarted())
                DBAccess.getInstance().recordSnookerBreaks(this,
                        getGame(),
                        snookerPlayer,
                        snookerPlayer.getSinglePolesInThisGame());
        } else if (framePlayer instanceof NumberedBallPlayer numberedBallPlayer) {
            // 炸清和接清
            numberedBallPlayer.flushSinglePoles();
            if (gameValues.isStandard() && game.isStarted())
                DBAccess.getInstance().recordNumberedBallResult(this,
                        getGame(),
                        numberedBallPlayer,
                        winingPlayer.getPlayerPerson().equals(
                                framePlayer.inGamePlayer.getPlayerPerson()),
                        numberedBallPlayer.getContinuousPots());
        }
    }

    public void quitGameDeleteRecord() {
        if (game != null) {
            game.quitGame();
        }
        if (!isFinished()) {
            if (gameValues.isStandard())
                DBAccess.getInstance().abortEntireGame(this);
        }
    }

    public void startNextFrame() {
        createNextFrame();
    }

    public String getStartTimeSqlString() {
        return Util.timeStampFmt(startTime);
    }

    public long getBeginTime() {
        return startTime.getTime();
    }

    private boolean nextFrameP1Breaks() {
        if (gameValues.rule.breakRule == BreakRule.ALTERNATE) {
            return !p1Breaks;
        } else {
            if (p1Wins + p2Wins == 0) return true;
            int lastWinner = winRecords.get(p1Wins + p2Wins);
            if (gameValues.rule.breakRule == BreakRule.WINNER) {
                return lastWinner == 1;
            } else {
                return lastWinner != 1;
            }
        }
    }

    private void createNextFrame() {
        p1Breaks = nextFrameP1Breaks();
        GameSettings gameSettings = new GameSettings.Builder()
                .player1Breaks(p1Breaks)
                .players(p1, p2)
                .build();
        game = Game.createGame(gameSettings, gameValues, this);
        
        if (totalFrames >= 5 && p1Wins + p2Wins + 1 == totalFrames) {
            // 决胜局
            AchManager.getInstance().addAchievement(Achievement.FINAL_FRAME, p1);
            AchManager.getInstance().addAchievement(Achievement.FINAL_FRAME, p2);  // 万一以后快速游戏也有呢？
            AchManager.getInstance().addAchievement(Achievement.FINAL_FRAME_USUAL_GUEST, p1);
            AchManager.getInstance().addAchievement(Achievement.FINAL_FRAME_USUAL_GUEST, p2);
            if (metaMatchInfo != null) {
                InGamePlayer human = p1.isHuman() ? p1 : p2;
                if (metaMatchInfo.stage == ChampionshipStage.FINAL) {
                    AchManager.getInstance().addAchievement(Achievement.FINAL_STAGE_FINAL_FRAME, human);
                }
            }
        }
        
        if (gameValues.isStandard())
            DBAccess.getInstance().recordAFrameStarts(
                    this, game);
    }
    
    public void addBreakLoseChance(int playerNum) {
        if (playerNum == 2) {
            p2BreakLoseChance++;
        } else {
            p1BreakLoseChance++;
        }
    }
    
    public void clearBreakLoseChance(int playerNum) {
        if (playerNum == 2) {
            p2BreakLoseChance = 0;
        } else {
            p1BreakLoseChance = 0;
        }
    }

    public int getBreakLoseChance(int playerNum) {
        if (playerNum == 2) {
            return p2BreakLoseChance;
        } else {
            return p1BreakLoseChance;
        }
    }
}
