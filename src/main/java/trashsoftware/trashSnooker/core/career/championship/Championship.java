package trashsoftware.trashSnooker.core.career.championship;

import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.*;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.*;
import java.util.*;

public abstract class Championship {

    protected final Calendar timestamp;
    protected final ChampionshipData data;

    protected final Map<String, Integer> careerSeedMap = new HashMap<>();
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
                championship = new ChineseEightChampionship(data, timestamp);
                break;
            case AMERICAN_NINE:
                championship = new AmericanNineChampionship(data, timestamp);
                break;
            case MINI_SNOOKER:
            default:
                throw new RuntimeException("Currently unsupported");
        }
        
        if (jsonObject.has("seedRanks")) {
            JSONObject sr = jsonObject.getJSONObject("seedRanks");
            for (String pid : sr.keySet()) {
                championship.careerSeedMap.put(pid, sr.getInt(pid));
            }
        }
        
        championship.currentStageIndex = jsonObject.getInt("stageIndex");
//        ChampionshipStage curStage = 
        championship.matchTree = MatchTree.fromJson(jsonObject.getJSONObject("matchTree"), championship);
//        championship.matchTree.getRoot().slCheck();
        championship.checkFinish();

        if (championship.finished != jsonObject.getBoolean("finished")) {
            throw new RuntimeException("Broken save");
        }
        
        if (!championship.isFinished()) {
            championship.loadMatchInProgress();
        }
        championship.loadExtraInfo(jsonObject);

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
        } catch (IOException | JSONException e) {
            return null;
        }
    }
    
    protected void loadExtraInfo(JSONObject root) {
    }
    
    private void loadMatchInProgress() {
        activeMatch = PlayerVsAiMatch.loadSaved(matchTree.getRoot(), this);  // 可以是null
    }

    public MatchTree getMatchTree() {
        return matchTree;
    }
    
    public boolean isHumanAlive() {
        return matchTree.isHumanAlive();
    }

    public Map<String, Integer> getCareerSeedMap() {
        return careerSeedMap;
    }

    private void saveProgressToJson() {
        saveProgressToJson(CareerManager.getChampionshipProgressFile());
    }
    
    private void saveAsHistory() {
        File file = new File(CareerManager.getChampionshipHistoryDir(), 
                timestamp.get(Calendar.YEAR) + "_" + data.getId());
        saveProgressToJson(file);
    }
    
    protected JSONObject toJson() {
        // 必须在started之后才能保存
        JSONObject saved = new JSONObject();
        saved.put("timestamp", CareerManager.calendarToString(timestamp));
        saved.put("championshipId", data.getId());
        saved.put("stageIndex", currentStageIndex);
        saved.put("matchTree", matchTree.saveProgressToJson());
        saved.put("finished", isFinished());

        JSONObject seeds = new JSONObject();
        for (Map.Entry<String, Integer> carSeed : careerSeedMap.entrySet()) {
            seeds.put(carSeed.getKey(), carSeed.getValue());
        }
        saved.put("seedRanks", seeds);
        
        return saved;
    }

    private void saveProgressToJson(File file) {
        JSONObject saved = toJson();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(saved.toString(2));
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }

    protected abstract List<TourCareer> getParticipantsByRank(boolean playerJoin, boolean humanQualified);
    
    protected Map<ChampionshipScore.Rank, List<String>> extraAwardsMap() {
        return new HashMap<>();
    }

    public boolean isStarted() {
        return matchTree != null;
    }
    
    public int getYear() {
        return timestamp.get(Calendar.YEAR);
    }

    public String fullName() {
        return getYear() + " " + data.getName();
    }
    
    public String uniqueId() {
        return String.format("%s+%d+%s", 
                CareerManager.getInstance().getCareerSave().getPlayerId(),
                getYear(),
                data.getId());
    }

    public ChampionshipStage getCurrentStage() {
        return data.getStages()[currentStageIndex];
    }

    public void startChampionship(boolean humanJoin, boolean humanQualified) {
        startChampionship(humanJoin, humanQualified, true);
    }

    public void startChampionship(boolean humanJoin, boolean humanQualified, boolean save) {
        // precondition: player有资格参加，应在manager内检查
        CareerManager.getInstance().saveToDisk();
        
        MatchTreeNode.restoreIdCounter();  // 每个赛事都从0计
        
        System.out.println("Starting " + data.getId());
        List<TourCareer> careers = getParticipantsByRank(humanJoin, humanQualified);

        System.out.println("Participants: " + careers);
        
        careerSeedMap.clear();
        List<Career> pureList = new ArrayList<>();
        for (TourCareer tc : careers) {
            pureList.add(tc.career);
            careerSeedMap.put(tc.career.getPlayerPerson().getPlayerId(), tc.seedNum);
        }

        List<Career> seeds = new ArrayList<>(pureList.subList(0, data.getSeedPlaces()));
        List<Career> nonSeeds = new ArrayList<>(pureList.subList(data.getSeedPlaces(), data.getTotalPlaces()));

        matchTree = new MatchTree(this, seeds, nonSeeds);
        currentStageIndex = data.getStages().length - 1;

        if (save) {
            saveProgressToJson();
        }
    }

    public boolean isFinished() {
        return finished;
    }
    
    public int getWonRoundsCount(Career career) {
        return matchTree.getRoot().getWonRounds(career, false);
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

        PlayerVsAiMatch playerVsAiMatch = matchTree.holdOneRoundMatches(this, stage);
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
        Map<ChampionshipScore.Rank, List<String>> extra = extraAwardsMap();
        
        matchTree.distributeAwards(data, timestamp, extra);
        CareerManager cm = CareerManager.getInstance();
        cm.updateRanking();
        cm.checkRankingAchievements();
        cm.saveToDisk();
    }

    public ChampionshipData getData() {
        return data;
    }
}
