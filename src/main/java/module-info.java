module TrashSnooker {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires org.json;
    requires org.jetbrains.annotations;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    exports trashsoftware.trashSnooker;
    exports trashsoftware.trashSnooker.core;
    exports trashsoftware.trashSnooker.core.ai;
    exports trashsoftware.trashSnooker.fxml;
    exports trashsoftware.trashSnooker.fxml.alert;
    exports trashsoftware.trashSnooker.recorder;
    exports trashsoftware.trashSnooker.core.scoreResult;
    exports trashsoftware.trashSnooker.core.table;

    opens trashsoftware.trashSnooker.fxml;
    opens trashsoftware.trashSnooker.fxml.alert;
    exports trashsoftware.trashSnooker.core.snooker;
    exports trashsoftware.trashSnooker.core.numberedGames;
    exports trashsoftware.trashSnooker.core.numberedGames.chineseEightBall;
    exports trashsoftware.trashSnooker.core.numberedGames.sidePocket;
    exports trashsoftware.trashSnooker.util;
    exports trashsoftware.trashSnooker.core.movement;
    exports trashsoftware.trashSnooker.util.db;
}