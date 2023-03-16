package trashsoftware.trashSnooker.recorder;

import org.json.JSONObject;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.util.Util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

public abstract class GameRecorder {
    public static final int RECORD_PRIMARY_VERSION = 12;
    public static final int RECORD_SECONDARY_VERSION = 7;
    public static final int HEADER_LENGTH = 50;
    public static final int PLAYER_HEADER_LENGTH = 98;
    public static final int TOTAL_HEADER_LENGTH = HEADER_LENGTH + PLAYER_HEADER_LENGTH * 2;
    public static final String SIGNATURE = "TSR_";

    public static final int FLAG_NOT_BEGUN = -1;
    public static final int FLAG_TERMINATE = 0;
    public static final int FLAG_CUE = 1;
    public static final int FLAG_HANDBALL = 2;
    public static final int FLAT_REPOSITION = 3;

    public static final int NO_COMPRESSION = 0;
    public static final int DEFLATE_COMPRESSION = 1;
    public static final int GZ_COMPRESSION = 2;
    public static final int XZ_COMPRESSION = 3;
    protected static final String RECORD_DIR = "user" + File.separator + "replays";
    protected int compression = 0;
    protected File outFile;
    protected OutputStream outputStream;
    private OutputStream wrapperStream;

    protected Game game;
    protected static DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

    protected CueRecord cueRecord;
    protected Movement movement;
    protected ScoreResult scoreResult;
    
    protected TargetRecord thisTarget;
    // 这玩意一定和下一杆的thisTarget一样，但是为了方便所以记录，反正3字节的事
    protected TargetRecord nextTarget;
    protected boolean finished = false;
    protected int nCues = 0;  // 只计击打，不计放置等
    protected long recordBeginTime;

    public GameRecorder(Game game) {
        this.game = game;
        
        createDirIfNotExist();
        
        setCompression(XZ_COMPRESSION);

        outFile = new File(String.format("%s%sreplay_%s.replay",
                RECORD_DIR, File.separator, dateFormat.format(new Date())));
    }
    
    public static boolean isPrimaryCompatible(int replayPrimary) {
        return replayPrimary == GameRecorder.RECORD_PRIMARY_VERSION || replayPrimary == 11;
    }

    public static boolean isSecondaryCompatible(int replaySecondary) {
        return replaySecondary == GameRecorder.RECORD_SECONDARY_VERSION;
    }
    
    public void setCompression(int compression) {
        this.compression = compression;
    }

    protected abstract byte recorderType();

    public abstract void recordPositions() throws IOException;

    public final void recordCue(CueRecord cueRecord, TargetRecord thisTarget) {
        if (this.cueRecord != null || this.thisTarget != null) 
            throw new RuntimeException("Repeated recording");
        this.cueRecord = cueRecord;
        this.thisTarget = thisTarget;
        this.nCues++;
    }

    public final void recordMovement(Movement movement) {
        if (this.movement != null) throw new RuntimeException("Repeated recording");
//        System.out.println(movement.getMovementMap().get(game.getCueBall()).size());
        this.movement = movement;
    }

    public final void recordScore(ScoreResult scoreResult) {
        if (this.scoreResult != null) throw new RuntimeException("Repeated recording");
        this.scoreResult = scoreResult;
    }
    
    public final void recordNextTarget(TargetRecord nextTarget) {
        if (this.nextTarget != null) throw new RuntimeException("Repeated recording");
        this.nextTarget = nextTarget;
    }

