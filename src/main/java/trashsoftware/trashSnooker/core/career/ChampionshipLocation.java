package trashsoftware.trashSnooker.core.career;

import trashsoftware.trashSnooker.core.career.transporation.City;
import trashsoftware.trashSnooker.core.career.transporation.Country;
import trashsoftware.trashSnooker.core.career.transporation.TransportationManager;

public record ChampionshipLocation(City city, Country taxCountry) {

    public static City getDefaultSpawn() {
        return TransportationManager.getInstance().getCityById(TransportationManager.DEFAULT_SPAWN_CITY_ID);
    }
}
