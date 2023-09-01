package trashsoftware.trashSnooker.res;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.EventLogger;

import java.util.Objects;

public class ResourcesLoader {
    
    private static ResourcesLoader instance;
    
    private final Image icon;
    private final Image awardIcon;
    private final Image awardGold;
    private final Image awardSilver;
    private final Image awardBronze;
    private final Image moneyIcon;
    private final Image expIcon;

    private ResourcesLoader() {
        icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("icon.png")));
        moneyIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("money.png")));
        expIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("exp.png")));

        awardIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("ach_gold.png")));
        awardGold = new Image(Objects.requireNonNull(getClass().getResourceAsStream("ach_gold.png")));
        awardSilver = new Image(Objects.requireNonNull(getClass().getResourceAsStream("ach_silver.png")));
        awardBronze = new Image(Objects.requireNonNull(getClass().getResourceAsStream("ach_bronze.png")));
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
    
    public Image getAwardImgByLevel(int nCompleted, int totalLevels) {
        if (totalLevels == 0) return awardGold;  // 那几个奇怪的成就，比如unique defeat
        if (nCompleted == 0) return null;  // not finished
        
        if (totalLevels == 1) {
            return awardGold;
        } else if (totalLevels == 2) {
            if (nCompleted == 1) {
                return awardSilver;
            } else if (nCompleted == 2) {
                return awardGold;
            }
        } else if (totalLevels == 3) {
            if (nCompleted == 1) {
                return awardBronze;
            } else if (nCompleted == 2) {
                return awardSilver;
            } else if (nCompleted == 3) {
                return awardGold;
            }
        }
        EventLogger.error("Award has " + totalLevels + " and has " + nCompleted + " finishes");
        return null;
    }

    public Image getIcon() {
        return icon;
    }

    public Image getExpImg() {
        return expIcon;
    }

    public Image getMoneyImg() {
        return moneyIcon;
    }

    public void setIconImage(Image image, ImageView imageView) {
        setIconImage(image, imageView, 1.773, 1.0);
    }

    public void setIconImage(Image image, ImageView imageView, double widthAspect, double scaleMul) {
        imageView.setImage(image);
        
        double iconHeight = App.FONT.getSize() * 1.2;
        double iconWidth = iconHeight * widthAspect;

        imageView.setSmooth(true);
        imageView.setFitHeight(iconHeight * scaleMul);
        imageView.setFitWidth(iconWidth * scaleMul);
    }
    
    private ImageView createIconImage(Image image) {
        ImageView imageView = new ImageView();
        
        setIconImage(image, imageView);
        
        return imageView;
    }
    
    public ImageView createExpIcon() {
        return createIconImage(getExpImg());
    }
    
    public ImageView createMoneyIcon() {
        return createIconImage(getMoneyImg());
    }
}
