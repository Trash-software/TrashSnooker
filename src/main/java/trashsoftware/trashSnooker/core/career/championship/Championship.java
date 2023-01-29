package trashsoftware.trashSnooker.core.career.championship;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.*;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public abstract class Championship {

    protected final Calendar timestamp;
    protected final ChampionshipData data;

    protected MatchTree matchTree;
    protected int currentStageIndex;
    protected boolean finished = false;

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
        championship.matchTree = MatchTree.fromJson(jsonObject.getJSONObject("matchTree"));
        championship.checkFinish();

        if (championship.finished != jsonObject.getBoolean("finished")) {
            throw new RuntimeException("Broken save");
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
        return data.getStages().get(currentStageIndex);
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
        currentStageIndex = data.getStages().size() - 1;

        if (save) {
            saveProgressToJson();
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean hasNextRound() {
        return currentStageIndex >= 0;
    }

    public PlayerVsAiMatch startNextRound() {
        return startNextRound(true);
    }

    public PlayerVsAiMatch startNextRound(boolean save) {
        ChampionshipStage stage = data.getStages().get(currentStageIndex);
        System.out.println(data.getId() + stage);
//        saveProgressToJson();

        PlayerVsAiMatch playerVsAiMatch = matchTree.holdOneRoundMatches(data, stage);
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
            playerVsAiMatch.setEndCallback(() -> {
                System.out.println("PvE end of " + data.getStages().get(currentStageIndex));
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
            });
            return playerVsAiMatch;
        }
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
