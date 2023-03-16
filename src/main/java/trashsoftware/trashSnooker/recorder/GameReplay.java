package trashsoftware.trashSnooker.recorder;

import org.tukaani.xz.XZInputStream;
import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.movement.MovementFrame;
import trashsoftware.trashSnooker.core.scoreResult.*;
import trashsoftware.trashSnooker.core.table.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public abstract class GameReplay implements GameHolder {

    public final GameValues gameValues;
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
    protected TargetRecord thisTarget;
    protected TargetRecord nextTarget;
    protected int currentFlag = GameRecorder.FLAG_NOT_BEGUN;
    protected Ball[] balls;
    protected HashMap<Integer, Ball> valueBallMap = new HashMap<>();
//    protected HashMap<Ball, double[]> lastPositions = new HashMap<>();
    protected Ball cueBall;
    protected int stepIndex = 0;
    protected boolean everFinished = false;
    
    protected List<CueStep> historySteps = new ArrayList<>();

    protected GameReplay(BriefReplayItem item) throws IOException, VersionException {
        if (!GameRecorder.isSecondaryCompatible(item.secondaryVersion)) 
            throw new VersionException(item.primaryVersion, item.secondaryVersion);

        this.item = item;
        GameRule gameRule = item.getGameType();
        gameValues = item.gameValues;
        this.p1 = item.getP1();
        this.p2 = item.getP2();

        this.wrapperStream = new FileInputStream(item.getFile());
        if (wrapperStream.skip(item.headerLength()) != item.headerLength()) {
            throw new IOException();
        }
        System.out.println(wrapperStream.available());
        createInputStream(item.compression);

        switch (gameRule) {
            case SNOOKER:
                table = new SnookerTable(gameValues.table);
                scoreFactory = new SnookerScoreFactory();
                break;
            case MINI_SNOOKER:
                table = new MiniSnookerTable(gameValues.table);
                scoreFactory = new SnookerScoreFactory();
                break;
            case CHINESE_EIGHT:
                table = new ChineseEightTable(gameValues.table);
                scoreFactory = new ChineseEightScoreFactory();
                break;
            case SIDE_POCKET:
                table = new SidePocketTable(gameValues.table);
                scoreFactory = new SidePocketScoreFactory();
                break;
            default:
                throw new EnumConstantNotPresentException(GameRule.class, gameRule.name());
        }
        scoreResBuf = new byte[scoreFactory.byteLength()];

        loadBallPositions();
    }

    public static GameReplay loadReplay(BriefReplayItem item) 
            throws IOException, VersionException {
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

    @Override
    public Table getTable() {
        return table;
    }

    @Override
    public Ball[] getAllBalls() {
        return balls;
    }

    @Override
    public Ball getCueBall() {
        return cueBall;
    }

    protected abstract void loadBallPositions() throws IOException;

    public boolean finished() {
        return currentFlag == GameRecorder.FLAG_TERMINATE;
    }

    public boolean loadNext() {
        System.out.println("Step: " + stepIndex + ", total: " + historySteps.size());
        if (stepIndex < historySteps.size()) {
            setCurrentFromHistory();
            Map<Ball, MovementFrame> endPos = currentMovement.getEndingPositions();
            for (Ball ball : getAllBalls()) {
                MovementFrame lastFrame = endPos.get(ball);
                ball.setPotted(lastFrame.potted);
                ball.setX(lastFrame.x);
                ball.setY(lastFrame.y);
            }
            stepIndex++;
            return true;
        } else if (everFinished) {
            currentFlag = GameRecorder.FLAG_TERMINATE;
            return false;
        } else {
            try {
                if (inputStream.read(buffer1) != buffer1.length) {
                    everFinished = true;
                    return false;
                }
                currentFlag = buffer1[0] & 0xff;
                System.out.println("Flag: " + currentFlag);
                
                if (currentFlag == GameRecorder.FLAG_TERMINATE) {
                    everFinished = true;
                    return false;
                }
                readNext();
                stepIndex++;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return currentFlag != GameRecorder.FLAG_TERMINATE;
        }
    }
    
    private void setCurrentFromHistory() {
        CueStep step = historySteps.get(stepIndex);
        if (step instanceof ActualStep) {
            ActualStep actualStep = (ActualStep) step;
            currentCueRecord = actualStep.cueRecord;
            currentMovement = actualStep.movement;
            currentMovement.reset();
            currentScoreResult = actualStep.scoreResult;
            thisTarget = actualStep.thisTarget;
            nextTarget = actualStep.nextTarget;
            currentFlag = GameRecorder.FLAG_CUE;

            Map<Ball, MovementFrame> sps = currentMovement.getStartingPositions();
            for (Ball ball : getAllBalls()) {
                MovementFrame sp = sps.get(ball);
                ball.setPotted(sp.potted);
                ball.setX(sp.x);
                ball.setY(sp.y);
            }
        } else if (step instanceof BallInHandStep) {
            currentFlag = GameRecorder.FLAG_HANDBALL;
        }
    }
    
    public boolean loadLast() {
        if (stepIndex < 1) return false;
        stepIndex -= 1;
        setCurrentFromHistory();
        return true;
    }

    public int getCurrentFlag() {
        return currentFlag;
    }

    protected synchronized void readNext() throws IOException {
        if (currentFlag == GameRecorder.FLAG_CUE) {
//            storeLastPositions();
            loadNextRecordAndMovement();
            loadNextScoreResult();
            loadBallPositions();
            System.out.println("Next: " + currentCueRecord.cuePlayer.getPlayerPerson().getPlayerId());
            ActualStep actualStep = new ActualStep(
                    currentCueRecord,
                    currentMovement,
                    currentScoreResult,
                    thisTarget,
                    nextTarget
            );
            historySteps.add(actualStep);
        } else if (currentFlag == GameRecorder.FLAG_HANDBALL) {
//            storeLastPositions();
            loadBallInHand();
            BallInHandStep step = new BallInHandStep();
            historySteps.add(step);
        } else if (currentFlag == GameRecorder.FLAG_TERMINATE) {
//            storeLastPositions();
        }
    }

    /**
     * @return {ball: [x, y, axisX, axisY, axisZ, rotationDeg, potted]}
     */
    public HashMap<Ball, double[]> getCurrentPositions() {
        HashMap<Ball, double[]> pos = new HashMap<>();
        for (Ball ball : balls) {
            pos.put(ball, new double[]{ball.getX(), ball.getY(), 
                    ball.getAxisX(), ball.getAxisY(), ball.getAxisZ(), ball.getFrameDegChange(),
                    ball.isPotted() ? 1 : 0});
        }
        return pos;
    }

    public Cue getCurrentCue() {
        return currentCueRecord.isBreaking ?
                currentCueRecord.cuePlayer.getBreakCue() : currentCueRecord.cuePlayer.getPlayCue();
    }

    public TargetRecord getNextTarget() {
        return nextTarget;
    }

    public TargetRecord getThisTarget() {
        return thisTarget;
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
    
    protected abstract static class CueStep {
    }
    
    protected static class BallInHandStep extends CueStep {
    }
    
    protected static class ActualStep extends CueStep {
        protected final CueRecord cueRecord;
        protected final Movement movement;
        protected final ScoreResult scoreResult;
        protected final TargetRecord thisTarget;
        protected final TargetRecord nextTarget;
        
        ActualStep(CueRecord cueRecord, Movement movement, ScoreResult scoreResult, 
                   TargetRecord thisTarget, TargetRecord nextTarget) {
            this.cueRecord = cueRecord;
            this.movement = movement;
            this.scoreResult = scoreResult;
            this.thisTarget = thisTarget;
            this.nextTarget = nextTarget;
        }
    }
}
