package trashsoftware.trashSnooker.core.metrics;

import org.json.JSONObject;
import trashsoftware.trashSnooker.util.DataLoader;

import java.io.File;

public class TablePreset {
    
    public final String id;
    public final String name;
    private final TableMetrics.TableBuilderFactory factory;
    private final File meshes;
    private final File logo;
    private final String nameOnTable;
    public final TableSpec tableSpec;
    
    public TablePreset(String id, 
                       String name,
                       TableMetrics.TableBuilderFactory factory,
                       TableSpec tableSpec,
                       String nameOnTable,
                       File meshes, 
                       File logo) {
        this.id = id;
        this.name = name;
        this.factory = factory;
        this.tableSpec = tableSpec;
        this.nameOnTable = nameOnTable;
        this.meshes = meshes;
        this.logo = logo;
    }
    
    public static TablePreset fromJson(JSONObject jsonObject) {
        TableMetrics.TableBuilderFactory factory = TableMetrics.fromFactoryName(jsonObject.getString("type"));
        
        File mesh = jsonObject.has("meshes") ? new File(jsonObject.getString("meshes")) : null;
        File logo = jsonObject.has("logo") ? new File(jsonObject.getString("logo")) : null;
        if (mesh != null && !mesh.exists()) mesh = null;
        if (logo != null && !logo.exists()) logo = null;
        
        return new TablePreset(
                jsonObject.getString("id"),
                DataLoader.getStringOfLocale(jsonObject.getJSONObject("name")),
                factory,
                TableSpec.fromJsonObject(jsonObject, factory),
                jsonObject.has("text") ? jsonObject.getString("text") : "",
                mesh,
                logo
        );
    }

    public TableSpec getTableSpec() {
        return tableSpec;
    }

    public File getLogo() {
        return logo;
    }

    public String getNameOnTable() {
        return nameOnTable;
    }

    public File getMeshes() {
        return meshes;
    }

    @Override
    public String toString() {
        return "TablePreset{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", nameOnTable='" + nameOnTable + '\'' +
                ", tableSpec=" + tableSpec +
                '}';
    }
}
