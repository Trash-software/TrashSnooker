package trashsoftware.trashSnooker.core.career.championship;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.Career;
import trashsoftware.trashSnooker.core.career.ChampionshipData;
import trashsoftware.trashSnooker.core.career.ChampionshipScore;
import trashsoftware.trashSnooker.core.career.ChampionshipStage;

import java.util.*;

public class MatchTree {

    private Championship championship;
    private MatchTreeNode root;  // 相当于根是决赛

//    private final List<List<Career>> players = new ArrayList<>();  // 越前面的list排名越高，资格赛轮数越少

    /**
     * @param seedPlayers    种子选手，按排名排序
     * @param nonSeedPlayers 非种子选手，按排名排序
     */
    public MatchTree(Championship championship, List<Career> seedPlayers, List<Career> nonSeedPlayers) {
        this.championship = championship;
        ChampionshipData data = championship.getData();
        if (seedPlayers.size() + nonSeedPlayers.size() != data.getTotalPlaces()) {
            throw new RuntimeException("Expected " + data.getTotalPlaces() + " players, got " +
                    (seedPlayers.size() + nonSeedPlayers.size()));
        }

        List<List<Career>> players = new ArrayList<>();  // 每一轮新加的球员，null表示待定。
        if (!seedPlayers.isEmpty()) {
            // 分上下半区
            List<Career> goodSeeds = new ArrayList<>(seedPlayers.subList(0, seedPlayers.size() / 2));
            List<Career> badSeeds = new ArrayList<>(seedPlayers.subList(seedPlayers.size() / 2, seedPlayers.size()));
            Collections.shuffle(goodSeeds);
            Collections.shuffle(badSeeds);

            List<Career> seedSeq = new ArrayList<>();
            for (int i = 0; i < goodSeeds.size(); i++) {
                seedSeq.add(goodSeeds.get(i));
                seedSeq.add(badSeeds.get(i));
            }

            players.add(seedSeq);
        }

        // fixme: 不对

        // 非种子选手进正赛的名额
        int[] preNewAdd = data.getPreMatchNewAdded();
        if (preNewAdd.length > 0) {
            for (int roundPos : preNewAdd) {
                List<Career> roundPlayers = new ArrayList<>(nonSeedPlayers.subList(0, roundPos));
                nonSeedPlayers = new ArrayList<>(nonSeedPlayers.subList(roundPos, nonSeedPlayers.size()));
                Collections.shuffle(roundPlayers);
                players.add(roundPlayers);
            }
        } else if (!nonSeedPlayers.isEmpty()) {
            // 没有预赛，也没有种子的比赛
            Collections.shuffle(nonSeedPlayers);
            players.add(nonSeedPlayers);
        }

        build(championship, players);
    }

    private MatchTree(MatchTreeNode root, Championship championship) {
        this.root = root;
        this.championship = championship;
    }

    public static MatchTree fromJson(JSONObject jsonObject, Championship championship) {
        JSONObject rootObj = jsonObject.getJSONObject("root");
        MatchTreeNode root = MatchTreeNode.fromJsonObject(rootObj);
        return new MatchTree(root, championship);
    }

    public JSONObject saveProgressToJson() {
        JSONObject saved = new JSONObject();

        saved.put("root", root.saveToJson());

        return saved;
    }

    public boolean isHumanAlive() {
        return root.isHumanAlive();
    }

