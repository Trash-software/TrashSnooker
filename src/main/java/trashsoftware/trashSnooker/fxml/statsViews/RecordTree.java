package trashsoftware.trashSnooker.fxml.statsViews;

import javafx.scene.layout.Pane;

import java.util.ResourceBundle;

public class RecordTree {
    protected final ResourceBundle strings;
    protected final String shown;

    RecordTree(String shown, ResourceBundle strings) {
        this.shown = shown;
        this.strings = strings;
    }

    void setRightPane(Pane rightPane) {
        rightPane.getChildren().clear();
    }

    @Override
    public String toString() {
        return shown;
    }

    public static String showPercent(double num, double denom, int digits) {
        if (denom == 0) return "--";
        String fmtStr = String.format("%%.%df", digits);
        return String.format(fmtStr + "%%", num / denom * 100.0);
    }
    
    public static String showOneOver10000(double num, double denom, int digits) {
        if (denom == 0) return "--";
        String fmtStr = String.format("%%.%df", digits);
        return String.format(fmtStr + "‱", num / denom * 10000.0);
    }
    
    public static String showPercent(double num, double denom) {
        if (denom == 0) return "--";
        return String.format("%.1f%%", num / denom * 100.0);
    }

    public static String showPercent(double fraction) {
        if (Double.isNaN(fraction)) return "--";
        return String.format("%.1f%%", fraction * 100.0);
    }
    
    public static String showNumber(int number) {
        return String.format("%,d", number);
    }
}
