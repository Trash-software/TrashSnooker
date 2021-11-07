CREATE TABLE IF NOT EXISTS Player (
    Name VARCHAR(32) PRIMARY KEY
);

-- Actually a 'Match'
CREATE TABLE IF NOT EXISTS EntireGame (
    EntireBeginTime TIMESTAMP PRIMARY KEY,
    GameType VARCHAR(16),
    Player1Name VARCHAR(32) REFERENCES Player ON DELETE CASCADE,
    Player2Name VARCHAR(32) REFERENCES Player ON DELETE CASCADE,
    TotalFrames INTEGER DEFAULT 1
);

-- Actually a 'Frame'
CREATE TABLE IF NOT EXISTS Game (
    EntireBeginTime TIMESTAMP REFERENCES EntireGame ON DELETE CASCADE,
    FrameIndex INTEGER,  -- Number of frame of this game in the entire game, starts from 1
    DurationSeconds INTEGER DEFAULT 0,
    WinnerName VARCHAR(32) REFERENCES Player DEFAULT NULL,
    PRIMARY KEY (EntireBeginTime, FrameIndex)
);

-- Record for one frame
-- Two records (p1, p2) for one single frame
CREATE TABLE IF NOT EXISTS GeneralRecord (
    EntireBeginTime TIMESTAMP REFERENCES Game ON DELETE CASCADE,
    FrameIndex INTEGER REFERENCES Game ON DELETE CASCADE,
    PlayerName VARCHAR(32) REFERENCES Player,
    Attempts INTEGER DEFAULT 0,
    Successes INTEGER DEFAULT 0,
    LongAttempts INTEGER DEFAULT 0,
    LongSuccesses INTEGER DEFAULT 0,
    Defenses INTEGER DEFAULT 0,
    DefenseSuccesses INTEGER DEFAULT 0,
    PRIMARY KEY (EntireBeginTime, FrameIndex, PlayerName)
);

-- 一局内的score
CREATE TABLE IF NOT EXISTS SnookerRecord (
    EntireBeginTime TIMESTAMP REFERENCES GeneralRecord ON DELETE CASCADE,
    FrameIndex INTEGER,
    PlayerName VARCHAR(32) REFERENCES Player,
    TotalScore INTEGER DEFAULT 0,
    Breaks50 INTEGER DEFAULT 0,
    Highest INTEGER DEFAULT 0,
    PRIMARY KEY (EntireBeginTime, FrameIndex, PlayerName)
);

CREATE TABLE IF NOT EXISTS ChineseEightRecord (
    EntireBeginTime TIMESTAMP REFERENCES GeneralRecord ON DELETE CASCADE,
    FrameIndex INTEGER,
    PlayerName VARCHAR(32) REFERENCES Player,
    BreakClear INTEGER DEFAULT 0,
    ContinueClear INTEGER DEFAULT 0,
    PRIMARY KEY (EntireBeginTime, FrameIndex, PlayerName)
);

CREATE TABLE IF NOT EXISTS SidePocketRecord (
    EntireBeginTime TIMESTAMP REFERENCES GeneralRecord ON DELETE CASCADE,
    FrameIndex INTEGER,
    PlayerName VARCHAR(32) REFERENCES Player,
    BreakClear INTEGER DEFAULT 0,
    ContinueClear INTEGER DEFAULT 0,
    PRIMARY KEY (EntireBeginTime, FrameIndex, PlayerName)
);
