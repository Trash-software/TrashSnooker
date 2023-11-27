package trashsoftware.trashSnooker.fxml.widgets;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import trashsoftware.trashSnooker.fxml.App;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class RadarChart extends VBox {

    public static final Color BACKGROUND = Color.WHITESMOKE;
    public static final Color TEXTS = Color.BLACK;
    public static final Color TICKS = Color.GRAY;
    public static final Color[] LINES = {
            Color.CORNFLOWERBLUE, 
            Color.DARKRED
    };
    public static final Color[] AREAS = {
            Color.CYAN.deriveColor(0, 1, 1, 0.3),
            Color.DARKRED.deriveColor(0, 1, 1, 0.3)
    };
    
    @FXML
    Canvas canvas;
    
    protected final ResourceBundle strings;
    
    private String[] titles;  // 从正上开始，顺时针
    private int nTicks = 5;  // 分几层
    private double[][] values;
    
    private double eachDeg;
    
    public RadarChart() {
        this(App.getStrings());
    }
    
    public RadarChart(ResourceBundle strings) {
        super();
        
        this.strings = strings;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "radarChart.fxml"), strings);
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * @param titles 文字标签
     * @param values 每个标签对应的值，[0-1]
     */
    public void setValues(String[] titles, double[]... values) {
        if (values.length != 0 && titles.length != values[0].length) {
            throw new IndexOutOfBoundsException("Number of titles not match with values");
        }
        
        this.titles = titles;
        this.values = values;
        
        this.eachDeg = 360.0 / titles.length;
        
        draw();
    }
    
    private void draw() {
        double width = getPrefWidth();
        double height = getPrefHeight();
        
        double size = Math.min(width, height);
        
        width = size * 1.2;
        height = size;
        
        canvas.setWidth(width);
        canvas.setHeight(height);
        
        double centerX = width * 0.5;
        double centerY = height * 0.5;
        
        double totalR = size * 0.35;
        double eachTickR = totalR / nTicks;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(BACKGROUND);
        gc.fillRect(0, 0, width, height);
        
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setStroke(TICKS);
        gc.setFill(TEXTS);

        List<List<double[]>> valueCoords = new ArrayList<>();
        for (var ignored : values) valueCoords.add(new ArrayList<>());
        
        for (int i = 0; i < titles.length; i++) {
            double deg0 = i * eachDeg;  // 从正上方按顺时针的角度
            double deg1 = deg0 - 90;
            double rad = Math.toRadians(deg1);  // y是反过来的，所以顺时针应该正好就对了
            
            double nextDeg0 = (i + 1) * eachDeg;
            double nextDeg1 = nextDeg0 - 90;
            double nextRad = Math.toRadians(nextDeg1);
            
            for (int j = 0; j < nTicks; j++) {
                double r = (j + 1) * eachTickR;
                double x0 = r * Math.cos(rad) + centerX;
                double y0 = r * Math.sin(rad) + centerY;
                double x1 = r * Math.cos(nextRad) + centerX;
                double y1 = r * Math.sin(nextRad) + centerY;
                
                gc.strokeLine(x0, y0, x1, y1);
            }

            double xEnd = totalR * Math.cos(rad) + centerX;
            double yEnd = totalR * Math.sin(rad) + centerY;
            gc.strokeLine(centerX, centerY, xEnd, yEnd);
            
            double textR = size * 0.425;
            double textX = textR * Math.cos(rad) + centerX;
            double textY = textR * Math.sin(rad) + centerY;
            gc.fillText(titles[i], textX, textY);
            
            // value
            for (int v = 0; v < values.length; v++) {
                double valueR = values[v][i] * totalR;
                double valueX = valueR * Math.cos(rad) + centerX;
                double valueY = valueR * Math.sin(rad) + centerY;
                valueCoords.get(v).add(new double[]{valueX, valueY});
            }
        }

        for (int v = 0; v < values.length; v++) {
            Color line = LINES[v % LINES.length];
            Color area = AREAS[v % LINES.length];
            gc.setStroke(line);
            gc.setFill(area);
            
            List<double[]> coordsV = valueCoords.get(v);
            int nPoints = coordsV.size();
            double[] xPoints = new double[nPoints];
            double[] yPoints = new double[nPoints];
            for (int i = 0; i < nPoints; i++) {
                double[] coord = coordsV.get(i);
                xPoints[i] = coord[0];
                yPoints[i] = coord[1];
            }

            gc.strokePolygon(xPoints, yPoints, nPoints);
            gc.fillPolygon(xPoints, yPoints, nPoints);
        }
    }

    public String[] getTitles() {
        return titles;
    }

    public double[][] getValues() {
        return values;
    }

    public void setNTicks(int nTicks) {
        this.nTicks = nTicks;
    }

    public int getNTicks() {
        return nTicks;
    }
}
