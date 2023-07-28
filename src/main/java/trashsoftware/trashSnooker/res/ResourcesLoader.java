package trashsoftware.trashSnooker.res;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import trashsoftware.trashSnooker.fxml.App;

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
