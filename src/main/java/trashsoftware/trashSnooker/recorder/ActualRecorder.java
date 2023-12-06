package trashsoftware.trashSnooker.recorder;

import org.json.JSONObject;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;
import trashsoftware.trashSnooker.core.Game;
import trashsoftware.trashSnooker.core.InGamePlayer;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.PlayerType;
import trashsoftware.trashSnooker.core.career.championship.MetaMatchInfo;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.util.config.ConfigLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

public abstract class ActualRecorder implements GameRecorder {
    public static final int RECORD_PRIMARY_VERSION = 14;
    public static final int RECORD_SECONDARY_VERSION = 1;
    public static final int HEADER_LENGTH = 64;
    public static final int PLAYER_HEADER_LENGTH = 40;
    public static final int TOTAL_HEADER_LENGTH = HEADER_LENGTH + PLAYER_HEADER_LENGTH * 2;
    public static final String SIGNATURE = "TSR_";

    public static final int FLAG_NOT_BEGUN = -1;
    public static final int FLAG_TERMINATE = 0;
    public static final int FLAG_CUE = 1;
    public static final int FLAG_HANDBALL = 2;
    public static final int FLAG_REPOSITION = 3;

    public static final int NO_COMPRESSION = 0;
    public static final int DEFLATE_COMPRESSION = 1;
    public static final int GZ_COMPRESSION = 2;
    public static final int XZ_COMPRESSION = 3;
    public static final String RECORD_DIR = "user" + File.separator + "replays";
    public static final String RECORD_EXPORT_DIR = "user" + File.separator + "replay_videos";
    protected static DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
    protected int compression = NO_COMPRESSION;
    protected File outFile;
    protected OutputStream outputStream;
    protected Game<?, ?> game;
    protected CueRecord cueRecord;
    protected Movement movement;
    protected ScoreResult scoreResult;
    protected TargetRecord thisTarget;
    // 这玩意一定和下一杆的thisTarget一样，但是为了方便所以记录，反正3字节的事
    protected TargetRecord nextTarget;
    protected CueAnimationRec animationRec;
    protected boolean finished = false;
    protected int nCues = 0;  // 只计击打，不计放置等
    protected int nMovementFrames = 0;  // 总共的动画帧数
    protected int totalBeforeCueMs = 0;  // 出杆动画在球出去之前花费的总时间，ms，1倍速
    protected long recordBeginTime;
    protected List<ExtraBlock> extraBlocks = new ArrayList<>();
    private OutputStream wrapperStream;

    public ActualRecorder(Game<?, ?> game, MetaMatchInfo metaMatchInfo) {
        this.game = game;
        if (metaMatchInfo != null) {
            this.extraBlocks.add(new ExtraBlock(ExtraBlock.TYPE_META_MATCH, metaMatchInfo));
        }
        
        createDirIfNotExist();

        setCompression(ConfigLoader.getInstance().getString("recordCompression", "xz"));

        outFile = new File(String.format("%s%sreplay_%s.replay",
                RECORD_DIR, File.separator, dateFormat.format(new Date())));
    }

    public static boolean isPrimaryCompatible(int replayPrimary) {
        return replayPrimary == ActualRecorder.RECORD_PRIMARY_VERSION;
    }

    public static boolean isSecondaryCompatible(int replayPrimary, int replaySecondary) {
        if (replaySecondary == ActualRecorder.RECORD_SECONDARY_VERSION) return true;
        
        if (ActualRecorder.RECORD_PRIMARY_VERSION == 12 && replayPrimary == 12) {
            if ((ActualRecorder.RECORD_SECONDARY_VERSION == 8 || ActualRecorder.RECORD_SECONDARY_VERSION == 9) 
                    && (replaySecondary >= 7 && replaySecondary <= 9)) {
                return true;
            }
        }
        
        return false;
    }

