package trashsoftware.trashSnooker.fxml.widgets;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class LabelTable<S> extends ScrollPane {

    private final List<S> items = new ArrayList<>();
    private final List<LabelTableColumn<S, ?>> columns = new ArrayList<>();
    private boolean showTitle = true;
    @FXML
    GridPane content;

    public LabelTable() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "labelTable.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
    
    public void contentExpand(boolean expand) {
        setFitToHeight(expand);
    }

    public void addItem(S item) {
        items.add(item);
        addItemToView(item);
    }
    
    public void addItems(S... items) {
        for (S s : items) addItem(s);
    }
    
    public void addItems(Collection<S> items) {
        for (S s : items) addItem(s);
    }

    public List<LabelTableColumn<S, ?>> getColumns() {
        return columns;
    }

    private void addItemToView(S item) {
        for (int i = 0; i < columns.size(); i++) {
            LabelTableColumn<S, ?> col = columns.get(i);

            Object result = col.cellValueFactoryProperty().get().call(item).getValue();
            Label label = new Label(result.toString());
            label.setWrapText(true);
            content.add(label, i, items.size());  // 第一行是title
        }
    }

    public S getItem(int index) {
        return items.get(index);
    }

    public void clearItems() {
        items.clear();
        refresh();
    }

    public void addColumn(LabelTableColumn<S, ?> column) {
        columns.add(column);
        refresh();
    }

    @SafeVarargs
    public final void addColumns(LabelTableColumn<S, ?>... columns) {
        this.columns.addAll(Arrays.asList(columns));
        refresh();
    }

    public void showTitle(boolean showTitle) {
        boolean change = showTitle != this.showTitle;
        this.showTitle = showTitle;
        if (change) refresh();
    }

    void refresh() {
        content.getChildren().clear();  // todo

        if (showTitle) {
            for (int i = 0; i < columns.size(); i++) {
                content.add(columns.get(i).getTitleLabel(), i, 0);
            }
        }

        for (S item : items) {
            addItemToView(item);
        }
    }
}
