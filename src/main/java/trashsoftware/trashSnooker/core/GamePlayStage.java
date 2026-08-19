package trashsoftware.trashSnooker.core;

public enum GamePlayStage {
    BREAK,
    NORMAL,
    OTHER_KEY_BALL,  // 其他的重要球，如临近被超分时的彩球
    NEXT_BALL_WIN,  // 打进下一颗球胜利/超分/147
    THIS_BALL_WIN,  // 打进目标球胜利/超分/147
    ENHANCE_WIN,  // 打进目标球锁定胜局
    NO_PRESSURE  // 可以随便打
}
