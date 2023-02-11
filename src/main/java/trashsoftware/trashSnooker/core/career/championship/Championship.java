package trashsoftware.trashSnooker.core.career.championship;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.career.*;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.*;
import java.util.*;

public abstract class Championship {

    protected final Calendar timestamp;
    protected final ChampionshipData data;

    protected MatchTree matchTree;
    protected int currentStageIndex;
    protected boolean finished = false;
    protected PlayerVsAiMatch activeMatch;

    public Championship(ChampionshipData data, Calendar timestamp) {
        this.data = data;
        this.timestamp = timestamp;
    }

    private static Championship loadProgressFromJson(JSONObject jsonObject) {
        ChampionshipData data = ChampDataManager.getInstance().findDataById(jsonObject.getString("championshipId"));
        Calendar timestamp = CareerManager.stringToCalendar(jsonObject.getString("timestamp"));
        Championship championship;
        switch (data.getType()) {
            case SNOOKER:
                championship = new SnookerChampionship(data, timestamp);
                break;
            case CHINESE_EIGHT:
            case SIDE_POCKET:
            case MINI_SNOOKER:
            default:
                throw new RuntimeException("Currently unsupported");
        }
        championship.currentStageIndex = jsonObject.getInt("stageIndex");
//        ChampionshipStage curStage = 
        championship.matchTree = MatchTree.fromJson(jsonObject.getJSONObject("matchTree"));
//        championship.matchTree.getRoot().slCheck();
        championship.checkFinish();

        if (championship.finished != jsonObject.getBoolean("finished")) {
            throw new RuntimeException("Broken save");
        }
        
        if (!championship.isFinished()) {
            championship.loadMatchInProgress();
        }

        return championship;
    }

    public static Championship restoreFromSaved() {
        try (BufferedReader br = new BufferedReader(new FileReader(CareerManager.getChampionshipProgressFile()))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
            return loadProgressFromJson(new JSONObject(builder.toString()));
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void loadMatchInProgress() {
        activeMatch = PlayerVsAiMatch.loadSaved(matchTree.getRoot(), data);  // 可以是null
    }

    public MatchTree getMatchTree() {
        return matchTree;
    }
    
    private void saveProgressToJson() {
        saveProgressToJson(CareerManager.getChampionshipProgressFile());
    }
    
    private void saveAsHistory() {
        File file = new File(CareerManager.getChampionshipHistoryDir(), 
                timestamp.get(Calendar.YEAR) + "_" + data.getId());
        saveProgressToJson(file);
    }

    private void saveProgressToJson(File file) {
        // 必须在started之后才能保存
        JSONObject saved = new JSONObject();
        saved.put("timestamp", CareerManager.calendarToString(timestamp));
        saved.put("championshipId", data.getId());
        saved.put("stageIndex", currentStageIndex);
        saved.put("matchTree", matchTree.saveProgressToJson());
        saved.put("finished", isFinished());

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(saved.toString(2));
        } catch (IOException e) {
            EventLogger.log(e);
        }
    }

    protected abstract List<Career> getParticipantsByRank(boolean playerJoin);

    public boolean isStarted() {
        return matchTree != null;
    }

    public String fullName() {
        return timestamp.get(Calendar.YEAR) + " " + data.getName();
    }

    public ChampionshipStage getCurrentStage() {
        return data.getStages()[currentStageIndex];
    }

    public void startChampionship(boolean playerJoin) {
        startChampionship(playerJoin, true);
    }

    public void startChampionship(boolean playerJoin, boolean save) {
        // precondition: player有资格参加，应在manager内检查
        CareerManager.getInstance().saveToDisk();
        
        System.out.println("Starting " + data.getId());
        List<Career> careers = getParticipantsByRank(playerJoin);

        List<Career> seeds = new ArrayList<>(careers.subList(0, data.getSeedPlaces()));
        List<Career> nonSeeds = new ArrayList<>(careers.subList(data.getSeedPlaces(), data.getTotalPlaces()));

        matchTree = new MatchTree(data, seeds, nonSeeds);
        currentStageIndex = data.getStages().length - 1;

        if (save) {
            saveProgressToJson();
        }
    }

    public boolean isFinished() {
        return finished;
    }
    
    public SortedMap<ChampionshipScore.Rank, List<Career>> getResults() {
        SortedMap<ChampionshipScore.Rank, List<Career>> result = new TreeMap<>();
        matchTree.getRoot().getResults(data, result, 0);
        return result;
    }

    public boolean hasNextRound() {
        return currentStageIndex >= 0;
    }
    
    public boolean hasSavedRound() {
        return activeMatch != null;
    }

    public PlayerVsAiMatch getSavedRound() {
        return activeMatch;
    }

    public PlayerVsAiMatch continueSavedRound() {
        assert activeMatch != null;
        setCallback(activeMatch, true);
        return activeMatch;
    }

    public PlayerVsAiMatch startNextRound() {
        return startNextRound(true);
    }

    public PlayerVsAiMatch startNextRound(boolean save) {
        CareerManager.getInstance().updateHandFeels();
        ChampionshipStage stage = data.getStages()[currentStageIndex];
        System.out.println(data.getId() + stage);
//        saveProgressToJson();

        PlayerVsAiMatch playerVsAiMatch = matchTree.holdOneRoundMatches(data, stage);
        activeMatch = playerVsAiMatch;
        if (playerVsAiMatch == null) {
            currentStageIndex--;
            checkFinish();
            if (save) {
                saveProgressToJson();
            }
            if (finished) {
                distributeAwards();
                if (save) {
                    saveAsHistory();
                }
            }
            return null;
        } else {
            if (save) {
                saveProgressToJson();  // 把AI对战结果先存了来再说
            }
            
            setCallback(playerVsAiMatch, save);
            return playerVsAiMatch;
        }
    }
    
    private void setCallback(PlayerVsAiMatch match, boolean save) {
        match.setEndCallback(() -> {
            System.out.println("PvE end of " + data.getStages()[currentStageIndex]);
            currentStageIndex--;
            checkFinish();
            activeMatch = null;
            if (save) {
                saveProgressToJson();
            }
            if (finished) {
                distributeAwards();
                if (save) {
                    saveAsHistory();
                }
            }
        });
    }

    private void checkFinish() {
        if (matchTree == null) finished = false;
        else {
            finished = matchTree.getRoot().isFinished();
        }
    }

    private void distributeAwards() {
        System.out.println("Award distributed!");
        matchTree.distributeAwards(data, timestamp);
        CareerManager.getInstance().updateRanking();
        CareerManager.getInstance().saveToDisk();
    }

    public ChampionshipData getData() {
        return data;
    }
}