    public void setCompression(String compression) {
        switch (compression.toLowerCase(Locale.ROOT)) {
            case "deflate":
                setCompression(DEFLATE_COMPRESSION);
                break;
            case "gz":
                setCompression(GZ_COMPRESSION);
                break;
            case "xz":
                setCompression(XZ_COMPRESSION);
                break;
            case "none":
            case "":
            default:
                setCompression(NO_COMPRESSION);
                break;
        }
    }

    public void setCompression(int compression) {
        this.compression = compression;
    }

    protected abstract byte recorderType();

    public abstract void recordPositions() throws IOException;

    public final void recordCue(CueRecord cueRecord, TargetRecord thisTarget) throws RecordingException {
        if (this.cueRecord != null || this.thisTarget != null)
            throw new RecordingException("Repeated recording");
        this.cueRecord = cueRecord;
        this.thisTarget = thisTarget;
        this.nCues++;
    }

    public final void recordMovement(Movement movement) throws RecordingException {
        if (this.movement != null) throw new RecordingException("Repeated recording");
//        System.out.println(movement.getMovementMap().get(game.getCueBall()).size());
        this.movement = movement;
    }
    
    public final void recordCueAnimation(CueAnimationRec cueAnimationRec) throws RecordingException {
        if (this.animationRec != null) throw new RecordingException("Repeated recording");
//        System.out.println(movement.getMovementMap().get(game.getCueBall()).size());
        this.animationRec = cueAnimationRec;
    }

    public final void recordScore(ScoreResult scoreResult) throws RecordingException {
        if (this.scoreResult != null) throw new RecordingException("Repeated recording");
        this.scoreResult = scoreResult;
    }

    public final void recordNextTarget(TargetRecord nextTarget) throws RecordingException {
        if (this.nextTarget != null) throw new RecordingException("Repeated recording");
        this.nextTarget = nextTarget;
    }

