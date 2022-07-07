package trashsoftware.trashSnooker.recorder;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.Game;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.movement.MovementFrame;
import trashsoftware.trashSnooker.util.Util;

import java.io.IOException;
import java.util.Deque;
import java.util.Map;

public class NaiveGameRecorder extends GameRecorder {

    public NaiveGameRecorder(Game game) {
        super(game);
    }

    @Override
    protected void writeInitMessage() throws IOException {

    }

    @Override
    protected void writeCue(CueRecord cueRecord, Movement movement) throws IOException {
        writeCueRecord(cueRecord);
        writeMovement(movement);
    }

    @Override
    protected void writeBallInHand() throws IOException {
        recordOneBallPos(game.getCueBall());
    }

    private void writeCueRecord(CueRecord cueRecord) throws IOException {
        byte[] buf = new byte[56];
        buf[0] = (byte) (game.getPlayer1().getInGamePlayer() == cueRecord.cuePlayer ? 1 : 2);
        buf[1] = (byte) (game.isBreaking() ? 1 : 0);

        Util.doubleToBytes(cueRecord.selectedPower, buf, 8);
        Util.doubleToBytes(cueRecord.aimUnitX, buf, 16);
        Util.doubleToBytes(cueRecord.aimUnitY, buf, 24);
        Util.doubleToBytes(cueRecord.wantedUnitVerSpin, buf, 32);
        Util.doubleToBytes(cueRecord.wantedUnitSideSpin, buf, 40);
        Util.doubleToBytes(cueRecord.cueAngle, buf, 48);

        outputStream.write(buf);
    }
    
    private void writeMovement(Movement movement) throws IOException {
        int steps = -1;
        for (Ball ball : game.getAllBalls()) {
            // 顺序很重要
            Deque<MovementFrame> frames = movement.getMovementMap().get(ball);
            if (steps == -1) {
                steps = frames.size();  // 记录有多少帧
                byte[] stepsBytes = new byte[4];
                Util.int32ToBytes(steps, stepsBytes, 0);
                outputStream.write(stepsBytes);
            }
            if (frames.size() != steps) 
                throw new RuntimeException();

            outputStream.write(ball.getValue());

            byte[] buf = new byte[17];
            for (MovementFrame frame : frames) {
                buf[0] = (byte) (frame.potted ? 1 : 0);
                Util.doubleToBytes(frame.x, buf, 1);
                Util.doubleToBytes(frame.y, buf, 9);
                outputStream.write(buf);
            }
        }
    }

    @Override
    public void recordPositions() throws IOException {
        for (Ball b : game.getAllBalls()) {
            recordOneBallPos(b);
        }
    }

    private void recordOneBallPos(Ball b) throws IOException {
        outputStream.write(b.getValue());
        outputStream.write(b.isPotted() ? 1 : 0);
        byte[] buffer = new byte[8];
        Util.doubleToBytes(b.getX(), buffer, 0);
        outputStream.write(buffer);
        Util.doubleToBytes(b.getY(), buffer, 0);
        outputStream.write(buffer);
    }

    @Override
    protected byte recorderType() {
        return 0;
    }
}
