package trashsoftware.trashSnooker.core.metrics;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.phy.TableCloth;

import java.util.Locale;

public class TableSpec {
    public final TableCloth tableCloth;
    public final TableMetrics tableMetrics;

    public TableSpec(TableCloth tableCloth, TableMetrics tableMetrics) {
        this.tableCloth = tableCloth;
        this.tableMetrics = tableMetrics;
    }
    
    public static TableSpec fromJsonObject(JSONObject table, 
                                           TableMetrics.TableBuilderFactory factory) {
        TableCloth cloth;
        TableMetrics metrics;
        
        cloth = new TableCloth(
                TableCloth.Goodness.valueOf(table.getString("goodness").toUpperCase(Locale.ROOT)),
                TableCloth.Smoothness.valueOf(table.getString("smoothness").toUpperCase(Locale.ROOT))
        );
        metrics = factory
                .create()
                .pocketDifficulty(PocketDifficulty.valueOf(factory, table.getString("pocketDifficulty")))
                .holeSize(PocketSize.valueOf(factory, table.getString("pocketSize")))
                .build();

        return new TableSpec(cloth, metrics);
    }

    @Override
    public String toString() {
        return "TableSpec{" +
                "tableCloth=" + tableCloth +
                ", tableMetrics=" + tableMetrics +
                '}';
    }
}
