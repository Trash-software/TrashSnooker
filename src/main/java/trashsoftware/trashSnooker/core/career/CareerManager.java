package trashsoftware.trashSnooker.core.career;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.career.championship.*;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.numberedGames.chineseEightBall.LetBall;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.JsonChecksum;
import trashsoftware.trashSnooker.util.Util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

public class CareerManager {

    public static final boolean LOG = false;
    private static final int DEFAULT_YEAR = 2023;
    private static final int DEFAULT_MONTH = Calendar.JANUARY;
    private static final int DEFAULT_DAY = 20;
    
    public static final int TIER_LIMIT = 64;

    public static final String LEVEL_INFO = "data/level.dat";
    public static final String CAREER_DIR = "user/career/";
    public static final String CAREER_JSON = "career.json";
    public static final String PROGRESS_FILE = "progress.json";
    public static final String HISTORY_DIR = "history";
    public static final String CACHE = "cache.json";
    //    public static final int PROFESSIONAL_LIMIT = 32;
    public static final int INIT_PERKS = 6;
    public static final int INIT_MONEY = 30000;
    public static final int BASE_LEVEL_PERK = 2;
    private static final int[] EXP_REQUIRED_LEVEL_UP = readExpLevelUp();
    private static CareerManager instance;
    private static CareerSave currentSave;
    private final CareerSave careerSave;
    private final ChampDataManager champDataManager = ChampDataManager.getInstance();
    private final List<Career> playerCareers = new ArrayList<>();
    private Date careerCreationTime;  // 真实世界的建档时间
    private final Calendar beginTimestamp;  // 建档的游戏内时间
    private final Calendar timestamp;
    private final List<CareerRanker.ByAwards> snookerRanking = new ArrayList<>();
    private final List<CareerRanker.ByAwards> snookerRankingSingleSeason = new ArrayList<>();
    private final List<CareerRanker.ByTier> chineseEightRanking = new ArrayList<>();
    private final List<CareerRanker.ByAwards> americanNineRanking = new ArrayList<>();
    private final InventoryManager inventoryManager;
    private HumanCareer humanPlayerCareer;  // 玩家的career
    private Championship inProgress;
    private int lastSavedVersion;
    private final List<SettingsHistory> settingsHistories = new ArrayList<>();
    private JSONObject cache;

    private double playerGoodness;
    private double aiGoodness;

    private CareerManager(CareerSave save) {
        this.careerSave = save;
        this.timestamp = Calendar.getInstance();
        this.timestamp.set(DEFAULT_YEAR, DEFAULT_MONTH, DEFAULT_DAY);  // 初始日期
        this.beginTimestamp = Calendar.getInstance();
        this.beginTimestamp.set(DEFAULT_YEAR, DEFAULT_MONTH, DEFAULT_DAY);
        
        inventoryManager = InventoryManager.createInstance(save);
    }

