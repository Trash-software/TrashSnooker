package trashsoftware.trashSnooker.fxml.widgets;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class AdversarialBar extends Pane {
    
    private Color leftColor = Color.TOMATO;
    private Color leftColorDark = leftColor;
    private Color rightColor = Color.CORNFLOWERBLUE;
    private Color rightColorDark = rightColor;
    Canvas canvas = new Canvas();
    
    public AdversarialBar() {
        canvas.setHeight(12.0);
        canvas.setWidth(150.0);
        getChildren().add(canvas);
    }
    
    public AdversarialBar(double percentageSplit) {
        this();
        setTwo(percentageSplit, 1 - percentageSplit);
    }
    
    public AdversarialBar(double v1, double rateInV1, double v2, double rateInV2) {
        this();
        setLeftColor(Color.TOMATO);
        setRightColor(Color.CORNFLOWERBLUE);
        setFour(v1, rateInV1, v2, rateInV2);
    }
    
    public void setFour(double v1, double rateInV1, double v2, double rateInV2) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        
        if (Double.isNaN(rateInV1)) rateInV1 = 0;
        if (Double.isNaN(rateInV2)) rateInV2 = 0;

        double total = v1 + v2;
        double lw = w * (v1 / total);
        double rw = w - lw;
        
        double w1 = lw * rateInV1;
        double w2 = lw - w1;
        double w3 = rw * (1 - rateInV2);
        double w4 = rw - w3;

        GraphicsContext context = canvas.getGraphicsContext2D();
        context.setFill(leftColor);
        context.fillRect(0, 0, w1, h);
        context.setFill(leftColorDark);
        context.fillRect(w1, 0, w2, h);
        context.setFill(rightColorDark);
        context.fillRect(w1 + w2, 0, w3, h);
        context.setFill(rightColor);
        context.fillRect(w1 + w2 + w3, 0, w4, h);
    }
    
    public void setTwo(double v1, double v2) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        
        double total = v1 + v2;
        double lw = w * (v1 / total);
        double rw = w - lw;
        GraphicsContext context = canvas.getGraphicsContext2D();
        context.setFill(leftColor);
        context.fillRect(0, 0, lw, h);
        context.setFill(rightColor);
        context.fillRect(lw, 0, rw, h);
    }

    public void setLeftColor(Color leftColor) {
        this.leftColor = leftColor;

        leftColorDark =
                leftColor.deriveColor(0, 1.0, 0.7, 1.0);
    }

    public void setRightColor(Color rightColor) {
        this.rightColor = rightColor;

        rightColorDark =
                rightColor.deriveColor(0, 1.0, 0.7, 1.0);
    }
}