    public void distributeAwards(ChampionshipData data, 
                                 Calendar timestamp, 
                                 Map<ChampionshipScore.Rank, List<String>> extra) {
//        root.distributeAwards(data, timestamp.get(Calendar.YEAR), 0);
        int year = timestamp.get(Calendar.YEAR);

        SortedMap<ChampionshipScore.Rank, List<Career>> result = new TreeMap<>();
        root.getResults(data, result, 0);

        for (Map.Entry<ChampionshipScore.Rank, List<Career>> entry : result.entrySet()) {
            ChampionshipScore.Rank rank = entry.getKey();
            for (Career career : entry.getValue()) {
                List<ChampionshipScore.Rank> playerRanks = new ArrayList<>();
                playerRanks.add(rank);
                
                for (Map.Entry<ChampionshipScore.Rank, List<String>> extraRank : extra.entrySet()) {
                    for (String pid : extraRank.getValue()) {
                        if (career.getPlayerPerson().getPlayerId().equals(pid)) {
                            playerRanks.add(extraRank.getKey());
                        }
                    }
                }
                
                SnookerBreakScore highestBreak = null;
                if (championship instanceof SnookerChampionship) {
                    SnookerChampionship sc = (SnookerChampionship) championship;
                    
                    // 依旧可以是null
                    highestBreak = sc.personalHighest.get(career.getPlayerPerson().getPlayerId());
                }
                
                // 这里是往career.json里写该球员在本届赛事的单杆最高分
                ChampionshipScore score = new ChampionshipScore(
                        data.getId(),
                        year,
                        playerRanks.toArray(new ChampionshipScore.Rank[0]),
                        highestBreak
                );
                career.addChampionshipScore(score);
            }
        }
    }

    /**
     * 进行一轮的所有AiVsAi比赛，然后返回玩家参与的那一场，或null如果玩家没参赛或者已被淘汰
     */
    PlayerVsAiMatch holdOneRoundMatches(Championship championship,
                                        ChampionshipStage stage) {
        return root.performMatches(championship, stage);
    }

    private void build(Championship championship, List<List<Career>> players) {
        ChampionshipData data = championship.getData();

        // players里是某个阶段新加的球员，越靠前的越靠前
        ChampionshipStage[] stages = data.getStages();

        List<MatchTreeNode> nodes = new ArrayList<>();
        List<Career> firstRound = players.get(players.size() - 1);
        int roundIndex = stages.length - 1;
        for (int i = 0; i < firstRound.size(); i += 2) {
            MatchTreeNode p1 = new MatchTreeNode(firstRound.get(i));
            MatchTreeNode p2 = new MatchTreeNode(firstRound.get(i + 1));
            nodes.add(new MatchTreeNode(p1,
                    p2,
                    stages[roundIndex],
                    championship));
        }
        roundIndex--;

        if (data.getPreMatchNewAdded().length > 0) {
            for (int index = players.size() - 2; index >= 0; index--) {
                List<Career> roundPlayers = players.get(index);
                List<MatchTreeNode> roundNodes = new ArrayList<>();
                int newAdd = roundPlayers.size();
                if (newAdd == 0) {
                    for (int i = 0; i < nodes.size(); i += 2) {
                        MatchTreeNode node = new MatchTreeNode(
                                nodes.get(i),
                                nodes.get(i + 1),
                                stages[roundIndex],
                                championship);
                        roundNodes.add(node);
                    }
                } else if (newAdd == nodes.size()) {
                    for (int i = 0; i < newAdd; i++) {
                        MatchTreeNode newPlayerNode = new MatchTreeNode(roundPlayers.get(i));
                        MatchTreeNode node = new MatchTreeNode(
                                newPlayerNode,
                                nodes.get(i),
                                stages[roundIndex],
                                championship
                        );
                        roundNodes.add(node);
                    }
                } else {
                    throw new RuntimeException("Match node inconsistency");
                }
                nodes = roundNodes;
                roundIndex--;
            }
        }

        // 正赛阶段
        while (nodes.size() > 1) {
            List<MatchTreeNode> roundMatch = new ArrayList<>();
            for (int i = 0; i < nodes.size(); i += 2) {
                roundMatch.add(new MatchTreeNode(nodes.get(i),
                        nodes.get(i + 1),
                        stages[roundIndex],
                        championship));
            }
            nodes = roundMatch;
            roundIndex--;
        }

        if (roundIndex != -1) throw new RuntimeException("Final is round " + roundIndex);

        root = nodes.get(0);
    }

    public MatchTreeNode getRoot() {
        return root;
    }
}
