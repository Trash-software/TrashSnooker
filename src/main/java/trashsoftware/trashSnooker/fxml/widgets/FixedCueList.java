package trashsoftware.trashSnooker.fxml.widgets;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import org.jetbrains.annotations.NotNull;
import trashsoftware.trashSnooker.core.CueSelection;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.HumanCareer;
import trashsoftware.trashSnooker.core.cue.CueTip;
import trashsoftware.trashSnooker.core.cue.CueTipBrand;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.alert.AlertShower;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class FixedCueList extends HBox {

    @FXML
    ScrollBar scrollBar;
    @FXML
    GridPane container;

    private int viewSlots = 5;

    private final List<Bundle> views = new ArrayList<>();

    private final ResourceBundle strings;

    public FixedCueList() {
        this(App.getStrings());
    }

    public FixedCueList(ResourceBundle strings) {
        this.strings = strings;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "fixedCueList.fxml"), strings);
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        scrollBar.valueProperty().addListener(((observable, oldValue, newValue) -> update()));
        setOnScroll(event -> {
            if (event.getDeltaY() == 0) return;
            int gaps = getNCues() - viewSlots;
            if (gaps <= 0) return;
            double tick = 1.0 / gaps;
            double delta = event.getDeltaY() < 0 ? tick : -tick;
            double value = scrollBar.getValue() + delta;
            value = Math.max(0.0, Math.min(1.0, value));
            scrollBar.setValue(value);
        });
        setScrollBar();
    }

    private void setScrollBar() {
        double visAmount = (double) viewSlots / getNCues();
        if (visAmount >= 1) {
            scrollBar.setVisible(false);
            scrollBar.setManaged(false);
            return;
        } else {
            scrollBar.setVisible(true);
            scrollBar.setManaged(true);
        }

        scrollBar.setMax(1.0);
        scrollBar.setMin(0.0);
        scrollBar.setVisibleAmount(visAmount);

        scrollBar.setValue(0);
    }

    private void update() {
        int gaps = views.size() - viewSlots;
        int viewPos;
        if (gaps <= 0) {
            viewPos = 0;
        } else {
            viewPos = (int) Math.round(scrollBar.getValue() * gaps);
        }

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

    public int getNCues() {
        return views.size();
    }

    public void setViewSlots(int viewSlots) {
        this.viewSlots = viewSlots;
    }

    public void addCue(CueSelection.CueAndBrand cueAndBrand,
                       double prefWidth,
                       HumanCareer humanCareer,
                       Runnable changeTipCallback,
                       String buttonText,
                       Runnable buttonCallback) {
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
        setScrollBar();
        update();
    }

    public void addCue(CueSelection.CueAndBrand cueAndBrand,
                       double prefWidth,
                       String buttonText,
                       Runnable buttonCallback) {
        addCue(cueAndBrand, prefWidth, null, null, buttonText, buttonCallback);
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
        gc.fillText(ss, canvas.getWidth() / 2, canvas.getHeight() / 2 + App.FONT.getSize() * 0.25);

        Button changeButton = new Button(strings.getString("changeTip"));
        changeButton.setOnAction(e -> {
            // todo: 临时
            CueTipBrand tipBrand = CueTipBrand.getById("stdTip");
            AlertShower.askConfirmation(
                    getScene().getWindow(),
                    String.format(strings.getString("changeTipPrice"),
                            Util.moneyToReadable(humanCareer.getMoney() - tipBrand.price())),
                    String.format(strings.getString("changeTipConfirm"),
                            Util.moneyToReadable(tipBrand.price())),
                    () -> {
                        CueTip newTip = CueTip.createByCue(cueAndBrand.brand,
                                tipBrand,
                                CareerManager.getInstance().getCareerSave());
                        humanCareer.buyCueTip(newTip.getInstanceId(), tipBrand.price());
                        humanCareer.getInventory().installTip(
                                newTip,
                                cueAndBrand.getCueInstance());
                        Platform.runLater(changeTipCallback);
                    },
                    null
            );
        });

        HBox box = new HBox();
        box.setSpacing(5.0);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(new Label(strings.getString("tipHp")));
        box.getChildren().add(canvas);
        box.getChildren().add(changeButton);

        bundle.extra.getChildren().add(box);
//        cueViewer.addExtra(box);
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

        gc.fillRect(0, 0, Math.max(0,canvas.getWidth() * percent), canvas.getHeight());

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
