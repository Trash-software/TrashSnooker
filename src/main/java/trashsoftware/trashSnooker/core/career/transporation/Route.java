package trashsoftware.trashSnooker.core.career.transporation;

import org.json.JSONObject;

import java.util.function.Function;

public class Route {
    private final String id;
    private final boolean flight;

    private final City city1;
    private final City city2;
    private final String type;

    private final double timeMinutes;
    private final double distance;

    private final int economyPrice;
    private final int businessPrice;
    private final int firstPrice;

    private Route(
            String id,
            boolean flight,
            City city1,
            City city2,
            String type,
            int economyPrice,
            int businessPrice,
            int firstPrice
    ) {
        this.id = id;
        this.flight = flight;
        this.city1 = city1;
        this.city2 = city2;
        this.type = type;
        this.economyPrice = economyPrice;
        this.businessPrice = businessPrice;
        this.firstPrice = firstPrice;
        
        this.distance = routeDistance(city1, city2, flight, type);
        this.timeMinutes = computeTimeMinutes(this.distance, flight, type);
    }

    public static double routeDistance(City city1, City city2, boolean flight, String type) {
        double straightDt = computeDistance(city1, city2);
        if (flight) return straightDt;
        else {
            if ("high_speed".equals(type)) return straightDt * 1.1;
            else return straightDt * 1.2;
        }
    }
    
    public static double computeDistance(City city1, City city2) {
        double r = 6371;
        double phi1 = Math.toRadians(city1.getLatitude());
        double phi2 = Math.toRadians(city2.getLatitude());
        double dPhi = Math.toRadians(city2.getLatitude() - city1.getLatitude());
        double dLambda = Math.toRadians(city2.getLongitude() - city1.getLongitude());
        double a = 
                Math.pow(Math.sin(dPhi / 2), 2)
                        + Math.cos(phi1) * Math.cos(phi2) * Math.pow(Math.sin(dLambda / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }
    
    public static double computeTimeMinutes(double distance, boolean flight, String type) {
        if (flight) {
            double climbDescend = 500;
            return climbDescend * 0.15 + (distance - climbDescend) * 0.075;
        } else {
            double avgSpeedKmh = switch (type) {
                case "high_speed" -> 230;
                case "international" -> 120;
                default -> 80;
            };
            return distance / avgSpeedKmh * 60;
        }
    }

    public String getId() {
        return id;
    }

    public boolean isFlight() {
        return flight;
    }
    
    public double averageSpeedKmh() {
        return distance / timeMinutes * 60;
    }

    public City getCity1() {
        return city1;
    }

    public City getCity2() {
        return city2;
    }

    public String getType() {
        return type;
    }

    public double getTimeMinutes() {
        return timeMinutes;
    }

    public double getDistance() {
        return distance;
    }

//    public double getComputedDistance() {
//        return computedDistance;
//    }

    public int getEconomyPrice() {
        return economyPrice;
    }

    public int getBusinessPrice() {
        return businessPrice;
    }

    public int getFirstPrice() {
        return firstPrice;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Route route) {
            return id.equals(route.id) && flight == route.flight;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s->%s, %b", city1.getId(), city2.getId(), isFlight());
    }

    public static Route fromJson(String id,
                                 boolean isFlight,
                                 Function<String, City> idToCity,
                                 JSONObject json) {
        JSONObject prices = json.getJSONObject("prices");

        return new Route(
                id,
                isFlight,
                idToCity.apply(json.getString("city1")),
                idToCity.apply(json.getString("city2")),
                json.getString("type"),
//                parseTimeMinutes(json.getString("time")),
//                json.getInt("distance"),
                prices.getInt("economy"),
                prices.getInt("business"),
                prices.getInt("first")
        );
    }

    private static int parseTimeMinutes(String time) {
        String[] parts = time.split(":");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid time format: " + time);
        }

        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);

        if (hours < 0 || minutes < 0 || minutes >= 60) {
            throw new IllegalArgumentException("Invalid time value: " + time);
        }

        return hours * 60 + minutes;
    }

    public int getPriceByClass(RouteResult.SeatClass seatClass) {
        return switch (seatClass) {
            case ECONOMY -> getEconomyPrice();
            case BUSINESS -> getBusinessPrice();
            case FIRST -> getFirstPrice();
        };
    }
}