    private CareerManager(CareerSave save, Calendar timestamp, Calendar beginTimestamp) {
        this.careerSave = save;
        this.timestamp = timestamp;
        this.beginTimestamp = beginTimestamp;

        inventoryManager = InventoryManager.createInstance(save);
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

    public static void deleteCareer(CareerSave toDelete) {
        if (!Util.deleteFile(toDelete.getDir())) {
            EventLogger.error("Cannot delete " + toDelete.getDir().getAbsolutePath());
        }
        if (toDelete == currentSave) {
            currentSave = null;
        }
        if (instance != null && instance.careerSave == toDelete) {
            instance = null;
        }
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

    public static CareerSave getCurrentSave() {
        return currentSave;
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

    public static CareerSave createNew(PlayerPerson playerPlayer, double playerGoodness, double aiGoodness) {
//        if (getInstance() != null) throw new RuntimeException("Shouldn't be");
        CareerSave save = new CareerSave(new File(CAREER_DIR, playerPlayer.getPlayerId()));
        try {
            save.create();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        setCurrentSave(save);
        CareerManager cm = new CareerManager(save);
        cm.careerCreationTime = new Date();
        cm.lastSavedVersion = App.VERSION_CODE;
        cm.playerGoodness = playerGoodness;
        cm.aiGoodness = aiGoodness;
        for (PlayerPerson person : DataLoader.getInstance().getAllPlayers()) {
            Career career;
            if (person.getPlayerId().equals(playerPlayer.getPlayerId())) {
                career = Career.createByPerson(person, true, cm);
                cm.humanPlayerCareer = (HumanCareer) career;
            } else {
                career = Career.createByPerson(person, false, cm);
            }
            cm.playerCareers.add(career);
        }
        if (cm.humanPlayerCareer == null) {
            throw new RuntimeException("No human player???");
        }
        cm.cache = new JSONObject();
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
        String validation = JsonChecksum.checksum(jsonObject);
        System.out.println(validation);
        if (jsonObject.has("checksum")) {
            String checksum = jsonObject.getString("checksum");
            if (!checksum.equals(validation)) {
                System.err.println("You hacked your career data!");
            }
        }
        JSONArray rootArr = jsonObject.getJSONArray("careers");
        String time = jsonObject.getString("timestamp");

        Calendar begin;
        if (jsonObject.has("beginTimestamp")) {
            begin = stringToCalendar(jsonObject.getString("beginTimestamp"));
        } else {
            begin = Calendar.getInstance();
            begin.set(DEFAULT_YEAR, DEFAULT_MONTH, DEFAULT_DAY);
        }
        CareerManager careerManager = new CareerManager(careerSave, stringToCalendar(time), begin);
        
        Date careerCreationTime = null;
        if (jsonObject.has("careerCreationTime")) {
            try {
                careerCreationTime = 
                        Util.TIME_FORMAT_SEC.parse(jsonObject.getString("careerCreationTime"));
            } catch (ParseException e) {
                EventLogger.error(e);
            }
        } 
        if (careerCreationTime == null) {
            Path path = careerSave.getDir().toPath();
            try {
                BasicFileAttributes bfa = Files.readAttributes(path, BasicFileAttributes.class);
                careerCreationTime = new Date(bfa.creationTime().toMillis());
            } catch (IOException e) {
                EventLogger.warning(e);
                careerCreationTime = new Date();
            }
        }
        careerManager.careerCreationTime = careerCreationTime;
        
        if (jsonObject.has("version")) {
            careerManager.lastSavedVersion = jsonObject.getInt("version");
        } else {
            careerManager.lastSavedVersion = 39;
        }
        System.out.println("Save file version: " + careerManager.lastSavedVersion);
        
        if (jsonObject.has("playerGoodness")) {
            careerManager.playerGoodness = jsonObject.getDouble("playerGoodness");
        } else {
            careerManager.playerGoodness = 1.0;
        }
        if (jsonObject.has("aiGoodness")) {
            careerManager.aiGoodness = jsonObject.getDouble("aiGoodness");
        } else {
            careerManager.aiGoodness = 1.0;
        }
        
        if (jsonObject.has("settingsHistory")) {
            JSONArray setHis = jsonObject.getJSONArray("settingsHistory");
            for (int i = 0; i < setHis.length(); i++) {
                JSONObject hisObj = setHis.getJSONObject(i);
                careerManager.settingsHistories.add(SettingsHistory.fromJson(hisObj));
            }
        }
        
        AchManager.setCareerInstance(careerSave);
        
        Map<String, PlayerPerson> newPlayers = DataLoader.getInstance().getPlayerPeopleCopy();

        for (int i = 0; i < rootArr.length(); i++) {
            JSONObject personObj = rootArr.getJSONObject(i);
            Career career = Career.fromJson(personObj, careerManager);
            if (career == null) continue;
            careerManager.playerCareers.add(career);
            newPlayers.remove(career.getPlayerPerson().getPlayerId());  // 有的

            if (career instanceof HumanCareer hc) {
                careerManager.humanPlayerCareer = hc;
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
            Career career = Career.createByPerson(newPlayer, false, careerManager);
            // 新球员初始就0分吧
            careerManager.playerCareers.add(career);
        }

        careerManager.updateRanking();
        
        careerManager.cache = DataLoader.loadFromDisk(new File(careerSave.getDir(), CACHE).getAbsolutePath());
        
        careerManager.saveCacheInfo();
        
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

    /**
     * @param newLevel 升到的级数
     * @return 升到这一级加几点。个位数级+2，从10级开始+3，20级+4，等等
     */
    public static int perksOfLevelUp(int newLevel) {
        return BASE_LEVEL_PERK + newLevel / 10;
    }

    public static Map<String, String> readCacheInfo(File infoFile) {
        Map<String, String> res = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(infoFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] spl = line.split("=");
                if (spl.length == 2) {
                    res.put(spl[0].strip(), spl[1].strip());
                }
            }
        } catch (FileNotFoundException e) {
            // do nothing
        } catch (IOException e) {
            EventLogger.error(e);
        }
        return res;
    }
    
    public static void closeInstance() {
        instance = null;
        currentSave = null;
    }

    public InventoryManager getInventory() {
        return inventoryManager;
    }

    private void saveCacheInfo() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(careerSave.getInfoFile()))) {
            String builder = "name=" + humanPlayerCareer.getPlayerPerson().getName() + "\n" +
                    "level=" + humanPlayerCareer.getLevel() + '\n' +
                    "lastModified=" + System.currentTimeMillis() + '\n';

            bw.write(builder);
            bw.newLine();
        } catch (IOException e) {
            EventLogger.error(e);
        }
        careerSave.updateCacheInfo();
    }

    public void simulateMatchesInPastTwoYears() {
        updateRanking();  // 按照能力初始化排名

        Calendar pastTime = Calendar.getInstance();
        pastTime.setTimeInMillis(timestamp.getTimeInMillis());
        pastTime.set(Calendar.YEAR, pastTime.get(Calendar.YEAR) - 2);

        while (pastTime.before(timestamp)) {
            try {
                Championship nextChamp = nextChampionship(pastTime);
                System.out.println("Simulating " + nextChamp.fullName());
                nextChamp.startChampionship(false, false, false);
                while (nextChamp.hasNextRound()) {
                    nextChamp.startNextRound(false);
                }
            } catch (Exception e) {
                System.err.println("Failed to simulate one");
                e.printStackTrace();
            }
        }
        updateRanking();  // 比赛完了最后排下名
        saveToDisk();
    }

    public List<RankedCareer> getRanking(GameRule rule, ChampionshipData.Selection selection) {
        List<RankedCareer> crs = new ArrayList<>();

        if (selection == ChampionshipData.Selection.ALL_CHAMP) {
            if (rule == GameRule.SNOOKER) {
                return snookerChampOfChampsRanking(16);
            } else {
                return new ArrayList<>();
            }
        }

        List<? extends CareerRanker> ranking = getRankingPrivate(rule, selection);
        boolean tierAssigned = false;
        for (int i = 0; i < ranking.size(); i++) {
            CareerRanker careerRanker = ranking.get(i);

            if (careerRanker instanceof CareerRanker.ByAwards byAwd) {
                int rankedAwd;
                if (selection == ChampionshipData.Selection.SINGLE_SEASON) {
                    rankedAwd = byAwd.getOneSeasonAwards();
                } else {
                    rankedAwd = byAwd.getTwoSeasonsAwards();
                }
                crs.add(RankedCareer.createByAwards(i, byAwd.career, rankedAwd, byAwd.getTotalAwards()));
            } else if (careerRanker instanceof CareerRanker.ByTier byTier) {
//                if (!byTier.canHaveTier() || i >= TIER_LIMIT) {
//                    if (!tierAssigned) {
//                        // 第一个没tier的人
//                        int haveTiers = crs.size();
//                        for (int j = 0; j < haveTiers; j++) {
//                            RankedCareer rc = crs.get(j);
//                            int winRateTier = CareerRanker.ByTier.computeTier(j, rc.getWinRateNum(), haveTiers);
//                            rc.setTier(winRateTier);
//                        }
//                        tierAssigned = true;
//                    }
//                }
                RankedCareer rc = RankedCareer.createByTier(i, byTier.career,
                        byTier.getTotalWins(), byTier.getTotalMatches(), byTier.getWinRate());
                rc.setTier(byTier.getTier());
                crs.add(rc);
            }
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
        playerId = playerId.replace('\'', '_');
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
    public boolean humanPlayerQualifiedToJoin(ChampionshipData championshipData,
                                              ChampionshipData.Selection selection) {
        if (!championshipData.isProfessionalOnly()) return true;  // 公开赛，游戏设定让玩家参加
        
        if (championshipData.getType() == GameRule.SNOOKER && selection == ChampionshipData.Selection.ALL_CHAMP) {
            List<RankedCareer> qualifiedPlayers = snookerChampOfChampsRanking(championshipData.getTotalPlaces());
            for (RankedCareer cr : qualifiedPlayers) {
                if (cr.career.isHumanPlayer()) return true;
            }
            return false;
        }

        RankedCareer human = humanPlayerRanking(championshipData.type, selection);
        if (human == null) return false;
        int humanRank = human.rank;
        return humanRank < championshipData.getTotalPlaces();
    }

    /**
     * @return 玩家的排名，从0开始计
     */
    public RankedCareer humanPlayerRanking(GameRule gameRule, ChampionshipData.Selection selection) {
        List<RankedCareer> list = getRanking(gameRule, selection);

        for (RankedCareer cr : list) {
            if (cr.getCareer().isHumanPlayer())
                return cr;
        }
        return null;
    }

    public int getHumanRegistryFee(ChampionshipData data, boolean humanQualified) {
        if (!humanQualified) {
            return data.getRegistryFee();
        }
        List<TourCareer> qualifiedCareers = participants(data, true, true);
        for (TourCareer tourCareer : qualifiedCareers) {
            if (tourCareer.career.isHumanPlayer()) {
                return data.getRegistryFee();
            }
        }
        return data.getRegistryFee();
    }

    /**
     * 前提条件是球员已经有资格参赛了
     */
    public List<TourCareer> participants(ChampionshipData data,
                                         boolean humanJoin, 
                                         boolean humanQualified) {
        if (data.getType() == GameRule.SNOOKER &&
                data.getSelection() == ChampionshipData.Selection.ALL_CHAMP) {
            return snookerChampOfChampParticipants(data.getTotalPlaces(), humanJoin, humanQualified);
        }
        if (data.professionalOnly) {
            return professionalParticipants(data, data.getTotalPlaces(), humanJoin, data.getType(), data.getSelection());
        } else {
            return openParticipants(data, data.getTotalPlaces(), humanJoin, data.getType(), data.getSelection());
        }
    }

    private List<? extends CareerRanker> getRankingPrivate(GameRule type,
                                                           ChampionshipData.Selection selection) {
        switch (type) {
            case SNOOKER:
                if (selection == ChampionshipData.Selection.REGULAR)
                    return snookerRanking;
                else if (selection == ChampionshipData.Selection.SINGLE_SEASON)
                    return snookerRankingSingleSeason;
            case CHINESE_EIGHT:
                return chineseEightRanking;
            case AMERICAN_NINE:
                return americanNineRanking;
            case LIS_EIGHT:
            case MINI_SNOOKER:
            default:
                return new ArrayList<>();
        }
    }

    private int rankOfCareer(List<? extends CareerRanker> rankings,
                             Career career) {
        for (int i = 0; i < rankings.size(); i++) {
            if (rankings.get(i).career == career) return i;  // 从0记
        }
        return Integer.MAX_VALUE;
    }

    private List<RankedCareer> snookerChampOfChampsRanking(int nPlayers) {
        if (snookerRanking.size() < nPlayers) {
            System.err.println("Snooker players not enough");
            return new ArrayList<>();
        }

        class ChampionshipLevel implements Comparable<ChampionshipLevel> {

            final ChampionshipData data;

            ChampionshipLevel(ChampionshipData data) {
                this.data = data;
            }

            @Override
            public int compareTo(@NotNull ChampionshipLevel o) {
                return Integer.compare(this.data.getClassLevel(), o.data.getClassLevel());  // class越小（级别越高）的越在前
            }
        }

        class ChampOfChampRank implements Comparable<ChampOfChampRank> {
            final Career career;
            final List<ChampionshipLevel> champions = new ArrayList<>();
            final int regularRank;

            ChampOfChampRank(Career career, ChampionshipData firstChamp, int regularRank) {
                this.career = career;
                this.regularRank = regularRank;
                this.champions.add(new ChampionshipLevel(firstChamp));
            }

            void addChamp(ChampionshipData data) {
                champions.add(new ChampionshipLevel(data));
            }

            boolean hasClass(int classLevel) {
                for (ChampionshipLevel cl : champions) {
                    if (cl.data.getClassLevel() == classLevel) return true;
                }
                return false;
            }

            int class1Champs() {
                int r = 0;
                for (ChampionshipLevel cl : champions)
                    if (cl.data.getClassLevel() == 1) r++;
                return r;
            }

            @Override
            public int compareTo(@NotNull ChampOfChampRank o) {
                for (int cl = 1; cl <= 4; cl++) {
                    boolean has = hasClass(cl);
                    boolean oHas = o.hasClass(cl);
                    if (has) {
                        if (!oHas) return -1;
                    }
                    if (oHas) {
                        if (!has) return 1;
                    }
                }
                int nChampsCmp = Integer.compare(this.champions.size(), o.champions.size());
                if (nChampsCmp != 0) return -nChampsCmp;  // 多的在前

                return Integer.compare(this.regularRank, o.regularRank);
            }
        }

        Map<String, ChampOfChampRank> playerChampRank = new HashMap<>();
        for (ChampionshipData data : ChampDataManager.getInstance().getChampionshipData()) {
            if (data.getType() == GameRule.SNOOKER && data.getClassLevel() <= 4) {
                Career lastChamp = getDefendingChampion(data);
                if (lastChamp != null) {
                    ChampOfChampRank ccr = playerChampRank.get(lastChamp.getPlayerPerson().getPlayerId());
                    if (ccr == null) {
                        int regularRank = rankOfCareer(snookerRanking, lastChamp);
                        playerChampRank.put(lastChamp.getPlayerPerson().getPlayerId(),
                                new ChampOfChampRank(lastChamp, data, regularRank));
                    } else {
                        ccr.addChamp(data);
                    }
                }
            }
        }
        List<ChampOfChampRank> ccrList = new ArrayList<>(playerChampRank.values());
        Collections.sort(ccrList);

        List<RankedCareer> result = new ArrayList<>();
        for (int i = 0; i < ccrList.size(); i++) {
            ChampOfChampRank ccr = ccrList.get(i);
            result.add(RankedCareer.createByAwards(i, ccr.career, ccr.class1Champs(), ccr.champions.size()));
        }

        if (result.size() < nPlayers) {
            OUT_LOOP:
            for (CareerRanker.ByAwards cwa : snookerRanking) {
                for (RankedCareer cr : result) {
                    if (cr.career == cwa.career) continue OUT_LOOP;
                }
                result.add(RankedCareer.createByAwards(result.size(), cwa.career, 0, 0));
                if (result.size() == nPlayers) break;
            }
        }
        return result;
    }
    
    public List<TourCareer> snookerChampOfChampParticipants(int n,
                                                            boolean humanJoin,
                                                            boolean humanQualified) {
        int realN = humanQualified && !humanJoin ? n + 1 : n;  // human占了位又不来，顺延一位
        List<RankedCareer> champOfChampPlayers = snookerChampOfChampsRanking(realN);
        
        List<TourCareer> result = new ArrayList<>();

        for (RankedCareer cr : champOfChampPlayers) {
            if (cr.career.isHumanPlayer() && !humanJoin) {
                continue;
            }
            result.add(new TourCareer(cr.career, result.size() + 1));
        }
        
        if (result.size() != n) {
            // 就是说human不在这里面
            throw new RuntimeException("Champ of Champ position mismatch!");
        }
        
        return result;
    }

    public List<TourCareer> professionalParticipants(ChampionshipData data,
                                                     int n,
                                                     boolean humanJoin,
                                                     GameRule type,
                                                     ChampionshipData.Selection selection) {
        List<TourCareer> result = new ArrayList<>();

        List<? extends CareerRanker> ranking = getRankingPrivate(type, selection);
        Career defendingChamp = getDefendingChampion(data);
        if (defendingChamp != null) {
            if (!defendingChamp.isHumanPlayer() || humanJoin) {
                TourCareer seed1 = new TourCareer(defendingChamp, 1);
                result.add(seed1);  // 我们认为卫冕冠军一定会参加
            }
        }

        for (int i = 0; i < ranking.size(); i++) {
            CareerRanker cwa = ranking.get(i);
            if (cwa.career.isHumanPlayer() && !humanJoin) continue;
            if (cwa.career == defendingChamp) continue;
            if (cwa.willJoinMatch(data,
                    i,
                    i == 0 ? null : ranking.get(i - 1),
                    i == ranking.size() - 1 ? null : ranking.get(i + 1))) {
                result.add(new TourCareer(cwa.career, result.size() + 1));
                if (result.size() == n) break;
            }
        }
        return result;
    }

    public List<TourCareer> openParticipants(ChampionshipData data,
                                             int n,
                                             boolean humanJoin,
                                             GameRule type,
                                             ChampionshipData.Selection selection) {
        List<TourCareer> result = new ArrayList<>();

        boolean humanAlreadyJoin = false;
        List<? extends CareerRanker> rankings = getRankingPrivate(type, selection);
        Career defendingChamp = getDefendingChampion(data);
        if (defendingChamp != null) {
            if (!defendingChamp.isHumanPlayer() || humanJoin) {
                TourCareer seed1 = new TourCareer(defendingChamp, 1);
                result.add(seed1);  // 我们认为AI卫冕冠军一定会参加
                if (seed1.career.isHumanPlayer()) humanAlreadyJoin = true;
            }
        }

        for (int i = 0; i < rankings.size(); i++) {
            CareerRanker cwa = rankings.get(i);
            if (cwa.career == defendingChamp) continue;
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
                    result.add(new TourCareer(cwa.career, result.size() + 1));

                if (result.size() == n) {
                    break;
                }
            }
        }

        if (result.size() < n) {
            for (int i = 0; i < rankings.size(); i++) {
                CareerRanker cwa = rankings.get(i);
                if (cwa.career == defendingChamp) continue;
                if (cwa.career.getPlayerPerson().isRandom || cwa.career.getPlayerPerson().category.equals("God")) {
                    if (cwa.willJoinMatch(data,
                            i,
                            i == 0 ? null : rankings.get(i - 1),
                            i == rankings.size() - 1 ? null : rankings.get(i + 1))) {
                        result.add(new TourCareer(cwa.career, result.size() + 1));
                        if (result.size() == n) {
                            break;
                        }
                    }
                }
            }
        }
        if (humanJoin && !humanAlreadyJoin) {
            result.remove(result.size() - 1);
            result.add(new TourCareer(humanPlayerCareer, result.size() + 1));
        }
        return result;
    }

    public Calendar getTimestamp() {
        return timestamp;
    }

    public Calendar getBeginTimestamp() {
        return beginTimestamp;
    }

    public ChampDataManager getChampDataManager() {
        return champDataManager;
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

        Championship championship = switch (nextData.getType()) {
            case SNOOKER -> new SnookerChampionship(nextData, timestamp);
            case CHINESE_EIGHT -> new ChineseEightChampionship(nextData, timestamp);
            case AMERICAN_NINE -> new AmericanNineChampionship(nextData, timestamp);
            default -> throw new UnsupportedOperationException();
        };
        inProgress = championship;
        return championship;
    }

    public void updateRanking() {
        snookerRanking.clear();
        for (Career career : playerCareers) {
            if (career.getPlayerPerson().isPlayerOf(GameRule.SNOOKER)) {
                snookerRanking.add(new CareerRanker.ByAwards(GameRule.SNOOKER, career, timestamp));
            }
        }

        snookerRankingSingleSeason.clear();
        snookerRankingSingleSeason.addAll(snookerRanking);

        snookerRanking.sort(CareerRanker.ByAwards::twoSeasonsCompare);
        snookerRankingSingleSeason.sort(CareerRanker.ByAwards::oneSeasonCompare);

        // 黑八两年
        chineseEightRanking.clear();
        for (Career career : playerCareers) {
            if (career.getPlayerPerson().isPlayerOf(GameRule.CHINESE_EIGHT)) {
                chineseEightRanking.add(new CareerRanker.ByTier(GameRule.CHINESE_EIGHT, career));
            }
        }

        chineseEightRanking.sort(CareerRanker.ByTier::roughCompare);
        int haveTiers = (int) chineseEightRanking.stream().filter(CareerRanker.ByTier::canHaveTier).count();
        
        for (int rank = 0; rank < chineseEightRanking.size(); rank++) {
            CareerRanker.ByTier byTier = chineseEightRanking.get(rank);
            if (byTier.canHaveTier()) {
                int winRateTier = CareerRanker.ByTier.computeTier(rank, byTier.getWinRate(), haveTiers);
                winRateTier -= LetBall.magicScore(byTier.career.getPlayerPerson());
                byTier.setTier(winRateTier);
            }
        }
        chineseEightRanking.sort(CareerRanker.ByTier::compareWithTierSet);
        
        // 美式九球两年
        americanNineRanking.clear();
        for (Career career : playerCareers) {
            if (career.getPlayerPerson().isPlayerOf(GameRule.AMERICAN_NINE)) {
                americanNineRanking.add(new CareerRanker.ByAwards(GameRule.AMERICAN_NINE, career, timestamp));
            }
        }

        americanNineRanking.sort(CareerRanker.ByAwards::twoSeasonsCompare);
    }
    
    public void checkRankingAchievements() {
        RankedCareer humanSnooker = humanPlayerRanking(GameRule.SNOOKER, ChampionshipData.Selection.REGULAR);
        
        if (humanSnooker.rank < 64) {
            AchManager.getInstance().addAchievement(Achievement.SNOOKER_TOP_64, null);
            if (humanSnooker.rank < 16) {
                AchManager.getInstance().addAchievement(Achievement.SNOOKER_TOP_16, null);
                if (humanSnooker.rank == 0) {  // 从0开始的
                    AchManager.getInstance().addAchievement(Achievement.SNOOKER_TOP_1, null);
                }
            }
        }
        
        Calendar check = Calendar.getInstance();
        check.setTimeInMillis(beginTimestamp.getTimeInMillis());
        
        // 一年
        check.add(Calendar.YEAR, 1);
        if (check.before(timestamp)) {
            AchManager.getInstance().addAchievement(Achievement.PLAY_ONE_YEAR, null);
            
            // 两年
            check.add(Calendar.YEAR, 1);
            if (check.before(timestamp)) {
                AchManager.getInstance().addAchievement(Achievement.PLAY_TWO_YEARS, null);
                
                // 五年
                check.add(Calendar.YEAR, 3);
                if (check.before(timestamp)) {
                    AchManager.getInstance().addAchievement(Achievement.PLAY_FIVE_YEARS, null);

                    // 十年
                    check.add(Calendar.YEAR, 5);
                    if (check.before(timestamp)) {
                        AchManager.getInstance().addAchievement(Achievement.PLAY_TEN_YEARS, null);

                        // 二十年
                        check.add(Calendar.YEAR, 10);
                        if (check.before(timestamp)) {
                            AchManager.getInstance().addAchievement(Achievement.PLAY_TWENTY_YEARS, null);
                        }
                    }
                }
            }
        }
    }

    public void updateHandFeels() {
        for (Career career : playerCareers) {
            career.updateHandFeel();
        }
    }

    public void updateEfforts() {
        updateEffortByRank(GameRule.SNOOKER);
        updateEffortByRank(GameRule.CHINESE_EIGHT);
        updateEffortByRank(GameRule.AMERICAN_NINE);
//        updateEffortByRank(GameRule.MINI_SNOOKER);
    }

    private List<? extends CareerRanker> getRankOfGame(GameRule rule) {
        return switch (rule) {
            case SNOOKER -> snookerRanking;
            case CHINESE_EIGHT -> chineseEightRanking;
            case AMERICAN_NINE -> americanNineRanking;
//            case MINI_SNOOKER, 
            default -> throw new RuntimeException();
        };
    }

    private void updateEffortByRank(GameRule rule) {
        List<? extends CareerRanker> rnk = getRankOfGame(rule);
//        
        for (CareerRanker cwa : rnk) {
            cwa.career.updateEffort(rule, 1.0);
        }
        CareerRanker top = rnk.get(0);
        CareerRanker second = rnk.get(1);
        if (top.rankedScore() > second.rankedScore() * 1.5) {
            // 放水
            double leakWaterRatio = (double) top.rankedScore() / second.rankedScore() - 0.5;  // 大于1
            leakWaterRatio = leakWaterRatio * 0.5 + 0.5;  // 0.5-1
            top.career.updateEffort(rule, Math.max(1 / leakWaterRatio, 0.7));
            System.out.println("Updated effort of " +
                    top.career.getPlayerPerson().getPlayerId() + ": " + top.career.getEffort(rule));
        }
    }

    public List<CareerRanker.ByAwards> getSnookerTopN(int n) {
        return new ArrayList<>(snookerRanking.subList(0, n));
    }

    public HumanCareer getHumanPlayerCareer() {
        return humanPlayerCareer;
    }

    public void reloadHumanPlayerPerson() {
        humanPlayerCareer.setPlayerPerson(
                DataLoader.getInstance().getPlayerPerson(humanPlayerCareer.getPlayerPerson().getPlayerId()));
    }

    public JSONObject getCache() {
        if (cache == null) cache = new JSONObject();
        return cache;
    }
    
    public void saveCache() {
        DataLoader.saveToDisk(cache, new File(careerSave.getDir(), CACHE).getAbsolutePath());
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

    public TournamentStats tournamentHistory(ChampionshipData data) {
        NavigableMap<Integer, Career> result = new TreeMap<>();
        NavigableSet<SnookerBreakScore> bestBreaks = new TreeSet<>();  // 考虑两个单杆完全相同
        for (Career career : playerCareers) {
            for (ChampionshipScore score : career.getChampionshipScores()) {
                if (score.data == data) {
                    if (Util.arrayContains(score.ranks, ChampionshipScore.Rank.CHAMPION)) {
                        result.put(score.getYear(), career);
                    }
                    if (data.getType().snookerLike()) {
                        SnookerBreakScore breakScore = score.getHighestBreak();
                        if (breakScore != null) {
                            if (bestBreaks.isEmpty()) {
                                bestBreaks.add(breakScore);
                            } else {
                                SnookerBreakScore curBest = bestBreaks.first();
                                int cmp = breakScore.compareTo(curBest);
                                if (cmp < 0) {
                                    bestBreaks.clear();
                                    bestBreaks.add(breakScore);
                                } else if (cmp == 0) {
                                    bestBreaks.add(breakScore);
                                }
                            }
                        }
                    }
                }
            }
        }
        return new TournamentStats(result, bestBreaks);
    }

    public Career getDefendingChampion(ChampionshipData data) {
        NavigableMap<Integer, Career> result = tournamentHistory(data).getHistoricalChampions();
        if (result.isEmpty()) return null;
        return result.lastEntry().getValue();
    }

    public void saveToDisk() {
        lastSavedVersion = App.VERSION_CODE;
        saveCacheInfo();

        File file = new File(careerSave.getDir(), CAREER_JSON);
        File dir = file.getParentFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Cannot create career dir");
            }
        }
        JSONObject root = new JSONObject();
        root.put("version", lastSavedVersion);
        root.put("timestamp", calendarToString(timestamp));
        root.put("beginTimestamp", calendarToString(beginTimestamp));
        root.put("humanPlayer", humanPlayerCareer.getPlayerPerson().getPlayerId());
        root.put("careerCreationTime", Util.TIME_FORMAT_SEC.format(careerCreationTime));

        root.put("playerGoodness", playerGoodness);
        root.put("aiGoodness", aiGoodness);
        
        JSONArray setHis = new JSONArray();
        for (SettingsHistory sh : settingsHistories) {
            setHis.put(sh.toJson());
        }
        root.put("settingsHistory", setHis);

        JSONArray rootArr = new JSONArray();

        for (Career career : playerCareers) {
            JSONObject careerRoot = career.toJsonObject();
            rootArr.put(careerRoot);
        }

        root.put("careers", rootArr);
        
        String checksum = JsonChecksum.checksum(root);
        root.put("checksum", checksum);
        
        String str = root.toString(2);

        try (BufferedWriter bw = new BufferedWriter(
                new FileWriter(file, StandardCharsets.UTF_8))) {
            bw.write(str);
            System.out.println("Saved!");
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }
    
    public void changeDifficulty(double playerGoodness, double aiGoodness) {
        this.playerGoodness = playerGoodness;
        this.aiGoodness = aiGoodness;
    }
    
    public void saveSettings() {
        settingsHistories.add(new SettingsHistory(
                getHumanPlayerCareer().getLevel(), 
                App.VERSION_CODE,
                new Date(),
                playerGoodness,
                aiGoodness
        ));
        saveToDisk();
    }

    public double getAiGoodness() {
        return aiGoodness;
    }

    public double getPlayerGoodness() {
        return playerGoodness;
    }

    public int getLastSavedVersion() {
        return lastSavedVersion;
    }
    
    public static class SettingsHistory {
        
        private final int level;
        private final int version;
        private final Date date;
        private final double playerGoodness, aiGoodness;
        
        SettingsHistory(int level, 
                        int version, 
                        Date date, 
                        double playerGoodness, 
                        double aiGoodness) {
            this.level = level;
            this.version = version;
            this.date = date;
            this.playerGoodness = playerGoodness;
            this.aiGoodness = aiGoodness;
        }
        
        static SettingsHistory fromJson(JSONObject json) {
            int level = json.getInt("level");
            int version = json.getInt("version");
            Date date;
            try {
                date = Util.TIME_FORMAT_SEC.parse(json.getString("date"));
            } catch (ParseException e) {
                date = new Date();
            }
            double playerGoodness = json.getDouble("playerGoodness");
            double aiGoodness = json.getDouble("aiGoodness");
            return new SettingsHistory(
                    level,
                    version,
                    date,
                    playerGoodness,
                    aiGoodness
            );
        }
        
        JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("version", version);
            json.put("date", Util.TIME_FORMAT_SEC.format(date));
            json.put("level", level);
            json.put("playerGoodness", playerGoodness);
            json.put("aiGoodness", aiGoodness);
            return json;
        }
    }
}
