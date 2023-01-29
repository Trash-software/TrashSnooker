package trashsoftware.trashSnooker.core.career.championship;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.Career;
import trashsoftware.trashSnooker.core.career.ChampionshipData;
import trashsoftware.trashSnooker.core.career.ChampionshipStage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class MatchTree {
    
    private MatchTreeNode root;  // 相当于根是决赛

//    private final List<List<Career>> players = new ArrayList<>();  // 越前面的list排名越高，资格赛轮数越少

    /**
     * @param seedPlayers    种子选手，按排名排序
     * @param nonSeedPlayers 非种子选手，按排名排序
     */
    public MatchTree(ChampionshipData data, List<Career> seedPlayers, List<Career> nonSeedPlayers) {
        Collections.shuffle(seedPlayers);  // todo: 上下半区
        List<List<Career>> players = new ArrayList<>();
        players.add(seedPlayers);

        // 非种子选手进正赛的名额
        int[] preNewAdd = data.getPreMatchNewAdded();
        for (int i = 0; i < preNewAdd.length; i++) {
            int roundPos = preNewAdd[i];
            List<Career> roundPlayers = new ArrayList<>(nonSeedPlayers.subList(0, roundPos));
            nonSeedPlayers = new ArrayList<>(nonSeedPlayers.subList(roundPos, nonSeedPlayers.size()));
            Collections.shuffle(roundPlayers);
            players.add(roundPlayers);
        }
//
//        int roundPos = data.getMainPlaces() - data.getSeedPlaces();
//        while (!nonSeedPlayers.isEmpty()) {
//            List<Career> roundPlayers = new ArrayList<>(nonSeedPlayers.subList(0, roundPos));
//            nonSeedPlayers = new ArrayList<>(nonSeedPlayers.subList(roundPos, nonSeedPlayers.size()));
//            Collections.shuffle(roundPlayers);
//            players.add(roundPlayers);
//            roundPos *= 2;
//        }
        
        build(data, players);
    }
    
    private MatchTree(MatchTreeNode root) {
        this.root = root;
    }
    
    public static MatchTree fromJson(JSONObject jsonObject) {
        JSONObject rootObj = jsonObject.getJSONObject("root");
        MatchTreeNode root = MatchTreeNode.fromJsonObject(rootObj);
        return new MatchTree(root);
    }
    
    public JSONObject saveProgressToJson() {
        JSONObject saved = new JSONObject();

        saved.put("root", root.saveToJson());
        
        return saved;
    }

    public void distributeAwards(ChampionshipData data, Calendar timestamp) {
        root.distributeAwards(data, timestamp.get(Calendar.YEAR), 0);
    }

    /**
     * 进行一轮的所有AiVsAi比赛，然后返回玩家参与的那一场，或null如果玩家没参赛或者已被淘汰
     */
    PlayerVsAiMatch holdOneRoundMatches(ChampionshipData data, 
                                               ChampionshipStage stage) {
        return root.performMatches(data, stage);
    }
    
    private void build(ChampionshipData data, List<List<Career>> players) {
        List<ChampionshipStage> stages = data.getStages();
        
        List<MatchTreeNode> nodes = new ArrayList<>();
        List<Career> firstRound = players.get(players.size() - 1);
        int roundIndex = stages.size() - 1;
        for (int i = 0; i < firstRound.size(); i += 2) {
            MatchTreeNode p1 = new MatchTreeNode(firstRound.get(i));
            MatchTreeNode p2 = new MatchTreeNode(firstRound.get(i + 1));
            nodes.add(new MatchTreeNode(p1, p2, stages.get(roundIndex)));
        }
        roundIndex--;
        
        for (int index = players.size() - 2; index >= 0; index--) {
            List<Career> roundPlayers = players.get(index);
            List<MatchTreeNode> roundNodes = new ArrayList<>();
            int newAdd = roundPlayers.size();
            if (newAdd == 0) {
                for (int i = 0; i < nodes.size(); i += 2) {
                    MatchTreeNode node = new MatchTreeNode(
                            nodes.get(i), 
                            nodes.get(i + 1), 
                            stages.get(roundIndex));
                    roundNodes.add(node);
                }
            } else if (newAdd == nodes.size()) {
                for (int i = 0; i < newAdd; i++) {
                    MatchTreeNode newPlayerNode = new MatchTreeNode(roundPlayers.get(i));
                    MatchTreeNode node = new MatchTreeNode(
                            newPlayerNode,
                            nodes.get(i),
                            stages.get(roundIndex)
                    );
                    roundNodes.add(node);
                }
            } else {
                throw new RuntimeException("Match node inconsistency");
            }
//            for (int i = 0; i < roundPlayers.size(); i++) {
//                MatchTreeNode playerNode = new MatchTreeNode(roundPlayers.get(i));
//                MatchTreeNode roundMatch = new MatchTreeNode(playerNode, nodes.get(i), stages.get(roundIndex));
//                roundNodes.add(roundMatch);
//            }
            nodes = roundNodes;
            roundIndex--;
        }
        
        // 正赛阶段
        while (nodes.size() > 1) {
            List<MatchTreeNode> roundMatch = new ArrayList<>();
            for (int i = 0; i < nodes.size(); i += 2) {
                roundMatch.add(new MatchTreeNode(nodes.get(i), nodes.get(i + 1), stages.get(roundIndex)));
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
