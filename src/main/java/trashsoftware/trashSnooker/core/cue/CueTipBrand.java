package trashsoftware.trashSnooker.core.cue;

import trashsoftware.trashSnooker.util.DataLoader;

import java.util.ArrayList;
import java.util.List;

public record CueTipBrand(
        String id,
        String shownName,
        double origThickness,
        double maxRadius,
        double minRadius,
        double origGrip,
        double origPower,
        double totalHp,
        int price
) {
    public static CueTipBrand createDefault(double diameter, double thickness) {
        return new CueTipBrand(
                "universalTip",
                "Universal Tip",
                thickness,
                diameter / 2,
                diameter / 2,
                1.0,
                1.0,
                1000,
                50
        );
    }

    public static CueTipBrand createCrossRest() {
        return new CueTipBrand(
                "crossRest",
                "Cross Rest",
                1,
                1,
                1,
                1,
                1,
                1,
                0
        );
    }
    
    public static CueTipBrand getById(String brandId) {
        return DataLoader.getInstance().getTipBrandById(brandId);
    }
    
    public static List<CueTipBrand> listAll() {
        return new ArrayList<>(DataLoader.getInstance().getCueTips().values());
    }
    
    public boolean isAvailableForCue(CueBrand cueBrand) {
        double tipRadius = cueBrand.cueTipWidth / 2;
        return tipRadius >= minRadius && tipRadius <= maxRadius;
    }

    @Override
    public String toString() {
        return shownName;
    }
}
