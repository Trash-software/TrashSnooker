package trashsoftware.trashSnooker.util;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.HashMap;
import java.util.Map;

public class OpacityColor {

    private static OpacityColor instance;

    private final Map<Color, Map<Double, Color>> colorOpaMap = new HashMap<>();

    private OpacityColor() {
    }

    public static OpacityColor getInstance() {
        if (instance == null) {
            instance = new OpacityColor();
        }
        return instance;
    }

    public Color getByOpacity(Color base, double opacity) {
        if (opacity == 1.0) return base;
        Map<Double, Color> opaOfCol = colorOpaMap.computeIfAbsent(base,
                b -> new HashMap<>());
        return opaOfCol.computeIfAbsent(opacity,
                p -> base.deriveColor(0, 1, 1, opacity));
    }
}
