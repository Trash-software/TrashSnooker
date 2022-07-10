package trashsoftware.trashSnooker.recorder;

import javafx.fxml.FXML;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Recorder;
import trashsoftware.trashSnooker.util.Util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class BriefReplayItem {
    
    private final File file;
    final int replayType;
    public final GameType gameType;
    final int recordVersion;
//    private final int totalCues;
    protected final int compression;
    public final long beginTime;
    public final int totalFrames;
    public final int p1Wins;
    public final int p2Wins;
//    private final int durationSec;
    private final InGamePlayer p1;
    private final InGamePlayer p2;
    
    public BriefReplayItem(File file) throws IOException {
        this.file = file;

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] header = new byte[GameRecorder.HEADER_LENGTH];
            if (raf.read(header) != header.length)
                throw new IOException();

            String sig = new String(header, 0, 4);
            if (!GameRecorder.SIGNATURE.equals(sig)) throw new RuntimeException("Not a replay");

            replayType = header[4];
            gameType = GameType.values()[header[5] & 0xff];

            recordVersion = (int) Util.bytesToIntN(header, 6, 2);

            compression = header[8] & 0xff;
            beginTime = Util.bytesToLong(header, 12);
            
            totalFrames = header[20] & 0xff;
            p1Wins = header[21] & 0xff;
            p2Wins = header[22] & 0xff;
            
            p1 = readOnePlayer(raf, 1);
            p2 = readOnePlayer(raf, 2);
        }
    }

    private InGamePlayer readOnePlayer(RandomAccessFile raf, int num) throws IOException {
        byte[] buf = new byte[2];
        if (raf.read(buf) != buf.length) throw new IOException();
        boolean isAi = buf[0] == 1;
        PlayerType type = isAi ? PlayerType.COMPUTER : PlayerType.PLAYER;

        byte[] nameBuf = new byte[32];
        if (raf.read(nameBuf) != nameBuf.length) throw new IOException();
        String pid = new String(nameBuf, 0, Util.indexOf((byte) 0, nameBuf), StandardCharsets.UTF_8);
        if (raf.read(nameBuf) != nameBuf.length) throw new IOException();
        String playCueId = new String(nameBuf, 0, Util.indexOf((byte) 0, nameBuf), StandardCharsets.UTF_8);
        if (raf.read(nameBuf) != nameBuf.length) throw new IOException();
        String breakCueId = new String(nameBuf, 0, Util.indexOf((byte) 0, nameBuf), StandardCharsets.UTF_8);

        PlayerPerson playerPerson = null;
        for (PlayerPerson person : Recorder.getPlayerPeople()) {
            if (person.getPlayerId().equals(pid)) {
                playerPerson = person;
                break;
            }
        }
        if (playerPerson == null) throw new RuntimeException("No such player");

        Cue playCue = Objects.requireNonNull(Recorder.getCues().get(playCueId));
        Cue breakCue = Objects.requireNonNull(Recorder.getCues().get(breakCueId));

        return new InGamePlayer(playerPerson, breakCue, playCue, type, num);
    }

    public InGamePlayer getP1() {
        return p1;
    }

    public InGamePlayer getP2() {
        return p2;
    }

    public File getFile() {
        return file;
    }

//    public int getTotalCues() {
//        return totalCues;
//    }

    public GameType getGameType() {
        return gameType;
    }

    public int getReplayType() {
        return replayType;
    }

//    public int getDurationSec() {
//        return durationSec;
//    }
    
    @FXML
    public String getFileName() {
        return file.getName();
    }
    
    @FXML
    public String getP1Name() {
        return p1.getPlayerPerson().getName() + (p1.getPlayerType() == PlayerType.COMPUTER ? "(AI)" : "");
    }
    
    @FXML
    public String getP2Name() {
        return p2.getPlayerPerson().getName() + (p2.getPlayerType() == PlayerType.COMPUTER ? "(AI)" : "");
    }

    @Override
    public String toString() {
        return "BriefReplayItem{" +
                "file=" + file +
                ", replayType=" + replayType +
                ", gameType=" + gameType +
                '}';
    }
}
