package trashsoftware.trashSnooker.core.career.transporation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RouteResult {

    private final City startCity;
    private final City endCity;
    private final List<RouteStep> steps;

    private final double totalTimeMinutes;
    private final double onBoardTimeMinutes;
    private final double totalDistance;

    private final int totalEconomyPrice;
    private final int totalBusinessPrice;
    private final int totalFirstPrice;

    public RouteResult(
            City startCity,
            City endCity,
            List<RouteStep> steps
    ) {
        this.startCity = startCity;
        this.endCity = endCity;
        this.steps = List.copyOf(steps);

        double totalTime = 0;
        double onBoardTime = 0;
        double distance = 0;

        int economy = 0;
        int business = 0;
        int first = 0;

        for (int i = 0; i < steps.size(); i++) {
            Route route = steps.get(i).route();

            onBoardTime += route.getTimeMinutes();
            distance += route.getDistance();

            economy += route.getEconomyPrice();
            business += route.getBusinessPrice();
            first += route.getFirstPrice();

            if (i < steps.size() - 1) {
                Route nextRoute = steps.get(i + 1).route();

                // Train -> Train: 1 hour
                // All other transfers: 3 hours
                totalTime += (!route.isFlight() && !nextRoute.isFlight())
                        ? 60
                        : 180;
            }
        }

        totalTime += onBoardTime;

        this.totalTimeMinutes = totalTime;
        this.onBoardTimeMinutes = onBoardTime;
        this.totalDistance = distance;

        this.totalEconomyPrice = economy;
        this.totalBusinessPrice = business;
        this.totalFirstPrice = first;
    }

    public City getStartCity() {
        return startCity;
    }

    public City getEndCity() {
        return endCity;
    }

    public List<RouteStep> getSteps() {
        return steps;
    }

    public boolean containsRoute(Route route) {
        for (RouteStep rs : steps) {
            if (rs.route.equals(route)) return true;
        }
        return false;
    }

    public double getTotalTimeMinutes() {
        return totalTimeMinutes;
    }

    public double getOnBoardTimeMinutes() {
        return onBoardTimeMinutes;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public int getTotalEconomyPrice() {
        return totalEconomyPrice;
    }

    public int getTotalBusinessPrice() {
        return totalBusinessPrice;
    }

    public int getTotalFirstPrice() {
        return totalFirstPrice;
    }

    public int getTransitCount() {
        return Math.max(0, steps.size() - 1);
    }

    public boolean isAllFlight() {
        for (RouteStep step : steps) {
            if (!step.route.isFlight()) return false;
        }
        return true;
    }

    public boolean isEmpty() {
        return steps.isEmpty();
    }

    public String cityNamesOnUi(ResourceBundle strings) {
        StringJoiner journey = new StringJoiner(" -> ");
        journey.add(startCity.getName(strings.getLocale()));

        for (RouteStep step : steps) {
            journey.add(step.toCity().getName(strings.getLocale()));
        }
        return journey.toString();
    }

    public String toUiString(ResourceBundle strings) {
        String journey = cityNamesOnUi(strings);

        return String.format(
                """
                        %s, %s: %d,
                        %s: %s, %s: %s,
                        %s: %.0f
                        %s:
                        %s: %d
                        %s: %d
                        %s: %d""",
                journey,
                "",
                getTransitCount(),
                strings.getString("routeTotalTime"),
                formatTime(totalTimeMinutes),
                strings.getString("routeOnboardTime"),
                formatTime(onBoardTimeMinutes),
                strings.getString("routeDistance"),
                totalDistance,
                strings.getString("prices"),
                strings.getString("economyClass"),
                totalEconomyPrice,
                strings.getString("businessClass"),
                totalBusinessPrice,
                strings.getString("firstClass"),
                totalFirstPrice
        );
    }

    @Override
    public String toString() {
        StringJoiner journey = new StringJoiner(" -> ");
        journey.add(startCity.getId());

        for (RouteStep step : steps) {
            journey.add(step.toCity().getId());
        }

        return String.format(
                "%s, transits: %d, total time: %s, on board time: %s, " +
                        "distance: %.0f, prices: economy: %d, business: %d, first: %d",
                journey,
                getTransitCount(),
                formatTime(totalTimeMinutes),
                formatTime(onBoardTimeMinutes),
                totalDistance,
                totalEconomyPrice,
                totalBusinessPrice,
                totalFirstPrice
        );
    }

    public boolean equivalent(RouteResult other) {
        if (startCity.equals(other.startCity) && endCity.equals(other.endCity)) {
            if (steps.size() == other.steps.size()) {
                for (int i = 0; i < steps.size(); i++) {
                    if (!steps.get(i).equals(other.steps.get(i))) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public static String formatTime(double minutes) {
        int intMinutes = (int) Math.round(minutes);
        return String.format(
                "%02d:%02d",
                intMinutes / 60,
                intMinutes % 60
        );
    }

    public record RouteStep(Route route, City fromCity, City toCity) {
        @Override
        public @NotNull String toString() {
            return route.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RouteStep(Route route1, City city, City toCity1)) {
                return fromCity.equals(city) && toCity.equals(toCity1) && route.equals(route1);
            }
            return false;
        }
    }

    public static class RoutesTree {
        final City fromCity;
        final RoutesTree parent;
        private final Map<RoutesTree, Double> children = new HashMap<>();  // children, distance

        RoutesTree(@NotNull City fromCity, @Nullable RoutesTree parent) {
            this.fromCity = fromCity;
            this.parent = parent;
        }

        void addChild(RoutesTree child) {
            children.put(child, null);
        }

        double routeDistance() {
            if (parent == null) return 0;

            double dt = parent.children.computeIfAbsent(this, k -> Route.computeDistance(fromCity, parent.fromCity));
            return parent.routeDistance() + dt;
        }

        boolean alreadyPassedCity(City city) {
            if (city == fromCity) return true;
            if (parent == null) return false;
            return parent.alreadyPassedCity(city);
        }

        void addToRouteResults(List<RouteResult> results,
                               List<RouteStep> building,
                               Map<City, List<Route>> graph,
                               City finalDestination) {
            if (children.isEmpty()) {
                if (building.isEmpty()) return;
                City routeEnd = building.getLast().toCity;
                if (routeEnd.equals(finalDestination)) {
                    RouteResult rr = new RouteResult(
                            building.getFirst().fromCity,
                            routeEnd,
                            building);
                    results.add(rr);
                }
            } else {
                for (RoutesTree child : children.keySet()) {
                    // 同两个城市之间可能有飞机/火车两条
                    List<Route> directRoutes = findDirectRouteBetween(fromCity, graph, child.fromCity);
                    for (Route routeToNext : directRoutes) {
                        RouteStep stepToNext = new RouteStep(routeToNext,
                                fromCity, child.fromCity);
                        List<RouteStep> branch = new ArrayList<>(building);
                        branch.add(stepToNext);
                        child.addToRouteResults(results, branch, graph, finalDestination);
                    }
                }
            }
        }

        private List<Route> findDirectRouteBetween(City from, Map<City, List<Route>> graph, City to) {
            List<Route> city1Out = graph.get(fromCity);
            List<Route> results = new ArrayList<>();
            for (Route r : city1Out) {
                if (r.getCity1().equals(from) && r.getCity2().equals(to)) results.add(r);
                else if (r.getCity2().equals(from) && r.getCity1().equals(to)) results.add(r);
            }
            return results;
        }

        @Override
        public String toString() {
            return "RoutesTree{" +
                    "fromCity=" + fromCity.getId() +
                    ", children=" + children +
                    '}';
        }
    }

    public record WithClass(RouteResult route, int classIndex, Calendar date) {
    }
}
