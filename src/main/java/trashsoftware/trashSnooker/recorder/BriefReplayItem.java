package trashsoftware.trashSnooker.recorder;

import javafx.fxml.FXML;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.career.championship.MetaMatchInfo;
import trashsoftware.trashSnooker.core.metrics.*;
import trashsoftware.trashSnooker.core.person.PlayerPerson;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.Util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class BriefReplayItem {
    
    private final File file;
    final int replayType;
    public final GameValues gameValues;
    private Collection<SubRule> subRules = List.of();
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
    public final int p2Wins;  // 应该是在这局之前P1和P2各赢了几局
    public final int frameWinnerNumber;
//    private final int durationSec;
    private final InGamePlayer p1;
    private final InGamePlayer p2;
    protected int frameRate;
    protected int nMovementFrames;
    protected int totalBeforeCueMs;
    
    private int extraLength;
    private List<ExtraBlock> extraBlocks = new ArrayList<>();
    
    private long personObjLength = 0;
    
    public BriefReplayItem(File file) throws IOException, VersionException, ReplayException {
        this.file = file;

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] header = new byte[ActualRecorder.HEADER_LENGTH];
            if (raf.read(header) != header.length)
                throw new IOException("Header length not match");

            String sig = new String(header, 0, 4);
            if (!ActualRecorder.SIGNATURE.equals(sig)) throw new ReplayException("Not a replay");

            replayType = header[4] & 0xff;
            compression = header[5] & 0xff;
            
            primaryVersion = (int) Util.bytesToIntN(header, 6, 2);
            secondaryVersion = (int) Util.bytesToIntN(header, 8, 2);
            if (!ActualRecorder.isPrimaryCompatible(primaryVersion)) {
                throw new VersionException(primaryVersion, secondaryVersion);
            }
//            System.out.println("Replay version: " + primaryVersion + "." + secondaryVersion);

            GameRule gameRule = GameRule.values()[header[10] & 0xff];
            TableMetrics.TableBuilderFactory factory = TableMetrics.TableBuilderFactory.values()[header[11] & 0xff];
            PocketSize pocketSize = factory.supportedHoles[header[12] & 0xff];
            PocketDifficulty pocketDifficulty = factory.supportedDifficulties[header[14] & 0xff];
            BallMetrics ballMetrics = BallMetrics.values()[header[13] & 0xff];
            TableMetrics tableMetrics = factory.create()
                    .pocketDifficulty(pocketDifficulty)
                    .holeSize(pocketSize)
                    .build();
            
            if (primaryVersion == 12 && secondaryVersion <= 8) {
                frameRate = 50;  // 以前就是50帧
            } else {
                frameRate = (int) Util.bytesToIntN(header, 15, 2);
            }
            
            totalFrames = header[20] & 0xff;
            p1Wins = header[21] & 0xff;
            p2Wins = header[22] & 0xff;
            
            gameBeginTime = Util.bytesToLong(header, 24);
            frameBeginTime = Util.bytesToLong(header, 32);
            duration = Util.bytesToInt32(header, 40);
            nCues = Util.bytesToInt32(header, 44);
            frameWinnerNumber = header[48] & 0xff;
            
            nMovementFrames = Util.bytesToInt32(header, 52);
            totalBeforeCueMs = Util.bytesToInt32(header, 56);

            gameValues = new GameValues(gameRule, null, tableMetrics, ballMetrics);
            // fixme: table preset
            p1 = readOnePlayer(raf, 1);
            p2 = readOnePlayer(raf, 2);
            
            if (primaryVersion > 12 || 
                    (primaryVersion == 12 && secondaryVersion >= 8)) {
                byte[] extraLenBytes = new byte[4];
                if (raf.read(extraLenBytes) != extraLenBytes.length)
                    throw new IOException("Cannot read extra field");

                extraLength = Util.bytesToInt32(extraLenBytes, 0);

                byte[] extra = new byte[extraLength - 4];
                if (raf.read(extra) != extra.length)
                    throw new IOException("Extra field length not match");

                readExtraBlocks(extra);
            } else {
                extraLength = 0;
            }
            
            // 读了extraField才有
            gameValues.setSubRules(subRules);
        }
    }

    private InGamePlayer readOnePlayer(RandomAccessFile raf, int num) throws IOException, ReplayException {
        byte[] buf = new byte[8];
        if (raf.read(buf) != buf.length) throw new IOException("Cannot read header buffer");
        boolean isAi = buf[0] == 1;
        PlayerType type = isAi ? PlayerType.COMPUTER : PlayerType.PLAYER;

        byte[] nameBuf = new byte[32];
//        byte[] cueInsBuf = new byte[64];
        if (raf.read(nameBuf) != nameBuf.length) throw new IOException("Cannot read name buffer");
        int nameLen = Util.indexOf((byte) 0, nameBuf);
        if (nameLen < 0 || nameLen > nameBuf.length) {
            System.err.println("Cannot read name from record");
            nameLen = 32;
        }
        
        String pid = new String(nameBuf, 0, nameLen, StandardCharsets.UTF_8);
        
//        if (raf.read(nameBuf) != nameBuf.length) throw new IOException("Name buffer length not match");
//        String playCueBrandId = new String(nameBuf, 0, Util.indexOf((byte) 0, nameBuf), StandardCharsets.UTF_8);
//        if (raf.read(nameBuf) != nameBuf.length) throw new IOException();
//        String playCueInsId = new String(nameBuf, 0, Util.indexOf((byte) 0, cueInsBuf), StandardCharsets.UTF_8);
//        if (raf.read(nameBuf) != nameBuf.length) throw new IOException();
//        String breakCueBrandId = new String(nameBuf, 0, Util.indexOf((byte) 0, nameBuf), StandardCharsets.UTF_8);
//        if (raf.read(nameBuf) != nameBuf.length) throw new IOException();
//        String breakCueInsId = new String(nameBuf, 0, Util.indexOf((byte) 0, cueInsBuf), StandardCharsets.UTF_8);

        PlayerPerson playerPerson = null;
        if (primaryVersion == 11) {
            for (PlayerPerson person : DataLoader.getInstance().getAllPlayers()) {
                if (person.getPlayerId().equals(pid)) {
                    playerPerson = person;
                    break;
                }
            }
            if (playerPerson == null)
                throw new ReplayException("Player " + pid + " does not exist in current database");
        } else {
            playerPerson = readPlayerPerson(pid, raf);
//            System.out.println(playerPerson.getPlayerId() + playerPerson.getCuePlayType().toString());
        }
        
//        CueBrand playCueBrand = Objects.requireNonNull(DataLoader.getInstance().getCues().get(playCueBrandId));
//        Cue playCue = Cue.createForFastGame(playCueBrand);  // todo: restore instance
//        CueBrand breakCueBrand = Objects.requireNonNull(DataLoader.getInstance().getCues().get(breakCueBrandId));
//        Cue breakCue = Cue.createForFastGame(breakCueBrand);

        return new InGamePlayer(playerPerson, type, null, gameValues.rule, num, 1.0);
    }
    
    private PlayerPerson readPlayerPerson(String pid, RandomAccessFile raf) throws IOException {
        byte[] lenBuf = new byte[4];
        if (raf.read(lenBuf) != lenBuf.length) {
            throw new IOException("Cannot read length of player person");
        }
        int byteLen = Util.bytesToInt32(lenBuf, 0);
//        System.out.println(byteLen);
        byte[] strBuf = new byte[byteLen];
        if (raf.read(strBuf) != strBuf.length) {
            throw new IOException("Cannot read player person");
        }
        personObjLength += strBuf.length + lenBuf.length;
        String str = new String(strBuf);
        JSONObject jsonObject = new JSONObject(str);
        return PlayerPerson.fromJson(
                pid, jsonObject
        );
    }
    
    private void readExtraBlocks(byte[] pureExtra) {
        int index = 0;
        while (index < pureExtra.length) {
            int nextBlockType = pureExtra[index] & 0xff;
            int nextBlockLen = Util.bytesToInt32(pureExtra, index + 1);
            
            if (nextBlockType == ExtraBlock.TYPE_META_MATCH) {
                byte[] matchIdBuf = new byte[256];
                System.arraycopy(pureExtra, 5, matchIdBuf, 0, 256);
                String matchId = new String(matchIdBuf, 0, Util.indexOf((byte) 0, matchIdBuf), StandardCharsets.UTF_8);
                MetaMatchInfo mmi = MetaMatchInfo.fromString(matchId);
                extraBlocks.add(new ExtraBlock(nextBlockType, mmi));
            } else if (nextBlockType == ExtraBlock.TYPE_SUB_RULES) {
                int nSubRules = pureExtra[index + 5] & 0xff;
                subRules = new ArrayList<>();
                for (int i = 0; i < nSubRules; i += 32) {
                    int sri = index + 6 + i * 32;
                    String srName = Util.decodeStringFromArr(pureExtra, sri, 32);
                    subRules.add(SubRule.valueOf(srName));
                }
            }
            
            index += nextBlockLen;
        }
    }
    
    public MetaMatchInfo getMetaMatchInfo() {
        for (ExtraBlock eb : extraBlocks) {
            if (eb.blockType == ExtraBlock.TYPE_META_MATCH) {
                return (MetaMatchInfo) eb.blockContent;
            }
        }
        return null;
    }

    public int getExtraLength() {
        return extraLength;
    }

    public long headerLength() {
        return ActualRecorder.TOTAL_HEADER_LENGTH + personObjLength;
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
        String rule = GameRule.toReadable(getGameType());
        
        String subRuleNames = subRules.stream().map(sr -> {
            if (sr == SubRule.RAW_STD) return "";
            else return sr.toString();
        }).collect(Collectors.joining(" "));
        if (subRuleNames.isBlank()) return rule;
        else return rule + " - " + subRuleNames;
    }
    
    @FXML
    public String getFrameBeginTimeString() {
        return Util.SHOWING_DATE_FORMAT.format(frameBeginTime);
    }

    public String getGameBeginTimeString() {
        return Util.SHOWING_DATE_FORMAT.format(gameBeginTime);
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
    public int getNCues() {
        return nCues;
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
