package trashsoftware.trashSnooker.fxml.widgets;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.util.Callback;

public class LabelTableColumn<S, T> {
    private final LabelTable<S> table;
    private ObjectProperty<Callback<S, ObservableValue<T>>> cellValueFactory;
    private String title;
    private final Label titleLabel = new Label();

    public LabelTableColumn(LabelTable<S> table, String title) {
        this(table, title, null);
    }

    public LabelTableColumn(LabelTable<S> table, String title, Callback<S, ObservableValue<T>> cellValueFactory) {
        this.table = table;
        this.title = title;
        titleLabel.setText(title);

        setCellValueFactory(cellValueFactory);
    }

    public String getTitle() {
        return title;
    }

    public Label getTitleLabel() {
        return titleLabel;
    }

    public void setTitle(String title) {
        this.title = title;
        
        titleLabel.setText(title);
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

    public static abstract class CellDataFeatures<S, T> {

    }
}
