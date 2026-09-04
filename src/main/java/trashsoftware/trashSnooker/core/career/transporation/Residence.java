package trashsoftware.trashSnooker.core.career.transporation;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.CareerSave;
import trashsoftware.trashSnooker.util.PermanentCounters;

import java.util.Calendar;
import java.util.Map;

public class Residence {
    public static final double DEFAULT_AREA = 50;
    
    private final String id;
    private final City city;
    private final double area;
    private final double unitPrice;
    private final Ownership ownership;

    /**
     * 也能是起租的时间
     */
    private final Calendar purchaseTime;
    
    Residence(String id,
              City city,
              double area,
              double unitPrice,
              Ownership ownership,
              Calendar purchaseTime) {
        this.id = id;
        this.city = city;
        this.area = area;
        this.unitPrice = unitPrice;
        this.ownership = ownership;
        this.purchaseTime = purchaseTime;
    }
    
    public static Residence createInitForCareer(City city, double area, Calendar purchaseTime, CareerSave owner) {
        String instanceId = "residence-" + city.getId() + ":" +
                owner.getPlayerId() + "-" + PermanentCounters.getInstance().nextResidence();
        return new Residence(
                instanceId,
                city,
                area,
                city.getHousePriceM2(),
                Ownership.INITIAL,
                purchaseTime
        );
    }
    
    public static Residence fromJson(JSONObject json, Map<String, City> cityMap) {
        String cityId = json.getString("city");
        City city = cityMap.get(cityId);
        Calendar purchaseTime = CareerManager.stringToCalendar(json.getString("purchaseTime"));
        return new Residence(
                json.getString("id"),
                city,
                json.getDouble("area"),
                json.getDouble("unitPrice"),
                Ownership.valueOf(json.optString("ownership", Ownership.INITIAL.name())),
                purchaseTime
        );
    }

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("city", city.getId());
        object.put("area", area);
        object.put("unitPrice", unitPrice);
        object.put("ownership", ownership.name());
        object.put("purchaseTime", CareerManager.calendarToString(purchaseTime));
        return object;
    }

    public Ownership getOwnership() {
        return ownership;
    }

    public Calendar getPurchaseTime() {
        return purchaseTime;
    }

    public City getCity() {
        return city;
    }

    public double getArea() {
        return area;
    }

    public double getUnitPrice() {
        return unitPrice;
    }
    
    public double getTotalPrice() {
        return area * unitPrice;
    }
    
    public enum Ownership {
        OWN,
        RENT,
        INITIAL
    }
}
