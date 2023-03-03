CREATE TABLE IF NOT EXISTS Player (
    Name VARCHAR(32) PRIMARY KEY
);

-- Actually a 'Match'
CREATE TABLE IF NOT EXISTS EntireGame (
    EntireBeginTime TIMESTAMP PRIMARY KEY,
    GameType VARCHAR(16),
    Player1Name VARCHAR(32) REFERENCES Player ON DELETE CASCADE,  -- Actually PlayerId
    Player2Name VARCHAR(32) REFERENCES Player ON DELETE CASCADE,
    Player1IsAI INTEGER,
    Player2IsAI INTEGER,
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
    PlayerIsAI INTEGER,
    Attempts INTEGER DEFAULT 0,
    Successes INTEGER DEFAULT 0,
    LongAttempts INTEGER DEFAULT 0,
    LongSuccesses INTEGER DEFAULT 0,
    Defenses INTEGER DEFAULT 0,
    DefenseSuccesses INTEGER DEFAULT 0,
    Positions INTEGER DEFAULT 0,
    PositionSuccesses INTEGER DEFAULT 0,
    RestAttempts INTEGER DEFAULT 0,
    RestSuccesses INTEGER DEFAULT 0,
    Solves INTEGER DEFAULT 0,
    SolveSuccesses INTEGER DEFAULT 0,
    PRIMARY KEY (EntireBeginTime, FrameIndex, PlayerName, PlayerIsAI)
);

-- 一局内的score
CREATE TABLE IF NOT EXISTS SnookerRecord (
    EntireBeginTime TIMESTAMP REFERENCES GeneralRecord ON DELETE CASCADE,
    FrameIndex INTEGER,
    PlayerName VARCHAR(32) REFERENCES Player,
    PlayerIsAI INTEGER,
    TotalScore INTEGER DEFAULT 0,
    Breaks50 INTEGER DEFAULT 0,
    Highest INTEGER DEFAULT 0,
    PRIMARY KEY (EntireBeginTime, FrameIndex, PlayerName, PlayerIsAI)
);

CREATE TABLE IF NOT EXISTS ChineseEightRecord (
    EntireBeginTime TIMESTAMP REFERENCES GeneralRecord ON DELETE CASCADE,
    FrameIndex INTEGER,
    PlayerName VARCHAR(32) REFERENCES Player,
    PlayerIsAI INTEGER,
    Breaks INTEGER DEFAULT 0,
    BreakPots INTEGER DEFAULT 0,
    BreakClear INTEGER DEFAULT 0,
    ContinueClear INTEGER DEFAULT 0,
    Highest INTEGER DEFAULT 0,  -- 最多单杆连续进球
    PRIMARY KEY (EntireBeginTime, FrameIndex, PlayerName, PlayerIsAI)
);

CREATE TABLE IF NOT EXISTS SidePocketRecord (
    EntireBeginTime TIMESTAMP REFERENCES GeneralRecord ON DELETE CASCADE,
    FrameIndex INTEGER,
    PlayerName VARCHAR(32) REFERENCES Player,
    PlayerIsAI INTEGER,
    Breaks INTEGER DEFAULT 0,
    BreakPots INTEGER DEFAULT 0,
    BreakClear INTEGER DEFAULT 0,
    ContinueClear INTEGER DEFAULT 0,
    Highest INTEGER DEFAULT 0,  -- 最多单杆连续进球
    PRIMARY KEY (EntireBeginTime, FrameIndex, PlayerName, PlayerIsAI)
);
