package trashsoftware.trashSnooker.core;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.ChampionshipStage;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.career.achievement.CareerAchManager;
import trashsoftware.trashSnooker.core.career.championship.MetaMatchInfo;
import trashsoftware.trashSnooker.core.infoRec.MatchInfoRec;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;
import trashsoftware.trashSnooker.core.person.PlayerPerson;
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
    private final Timestamp startTime;
    private int p1Wins;
    private int p2Wins;
    private boolean p1Breaks;
    private int codeGameCounter;
    
    private int p1BreakLoseChance;
    private int p2BreakLoseChance;
    
    protected MatchInfoRec matchInfoRec;

    public EntireGame(InGamePlayer p1, 
                      InGamePlayer p2, 
                      GameValues gameValues,
                      int totalFrames, 
                      TableCloth cloth,
                      MetaMatchInfo metaMatchInfo) {
        this(p1, p2, gameValues, totalFrames, cloth,true, System.currentTimeMillis(), metaMatchInfo);
    }

    private EntireGame(InGamePlayer p1, 
                       InGamePlayer p2, 
                       GameValues gameValues,
                       int totalFrames, 
                       TableCloth cloth, 
                       boolean isNewCreate,
                       long startTime,
                       MetaMatchInfo metaMatchInfo) {
        this.p1 = p1;
        this.p2 = p2;
        if (totalFrames % 2 != 1) {
            throw new RuntimeException("Total frames must be odd.");
        }
        this.startTime = new Timestamp(startTime);
        this.gameValues = gameValues;
        this.totalFrames = totalFrames;
        this.cloth = cloth;
        this.playPhy = Phy.Factory.createPlayPhy(cloth);
        this.predictPhy = Phy.Factory.createAiPredictPhy(cloth);
        this.whitePhy = Phy.Factory.createWhitePredictPhy(cloth);
        this.metaMatchInfo = metaMatchInfo;

        String careerMatchId = null;
        
        if (metaMatchInfo != null) {
            careerMatchId = metaMatchInfo.matchId;
            
            if (metaMatchInfo.stage == ChampionshipStage.SEMI_FINAL) {
                AchManager.getInstance().addAchievement(Achievement.SEMIFINAL_STAGE,
                        p1.isHuman() ? p1 : p2);
            } else if (metaMatchInfo.stage == ChampionshipStage.FINAL) {
                AchManager.getInstance().addAchievement(Achievement.FINAL_STAGE, 
                        p1.isHuman() ? p1 : p2);
            }
        }

        if (gameValues.isStandard()) {
            String entireBeginTime = getEntireBeginTimeFileName();
            if (isNewCreate) {
                DBAccess.getInstance().recordAnEntireGameStarts(this, metaMatchInfo);
                matchInfoRec = MatchInfoRec.createMatchRec(gameValues, 
                        totalFrames,
                        entireBeginTime, 
                        careerMatchId,
                        new String[]{p1.getPlayerPerson().getPlayerId(), p2.getPlayerPerson().getPlayerId()});
            } else {
                matchInfoRec = MatchInfoRec.loadMatchRec(entireBeginTime);
            }
        } else {
            matchInfoRec = MatchInfoRec.INVALID;
        }

//        createNextFrame();
    }

    public static EntireGame fromJson(JSONObject jsonObject) {
        int frames = jsonObject.getInt("totalFrames");
        JSONObject clothObj = jsonObject.getJSONObject("cloth");
        TableCloth cloth = TableCloth.fromJson(clothObj);

        GameValues gameValues = GameValues.fromJson(jsonObject.getJSONObject("gameValues"));

        InGamePlayer p1 = InGamePlayer.fromJson(jsonObject.getJSONObject("p1"));
        InGamePlayer p2 = InGamePlayer.fromJson(jsonObject.getJSONObject("p2"));

        EntireGame entireGame = new EntireGame(
                p1,
                p2,
                gameValues,
                frames,
                cloth,
                false,
                jsonObject.getLong("startTime"),
                jsonObject.has("matchId") ?
                        MetaMatchInfo.fromString(jsonObject.getString("matchId")) :
                        null
        );

        entireGame.p1Wins = jsonObject.getInt("p1Wins");
        entireGame.p2Wins = jsonObject.getInt("p2Wins");
        entireGame.p1Breaks = jsonObject.getBoolean("p1Breaks");
        entireGame.codeGameCounter = jsonObject.optInt("codeGameCounter", 
                entireGame.p1Wins + entireGame.p2Wins);
        
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
            System.out.println("Found active fast game save!");
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
//        object.put("entireBeginTime", Util.entireBeginTimeNoQuote(startTime));
        object.put("p1Wins", p1Wins);
        object.put("p2Wins", p2Wins);
        object.put("p1Breaks", p1Breaks);
        object.put("codeGameCounter", codeGameCounter);

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
    
//    public int[] getNextFrameIndexAndRestartIndex() {
//        return new int[]{p1Wins + p2Wins + 1, game == null ? 0 : game.frameRestartIndex};
//    }

    public InGamePlayer getPlayer1() {
        return p1;
    }

    public InGamePlayer getPlayer2() {
        return p2;
    }

    /**
     * 返回赢了这一局之后，比赛是不是就结束了
     */
    public boolean playerWinsAframe(InGamePlayer frameWinner) {
        return playerWinsAframe(frameWinner, true);
    }

    /**
     * 结合{@link EntireGame#restartThisFrame(boolean)}
     */
    public void cancelCurrentFrame(boolean record) {
        if (game != null && record) {
            if (gameValues.isStandard()) {
                DBAccess.getInstance().recordFrameCancelled(
                        this, game);
            }
            updateFrameRecords(game.getPlayer1(), null);
            updateFrameRecords(game.getPlayer2(), null);
        }
    }
    
    private boolean playerWinsAframe(InGamePlayer frameWinner, boolean record) {
        if (game != null && record) {
            if (gameValues.isStandard()) {
                DBAccess.getInstance().recordAFrameEnds(
                        this, game, frameWinner);
            }
            updateFrameRecords(game.getPlayer1(), frameWinner);
            updateFrameRecords(game.getPlayer2(), frameWinner);
        }

        boolean end;
        InGamePlayer matchWinner = null;
        int frameIndex = p1Wins + p2Wins + 1;
        if (frameWinner.getPlayerPerson().equals(p1.getPlayerPerson())) {
            winRecords.put(frameIndex, 1);
            end = p1WinsAFrame();
            if (end) matchWinner = p1;
        } else {
            winRecords.put(frameIndex, 2);
            end = p2WinsAFrame();
            if (end) matchWinner = p2;
        }
        if (end && record && gameValues.isStandard()) {
            EntireGameRecord egr = DBAccess.getInstance().getMatchDetail(toEgt());
            int winnerIndex = matchWinner.getPlayerNumber() == 1 ? 0 : 1;
            if (matchWinner.isHuman()) {
                int[][] totalBasic = egr.totalBasicStats();
                if (totalBasic[winnerIndex][0] >= 50) {
                    double potSuccessRate = (double) totalBasic[winnerIndex][1] / totalBasic[winnerIndex][0];
                    if (potSuccessRate >= 0.9) {
                        CareerAchManager.getInstance().addAchievement(Achievement.ACCURACY_WIN, matchWinner);
                    }
                }
                if (totalBasic[winnerIndex][2] >= 8) {
                    double longPotSuccessRate = (double) totalBasic[winnerIndex][3] / totalBasic[winnerIndex][2];
                    if (longPotSuccessRate >= 0.75) {
                        CareerAchManager.getInstance().addAchievement(Achievement.ACCURACY_WIN_LONG, matchWinner);
                    }
                }
            }
        }
        try {
            matchInfoRec.finishCurrentFrame(end, frameWinner.getPlayerNumber());
        } catch (JSONException je) {
            EventLogger.error(je);
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
                metaMatchInfo == null ? null : metaMatchInfo.toString(),
                gameValues.getSubRules()
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

    private void updateFrameRecords(Player framePlayer, @Nullable InGamePlayer winingPlayer) {
        if (framePlayer instanceof SnookerPlayer snookerPlayer) {
            snookerPlayer.flushSinglePoles();
            if (gameValues.isStandard() && game.isStarted())
                DBAccess.getInstance().recordSnookerBreaks(this,
                        getGame(),
                        snookerPlayer,
                        snookerPlayer.getSinglePolesInThisGame(),
                        snookerPlayer.getMaximumType(),
                        winingPlayer != null);
        } else if (framePlayer instanceof NumberedBallPlayer numberedBallPlayer) {
            // 炸清和接清
            numberedBallPlayer.flushSinglePoles();
            if (gameValues.isStandard() && game.isStarted())
                DBAccess.getInstance().recordNumberedBallResult(this,
                        getGame(),
                        numberedBallPlayer,
                        winingPlayer != null,
                        winingPlayer != null && winingPlayer.getPlayerPerson().equals(
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
        p1Breaks = nextFrameP1Breaks();
        createNextFrame(p1Breaks, false);
    }

    public String getStartTimeSqlString() {
        return Util.timeStampFmt(startTime);
    }
    
    public String getEntireBeginTimeFileName() {
        return Util.entireBeginTimeToFileName(startTime);
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

    /**
     * 重开这一局，比如开球失机重开、斯诺克死局重开等
     * 
     * @param keepBreakPlayer 是否还是由原先开球的球员开球。在轮开制中，无论是否，都不影响后续开球顺序。
     */
    public void restartThisFrame(boolean keepBreakPlayer) {
        createNextFrame(keepBreakPlayer == p1Breaks, true);
    }

    private void createNextFrame(boolean isP1Break, boolean isRestart) {
        GameSettings gameSettings = new GameSettings.Builder()
                .player1Breaks(isP1Break)
                .players(p1, p2)
                .build();
        
        game = Game.createGame(gameSettings, gameValues, this, 
                ++codeGameCounter, p1Wins + p2Wins + 1);
        
        if (!isRestart) {
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
        }
        
        if (gameValues.isStandard())
            DBAccess.getInstance().recordAFrameStarts(
                    this, game);
        
        matchInfoRec.startNextFrame(game.frameIndex, game.frameNumber);
    }

    public MatchInfoRec getMatchInfoRec() {
        return matchInfoRec;
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
