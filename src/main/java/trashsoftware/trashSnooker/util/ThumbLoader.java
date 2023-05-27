package trashsoftware.trashSnooker.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class ThumbLoader {
    
    public static final String THUMB_DIR_PATH = "user/thumbnails";
    public static final File THUMB_DIR = new File(THUMB_DIR_PATH);
    
    private static File createImageFile(String name) {
        createDirIfNotExists();
        
        return new File(THUMB_DIR, name + ".png");
    }
    
    public static void writeThumbnail(WritableImage image, String name) {
        File imgFile = createImageFile(name);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", imgFile);
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }
    
    public static Image loadThumbOf(String name) {
        File file = new File(THUMB_DIR, name + ".png");
        try (FileInputStream fis = new FileInputStream(file)) {
//            BufferedImage bi = ImageIO.read(fis);
            return new Image(fis);
        } catch (FileNotFoundException fnf) {
            createDirIfNotExists();
        } catch (IOException e) {
            EventLogger.error(e);
        }
        return null;
    }
    
    private static void createDirIfNotExists() {
        if (!THUMB_DIR.exists()) {
            if (!THUMB_DIR.mkdirs()) {
                EventLogger.error("Cannot create thumbnail dir");
            }
        }
    }
}
