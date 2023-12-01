package trashsoftware.trashSnooker.fxml.widgets;

import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import trashsoftware.trashSnooker.core.cue.CueBrand;
import trashsoftware.trashSnooker.core.cue.CueTip;
import trashsoftware.trashSnooker.core.cue.CueTipBrand;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.drawing.CueTipModel;
import trashsoftware.trashSnooker.util.Util;

import java.util.ArrayList;
import java.util.List;

public class FixedTipList extends FixedModelList {

    private final List<Bundle> views = new ArrayList<>();
    
    public FixedTipList() {
        super();
    }

    @Override
    public void clear() {
        container.getChildren().clear();
        views.clear();
    }

    @Override
    public int getNModels() {
        return views == null ? 0 : views.size();
    }

    @Override
    protected void update() {
        int viewPos = getViewPos();

        container.getChildren().clear();
        for (int i = viewPos; i < Math.min(viewPos + viewSlots, views.size()); i++) {
            int row = i - viewPos;
            Bundle bundle = views.get(i);
            container.add(bundle.modelPane, 0, row);
            container.add(bundle.view, 1, row);
        }
    }
    
    public void addTip(CueTipBrand tipBrand, CueBrand cueBrand, Runnable buttonCallback) {
        HBox infoBox = new HBox();
        infoBox.setSpacing(5.0);
        infoBox.setAlignment(Pos.CENTER_RIGHT);
        Label nameText = new Label(tipBrand.shownName());
        nameText.setFont(new Font(App.FONT.getName(), 16.0));
        
        double hp = CueTip.calculateTotalHp(tipBrand.totalHp(), 
                tipBrand.maxRadius() * 2,
                cueBrand.getCueTipWidth());
        Label hpText = new Label(String.format(strings.getString("tipHpFmt"), 
                String.format("%1$5s", (int) hp)));

        VBox vBox = new VBox();
        vBox.setSpacing(5.0);

        vBox.getChildren().addAll(
                createDataBox(strings.getString("tipGrip"), tipBrand.origGrip(), 1.2, Color.CORNFLOWERBLUE),
                createDataBox(strings.getString("tipPower"), tipBrand.origPower(), 1.2, Color.GOLD)
        );

        Button buyButton = new Button(strings.getString("buy") + " -" + 
                Util.moneyToReadable(tipBrand.price()));
        buyButton.setPrefWidth(80.0);
        buyButton.setOnAction(e -> buttonCallback.run());
        
        infoBox.getChildren().addAll(nameText, vBox, hpText, buyButton);
        
        CueTip previewInstance = CueTip.createPreviewInstance(tipBrand);
        CueTipModel tipModel = (CueTipModel) CueTipModel.create(previewInstance);
        tipModel.setScale(3.0);
        
        Rotate rotateX = new Rotate(60, 0, 0, 0, new Point3D(1, 0, 0));
        Rotate rotateZ = new Rotate(-45, 0, 0, 0, new Point3D(0, 0, 1));
        tipModel.getTransforms().addAll(rotateX, rotateZ);
        tipModel.setTranslateX(30.0);
        tipModel.setTranslateY(30.0);
        
        Pane modelPane = new Pane();
        modelPane.setPrefHeight(60.0);
        modelPane.getChildren().add(tipModel);
        
        Bundle bundle = new Bundle(modelPane, infoBox);
        views.add(bundle);

        setScrollBar();
        update();
    }
    
    private HBox createDataBox(String text, double number, double max, Color fill) {
        Label label = new Label(text);
        Canvas gripCanvas = createBar(number, max, fill);
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setSpacing(5.0);
        box.getChildren().addAll(label, gripCanvas);
        
        return box;
    }
    
    private Canvas createBar(double number, double max, Color fill) {
        Canvas canvas = new Canvas();
        canvas.setHeight(16.0);
        canvas.setWidth(80.0);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(fill);
        gc.setStroke(Color.BLACK);
        
        double percent = Math.max(0, Math.min(1, number / max));

        gc.fillRect(0, 0, canvas.getWidth() * percent, canvas.getHeight());

        gc.setLineWidth(2.0);
        gc.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.BLACK);
        gc.fillText(String.valueOf(number), 
                canvas.getWidth() / 2, canvas.getHeight() / 2 + App.FONT.getSize() * 0.33);
        
        return canvas;
    }
    
    public static class Bundle {
        final Pane modelPane;
        final HBox view;
        
        Bundle(Pane modelPane, HBox view) {
            this.modelPane = modelPane;
            this.view = view;
        }
    }
}
