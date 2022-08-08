package trashsoftware.trashSnooker.util;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class BallImageGenerator extends Application {

    private static void makePoolBall(int num) {
        Color baseColor = PoolBall.poolBallBaseColor(num);
        Font font = new Font(104);
        Canvas canvas = generateBasic(256, baseColor, num > 8, String.valueOf(num), font);

        File file = new File("images/pool/pool" + num + ".png");
        WritableImage image = canvas.snapshot(new SnapshotParameters(), null);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void makeSnookerBall(int value) {
        Color baseColor = Ball.snookerColor(value);
        Canvas canvas = generateBasic(256, baseColor, false, null, null);

        File file = new File("images/snooker/snooker" + value + ".png");
        WritableImage image = canvas.snapshot(new SnapshotParameters(), null);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Canvas generateBasic(int height,
                                        Color color,
                                        boolean whiteBorder,
                                        String text,
                                        Font font) {
        double width = Math.round(height * Math.PI);

        Canvas canvas = new Canvas();
        canvas.setWidth(width);
        canvas.setHeight(height);

        final double ratio = 0.5;
        double centerCircleH = height * ratio;
        double centerCircleW = height * Math.asin(ratio);

        double ccTopLeftY = height / 2.0 - centerCircleH / 2;

        GraphicsContext gc2d = canvas.getGraphicsContext2D();
        gc2d.setImageSmoothing(false);
        gc2d.setFill(color);
        gc2d.fillRect(0, 0, width, height);

        if (text != null) {
            gc2d.setFill(Ball.poolBallBaseColor(0));
            gc2d.fillOval(0, ccTopLeftY, centerCircleW, centerCircleH);
            gc2d.fillOval(width / 2, ccTopLeftY, centerCircleW, centerCircleH);
        }

        if (whiteBorder) {
            double borderH = height * 0.1875;
            gc2d.fillRect(0, 0, width, borderH);
            gc2d.fillRect(0, height - borderH, width, borderH);
        }

        gc2d.setFill(Color.BLACK);
        gc2d.setTextAlign(TextAlignment.CENTER);

        if (text != null) {
            if (text.equals("0")) {
                Color dotColor = Color.RED;
                gc2d.setFill(dotColor);

                double dotRatio = 0.12;
                double dotH = height * dotRatio;
                double dotW = height * Math.asin(dotRatio);
                double dotY = height / 2.0 - dotH / 2.0;
                double dotGap = width / 4.0;
                for (int i = 0; i < 4; i++) {
                    gc2d.fillOval(i * dotGap, dotY, dotW, dotH);  // 中间一圈的点
                }
                // 极点的点，为了简单这都是近似值
                double faceRadiusRatio = dotRatio / (Math.PI * 2);
                double centerAngle = Math.atan(faceRadiusRatio);
                double polarDotH = dotH * faceRadiusRatio;
//                System.out.println(Math.toDegrees(centerAngle));
//                gc2d.fillRect(0, 0, width, polarDotH);
//                gc2d.fillRect(0, height - polarDotH, width, height);
                // canvas有抗锯齿，这里直接搞
                PixelWriter pixelWriter = gc2d.getPixelWriter();
                int start = (int) Math.round(polarDotH);
                int botStart = (height - start);
                for (int r = 0; r < start; r++) {
                    for (int c = 0; c < width; c++) {
                        pixelWriter.setColor(c, r, dotColor);
                    }
                }
                for (int r = botStart; r < height; r++) {
                    for (int c = 0; c < width; c++) {
                        pixelWriter.setColor(c, r, dotColor);
                    }
                }
            } else {
                gc2d.setFont(font);
                double textDown = font.getSize() * 0.36;
                gc2d.fillText(
                        text,
                        centerCircleW / 2,
                        height / 2.0 + textDown);
                gc2d.fillText(
                        text,
                        width / 2 + centerCircleW / 2,
                        height / 2.0 + textDown);

                if ("6".equals(text) || "9".equals(text)) {
                    gc2d.setLineWidth(6.0);
                    gc2d.setStroke(Color.BLACK);
                    gc2d.strokeOval(centerCircleW / 2, height * 0.72, 1, 1);
                    gc2d.strokeOval(width / 2 + centerCircleW / 2, height * 0.72, 1, 1);
                }
            }
        }

        return canvas;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        for (int i = 0; i <= 15; i++) {
            makePoolBall(i);
        }
        for (int i = 0; i <= 7; i++) {
            makeSnookerBall(i);
        }

        primaryStage.show();
    }
}
