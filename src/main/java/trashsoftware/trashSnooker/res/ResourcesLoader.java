package trashsoftware.trashSnooker.res;

import javafx.scene.image.Image;

import java.util.Objects;

public class ResourcesLoader {
    
    private static ResourcesLoader instance;
    
    private final Image icon;
    private final Image awardIcon;
    private final Image moneyIcon;
    private final Image expIcon;

    private ResourcesLoader() {
        icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("icon.png")));
        awardIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("trophy.png")));
        moneyIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("money.png")));
        expIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("exp.png")));
    }
    
    public static ResourcesLoader getInstance() {
        if (instance == null) {
            instance = new ResourcesLoader();
        }
        return instance;
    }

    public Image getAwardIcon() {
        return awardIcon;
    }

    public Image getIcon() {
        return icon;
    }

    public Image getExpIcon() {
        return expIcon;
    }

    public Image getMoneyIcon() {
        return moneyIcon;
    }
}
