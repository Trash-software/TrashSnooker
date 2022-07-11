package trashsoftware.trashSnooker.recorder;

import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;
import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.Game;
import trashsoftware.trashSnooker.core.Player;
import trashsoftware.trashSnooker.core.PlayerType;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

public abstract class GameRecorder {
    public static final int HEADER_LENGTH = 32;
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
    protected EntireGame entireGame;
    protected DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

    protected CueRecord cueRecord;
    protected Movement movement;
    protected ScoreResult scoreResult;

    public GameRecorder(Game game, EntireGame entireGame) {
        this.game = game;
        this.entireGame = entireGame;
        
        createDirIfNotExist();
        
        setCompression(XZ_COMPRESSION);

        outFile = new File(String.format("%s%sreplay_%s.replay",
                RECORD_DIR, File.separator, dateFormat.format(new Date())));
    }
    
    public void setCompression(int compression) {
        this.compression = compression;
    }

    protected abstract byte recorderType();

    public abstract void recordPositions() throws IOException;

    public final void recordCue(CueRecord cueRecord) {
        if (this.cueRecord != null) throw new RuntimeException("Repeated recording");
        this.cueRecord = cueRecord;
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

    public final void writeCueToStream() {
        if (cueRecord == null || movement == null || scoreResult == null) {
            throw new RuntimeException("Score not filled");
        }
        System.out.println(movement);
        try {
            outputStream.write(FLAG_CUE);
            writeCue(cueRecord, movement);
            byte[] b = scoreResult.toBytes();
            outputStream.write(b);
            recordPositions();

            cueRecord = null;
            movement = null;
            scoreResult = null;
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
                                     Movement movement) throws IOException;

    protected abstract void writeBallInHand() throws IOException;

    public final void startRecoding() throws IOException {
        wrapperStream = new FileOutputStream(outFile);

        byte[] header = new byte[HEADER_LENGTH];

        Player p1 = game.getPlayer1();
        Player p2 = game.getPlayer2();

        byte[] signature = SIGNATURE.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(signature, 0, header, 0, 4);

        header[4] = recorderType();
        header[5] = (byte) game.getGameType().ordinal();

        Util.intToBytesN(App.VERSION, header, 6, 2);

        header[8] = (byte) compression;

        Util.longToBytes(System.currentTimeMillis(), header, 12);
        header[20] = (byte) entireGame.getTotalFrames();  // todo
        header[21] = (byte) entireGame.getP1Wins();
        header[22] = (byte) entireGame.getP2Wins(); 
        
        /*
        header:
        Part             Index  Length  Comment
        Signature        0      4       TSR_
        RecordType       4      1
        GameType         5      1
        RecordVersion    6      2
        Compression      8      1
        Reserved         9      3
        BeginTime        12     8
        TotalFrames      20     1
        P1Wins           21     1       都不包含当前这一局
        P2Wins           22     1
         */

        wrapperStream.write(header);

        writeOnePlayer(p1);
        writeOnePlayer(p2);

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

    private void writeOnePlayer(Player player) throws IOException {
        byte[] buffer = new byte[PLAYER_HEADER_LENGTH];
        buffer[0] = (byte) (player.getInGamePlayer().getPlayerType() == PlayerType.PLAYER ? 0 : 1);

        byte[] id = player.getPlayerPerson().getPlayerId().getBytes(StandardCharsets.UTF_8);
        System.arraycopy(id, 0, buffer, 2, id.length);

        byte[] playCueId = player.getInGamePlayer().getPlayCue().cueId.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(playCueId, 0, buffer, 34, playCueId.length);
        byte[] breakCueId = player.getInGamePlayer().getBreakCue().cueId.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(breakCueId, 0, buffer, 66, breakCueId.length);

        wrapperStream.write(buffer);
    }

    public void stopRecording() {
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
