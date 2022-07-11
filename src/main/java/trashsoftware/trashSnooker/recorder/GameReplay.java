package trashsoftware.trashSnooker.recorder;

import org.tukaani.xz.XZInputStream;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.scoreResult.*;
import trashsoftware.trashSnooker.core.table.Table;
import trashsoftware.trashSnooker.core.table.Tables;
import trashsoftware.trashSnooker.fxml.App;

import java.io.*;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

public abstract class GameReplay implements GameHolder {

    public final GameType gameType;
    public final Table table;
    protected final ScoreFactory scoreFactory;
    protected final byte[] buffer1 = new byte[1];
    protected final byte[] scoreResBuf;
    private final InputStream wrapperStream;
    protected InputStream inputStream;
    protected BriefReplayItem item;
    protected InGamePlayer p1;
    protected InGamePlayer p2;
    protected CueRecord currentCueRecord;
    protected Movement currentMovement;
    protected ScoreResult currentScoreResult;
    protected int currentFlag = GameRecorder.FLAG_NOT_BEGUN;
    protected Ball[] balls;
    protected HashMap<Integer, Ball> valueBallMap = new HashMap<>();
    protected HashMap<Ball, double[]> lastPositions = new HashMap<>();
    protected Ball cueBall;
    protected int movementCount = 0;

    protected GameReplay(BriefReplayItem item) throws IOException {
        if (item.recordVersion != App.VERSION) throw new RuntimeException("Record of old version");

        this.item = item;
        this.gameType = item.getGameType();
        this.p1 = item.getP1();
        this.p2 = item.getP2();

        this.wrapperStream = new FileInputStream(item.getFile());
        if (wrapperStream.skip(GameRecorder.TOTAL_HEADER_LENGTH) != GameRecorder.TOTAL_HEADER_LENGTH) {
            throw new IOException();
        }
        System.out.println(wrapperStream.available());
        createInputStream(item.compression);
//        inputStream = new BufferedInputStream(wrapperStream);

        switch (gameType) {
            case SNOOKER:
                table = Tables.SNOOKER_TABLE;
                scoreFactory = new SnookerScoreFactory();
                break;
            case MINI_SNOOKER:
                table = Tables.MINI_SNOOKER_TABLE;
                scoreFactory = new SnookerScoreFactory();
                break;
            case CHINESE_EIGHT:
                table = Tables.CHINESE_EIGHT_TABLE;
                scoreFactory = new ChineseEightScoreFactory();
                break;
            case SIDE_POCKET:
                table = Tables.SIDE_POCKET_TABLE;
                scoreFactory = new SidePocketScoreFactory();
                break;
            default:
                throw new EnumConstantNotPresentException(GameType.class, gameType.name());
        }
        scoreResBuf = new byte[scoreFactory.byteLength()];

        loadBallPositions();
    }

    public static GameReplay loadReplay(BriefReplayItem item) throws IOException {
        if (item.replayType == 0) return new NaiveGameReplay(item);

        throw new RuntimeException("No such record type");
    }

    public static File[] listReplays() {
        File dir = new File(GameRecorder.RECORD_DIR);
        return dir.listFiles();
    }

    private void createInputStream(int compression) throws IOException {
        switch (compression) {
            case GameRecorder.NO_COMPRESSION:
                inputStream = new BufferedInputStream(wrapperStream);
                break;
            case GameRecorder.GZ_COMPRESSION:
                inputStream = new GZIPInputStream(wrapperStream);
                break;
            case GameRecorder.XZ_COMPRESSION:
                inputStream = new XZInputStream(wrapperStream);
                break;
            default:
                throw new RuntimeException();
        }
    }

    public Table getTable() {
        return table;
    }

    public Ball[] getAllBalls() {
        return balls;
    }

    public Ball getCueBall() {
        return cueBall;
    }

    protected abstract void loadBallPositions() throws IOException;

    public boolean finished() {
        return currentFlag == GameRecorder.FLAG_TERMINATE;
    }

    public boolean loadNext() {
        try {
            if (inputStream.read(buffer1) != buffer1.length) throw new IOException();
            currentFlag = buffer1[0] & 0xff;
            System.out.println("Flag: " + currentFlag);
            next();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return currentFlag != GameRecorder.FLAG_TERMINATE;
    }

    public int getCurrentFlag() {
        return currentFlag;
    }

    protected synchronized void next() throws IOException {
        if (currentFlag == GameRecorder.FLAG_CUE) {
            storeLastPositions();
            loadNextRecordAndMovement();
            loadNextScoreResult();
            loadBallPositions();
            System.out.println("Next: " + currentCueRecord.cuePlayer.getPlayerPerson().getName());
        } else if (currentFlag == GameRecorder.FLAG_HANDBALL) {
            storeLastPositions();
            loadBallInHand();
        } else if (currentFlag == GameRecorder.FLAG_TERMINATE) {
            storeLastPositions();
        }
    }

    private void storeLastPositions() {
        for (Ball ball : balls) {
            lastPositions.put(ball, new double[]{ball.getX(), ball.getY(), ball.isPotted() ? 1 : 0});
        }
    }

    public HashMap<Ball, double[]> getLastPositions() {
        return lastPositions;
    }

    public Cue getCurrentCue() {
        return currentCueRecord.isBreaking ?
                currentCueRecord.cuePlayer.getBreakCue() : currentCueRecord.cuePlayer.getPlayCue();
    }

    @Override
    public Ball getBallByValue(int value) {
        return valueBallMap.get(value);
    }

    public BriefReplayItem getItem() {
        return item;
    }

    public ScoreResult getScoreResult() {
        return currentScoreResult;
    }

    public int getMovementCount() {
        return movementCount;
    }

    public CueRecord getCueRecord() {
        return currentCueRecord;
    }

    public Movement getMovement() {
        return currentMovement;
    }

    protected abstract void loadBallInHand();

    protected abstract void loadNextRecordAndMovement();

    protected void loadNextScoreResult() throws IOException {
        if (inputStream.read(scoreResBuf) != scoreResBuf.length) {
            throw new IOException();
        }
        currentScoreResult = scoreFactory.fromBytes(this, scoreResBuf);
    }

    public InGamePlayer getP1() {
        return p1;
    }

    public InGamePlayer getP2() {
        return p2;
    }

    public void close() throws IOException {
        inputStream.close();
        wrapperStream.close();
    }
}
