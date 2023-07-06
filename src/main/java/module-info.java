module TrashSnooker {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.swing;
    requires org.json;
    requires org.jetbrains.annotations;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires org.tukaani.xz;
    requires java.desktop;
    requires commons.math3;
    requires jcodec;
//    requires jcodec.javase;

    exports trashsoftware.trashSnooker;
    exports trashsoftware.trashSnooker.core;
    exports trashsoftware.trashSnooker.core.ai;
    exports trashsoftware.trashSnooker.fxml;
    exports trashsoftware.trashSnooker.fxml.alert;
    exports trashsoftware.trashSnooker.recorder;
    exports trashsoftware.trashSnooker.core.scoreResult;
    exports trashsoftware.trashSnooker.core.table;
    exports trashsoftware.trashSnooker.fxml.drawing;
    exports trashsoftware.trashSnooker.fxml.widgets;
    exports trashsoftware.trashSnooker.core.career;
    exports trashsoftware.trashSnooker.core.career.championship;
    exports trashsoftware.trashSnooker.core.training;
    exports trashsoftware.trashSnooker.core.career.achievement;

    opens trashsoftware.trashSnooker.fxml;
    opens trashsoftware.trashSnooker.fxml.alert;
    opens trashsoftware.trashSnooker.fxml.widgets;
    opens trashsoftware.trashSnooker.core.career to javafx.base;

    exports trashsoftware.trashSnooker.core.snooker;
    exports trashsoftware.trashSnooker.core.numberedGames;
    exports trashsoftware.trashSnooker.core.numberedGames.chineseEightBall;
    exports trashsoftware.trashSnooker.core.numberedGames.nineBall;
    exports trashsoftware.trashSnooker.util;
    exports trashsoftware.trashSnooker.core.movement;
    exports trashsoftware.trashSnooker.util.db;
    exports trashsoftware.trashSnooker.core.phy;
    exports trashsoftware.trashSnooker.core.metrics;
    exports trashsoftware.trashSnooker.fxml.statsViews;
    opens trashsoftware.trashSnooker.fxml.statsViews;
    opens trashsoftware.trashSnooker.core.career.challenge to javafx.base;
    exports trashsoftware.trashSnooker.core.career.challenge;
    exports trashsoftware.trashSnooker.core.career.awardItems;
    exports trashsoftware.trashSnooker.util.config;
    exports trashsoftware.trashSnooker.res;
}