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
    private final Image inventoryIcon;
    private final Image storeIcon;
    private final Image filterIcon;
    private final Image radarIcon;
    private final Image barIcon;
    private final Image lineIcon;
    
    private final WavInfo cueSoundSmallMidPower;
    private final WavInfo cueSoundCarbonMidPower;
    private final WavInfo miscueSoundGeneral;
    
    private final WavInfo[] ballSoundsSnookerLoud = new WavInfo[4];  // 噼里啪啦
    private int ballSoundsSnookerLoudIndex = 0;
    
    private final WavInfo cushionSnooker;
    private final WavInfo pocketBackGeneral;

    private ResourcesLoader() {
        icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("icon.png")),
                0, 0, true, true);
        moneyIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("money.png")),
                0, 0, true, true);
        expIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("exp.png")),
                0, 0, true, true);
        inventoryIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("inventory.png")),
                0, 0, true, true);
        storeIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("store.png")),
                0, 0, true, true);
        filterIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("filter.png")),
                0, 0, true, true);
        radarIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("radar.png")),
                0, 0, true, true);
        barIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("bar-chart.png")),
                0, 0, true, true);
        lineIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("line-chart.png")),
                0, 0, true, true);

        awardIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("ach_gold.png")),
                0, 0, true, true);
        awardGold = new Image(Objects.requireNonNull(getClass().getResourceAsStream("ach_gold.png")),
                0, 0, true, true);
        awardSilver = new Image(Objects.requireNonNull(getClass().getResourceAsStream("ach_silver.png")),
                0, 0, true, true);
        awardBronze = new Image(Objects.requireNonNull(getClass().getResourceAsStream("ach_bronze.png")),
                0, 0, true, true);
        
        cueSoundSmallMidPower = WavInfo.load("data/sound/cue_wood_small_mid.wav");
        miscueSoundGeneral = WavInfo.load("data/sound/cue_wood_small_miscue.wav");
        cueSoundCarbonMidPower = WavInfo.load("data/sound/cue_carbon_mid.wav");
        
        for (int i = 0; i < ballSoundsSnookerLoud.length; i++) {
            ballSoundsSnookerLoud[i] = WavInfo.load("data/sound/ball_snooker_collision.wav");
        }
        
        cushionSnooker = WavInfo.load("data/sound/cushion_snooker.wav", 0.5f);
        pocketBackGeneral = WavInfo.load("data/sound/pocket_back_mid.wav", 1.5f);
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

    public Image getInventoryIcon() {
        return inventoryIcon;
    }

    public Image getStoreIcon() {
        return storeIcon;
    }

    public Image getFilterImage() {
        return filterIcon;
    }

    public Image getRadarIcon() {
        return radarIcon;
    }

    public Image getBarIcon() {
        return barIcon;
    }

    public Image getLineIcon() {
        return lineIcon;
    }

    public void setIconImage1x1(Image image, ImageView imageView) {
        setIconImage1x1(image, imageView, 1.0);
    }

    public void setIconImage1x1(Image image, ImageView imageView, double scale) {
        setIconImage(image, imageView, 1.0, scale);
    }

    public void setIconImage(Image image, ImageView imageView) {
        setIconImage(image, imageView, 1.773, 1.0);
    }

    public void setIconImage(Image image, ImageView imageView, double widthAspect, double scaleMul) {
        imageView.setImage(image);
        imageView.setSmooth(true);

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

    public WavInfo getBallSoundSnookerLoud() {
        return ballSoundsSnookerLoud[ballSoundsSnookerLoudIndex++ % ballSoundsSnookerLoud.length];
    }

    public WavInfo getMiscueSoundGeneral() {
        return miscueSoundGeneral;
    }

    public WavInfo getCueSoundCarbonMidPower() {
        return cueSoundCarbonMidPower;
    }

    public WavInfo getCueSoundSmallMidPower() {
        return cueSoundSmallMidPower;
    }

    public WavInfo getCushionSnooker() {
        return cushionSnooker;
    }

    public WavInfo getPocketBackGeneral() {
        return pocketBackGeneral;
    }
}
