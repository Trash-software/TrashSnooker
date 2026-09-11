package trashsoftware.trashSnooker.core.career.transporation;

import org.json.JSONObject;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.util.*;

public class TransportationManager {
    
    public static final String DEFAULT_SPAWN_CITY_ID = "Chongqing";

    private static TransportationManager instance;

    private final Map<String, City> cities = new HashMap<>();

    private final Map<String, Route> routes = new HashMap<>();
    private final Map<City, List<Route>> graph = new HashMap<>();

    TransportationManager() {
        JSONObject citiesJson = DataLoader.loadFromDisk(DataLoader.CITIES_JSON_PATH).getJSONObject("cities");
        for (String id : citiesJson.keySet()) {
            City city = City.fromJson(
                    id,
                    citiesJson.getJSONObject(id)
            );

            cities.put(id, city);
        }

        JSONObject routesJson = DataLoader.loadFromDisk(DataLoader.ROUTES_JSON_PATH);
        loadGroup(routesJson.optJSONObject("flights"), true);
        loadGroup(routesJson.optJSONObject("trains"), false);
    }

    public static TransportationManager getInstance() {
        if (instance == null) {
            instance = new TransportationManager();
        }
        return instance;
    }

    public String getCityShownName(String cityId) {
        City city = cities.get(cityId);
        return city.getName(ConfigLoader.getInstance().getLocale());
    }

    public List<Route> getRoutes() {
        return new ArrayList<>(routes.values());
    }
    
    public Route getRoutById(String routeId) {
        return routes.get(routeId);
    }

    public Map<String, City> getCities() {
        return cities;
    }

    public List<City> getCityList() {
        List<City> list = new ArrayList<>(cities.values());
        list.sort(Comparator.comparing(City::getCountry).thenComparing(City::getId));
        return list;
    }

    public City getCityById(String cityId) {
        return cities.get(cityId);
    }

    public RouteResult findFastestRoute(City startCity, City endCity) {
        return findRoute(startCity, endCity, Mode.TIME);
    }

    public RouteResult findCheapestRoute(City startCity, City endCity) {
        return findRoute(startCity, endCity, Mode.PRICE);
    }

    public RouteResult findShortestRoute(City startCity, City endCity) {
        return findRoute(startCity, endCity, Mode.DISTANCE);
    }

    private void loadGroup(JSONObject group, boolean isFlight) {
        if (group == null) {
            return;
        }

        for (String id : group.keySet()) {
            Route route = Route.fromJson(
                    id,
                    isFlight,
                    this::getCityById,
                    group.getJSONObject(id)
            );

            routes.put(id, route);

            graph.computeIfAbsent(
                    route.getCity1(),
                    k -> new ArrayList<>()
            ).add(route);

            graph.computeIfAbsent(
                    route.getCity2(),
                    k -> new ArrayList<>()
            ).add(route);
        }
    }

    public RouteResult findRoute(
            City startCity,
            City endCity,
            Mode mode
    ) {
        if (startCity.equals(endCity)) {
            return new RouteResult(
                    startCity,
                    endCity,
                    Collections.emptyList()
            );
        }

        if (!graph.containsKey(startCity)
                || !graph.containsKey(endCity)) {
            return null;
        }

        State start = new State(startCity, null);

        Map<State, Cost> best = new HashMap<>();
        Map<State, Previous> previous = new HashMap<>();

        PriorityQueue<Node> queue =
                new PriorityQueue<>(
                        Comparator.comparing(Node::cost)
                );

        Cost zero = new Cost(0, 0, 0);
        Cost max = new Cost(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        best.put(start, zero);
        queue.add(new Node(start, zero));

        State endState = null;

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            if (current.cost()
                    != best.getOrDefault(
                    current.state(),
                    max
            )) {
                continue;
            }

            if (current.state().city().equals(endCity)) {
                endState = current.state();
                break;
            }

            for (Route route : graph.getOrDefault(
                    current.state().city(),
                    Collections.emptyList()
            )) {
                City nextCity = otherCity(
                        route,
                        current.state().city()
                );

                State nextState = new State(
                        nextCity,
                        route.isFlight()
                );

                Cost oldCost = current.cost();
                Cost edgeCost = routeCost(
                        current.state(),
                        route,
                        mode
                );

                Cost newCost = new Cost(
                        oldCost.primary() + edgeCost.primary(),
                        oldCost.secondary() + edgeCost.secondary(),
                        oldCost.ternary() + edgeCost.ternary()
                );

                Cost known = best.get(nextState);

                if (known == null || newCost.compareTo(known) < 0) {
                    best.put(nextState, newCost);

                    previous.put(
                            nextState,
                            new Previous(
                                    current.state(),
                                    route
                            )
                    );

                    queue.add(
                            new Node(
                                    nextState,
                                    newCost
                            )
                    );
                }
            }
        }

        if (endState == null) {
            return null;
        }

        return new RouteResult(
                startCity,
                endCity,
                buildPath(start, endState, previous)
        );
    }

