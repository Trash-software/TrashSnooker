package trashsoftware.trashSnooker.fxml.widgets;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import trashsoftware.trashSnooker.core.CueSelection;
import trashsoftware.trashSnooker.core.career.HumanCareer;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueTip;
import trashsoftware.trashSnooker.core.cue.CueTipBrand;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.BuyTipView;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FixedCueList extends FixedModelList {

    private final List<Bundle> views = new ArrayList<>();
    private Comparator<CueSelection.CueAndBrand> displayComparator;

    public FixedCueList() {
        super();
    }
    
    @Override
    protected void update() {
        int viewPos = getViewPos();

        container.getChildren().clear();
        for (int i = viewPos; i < Math.min(viewPos + viewSlots, views.size()); i++) {
            int row = i - viewPos;
            Bundle bundle = views.get(i);
            container.add(bundle.cueViewer, 0, row * 2, 2, 2);
            if (bundle.extra != null && !bundle.extra.getChildrenUnmodifiable().isEmpty()) {
                bundle.extra.setTranslateY(18.0);
                container.add(bundle.extra, 1, row * 2);
            }
        }
    }

    public void clear() {
        container.getChildren().clear();
        views.clear();
    }

    @Override
    public int getNModels() {
        return views == null ? 0 : views.size();
    }

    public void addCue(CueSelection.CueAndBrand cueAndBrand,
                       double prefWidth,
                       HumanCareer humanCareer,
                       Runnable changeTipCallback,
                       String buttonText,
                       Runnable buttonCallback,
                       boolean refresh) {
        try {
            CueViewer viewer = new CueViewer(strings, cueAndBrand, prefWidth);
            Bundle bundle = new Bundle(viewer);

            if (humanCareer != null) {
                addTipChangeNodes(bundle, cueAndBrand, humanCareer, changeTipCallback);
            }
            if (buttonText != null) {
                Button actionButton = new Button(buttonText);
                if (buttonCallback == null) {
                    actionButton.setDisable(true);
                } else {
                    actionButton.setOnAction(event -> buttonCallback.run());
                }
                bundle.extra.getChildren().add(actionButton);
            }

            views.add(bundle);
        } catch (Exception e) {
            EventLogger.error(e);
        }
        if (refresh) {
            display();
        }
    }

    public void addCue(CueSelection.CueAndBrand cueAndBrand,
                       double prefWidth,
                       String buttonText,
                       Runnable buttonCallback,
                       boolean refresh) {
        addCue(cueAndBrand, prefWidth, null, null, buttonText, buttonCallback, refresh);
    }

    public void setDisplayComparator(Comparator<CueSelection.CueAndBrand> displayComparator) {
        this.displayComparator = displayComparator;
    }
    
    public void display() {
        if (displayComparator != null) {
            views.sort((a, b) -> displayComparator.compare(a.cueViewer.getCueAndBrand(), b.cueViewer.getCueAndBrand()));
        }
        setScrollBar();
        update();
    }

    private void updateCueModels() {
        for (Bundle bundle : views) {
            bundle.cueViewer.updateModel();
        }
    }

    private void addTipChangeNodes(Bundle bundle,
                                   CueSelection.CueAndBrand cueAndBrand,
                                   HumanCareer humanCareer,
                                   Runnable changeTipCallback) {
        CueTip currentTip = cueAndBrand.getCueInstance().getCueTip();  // assert not-null
        double percent = currentTip.getHpPercentage();
        String ss = String.format("%.0f/%.0f", currentTip.getHp(), currentTip.getTotalDurability());
//        String text = String.format(strings.getString("tipHpFmt"), ss);

        Canvas canvas = new Canvas();
        canvas.setWidth(80.0);
        canvas.setHeight(20.0);

        GraphicsContext gc = getGraphicsContext(percent, canvas);
        gc.fillText(ss, canvas.getWidth() / 2, canvas.getHeight() / 2 + App.FONT.getSize() * 0.33);

        Button changeButton = new Button(strings.getString("changeTip"));
        changeButton.setOnAction(e -> {
            showTipChangeView(cueAndBrand.getCueInstance(), humanCareer, changeTipCallback);
        });

        String tipName = currentTip.getBrand().shownName();

        HBox box = new HBox();
        box.setSpacing(5.0);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(new Label(String.format(strings.getString("tipNameFmt"), tipName)));
        box.getChildren().add(new Label(strings.getString("tipHp")));
        box.getChildren().add(canvas);
        box.getChildren().add(changeButton);

        bundle.extra.getChildren().add(box);
//        cueViewer.addExtra(box);
    }
    
    private void showTipChangeView(Cue cue, 
                                   HumanCareer humanCareer,
                                   Runnable changeTipCallback) {
        Stage stage = new Stage();
        stage.initOwner(getScene().getWindow());
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setResizable(false);
        
        stage.setTitle(strings.getString("changeTip"));

        FXMLLoader fxmlLoader = new FXMLLoader(BuyTipView.class.getResource(
                "buyTipView.fxml"), strings);
        try {
            Parent root = fxmlLoader.load();

            Scene scene = new Scene(root);
            stage.setScene(scene);

            BuyTipView controller = fxmlLoader.getController();
            controller.setup(stage, humanCareer, cue, changeTipCallback);

            stage.show();
            
            controller.fillTipBrands();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @NotNull
    private static GraphicsContext getGraphicsContext(double percent, Canvas canvas) {
        Color fill;
        if (percent < CueTip.TIP_HEALTH_LOW) fill = Color.DIMGRAY;
        else if (percent < 0.25) fill = Color.DARKRED;
        else if (percent < 0.5) fill = Color.GOLDENROD;
        else fill = Color.LIMEGREEN;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(fill);
        gc.setStroke(Color.BLACK);

        gc.fillRect(0, 0, Math.max(0, canvas.getWidth() * percent), canvas.getHeight());

        gc.setLineWidth(2.0);
        gc.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.BLACK);
        return gc;
    }

    private static class Bundle {
        final CueViewer cueViewer;
        final HBox extra = new HBox();

        Bundle(CueViewer cueViewer) {
            this.cueViewer = cueViewer;
            extra.setAlignment(Pos.CENTER_RIGHT);
            extra.setSpacing(5.0);
        }
    }
}
