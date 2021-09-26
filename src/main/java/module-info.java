module TrashSnooker {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires ConfigLoader;
    requires org.json;
    requires org.jetbrains.annotations;

    exports trashsoftware.trashSnooker;
    exports trashsoftware.trashSnooker.fxml;
    exports trashsoftware.trashSnooker.core;

    opens trashsoftware.trashSnooker.fxml;
    exports trashsoftware.trashSnooker.core.snooker;
    exports trashsoftware.trashSnooker.core.numberedGames;
    exports trashsoftware.trashSnooker.core.numberedGames.chineseEightBall;
    exports trashsoftware.trashSnooker.core.numberedGames.sidePocket;
    exports trashsoftware.trashSnooker.util;
}