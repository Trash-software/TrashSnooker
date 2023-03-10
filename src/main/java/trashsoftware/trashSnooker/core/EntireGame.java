package trashsoftware.trashSnooker.core;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.numberedGames.NumberedBallPlayer;
import trashsoftware.trashSnooker.core.phy.Phy;
import trashsoftware.trashSnooker.core.phy.TableCloth;
import trashsoftware.trashSnooker.core.snooker.SnookerPlayer;
import trashsoftware.trashSnooker.util.GeneralSaveManager;
import trashsoftware.trashSnooker.util.Util;
import trashsoftware.trashSnooker.util.db.DBAccess;

import java.io.File;
import java.sql.Timestamp;

public class EntireGame {

    public final int totalFrames;
    public final GameValues gameValues;
    public final TableCloth cloth;
    public final Phy playPhy;
    public final Phy predictPhy;
    public final Phy whitePhy;
    final InGamePlayer p1;
    final InGamePlayer p2;
    Game<? extends Ball, ? extends Player> game;
    private Timestamp startTime = new Timestamp(System.currentTimeMillis());
    private int p1Wins;
    private int p2Wins;
    private boolean p1Breaks;

    public EntireGame(InGamePlayer p1, InGamePlayer p2, GameValues gameValues,
                      int totalFrames, TableCloth cloth) {
        this(p1, p2, gameValues, totalFrames, cloth, true);
    }

    private EntireGame(InGamePlayer p1, InGamePlayer p2, GameValues gameValues,
                      int totalFrames, TableCloth cloth, boolean isNewCreate) {
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

        if (isNewCreate && gameValues.isStandard())
            DBAccess.getInstance().recordAnEntireGameStarts(this);

//        createNextFrame();
    }

    public static EntireGame fromJson(JSONObject jsonObject) {
        int frames = jsonObject.getInt("totalFrames");
        GameValues gameValues = GameValues.fromJson(jsonObject.getJSONObject("gameValues"));

        JSONObject clothObj = jsonObject.getJSONObject("cloth");
        TableCloth cloth = TableCloth.fromJson(clothObj);

        InGamePlayer p1 = InGamePlayer.fromJson(jsonObject.getJSONObject("p1"));
        InGamePlayer p2 = InGamePlayer.fromJson(jsonObject.getJSONObject("p2"));

        EntireGame entireGame = new EntireGame(
                p1,
                p2,
                gameValues,
                frames,
                cloth,
                false
        );

        entireGame.p1Wins = jsonObject.getInt("p1Wins");
        entireGame.p2Wins = jsonObject.getInt("p2Wins");
        entireGame.p1Breaks = jsonObject.getBoolean("p1Breaks");
        entireGame.startTime = new Timestamp(jsonObject.getLong("startTime"));
        return entireGame;
    }

    public static EntireGame loadFrom(File file) {
        if (!file.exists()) return null;
        JSONObject json = Util.readJson(file);
        assert json != null;
        EntireGame eg = fromJson(json);
        if (eg.isFinished()) return null;
        return eg;
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

        return object;
    }

    public void generalSave() {
        GeneralSaveManager.getInstance().save(this);
    }

    public Game<? extends Ball, ? extends Player> getGame() {
        return game;
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

    public boolean playerWinsAframe(InGamePlayer player) {
        if (gameValues.isStandard()) {
            DBAccess.getInstance().recordAFrameEnds(
                    this, game, player.getPlayerPerson());
        }
        updateFrameRecords(game.getPlayer1(), player);
        updateFrameRecords(game.getPlayer2(), player);

        if (player.getPlayerPerson().equals(p1.getPlayerPerson())) {
            return p1WinsAFrame();
        } else {
            return p2WinsAFrame();
        }
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
        if (framePlayer instanceof SnookerPlayer) {
            SnookerPlayer snookerPlayer = (SnookerPlayer) framePlayer;
            snookerPlayer.flushSinglePoles();
            if (gameValues.isStandard())
                DBAccess.getInstance().recordSnookerBreaks(this,
                        getGame(),
                        snookerPlayer,
                        snookerPlayer.getSinglePolesInThisGame());
        } else if (framePlayer instanceof NumberedBallPlayer) {
            // 炸清和接清
            NumberedBallPlayer numberedBallPlayer = (NumberedBallPlayer) framePlayer;
            numberedBallPlayer.flushSinglePoles();
            if (gameValues.isStandard())
                DBAccess.getInstance().recordNumberedBallResult(this,
                        getGame(),
                        numberedBallPlayer,
                        winingPlayer.getPlayerPerson().equals(
                                framePlayer.inGamePlayer.getPlayerPerson()),
                        numberedBallPlayer.getContinuousPots());
        }
    }

    public void quitGame() {
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

    private void createNextFrame() {
        p1Breaks = !p1Breaks;
        GameSettings gameSettings = new GameSettings.Builder()
                .player1Breaks(p1Breaks)
                .players(p1, p2)
                .build();
        game = Game.createGame(gameSettings, gameValues, this);
        if (gameValues.isStandard())
            DBAccess.getInstance().recordAFrameStarts(
                    this, game);
    }
}
