package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.shape.MeshView;

public abstract class CustomSphere extends MeshView {
    
    public abstract void setRadius(double radius);
    
    public abstract CustomSphere copyNoTrans();
}