    public final void writeCueToStream() {
        if (cueRecord == null || movement == null || scoreResult == null || thisTarget == null || nextTarget == null) {
            throw new RuntimeException(String.format("Score not filled: %s, %s, %s, %s\n",
                    cueRecord, movement, scoreResult, nextTarget));
        }
//        System.out.println(movement);
        try {
            outputStream.write(FLAG_CUE);
            writeCue(cueRecord, movement, thisTarget, nextTarget);
            byte[] b = scoreResult.toBytes();
            outputStream.write(b);
            recordPositions();

            cueRecord = null;
            movement = null;
            scoreResult = null;
            thisTarget = null;
            nextTarget = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public final void writeBallInHandPlacement() {
        try {
            outputStream.write(FLAG_HANDBALL);
            writeBallInHand();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract void writeCue(CueRecord cueRecord,
                                     Movement movement,
                                     TargetRecord thisTarget,
                                     TargetRecord nextTarget) throws IOException;

    protected abstract void writeBallInHand() throws IOException;

    public final void startRecoding() throws IOException {
        wrapperStream = new FileOutputStream(outFile);

        byte[] header = new byte[HEADER_LENGTH];

        InGamePlayer p1 = game.getPlayer1().getInGamePlayer();
        InGamePlayer p2 = game.getPlayer2().getInGamePlayer();

        byte[] signature = SIGNATURE.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(signature, 0, header, 0, 4);

        header[4] = recorderType();
        header[5] = (byte) compression;

        Util.intToBytesN(RECORD_PRIMARY_VERSION, header, 6, 2);
        Util.intToBytesN(RECORD_SECONDARY_VERSION, header, 8, 2);
        
        header[10] = (byte) game.getGameValues().rule.ordinal();
        header[11] = (byte) game.getGameValues().table.getOrdinal();
        header[12] = (byte) game.getGameValues().table.getHoleSizeOrdinal();
        header[13] = (byte) game.getGameValues().ball.ordinal();
        
        header[20] = (byte) game.getEntireGame().getTotalFrames();  // todo
        header[21] = (byte) game.getEntireGame().getP1Wins();
        header[22] = (byte) game.getEntireGame().getP2Wins();

        Util.longToBytes(game.getEntireGame().getBeginTime(), header, 24);
        recordBeginTime = game.frameStartTime;
        Util.longToBytes(recordBeginTime, header, 32);
        
        /*
        header:
        Part             Index  Length  Comment
        Signature        0      4       TSR_
        RecordType       4      1
        Compression      5      1
        PrimaryVersion   6      2
        SecondaryVersion 8      2
        GameRule         10     1
        TableTypeIndex   11     1      
        HoleSizeIndex    12     1
        BallTypeIndex    13     1
        Reserved
        TotalFrames      20     1
        P1Wins           21     1       都不包含当前这一局
        P2Wins           22     1
        GameBeginTime    24     8       整场比赛开始时间
        FrameBeginTime   32     8       单局开始时间
        DurationMs       40     4       关闭stream时写入
        nCues            44     4       关闭stream时写入
        winnerNumber     48     1       胜者是1还是2,关闭时写入
         */

        wrapperStream.write(header);

        // Version 12之后为了standalone新加的功能
        writeOnePlayer(p1);
        writePlayerJson(p1);
        writeOnePlayer(p2);
        writePlayerJson(p2);

//        outputStream = new BufferedOutputStream(wrapperStream);
        createStream();

        recordPositions();
    }
    
    private void createStream() throws IOException {
        switch (compression) {
            case NO_COMPRESSION:
                outputStream = new BufferedOutputStream(wrapperStream);
                break;
            case DEFLATE_COMPRESSION:
                outputStream = new DeflaterOutputStream(wrapperStream, true);
                break;
            case GZ_COMPRESSION:
                outputStream = new GZIPOutputStream(wrapperStream, true);
                break;
            case XZ_COMPRESSION:
                outputStream = new XZOutputStream(wrapperStream, new LZMA2Options());
                break;
            default:
                throw new RuntimeException();
        }
    }

    private void writeOnePlayer(InGamePlayer player) throws IOException {
        byte[] buffer = new byte[PLAYER_HEADER_LENGTH];
        buffer[0] = (byte) (player.getPlayerType() == PlayerType.PLAYER ? 0 : 1);

        byte[] id = player.getPlayerPerson().getPlayerId().getBytes(StandardCharsets.UTF_8);
        System.arraycopy(id, 0, buffer, 2, id.length);

        byte[] playCueId = player.getPlayCue().cueId.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(playCueId, 0, buffer, 34, playCueId.length);
        byte[] breakCueId = player.getBreakCue().cueId.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(breakCueId, 0, buffer, 66, breakCueId.length);

        wrapperStream.write(buffer);
    }
    
    private void writePlayerJson(InGamePlayer player) throws IOException {
        PlayerPerson person = player.getPlayerPerson();
        JSONObject personJson = person.toJsonObject();
        // todo: 由于cue没有toJson,暂不考虑自己的杆
        String str = personJson.toString(0);
        byte[] byteStr = str.getBytes(StandardCharsets.UTF_8);
        byte[] lenBuf = new byte[4];
        Util.int32ToBytes(byteStr.length, lenBuf, 0);
        wrapperStream.write(lenBuf);
        wrapperStream.write(byteStr);
    }

    public boolean isFinished() {
        return finished;
    }

    public void stopRecording(boolean normalFinish) {
        this.finished = true;
        if (outputStream != null && wrapperStream != null) {
            try {
                outputStream.write(FLAG_TERMINATE);
                outputStream.flush();
                
                if (outputStream instanceof GZIPOutputStream) {
                    ((GZIPOutputStream) outputStream).finish();
                } else if (outputStream instanceof XZOutputStream) {
                    ((XZOutputStream) outputStream).finish();
                }
                
                outputStream.close();
                wrapperStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            
            try (RandomAccessFile raf = new RandomAccessFile(outFile, "rw")) {
                long duration = System.currentTimeMillis() - recordBeginTime;
                byte[] buffer4 = new byte[4];
                
                raf.seek(40);
                Util.int32ToBytes((int) duration, buffer4, 0);
                raf.write(buffer4);
                
                Util.int32ToBytes(nCues, buffer4, 0);
                raf.write(buffer4);
                
                if (normalFinish)
                    raf.write(game.getWiningPlayer().getInGamePlayer().getPlayerNumber());
                else 
                    raf.write(0);
            } catch (IOException e) {
                e.printStackTrace();
            }

//            long time = System.currentTimeMillis();
//            int duration = (int) ((time - startTime) / 1000);
//            
//            RandomAccessFile raf = new RandomAccessFile(outFile, "rw");
//            raf.seek(8);
//            byte[] buf = new byte[4];
//            Util.int32ToBytes(nCues, buf, 0);
//            raf.write(buf);
//            Util.int32ToBytes(duration, buf, 0);
//            raf.write(buf);
//            raf.close();
        }
    }

    private void createDirIfNotExist() {
        File dir = new File(RECORD_DIR);

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException();
            }
        }
    }
}
