package trashsoftware.trashSnooker.core.metrics;

public enum Rule {
    FOUL_AND_MISS,  // 复位
    HIT_CUSHION,  // 每一杆必须碰库
    POCKET_INDICATION,  // 指袋
    FOUL_BALL_IN_HAND,  // 犯规后送手中球
    CAN_EMPTY,  // 某些情况下可以空杆
}
