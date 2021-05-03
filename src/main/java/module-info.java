module TrashSnooker {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    exports trashsoftware.trashSnooker;
    exports trashsoftware.trashSnooker.fxml;
    exports trashsoftware.trashSnooker.core;

    opens trashsoftware.trashSnooker.fxml;
}