package trashsoftware.trashSnooker.core.career.transporation;

import org.json.JSONObject;

import java.util.*;

public class City {

    private final String id;
    private final Map<String, String> names;
    private final Country country;
    private final String countryId;  // 处理一些未知国家的情况
    private final boolean capital;

    private final int housePriceM2;

    private final double longitude;
    private final double latitude;

    public City(
            String id,
            Map<String, String> names,
            Country country,
            String countryId,
            boolean capital,
            int housePriceM2,
            double longitude,
            double latitude
    ) {
        this.id = id;
        this.names = Map.copyOf(names);
        this.country = country;
        this.countryId = countryId;
        this.capital = capital;
        this.housePriceM2 = housePriceM2;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getNames() {
        return names;
    }

    public String getName(Locale locale) {
        if (!names.isEmpty()) {
            String language = locale.getLanguage();

            if (names.containsKey(language)) {
                return names.get(language);
            }

            return names.values().iterator().next();
        }

        return id;
    }

    public Country getCountry() {
        return country;
    }
    
    public String getCountryDisplay(ResourceBundle strings) {
        if (country == Country.OTHERS) {
            return countryId;
        } else {
            return country.shownName(strings);
        }
    }

    public boolean isCapital() {
        return capital;
    }

    public int getHousePriceM2() {
        return housePriceM2;
    }
    
    public int hotelPricePerDay() {
        return (int) Math.round(housePriceM2 * 0.015);
    }
    
    public int rentalPricePerDay(double m2) {
        return (int) Math.round(housePriceM2 * m2 / 200.0 / 30);
    }
    
    public int livingCostPerDay() {
        return (int) Math.round(Math.pow(housePriceM2, 0.75) * 0.05);
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }
    
    public double[] getLonLat() {
        return new double[]{longitude, latitude};
    }

    public static City fromJson(String id, JSONObject json) {
        JSONObject namesJson = json.getJSONObject("names");
        Map<String, String> names = new HashMap<>();

        for (String language : namesJson.keySet()) {
            names.put(
                    language,
                    namesJson.getString(language)
            );
        }
        
        String countryId = json.getString("country");
        Country country = Country.fromStringKey(countryId);

        return new City(
                id,
                names,
                country,
                countryId,
                json.optBoolean("capital", false),
                json.getInt("housePriceM2"),
                json.getDouble("longitude"),
                json.getDouble("latitude")
        );
    }

    @Override
    public String toString() {
        return getName(Locale.ENGLISH)
                + ", "
                + country
                + ", house price: "
                + housePriceM2
                + "/m²"
                + ", location: "
                + latitude
                + ", "
                + longitude;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof City city)
            return Objects.equals(this.id, city.id);
        return false;
    }
}