    public void writeCueToStream() throws RecordingException {
        if (cueRecord == null || movement == null || scoreResult == null || 
                thisTarget == null || nextTarget == null) {
            throw new RecordingException(String.format("Score not filled: %s, %s, %s, %s\n",
                    cueRecord, movement, scoreResult, nextTarget));
        }
        if (animationRec == null) {
            EventLogger.warning("Animation record is null");
        }
        
        try {
            outputStream.write(FLAG_CUE);
            writeCue(cueRecord, movement, thisTarget, nextTarget, animationRec);
            byte[] b = scoreResult.toBytes();
            outputStream.write(b);
            recordPositions();

            cueRecord = null;
            movement = null;
            scoreResult = null;
            thisTarget = null;
            nextTarget = null;
            animationRec = null;
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }

    public void writeBallInHandPlacement() {
        try {
            outputStream.write(FLAG_HANDBALL);
            writeBallInHand();
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }

    public abstract void writeCue(CueRecord cueRecord,
                                  Movement movement,
                                  TargetRecord thisTarget,
                                  TargetRecord nextTarget,
                                  CueAnimationRec animationRec) throws IOException;

    public abstract void writeBallInHand() throws IOException;

    public void startRecoding() throws IOException {
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
        header[14] = (byte) game.getGameValues().table.getPocketDifficultyOrdinal(); 
        
        Util.intToBytesN(GameView.productionFrameRate, header, 15, 2);

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
        PocketDiffIndex  14     1
        FrameRate        15     2
        Reserved
        TotalFrames      20     1
        P1Wins           21     1       都不包含当前这一局
        P2Wins           22     1
        GameBeginTime    24     8       整场比赛开始时间
        FrameBeginTime   32     8       单局开始时间
        DurationMs       40     4       关闭stream时写入
        nCues            44     4       关闭stream时写入
        winnerNumber     48     1       胜者是1还是2,关闭时写入
        nMovementFrames  52     4       总动画帧数，关闭时写入
        totalBeforeCueMs 56     4       出杆动画占用的总额外时长
         */

        wrapperStream.write(header);

        // Version 12之后为了standalone新加的功能
        writeOnePlayer(p1);
        writePlayerJson(p1);
        writeOnePlayer(p2);
        writePlayerJson(p2);

        // Version 12.8 之后的extra field
        // 前4位是总长度（包含记录长度的这4字节）
        byte[] extraLength = new byte[4];
        byte[] extra = createExtras();

        Util.int32ToBytes(extra.length + 4, extraLength, 0);
        wrapperStream.write(extraLength);
        wrapperStream.write(extra);

//        outputStream = new BufferedOutputStream(wrapperStream);
        createStream();

        recordPositions();
    }

    protected byte[] createExtras() {
        List<byte[]> contents = new ArrayList<>();
        for (ExtraBlock block : extraBlocks) {
            /*
             每个block结构：
             0: type
             1-5: 本block长度，包含0位的type
             5: 内容
             */

            if (block.blockType == ExtraBlock.TYPE_META_MATCH) {
                byte[] blockContent = new byte[261];  // 我们认为matchId最多256字节
                blockContent[0] = (byte) block.blockType;
                Util.int32ToBytes(blockContent.length, blockContent, 1);
                byte[] bid = block.blockContent.toString().getBytes(StandardCharsets.UTF_8);
                System.arraycopy(bid, 0, blockContent, 5, bid.length);
                contents.add(blockContent);
            }
        }

        int totalLen = 0;
        for (byte[] bb : contents) totalLen += bb.length;

        byte[] res = new byte[totalLen];
        int index = 0;
        for (byte[] bb : contents) {
            System.arraycopy(bb, 0, res, index, bb.length);
            index += bb.length;
        }
        return res;
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
                throw new RecordingException();
        }
    }

    private void writeOnePlayer(InGamePlayer player) throws IOException {
        byte[] buffer = new byte[PLAYER_HEADER_LENGTH];
        buffer[0] = (byte) (player.getPlayerType() == PlayerType.PLAYER ? 0 : 1);

        byte[] id = player.getPlayerPerson().getPlayerId().getBytes(StandardCharsets.UTF_8);
        if (id.length > 32) {
            System.err.println("Id too long: " + player.getPlayerPerson().getPlayerId());
            id = Arrays.copyOf(id, 32);
        }
        // 前8reserve
        System.arraycopy(id, 0, buffer, 8, id.length);

//        // todo: 存cuebrand
//        byte[] playCueBrandId = player.getPlayCue().getInstanceId().getBytes(StandardCharsets.UTF_8);
//        System.arraycopy(playCueBrandId, 0, buffer, 34, playCueBrandId.length);
//        byte[] playCueInsId = player.getPlayCue().getInstanceId().getBytes(StandardCharsets.UTF_8);
//        System.arraycopy(playCueInsId, 0, buffer, 66, playCueInsId.length);
//        
//        byte[] breakCueBrandId = player.getBreakCue().getInstanceId().getBytes(StandardCharsets.UTF_8);
//        System.arraycopy(breakCueBrandId, 0, buffer, 130, breakCueBrandId.length);
//        byte[] breakCueInsId = player.getBreakCue().getInstanceId().getBytes(StandardCharsets.UTF_8);
//        System.arraycopy(breakCueInsId, 0, buffer, 162, breakCueInsId.length);

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
                EventLogger.error(e);
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

                raf.seek(52);
                Util.int32ToBytes(nMovementFrames, buffer4, 0);
                raf.write(buffer4);
                Util.int32ToBytes(totalBeforeCueMs, buffer4, 0);
                raf.write(buffer4);
                
            } catch (IOException e) {
                EventLogger.error(e);
            }
        }
    }

    @Override
    public void abort() {
        stopRecording(false);
    }

    @Override
    public void deleteRecord() {
        if (!outFile.delete()) {
            EventLogger.log("Cannot delete record: " + outFile, EventLogger.INFO, true);
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
