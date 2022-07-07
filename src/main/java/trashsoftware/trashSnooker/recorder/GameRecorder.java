package trashsoftware.trashSnooker.recorder;

import trashsoftware.trashSnooker.core.Game;
import trashsoftware.trashSnooker.core.Player;
import trashsoftware.trashSnooker.core.PlayerType;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class GameRecorder {
    public static final int HEADER_LENGTH = 32;
    public static final int PLAYER_HEADER_LENGTH = 98;
    public static final int TOTAL_HEADER_LENGTH = HEADER_LENGTH + PLAYER_HEADER_LENGTH * 2;
    public static final String SIGNATURE = "TSR_";
    
    public static final int FLAG_NOT_BEGUN = -1;
    public static final int FLAG_TERMINATE = 0;
    public static final int FLAG_CUE = 1;
    public static final int FLAG_HANDBALL = 2;
    
    protected static final String RECORD_DIR = "user" + File.separator + "replays";
    protected File outFile;
    protected OutputStream outputStream;
    protected OutputStream wrapperStream;
    
    protected int nCues = 0;
    protected long startTime = System.currentTimeMillis();
    
    protected Game game;
    protected DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
    
    public GameRecorder(Game game) {
        this.game = game;
        
        createDirIfNotExist();
        
        outFile = new File(String.format("%s%sreplay_%s.replay", 
                RECORD_DIR, File.separator, dateFormat.format(new Date())));
    }
    
    protected abstract void writeInitMessage() throws IOException;
    
    protected abstract byte recorderType();
    
    public abstract void recordPositions() throws IOException;
    
    public final void recordCue(CueRecord cueRecord, Movement movement) throws IOException {
        outputStream.write(FLAG_CUE);
        writeCue(cueRecord, movement);
        nCues++;
    }
    
    public final void recordBallInHandPlacement() throws IOException {
        outputStream.write(FLAG_HANDBALL);
        writeBallInHand();
    }
    
    protected abstract void writeCue(CueRecord cueRecord, Movement movement) throws IOException;
    
    protected abstract void writeBallInHand() throws IOException;
    
    public final void startRecoding() throws IOException {
        wrapperStream = new BufferedOutputStream(new FileOutputStream(outFile));
        outputStream = new BufferedOutputStream(wrapperStream);

        byte[] header = new byte[HEADER_LENGTH];

        Player p1 = game.getPlayer1();
        Player p2 = game.getPlayer2();

        byte[] signature = SIGNATURE.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(signature, 0, header, 0, 4);

        header[4] = recorderType();
        header[5] = (byte) game.getGameType().ordinal();
        
        Util.intToBytesN(App.VERSION, header, 6, 2);
        
        /*
        header:
        Part             Index  Length  Comment
        Signature        0      4       TSR_
        RecordType       4      1
        GameType         5      1
        RecordVersion    6      2
        TotalCues        8      4
        DurationSecs     12     4
         */

        wrapperStream.write(header);  // Naive

        writeOnePlayer(p1);
        writeOnePlayer(p2);
        
        writeInitMessage();
        recordPositions();
    }

    private void writeOnePlayer(Player player) throws IOException {
        byte[] buffer = new byte[PLAYER_HEADER_LENGTH];
        buffer[0] = (byte) (player.getInGamePlayer().getPlayerType() == PlayerType.PLAYER ? 0: 1);
        
        byte[] id = player.getPlayerPerson().getPlayerId().getBytes(StandardCharsets.UTF_8);
        System.arraycopy(id, 0, buffer, 2, id.length);
        
        byte[] playCueId = player.getInGamePlayer().getPlayCue().cueId.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(playCueId, 0, buffer, 34, playCueId.length);
        byte[] breakCueId = player.getInGamePlayer().getBreakCue().cueId.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(breakCueId, 0, buffer, 66, breakCueId.length);
        
        wrapperStream.write(buffer);
    }
    
    public void stopRecording() throws IOException {
        if (outputStream != null && wrapperStream != null) {
            outputStream.write(FLAG_TERMINATE);
            outputStream.flush();
            outputStream.close();
            wrapperStream.close();
            
            long time = System.currentTimeMillis();
            int duration = (int) ((time - startTime) / 1000);
            
            RandomAccessFile raf = new RandomAccessFile(outFile, "rw");
            raf.seek(8);
            byte[] buf = new byte[4];
            Util.int32ToBytes(nCues, buf, 0);
            raf.write(buf);
            Util.int32ToBytes(duration, buf, 0);
            raf.write(buf);
            raf.close();
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
