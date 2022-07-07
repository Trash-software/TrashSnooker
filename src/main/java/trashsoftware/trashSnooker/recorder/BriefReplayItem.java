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
    private final int totalCues; 
    private final int durationSec;
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

            int version = (int) Util.bytesToIntN(header, 6, 2);
            if (version != App.VERSION) throw new RuntimeException("Record of old version");

            totalCues = Util.bytesToInt32(header, 8);
            durationSec = Util.bytesToInt32(header, 12);
            
            p1 = readOnePlayer(raf);
            p2 = readOnePlayer(raf);
        }
    }

    private InGamePlayer readOnePlayer(RandomAccessFile raf) throws IOException {
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

        return new InGamePlayer(playerPerson, breakCue, playCue, type);
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

    public int getTotalCues() {
        return totalCues;
    }

    public GameType getGameType() {
        return gameType;
    }

    public int getReplayType() {
        return replayType;
    }

    public int getDurationSec() {
        return durationSec;
    }
    
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
                ", totalCues=" + totalCues +
                ", durationSec=" + durationSec +
                '}';
    }
}
