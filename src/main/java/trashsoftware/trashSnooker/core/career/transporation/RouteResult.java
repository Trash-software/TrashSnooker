package trashsoftware.trashSnooker.core.career.transporation;

import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringJoiner;

public class RouteResult {

    private final City startCity;
    private final City endCity;
    private final List<RouteStep> steps;

    private final int totalTimeMinutes;
    private final int onBoardTimeMinutes;
    private final int totalDistance;

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

        int totalTime = 0;
        int onBoardTime = 0;
        int distance = 0;

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
        return Collections.unmodifiableList(steps);
    }

    public int getTotalTimeMinutes() {
        return totalTimeMinutes;
    }

    public int getOnBoardTimeMinutes() {
        return onBoardTimeMinutes;
    }

    public int getTotalDistance() {
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

    public boolean isEmpty() {
        return steps.isEmpty();
    }
    
    public String toUiString(ResourceBundle strings) {
        StringJoiner journey = new StringJoiner(" -> ");
        journey.add(startCity.getName(strings.getLocale()));

        for (RouteStep step : steps) {
            journey.add(step.toCity().getName(strings.getLocale()));
        }

        return String.format(
                """
                        %s, %s: %d,
                        %s: %s, %s: %s,
                        %s: %d
                        %s: %s: %d
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
                        "distance: %d, prices: economy: %d, business: %d, first: %d",
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

    public static String formatTime(double minutes) {
        int intMinutes = (int) Math.round(minutes);
        return String.format(
                "%02d:%02d",
                intMinutes / 60,
                intMinutes % 60
        );
    }

    public record RouteStep(Route route, City fromCity, City toCity) {

    }
}