    private Cost routeCost(
            State current,
            Route route,
            Mode mode
    ) {
        double time = route.getTimeMinutes()
                + transferTime(
                current.previousFlight(),
                route.isFlight());

        int price = route.getEconomyPrice();
        double distance = route.getDistance();
        
        return switch (mode) {
            case TIME -> new Cost(time, distance, price);
            case PRICE -> new Cost(price, time, distance);
            case DISTANCE -> new Cost(distance, time, price);
        };
    }

    private int transferTime(
            Boolean previousFlight,
            boolean nextFlight
    ) {
        if (previousFlight == null) {
            return 0;
        }

        if (!previousFlight && !nextFlight) {
            return 60;
        }

        return 180;
    }

    private List<RouteResult.RouteStep> buildPath(
            State start,
            State end,
            Map<State, Previous> previous
    ) {
        LinkedList<RouteResult.RouteStep> result =
                new LinkedList<>();

        State current = end;

        while (!current.equals(start)) {
            Previous step = previous.get(current);

            if (step == null) {
                return Collections.emptyList();
            }

            result.addFirst(
                    new RouteResult.RouteStep(
                            step.route(),
                            step.state().city(),
                            current.city()
                    )
            );

            current = step.state();
        }

        return result;
    }

    private City otherCity(Route route, City city) {
        if (route.getCity1().equals(city)) {
            return route.getCity2();
        }

        if (route.getCity2().equals(city)) {
            return route.getCity1();
        }

        throw new IllegalArgumentException(
                "City " + city
                        + " is not on route "
                        + route.getId()
        );
    }

    public enum Mode {
        TIME,
        PRICE,
        DISTANCE
    }

    private record State(
            City city,
            Boolean previousFlight
    ) {
    }

    private record Node(
            State state,
            Cost cost
    ) {
    }

    private record Previous(
            State state,
            Route route
    ) {
    }

    public List<RouteResult> findAllFeasibleRoutes(City startCity, City endCity) {
//        double distance = Route.computeDistance(startCity, endCity);
//        double maxRouteDistance = Math.min(40000, (distance + 500) * 2);
//        RouteResult.RoutesTree root = addPathsToAdjacentCities(startCity, endCity, maxRouteDistance, null);
//        System.out.println(root);
        
        RouteResult shortest = findShortestRoute(startCity, endCity);
//        if (shortest == null) return List.of();
        
        double maxRouteDistance = Math.min(40000, shortest.getTotalDistance() * 1.5);
        RouteResult.RoutesTree root = addPathsToAdjacentCities(startCity, endCity, maxRouteDistance, null);

        List<RouteResult> results = new ArrayList<>();
        root.addToRouteResults(results, new ArrayList<>(), graph, endCity);
        return results;
    }

    private RouteResult.RoutesTree addPathsToAdjacentCities(City city,
                                                            City finalDestination,
                                                            double maxRouteDistance,
                                                            RouteResult.RoutesTree parent) {
        RouteResult.RoutesTree node = new RouteResult.RoutesTree(city, parent);
        if (city.equals(finalDestination)) {
            return node;
        }
        
        Set<City> adjacent = findAdjacentCities(city);
        for (City adj : adjacent) {
            if (parent == null || !node.alreadyPassedCity(adj)) {
                double dt = Route.computeDistance(city, adj);
                double cumulativeDt = dt + node.routeDistance();
                if (cumulativeDt <= maxRouteDistance) {
                    node.addChild(addPathsToAdjacentCities(adj, finalDestination, maxRouteDistance, node));
                }
            }
        }
        return node;
    }

    private Set<City> findAdjacentCities(City city) {
        // 用Set以避免既有飞机又有火车的城市线路被加两遍
        Set<City> result = new HashSet<>();
        for (Route route : routes.values()) {
            if (route.getCity1().equals(city)) {
                result.add(route.getCity2());
            } else if (route.getCity2().equals(city)) {
                result.add(route.getCity1());
            }
        }
        return result;
    }

    private record Cost(double primary, double secondary, double ternary)
            implements Comparable<Cost> {

        @Override
        public int compareTo(Cost other) {
            int cmp = Double.compare(primary, other.primary);
            if (cmp != 0) {
                return cmp;
            }

            return Double.compare(secondary, other.secondary);
        }
    }

    static void main() {
        TransportationManager tm = TransportationManager.getInstance();
//        RouteResult cqToDgp1 = tm.findCheapestRoute(tm.getCityById("Chongqing"), tm.getCityById("Doggivepower"));
//        System.out.println(cqToDgp1);

        List<RouteResult> cqToDgp2 = tm.findAllFeasibleRoutes(tm.getCityById("Harbin"), tm.getCityById("Doggivepower"));
        for (RouteResult routeResult : cqToDgp2) {
            System.out.println(routeResult);
        }
    }
}