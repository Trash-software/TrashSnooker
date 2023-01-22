package trashsoftware.trashSnooker.recorder;

import javafx.fxml.FXML;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.util.Recorder;
import trashsoftware.trashSnooker.util.Util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Objects;

public class BriefReplayItem {
    
    private final File file;
    final int replayType;
    public final GameValues gameValues;
    public final int primaryVersion;
    public final int secondaryVersion;
//    private final int totalCues;
    protected final int compression;
    public final long gameBeginTime;
    public final long frameBeginTime;
    public final long duration;
    public final int nCues;
    public final int totalFrames;
    public final int p1Wins;
    public final int p2Wins;
    public final int frameWinnerNumber;
//    private final int durationSec;
    private final InGamePlayer p1;
    private final InGamePlayer p2;

    protected static DateFormat showingDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    
    public BriefReplayItem(File file) throws IOException, VersionException, RecordException {
        this.file = file;

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] header = new byte[GameRecorder.HEADER_LENGTH];
            if (raf.read(header) != header.length)
                throw new IOException();

            String sig = new String(header, 0, 4);
            if (!GameRecorder.SIGNATURE.equals(sig)) throw new RecordException("Not a replay");

            replayType = header[4] & 0xff;
            compression = header[5] & 0xff;
            
            primaryVersion = (int) Util.bytesToIntN(header, 6, 2);
            secondaryVersion = (int) Util.bytesToIntN(header, 8, 2);
            if (primaryVersion != GameRecorder.RECORD_PRIMARY_VERSION) {
                throw new VersionException(primaryVersion, secondaryVersion);
            }

            GameRule gameRule = GameRule.values()[header[10] & 0xff];
            TableMetrics.TableBuilderFactory factory = TableMetrics.TableBuilderFactory.values()[header[11] & 0xff];
            TableMetrics.HoleSize holeSize = factory.supportedHoles[header[12] & 0xff];
            BallMetrics ballMetrics = BallMetrics.values()[header[13] & 0xff];
            TableMetrics tableMetrics = factory.create().holeSize(holeSize).build();
            gameValues = new GameValues(gameRule, tableMetrics, ballMetrics);
            
            totalFrames = header[20] & 0xff;
            p1Wins = header[21] & 0xff;
            p2Wins = header[22] & 0xff;
            
            gameBeginTime = Util.bytesToLong(header, 24);
            frameBeginTime = Util.bytesToLong(header, 32);
            duration = Util.bytesToInt32(header, 40);
            nCues = Util.bytesToInt32(header, 44);
            frameWinnerNumber = header[48] & 0xff;
            
            p1 = readOnePlayer(raf, 1);
            p2 = readOnePlayer(raf, 2);
        }
    }

    private InGamePlayer readOnePlayer(RandomAccessFile raf, int num) throws IOException, RecordException {
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
        if (playerPerson == null) throw new RecordException("Player " + pid + " does not exist in current database");

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

    public GameRule getGameType() {
        return gameValues.rule;
    }

    public GameValues getGameValues() {
        return gameValues;
    }

    @FXML
    public String getGameTypeName() {
        return GameRule.toReadable(getGameType());
    }
    
    @FXML
    public String getFrameBeginTimeString() {
        return showingDateFormat.format(frameBeginTime);
    }

    public String getGameBeginTimeString() {
        return showingDateFormat.format(gameBeginTime);
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
    
    @FXML
    public String getDuration() {
        return Util.timeToReadable(duration);
    }
    
    @FXML
    public String getNCues() {
        return String.valueOf(nCues);
    }

    @Override
    public String toString() {
        return "BriefReplayItem{" +
                "file=" + file +
                ", replayType=" + replayType +
                ", gameValues=" + gameValues +
                '}';
    }
}
