package trashsoftware.trashSnooker.recorder;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.Cue;
import trashsoftware.trashSnooker.core.GameType;
import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.table.Table;
import trashsoftware.trashSnooker.core.table.Tables;

import java.io.*;

public abstract class GameReplay {

    public final GameType gameType;
    public final Table table;
    protected final int totalCues;
    protected InputStream wrapperStream;
    protected InputStream inputStream;
    protected BriefReplayItem item;
    protected InGamePlayer p1;
    protected InGamePlayer p2;
    protected CueRecord currentCueRecord;
    protected Movement currentMovement;
    protected int currentFlag = GameRecorder.FLAG_NOT_BEGUN;

    protected Ball[] balls;
    protected Ball cueBall;

    protected int movementCount = 0;
    protected final byte[] buffer1 = new byte[1];

    protected GameReplay(BriefReplayItem item) throws IOException {
        this.item = item;
        this.totalCues = item.getTotalCues();
        this.gameType = item.getGameType();
        this.p1 = item.getP1();
        this.p2 = item.getP2();

        this.wrapperStream = new BufferedInputStream(new FileInputStream(item.getFile()));
        if (wrapperStream.skip(GameRecorder.TOTAL_HEADER_LENGTH) != GameRecorder.TOTAL_HEADER_LENGTH) {
            throw new IOException();
        }
        inputStream = new BufferedInputStream(wrapperStream);

        switch (gameType) {
            case SNOOKER:
                table = Tables.SNOOKER_TABLE;
                break;
            case MINI_SNOOKER:
                table = Tables.MINI_SNOOKER_TABLE;
                break;
            case CHINESE_EIGHT:
                table = Tables.CHINESE_EIGHT_TABLE;
                break;
            case SIDE_POCKET:
                table = Tables.SIDE_POCKET_TABLE;
                break;
            default:
                throw new RuntimeException();
        }

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

    public boolean hasNext() {
        try {
            if (inputStream.read(buffer1) != buffer1.length) throw new IOException();
            currentFlag = buffer1[0] & 0xff;
            System.out.println("Flag: " + currentFlag);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return currentFlag != GameRecorder.FLAG_TERMINATE;
    }

    public int getCurrentFlag() {
        return currentFlag;
    }

    public void next() {
        if (currentFlag == GameRecorder.FLAG_CUE) {
            loadNextRecordAndMovement();
            movementCount++;
            try {
                loadBallPositions();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Next: " + currentCueRecord.cuePlayer.getPlayerPerson().getName());
        } else if (currentFlag == GameRecorder.FLAG_HANDBALL) {
            loadBallInHand();
        }
    }

    public Cue getCurrentCue() {
        return currentCueRecord.isBreaking ?
                currentCueRecord.cuePlayer.getBreakCue() : currentCueRecord.cuePlayer.getPlayCue();
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
