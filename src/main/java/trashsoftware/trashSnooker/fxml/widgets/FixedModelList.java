package trashsoftware.trashSnooker.fxml.widgets;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import trashsoftware.trashSnooker.fxml.App;

import java.io.IOException;
import java.util.ResourceBundle;

public abstract class FixedModelList extends HBox {

    @FXML
    protected ScrollBar scrollBar;
    @FXML
    protected GridPane container;

    protected final ResourceBundle strings;

    protected int viewSlots = 5;

    public FixedModelList() {
        this(App.getStrings());
    }

    public FixedModelList(ResourceBundle strings) {
        this.strings = strings;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "fixedModelList.fxml"), strings);
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
            int gaps = getNModels() - viewSlots;
            if (gaps <= 0) return;
            double tick = 1.0 / gaps;
            double delta = event.getDeltaY() < 0 ? tick : -tick;
            double value = scrollBar.getValue() + delta;
            value = Math.max(0.0, Math.min(1.0, value));
            scrollBar.setValue(value);
        });
        setScrollBar();
    }

    protected void setScrollBar() {
        double visAmount = (double) viewSlots / getNModels();
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
    
    protected int getViewPos() {
        int gaps = getNModels() - viewSlots;
        int viewPos;
        if (gaps <= 0) {
            viewPos = 0;
        } else {
            viewPos = (int) Math.round(scrollBar.getValue() * gaps);
        }
        return viewPos;
    }

    public void setViewSlots(int viewSlots) {
        this.viewSlots = viewSlots;
    }

    public abstract int getNModels();

    protected abstract void update();

    public void clear() {
        container.getChildren().clear();
    }
}
