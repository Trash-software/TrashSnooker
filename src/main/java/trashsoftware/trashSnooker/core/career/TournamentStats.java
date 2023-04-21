package trashsoftware.trashSnooker.core.career;

import trashsoftware.trashSnooker.core.career.championship.SnookerBreakScore;

import java.util.NavigableMap;
import java.util.Set;

public class TournamentStats {
    private final NavigableMap<Integer, Career> historicalChampions;
    private final Set<SnookerBreakScore> highestBreak;  // 有可能平手

    TournamentStats(NavigableMap<Integer, Career> historicalChampions, Set<SnookerBreakScore> highestBreak) {
        this.historicalChampions = historicalChampions;
        this.highestBreak = highestBreak;
    }

    public NavigableMap<Integer, Career> getHistoricalChampions() {
        return historicalChampions;
    }

    public Set<SnookerBreakScore> getHighestBreak() {
        return highestBreak;
    }
}
