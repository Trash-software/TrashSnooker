package trashsoftware.trashSnooker.core.metrics;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.json.JSONObject;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.io.File;
import java.net.URL;
import java.util.Objects;

public class TablePreset {
    
    public final String id;
    public final String name;
    private final TableMetrics.TableBuilderFactory factory;
    private String clothMeshName;
    private Image clothMesh;
    private final File logo;
    private final String nameOnTable;
    private final Color nameOnTableColor;
    public final TableSpec tableSpec;
    public Color tableBorderColor, clothColor;
    
    public TablePreset(String id, 
                       String name,
                       TableMetrics.TableBuilderFactory factory,
                       TableSpec tableSpec,
                       String nameOnTable,
                       Color nameOnTableColor,
                       String clothMeshName, 
                       File logo,
                       Color tableBorderColor,
                       Color clothColor) {
        this.id = id;
        this.name = name;
        this.factory = factory;
        this.tableSpec = tableSpec;
        this.nameOnTable = nameOnTable;
        this.nameOnTableColor = nameOnTableColor;
        this.logo = logo;
        
        this.clothMeshName = clothMeshName;
        this.tableBorderColor = tableBorderColor;
        this.clothColor = clothColor;
    }
    
    private void loadMeshes() {
        if (clothMeshName == null) return;

        String fileName = "/trashsoftware/trashSnooker/res/img/"
                + ConfigLoader.getInstance().getBallMaterialResolution() + "/table/" + clothMeshName;
        URL url = getClass().getResource(fileName);
        if (url == null) {
            EventLogger.error("Cannot load resource: " + fileName);
            return;
        }
        clothMesh = new Image(url.toExternalForm());
    }
    
    public static TablePreset fromJson(JSONObject jsonObject) {
        TableMetrics.TableBuilderFactory factory = TableMetrics.fromFactoryName(jsonObject.getString("type"));
        
        String mesh = jsonObject.has("clothMesh") ? jsonObject.getString("clothMesh") : null;
        File logo = jsonObject.has("logo") ? new File(jsonObject.getString("logo")) : null;
        if (logo != null && !logo.exists()) logo = null;
        
        return new TablePreset(
                jsonObject.getString("id"),
                DataLoader.getObjectOfLocale(jsonObject.getJSONObject("name")),
                factory,
                TableSpec.fromJsonObject(jsonObject, factory),
                jsonObject.has("text") ? jsonObject.getString("text") : "",
                jsonObject.has("textColor") ? 
                        DataLoader.parseColor(jsonObject.getString("textColor")) : 
                        Color.BLACK.brighter(),
                mesh,
                logo,
                jsonObject.has("tableOutColor") ?
                        DataLoader.parseColor(jsonObject.getString("tableOutColor")) :
                        null,
                jsonObject.has("clothColor") ?
                        DataLoader.parseColor(jsonObject.getString("clothColor")) :
                        null
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

    public Color getNameOnTableColor() {
        return nameOnTableColor;
    }

    public Image getClothMesh() {
        if (clothMesh == null) loadMeshes();
        
        return clothMesh;
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
