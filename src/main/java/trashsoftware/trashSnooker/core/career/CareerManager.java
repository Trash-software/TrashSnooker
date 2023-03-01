package trashsoftware.trashSnooker.core.career;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.championship.Championship;
import trashsoftware.trashSnooker.core.career.championship.ChineseEightChampionship;
import trashsoftware.trashSnooker.core.career.championship.SnookerChampionship;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class CareerManager {

    public static final boolean LOG = true;

    public static final String LEVEL_INFO = "data/level.dat";
    public static final String CAREER_DIR = "user/career/";
    public static final String CAREER_JSON = "career.json";
    public static final String PROGRESS_FILE = "progress.json";
    public static final String HISTORY_DIR = "history";
    //    public static final int PROFESSIONAL_LIMIT = 32;
    public static final int INIT_PERKS = 6;
    public static final int PERKS_PER_LEVEL = 2;
    private static final int[] EXP_REQUIRED_LEVEL_UP = readExpLevelUp();
    private static CareerManager instance;
    private static CareerSave currentSave;
    private final CareerSave careerSave;
    private final ChampDataManager champDataManager = ChampDataManager.getInstance();
    private final List<Career> playerCareers = new ArrayList<>();
    private final Calendar timestamp;
    private final List<Career.CareerWithAwards> snookerRanking = new ArrayList<>();
    private final List<Career.CareerWithAwards> snookerRankingSingleSeason = new ArrayList<>();
    private final List<Career.CareerWithAwards> chineseEightRanking = new ArrayList<>();
    private Career humanPlayerCareer;  // 玩家的career
    private Championship inProgress;

    private CareerManager(CareerSave save) {
        this.careerSave = save;
        this.timestamp = Calendar.getInstance();
        this.timestamp.set(2023, Calendar.JANUARY, 20);  // 初始日期
    }

    private CareerManager(CareerSave save, Calendar timestamp) {
        this.careerSave = save;
        this.timestamp = timestamp;
    }

    private static int[] readExpLevelUp() {
        try (BufferedReader br = new BufferedReader(new FileReader(LEVEL_INFO))) {
            String line;
            List<Integer> levelExp = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (!line.isBlank()) {
                    String[] parts = line.split(",");
                    int exp = Integer.parseInt(parts[1].strip());
                    levelExp.add(exp);
                }
            }
            int[] res = new int[levelExp.size()];
            for (int i = 0; i < res.length; i++) {
                res[i] = levelExp.get(i);
            }
            return res;
        } catch (FileNotFoundException e) {
            DataGenerator.generateExpPerLevel();
            return readExpLevelUp();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<CareerSave> careerLists() {
        File dir = new File(CAREER_DIR);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Cannot create");
            }
        }
        File[] saves = dir.listFiles();
        if (saves == null) return new ArrayList<>();
        List<CareerSave> careerSaves = new ArrayList<>();
        for (File file : saves) {
            if (file != null && file.isDirectory()) {
                CareerSave save = new CareerSave(file);
                careerSaves.add(save);
            }
        }
        return careerSaves;
    }

    public static CareerManager getInstance() {
        if (instance == null || instance.careerSave != currentSave) {
            if (currentSave == null) throw new RuntimeException("Career not set");
            try {
                instance = loadFromFile(currentSave);
            } catch (IOException e) {
                throw new RuntimeException("Career not readable");
            }
        }
        return instance;
    }

    public static void setCurrentSave(CareerSave currentSave) {
        CareerManager.currentSave = currentSave;
    }

    private static CareerManager loadFromFile(CareerSave careerSave) throws IOException {
        File file = new File(careerSave.getDir(), CAREER_JSON);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    builder.append(line);
                }

                return fromJson(new JSONObject(builder.toString()), careerSave);
            }
        } else {
            throw new IOException("Career does not exist");
        }
    }

    public static CareerSave createNew(PlayerPerson playerPlayer) {
//        if (getInstance() != null) throw new RuntimeException("Shouldn't be");
        CareerSave save = new CareerSave(new File(CAREER_DIR, playerPlayer.getPlayerId()));
        try {
            save.create();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        setCurrentSave(save);
        CareerManager cm = new CareerManager(save);
        for (PlayerPerson person : DataLoader.getInstance().getAllPlayers()) {
            Career career;
            if (person.getPlayerId().equals(playerPlayer.getPlayerId())) {
                career = Career.createByPerson(person, true);
                cm.humanPlayerCareer = career;
            } else {
                career = Career.createByPerson(person, false);
            }
            cm.playerCareers.add(career);
        }
        if (cm.humanPlayerCareer == null) {
            throw new RuntimeException("No human player???");
        }
        instance = cm;

//        instance.updateRanking();
//        instance.saveToDisk();

        return save;
    }

    public static File getChampionshipProgressFile() {
        return new File(currentSave.getDir(), PROGRESS_FILE);
    }

    public static File getChampionshipHistoryDir() {
        File dir = new File(currentSave.getDir(), HISTORY_DIR);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Cannot create " + dir);
            }
        }
        return dir;
    }

    private static CareerManager fromJson(JSONObject jsonObject, CareerSave careerSave) {
        JSONArray rootArr = jsonObject.getJSONArray("careers");
        String time = jsonObject.getString("timestamp");

        CareerManager careerManager = new CareerManager(careerSave, stringToCalendar(time));

        Map<String, PlayerPerson> newPlayers = DataLoader.getInstance().getPlayerPeopleCopy();

        for (int i = 0; i < rootArr.length(); i++) {
            JSONObject personObj = rootArr.getJSONObject(i);
            Career career = Career.fromJson(personObj);
            if (career == null) continue;
            careerManager.playerCareers.add(career);
            newPlayers.remove(career.getPlayerPerson().getPlayerId());  // 有的

            if (career.isHumanPlayer()) {
                careerManager.humanPlayerCareer = career;
            }
        }

        if (careerManager.humanPlayerCareer == null) {
            throw new RuntimeException("Loaded file does not have human player.");
        }
        if (!careerManager.humanPlayerCareer.getPlayerPerson().getPlayerId().equals(jsonObject.getString("humanPlayer"))) {
            throw new RuntimeException("Inconsistent");
        }

        // 把可能存在的新增球员加进生涯管理器中
        for (PlayerPerson newPlayer : newPlayers.values()) {
            Career career = Career.createByPerson(newPlayer, false);
            // 新球员初始就0分吧
            careerManager.playerCareers.add(career);
        }

        careerManager.updateRanking();
        return careerManager;
    }

    public static Calendar stringToCalendar(String s) {
        String[] split = s.split("/");
        if (split.length != 3) throw new RuntimeException("Exact day must formed by 3 parts");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Integer.parseInt(split[0]),
                Integer.parseInt(split[1]) - 1,
                Integer.parseInt(split[2]));
        return calendar;
    }

    public static String calendarToString(Calendar c) {
        return String.format("%d/%d/%d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH));
    }

    public static int[] getExpRequiredLevelUp() {
        return EXP_REQUIRED_LEVEL_UP;
    }

    public void simulateMatchesInPastTwoYears() {
        updateRanking();  // 按照能力初始化排名

        Calendar pastTime = Calendar.getInstance();
        pastTime.setTimeInMillis(timestamp.getTimeInMillis());
        pastTime.set(Calendar.YEAR, pastTime.get(Calendar.YEAR) - 2);

        while (pastTime.before(timestamp)) {
            Championship nextChamp = nextChampionship(pastTime);
            System.out.println("Simulating " + nextChamp.fullName());
            nextChamp.startChampionship(false, false);
            while (nextChamp.hasNextRound()) {
                nextChamp.startNextRound(false);
            }
        }
        updateRanking();  // 比赛完了最后排下名
        saveToDisk();
    }

    public List<CareerRank> getRanking(GameRule rule, ChampionshipData.Selection selection) {
        List<CareerRank> crs = new ArrayList<>();
        List<Career.CareerWithAwards> ranking = getRankingPrivate(rule, selection);
        for (int i = 0; i < ranking.size(); i++) {
            Career.CareerWithAwards cwa = ranking.get(i);
            int rankedAwd;
            if (selection == ChampionshipData.Selection.SINGLE_SEASON) {
                rankedAwd = cwa.getOneSeasonAwards();
            } else {
                rankedAwd = cwa.getTwoSeasonsAwards();
            }
            crs.add(new CareerRank(i, cwa.career, rankedAwd, cwa.getTotalAwards()));
        }
        return crs;
    }

    public CareerSave getCareerSave() {
        return careerSave;
    }

    public int getExpNeededToLevelUp(int currentLevel) {
        int index = currentLevel - 1;
        if (index == EXP_REQUIRED_LEVEL_UP.length) return Integer.MAX_VALUE;
        return EXP_REQUIRED_LEVEL_UP[index];
    }

    public Career findCareerByPlayerId(String playerId) {
        for (Career career : playerCareers) {
            if (career.getPlayerPerson().getPlayerId().equals(playerId)) {
                return career;
            }
        }
        throw new RuntimeException("Career " + playerId + " does not exist");
    }

    /**
     * @return 玩家当前有没有资格参加
     */
    public boolean humanPlayerQualifiedToJoinSnooker(ChampionshipData championshipData,
                                                     ChampionshipData.Selection selection) {
        int humanRank = humanPlayerRanking(championshipData.type, selection).rank;
//        if (championshipData.professionalOnly) {
//            if (humanRank >= PROFESSIONAL_LIMIT) return false;
//        }
        if (championshipData.preMatchNewAdded.length == 0) {
            // 没有资格赛
            if (humanRank >= championshipData.mainPlaces) return false;
        }
        if (championshipData.mainPlaces == championshipData.seedPlaces) {
            // 正赛只有种子选手（大师赛）
            if (humanRank >= championshipData.seedPlaces) return false;
        }
        return true;
    }

    /**
     * @return 玩家的排名，从0开始计
     */
    public CareerRank humanPlayerRanking(GameRule gameRule, ChampionshipData.Selection selection) {
        List<CareerRank> list = getRanking(gameRule, selection);

        for (CareerRank cr : list) {
            if (cr.getCareer().isHumanPlayer())
                return cr;
        }
        throw new RuntimeException("No human player");
    }

    /**
     * 前提条件是球员已经有资格参赛了
     */
    public List<Career> participants(ChampionshipData data,
                                     boolean humanJoin) {
        if (data.professionalOnly) {
            return professionalParticipants(data, data.getTotalPlaces(), humanJoin, data.getType(), data.getSelection());
        } else {
            return openParticipants(data, data.getTotalPlaces(), humanJoin, data.getType(), data.getSelection());
        }
    }

    private List<Career.CareerWithAwards> getRankingPrivate(GameRule type,
                                                            ChampionshipData.Selection selection) {
        switch (type) {
            case SNOOKER:
                if (selection == ChampionshipData.Selection.REGULAR)
                    return snookerRanking;
                else if (selection == ChampionshipData.Selection.SINGLE_SEASON)
                    return snookerRankingSingleSeason;
            case CHINESE_EIGHT:
                return chineseEightRanking;
            case MINI_SNOOKER:
            case SIDE_POCKET:
            default:
                return new ArrayList<>();
        }
    }

    public List<Career> professionalParticipants(ChampionshipData data,
                                                 int n,
                                                 boolean humanJoin,
                                                 GameRule type,
                                                 ChampionshipData.Selection selection) {
        List<Career> result = new ArrayList<>();

        List<Career.CareerWithAwards> ranking = getRankingPrivate(type, selection);
        for (int i = 0; i < ranking.size(); i++) {
            Career.CareerWithAwards cwa = ranking.get(i);
            if (cwa.career.isHumanPlayer() && !humanJoin) continue;
            if (cwa.willJoinMatch(data,
                    i,
                    i == 0 ? null : ranking.get(i - 1),
                    i == ranking.size() - 1 ? null : ranking.get(i + 1))) {
                result.add(cwa.career);
                if (result.size() == n) break;
            }
        }
        return result;
    }

    public List<Career> openParticipants(ChampionshipData data,
                                         int n,
                                         boolean humanJoin,
                                         GameRule type,
                                         ChampionshipData.Selection selection) {
        List<Career> result = new ArrayList<>();

        boolean humanAlreadyJoin = false;
        List<Career.CareerWithAwards> rankings = getRankingPrivate(type, selection);

        for (int i = 0; i < rankings.size(); i++) {
            Career.CareerWithAwards cwa = rankings.get(i);
            if (!cwa.career.getPlayerPerson().isRandom && !cwa.career.getPlayerPerson().category.equals("God")) {
                if (cwa.career.isHumanPlayer()) {
                    if (humanJoin) {
                        humanAlreadyJoin = true;
                    } else {
                        continue;
                    }
                }
                if (cwa.willJoinMatch(data,
                        i,
                        i == 0 ? null : rankings.get(i - 1),
                        i == rankings.size() - 1 ? null : rankings.get(i + 1)))
                    result.add(cwa.career);

                if (result.size() == n) {
                    break;
                }
            }
        }

        if (result.size() < n) {
            for (int i = 0; i < rankings.size(); i++) {
                Career.CareerWithAwards cwa = rankings.get(i);
                if (cwa.career.getPlayerPerson().isRandom || cwa.career.getPlayerPerson().category.equals("God")) {
                    if (cwa.career.isHumanPlayer()) {
                        if (!humanJoin) continue;
                        humanAlreadyJoin = true;
                    }
                    if (cwa.willJoinMatch(data,
                            i,
                            i == 0 ? null : rankings.get(i - 1),
                            i == rankings.size() - 1 ? null : rankings.get(i + 1))) {
                        result.add(cwa.career);
                        if (result.size() == n) {
                            break; 
                        }
                    }
                }
            }
        }
        if (humanJoin && !humanAlreadyJoin) {
            result.remove(result.size() - 1);
            result.add(humanPlayerCareer);
        }
        return result;
    }

    public Calendar getTimestamp() {
        return timestamp;
    }

    /**
     * @return 只返回下一个比赛是什么，不推进时间
     */
    public ChampionshipData.WithYear nextChampionshipData() {
        return champDataManager.getNextChampionship(
                timestamp.get(Calendar.YEAR),
                timestamp.get(Calendar.MONTH) + 1,
                timestamp.get(Calendar.DAY_OF_MONTH)
        );
    }

    public Championship startNextChampionship() {
        return nextChampionship(timestamp);
    }

    private Championship nextChampionship(Calendar timestamp) {
        int curMonth = timestamp.get(Calendar.MONTH) + 1;
        ChampionshipData.WithYear nextDataWithYear = champDataManager.getNextChampionship(
                timestamp.get(Calendar.YEAR),
                curMonth,
                timestamp.get(Calendar.DAY_OF_MONTH) + 1);
        ChampionshipData nextData = nextDataWithYear.data;

        if (nextData.month < curMonth) {
            timestamp.set(Calendar.YEAR, timestamp.get(Calendar.YEAR) + 1);
        }
        timestamp.set(Calendar.MONTH, nextData.month - 1);
        timestamp.set(Calendar.DAY_OF_MONTH, nextData.day);

        updateRanking();
        updateEfforts();  // 在update ranking之后

        Championship championship;
        switch (nextData.getType()) {
            case SNOOKER:
                championship = new SnookerChampionship(nextData, timestamp);
                break;
            case CHINESE_EIGHT:
                championship = new ChineseEightChampionship(nextData, timestamp);
                break;
            case SIDE_POCKET:
            case MINI_SNOOKER:
            default:
                throw new UnsupportedOperationException();
        }
        inProgress = championship;
        return championship;
    }

    public void updateRanking() {
        snookerRanking.clear();
        for (Career career : playerCareers) {
            snookerRanking.add(new Career.CareerWithAwards(GameRule.SNOOKER, career, timestamp));
        }

        snookerRankingSingleSeason.clear();
        snookerRankingSingleSeason.addAll(snookerRanking);

        snookerRanking.sort(Career.CareerWithAwards::twoSeasonsCompare);
        snookerRankingSingleSeason.sort(Career.CareerWithAwards::oneSeasonCompare);

        // 黑八两年
        chineseEightRanking.clear();
        for (Career career : playerCareers) {
            chineseEightRanking.add(new Career.CareerWithAwards(GameRule.CHINESE_EIGHT, career, timestamp));
        }

        chineseEightRanking.sort(Career.CareerWithAwards::twoSeasonsCompare);
    }

    public void updateHandFeels() {
        for (Career career : playerCareers) {
            career.updateHandFeel();
        }
    }

    public void updateEfforts() {
        updateEffortByRank(GameRule.SNOOKER);
//        updateEffortByRank(GameRule.CHINESE_EIGHT);
//        updateEffortByRank(GameRule.SIDE_POCKET);
//        updateEffortByRank(GameRule.MINI_SNOOKER);
    }

    private List<Career.CareerWithAwards> getRankOfGame(GameRule rule) {
        List<Career.CareerWithAwards> rnk;

        switch (rule) {
            case SNOOKER:
                rnk = snookerRanking;
                break;
            case CHINESE_EIGHT:
                rnk = chineseEightRanking;
                break;
            case MINI_SNOOKER:
            case SIDE_POCKET:
            default:
                throw new RuntimeException();
        }

        return rnk;
    }

    private void updateEffortByRank(GameRule rule) {
        List<Career.CareerWithAwards> rnk = getRankOfGame(rule);
//        
        for (Career.CareerWithAwards cwa : rnk) {
            cwa.career.updateEffort(rule, 1.0);
        }
        Career.CareerWithAwards top = rnk.get(0);
        Career.CareerWithAwards second = rnk.get(1);
        if (top.getTwoSeasonsAwards() > second.getTwoSeasonsAwards() * 1.5) {
            // 放水
            double leakWaterRatio = (double) top.getTwoSeasonsAwards() / second.getTwoSeasonsAwards() - 0.5;  // 大于1
            leakWaterRatio = leakWaterRatio * 0.5 + 0.5;  // 0.5-1
            top.career.updateEffort(rule, Math.max(1 / leakWaterRatio, 0.7));
            System.out.println("Updated effort of " +
                    top.career.getPlayerPerson().getPlayerId() + ": " + top.career.getEffort(rule));
        }
    }

    public List<Career.CareerWithAwards> getSnookerTopN(int n) {
        return new ArrayList<>(snookerRanking.subList(0, n));
    }

    public Career getHumanPlayerCareer() {
        return humanPlayerCareer;
    }

    public void reloadHumanPlayerPerson() {
        humanPlayerCareer.setPlayerPerson(
                DataLoader.getInstance().getPlayerPerson(humanPlayerCareer.getPlayerPerson().getPlayerId()));
    }

    /**
     * @return 当前正在进行且还没结束的比赛
     */
    public Championship getChampionshipInProgress() {
        if (inProgress == null) {
            inProgress = Championship.restoreFromSaved();
            if (inProgress == null || inProgress.isFinished()) {
                return null;
            }
            return inProgress;
        } else {
            return inProgress.isFinished() ? null : inProgress;
        }
    }

    public void saveToDisk() {
        File file = new File(careerSave.getDir(), CAREER_JSON);
        File dir = file.getParentFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Cannot create career dir");
            }
        }
        JSONObject root = new JSONObject();
        root.put("timestamp", calendarToString(timestamp));
        root.put("humanPlayer", humanPlayerCareer.getPlayerPerson().getPlayerId());

        JSONArray rootArr = new JSONArray();

        for (Career career : playerCareers) {
            JSONObject careerRoot = career.toJsonObject();
            rootArr.put(careerRoot);
        }

        root.put("careers", rootArr);
        String str = root.toString(2);

        try (BufferedWriter bw = new BufferedWriter(
                new FileWriter(file, StandardCharsets.UTF_8))) {
            bw.write(str);
        } catch (IOException e) {
            EventLogger.log(e);
        }
    }
}
