package trashsoftware.trashSnooker.fxml.widgets;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.util.Callback;

public class LabelTableColumn<S, T> {
    private final LabelTable<S> table;
    private ObjectProperty<Callback<S, ObservableValue<T>>> cellValueFactory;
    private Node title;

    public LabelTableColumn(LabelTable<S> table, Node title) {
        this(table, title, null);
    }

    public LabelTableColumn(LabelTable<S> table, Callback<S, ObservableValue<T>> cellValueFactory) {
        this(table, new Label(""), cellValueFactory);
    }

    public LabelTableColumn(LabelTable<S> table, String titleText, Callback<S, ObservableValue<T>> cellValueFactory) {
        this(table, new Label(titleText), cellValueFactory);
    }

    public LabelTableColumn(LabelTable<S> table, Node title, Callback<S, ObservableValue<T>> cellValueFactory) {
        this.table = table;
        this.title = title;

        setCellValueFactory(cellValueFactory);
    }

    public Node getTitleNode() {
        return title;
    }

    public void setTitleText(String titleText) {
        this.title = new Label(titleText);
        
        if (table != null) {
            table.refresh();
        }
    }

    public final ObjectProperty<Callback<S, ObservableValue<T>>> cellValueFactoryProperty() {
        if (cellValueFactory == null) {
            cellValueFactory = new SimpleObjectProperty<>(this, "cellValueFactory");
        }
        return cellValueFactory;
    }

    public final void setCellValueFactory(Callback<S, ObservableValue<T>> value) {
        cellValueFactoryProperty().set(value);
        if (table != null) table.refresh();
    }
}
