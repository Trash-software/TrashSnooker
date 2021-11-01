package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.util.GameSaver;

public class EntireGame {
    
    public final int totalFrames;
    private int p1Wins;
    private int p2Wins;
    private boolean p1Breaks;
    InGamePlayer p1;
    InGamePlayer p2;
    
    private final GameView gameView;
    Game game;
    public final GameType gameType;
    
    public EntireGame(GameView gameView, InGamePlayer p1, InGamePlayer p2, GameType gameType,
                      int totalFrames) {
        this.p1 = p1;
        this.p2 = p2;
        if (totalFrames % 2 != 1) {
            throw new RuntimeException("Total frames must be odd.");
        }
        this.gameType = gameType;
        this.totalFrames = totalFrames;
        this.gameView = gameView;
        
        createNextFrame();
    }
    
    public static EntireGame load() {
        return GameSaver.load();
    }
    
    public void save() {
        GameSaver.save(this);
    }
    
    public Game getGame() {
        return game;
    }

    public int getP1Wins() {
        return p1Wins;
    }

    public int getP2Wins() {
        return p2Wins;
    }

    public InGamePlayer getPlayer1() {
        return p1;
    }

    public InGamePlayer getPlayer2() {
        return p2;
    }
    
    public boolean playerWinsAframe(InGamePlayer player) {
        if (player.getPlayerPerson().equals(p1.getPlayerPerson())) {
            return p1WinsAFrame();
        } else {
            return p2WinsAFrame();
        }
    }

    private boolean p1WinsAFrame() {
        p1Wins++;
        return p1Wins > totalFrames / 2;
    }
    
    private boolean p2WinsAFrame() {
        p2Wins++;
        return p2Wins > totalFrames / 2;
    }
    
    public void quitGame() {
        if (game != null) {
            game.quitGame();
        }
    }
    
    public void startNextFrame() {
        createNextFrame();
    }
    
    private void createNextFrame() {
        p1Breaks = !p1Breaks;
        GameSettings gameSettings = new GameSettings.Builder()
                .player1Breaks(p1Breaks)
                .players(p1, p2)
                .build();
        game = Game.createGame(gameView, gameSettings, gameType);
    }
}
