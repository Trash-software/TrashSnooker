package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.TexturedCueBrand;
import trashsoftware.trashSnooker.core.person.PlayerHand;
import trashsoftware.trashSnooker.util.DataLoader;

import java.util.Map;
import java.util.TreeMap;

public class CueModel3D extends CueModel {
    
    public static final int CIRCLE_DIVISION = 32;

    private final Cue cue;
    private final TexturedCueBrand brand;
    private final Map<PlayerHand.CueExtension, CueExtensionModel3D> extensionModels = new TreeMap<>();
    private PlayerHand.CueExtension currentExtension = PlayerHand.CueExtension.NO;
    private Color tipColor;
    private final int conePoly;

    Cylinder ring;
    Cylinder endSeal;
    TipModel tipModel;

    private final Rotate cueAngleRotate = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
    private final Rotate rollRotate = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
    private double scale;
    private double renderingScale;

    public CueModel3D(Cue cue,
                      Color tipColor,
                      double initScale) {
        this(cue, tipColor, CIRCLE_DIVISION, initScale);
    }

    public CueModel3D(Cue cue,
                      Color tipColor,
                      int conePoly,
                      double initScale) {
        this.cue = cue;
        this.tipColor = tipColor;
        this.conePoly = conePoly;
        
        this.brand = (TexturedCueBrand) cue.getBrand();
        
        this.scale = initScale;
        this.renderingScale = initScale;

        build();

        Rotate alignRotate = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);
        alignRotate.setAngle(270);

        rollRotate.setAngle(180);

        getTransforms().addAll(alignRotate, baseRotate, cueAngleRotate, rollRotate);
    }

    public static CueModel3D makeDefault(String cueId, int poly) {
        TexturedCueBrand tcb = (TexturedCueBrand) DataLoader.getTestInstance().getCueById(cueId);
        Cue testCue = Cue.createOneTimeInstance(tcb);
        return new CueModel3D(testCue,
                Color.BLUE,
                poly,
                1.0);
    }

    protected void build() {
        getChildren().clear();

        double tipRadius = cue.getCueTipWidth() / 2 * scale;
//        double tipThickness = cue.cueTipThickness * scale;
        double ringThickness = brand.tipRingThickness * scale;
        double endSealRadius = brand.getSegments().getLast().diameter2() / 2 * scale;
        
        tipModel = CueTipModel.create(cue.getCueTip());
        tipModel.setScale(scale);
        ring = new Cylinder(tipRadius, ringThickness);
        ring.setMaterial(new PhongMaterial(cue.getBrand().tipRingColor));
        endSeal = new Cylinder(endSealRadius, 1);
        endSeal.setMaterial(new PhongMaterial(cue.getBrand().backColor));

//        tip.setTranslateY(-tipThickness / 2);
        ring.setTranslateY(ringThickness / 2);
        
        getChildren().add(tipModel);
        getChildren().add(ring);

        double position = ringThickness;

        for (TexturedCueBrand.Segment segment : brand.getSegments()) {
            for (TexturedCueBrand.GraphicSegment gs : segment.getGraphicSegments()) {
                double len = gs.getLength() * scale;
                TruncateCone cone = new TruncateCone(conePoly,
                        1,
                        (float) (gs.getDiameter1() * 0.5 * scale),
                        (float) (gs.getDiameter2() * 0.5 * scale),
                        (float) len,
                        gs.getMaterial()
                );
                cone.setTranslateY(position);
                getChildren().add(cone);
                position += len;
            }
        }
        endSeal.setTranslateY(position);
        getChildren().add(endSeal);
        
        CueExtensionModel3D ext = extensionModels.get(currentExtension);
        if (currentExtension != PlayerHand.CueExtension.NO && ext != null) {
            if (currentExtension.socketLike) {
                position -= PlayerHand.CueExtension.SOCKET_COVER_LENGTH * scale;
            }
            for (TexturedCueBrand.Segment segment : ext.getSegments()) {
                for (TexturedCueBrand.GraphicSegment gs : segment.getGraphicSegments()) {
                    double len = gs.getLength() * scale;
                    TruncateCone cone = new TruncateCone(conePoly,
                            1,
                            (float) (gs.getDiameter1() * 0.5 * scale),
                            (float) (gs.getDiameter2() * 0.5 * scale),
                            (float) len,
                            gs.getMaterial()
                    );
                    cone.setTranslateY(position);
                    getChildren().add(cone);
                    position += len;
                }
            }
        }
    }

    @Override
    public void setCueAngle(double cueAngleDeg) {
        cueAngleRotate.setAngle(cueAngleDeg);
    }

    @Override
    public void setScale(double scale) {
        if (scale != renderingScale) {
            this.scale = scale;
            build();
            renderingScale = scale;
        }
    }

    @Override
    public void redraw() {
        build();
    }

    @Override
    public void setCueRotation(double rotationDeg) {
        rollRotate.setAngle(rotationDeg);
    }

    @Override
    public void setExtension(PlayerHand.CueExtension extension) {
        if (brand.isRest) {
            if (extension.socketLike) {
                extension = PlayerHand.CueExtension.SOCKET;  // 架杆加双层套筒属实有点没必要
            } else if (extension == PlayerHand.CueExtension.SHORT) {
                extension = PlayerHand.CueExtension.NO;  // 架杆哪来的加长把
            }
        }
        
        boolean changed = currentExtension != extension;
        if (changed) {
            extensionModels.computeIfAbsent(extension, this::createExtensionModel);
            
            currentExtension = extension;
            build();
        }
    }
    
    private CueExtensionModel3D createExtensionModel(PlayerHand.CueExtension cueExtension) {
        if (cueExtension == PlayerHand.CueExtension.SHORT) {
            CueExtensionModel3D cem = new CueExtensionModel3D(cueExtension);
            double thick = brand.getEndWidth();
            TexturedCueBrand.Segment segment = TexturedCueBrand.Segment.fromPureColor(
                    brand.backColor,
                    brand.getExtensionLength(cueExtension),
                    thick,
                    thick
            );
            cem.getSegments().add(segment);
            return cem;
        } else if (cueExtension.socketLike) {
            CueExtensionModel3D cem = new CueExtensionModel3D(cueExtension);
            double singleSocketLen = brand.getExtensionLength(PlayerHand.CueExtension.SOCKET);
            addSegmentToSocket(cem, singleSocketLen, cueExtension == PlayerHand.CueExtension.SOCKET);
            if (cueExtension == PlayerHand.CueExtension.SOCKET_DOUBLE) {
                addSegmentToSocket(cem, singleSocketLen, true);
            }
            return cem;
        }
        
        return null;
    }
    
    private void addSegmentToSocket(CueExtensionModel3D cem, 
                                    double socketLen,
                                    boolean isLast) {
        if (!isLast) socketLen -= PlayerHand.CueExtension.SOCKET_COVER_LENGTH;
        Color socketBaseColor = Color.valueOf("#333333");
        TexturedCueBrand.Segment socketPart = TexturedCueBrand.Segment.fromPureColor(
                socketBaseColor,
                200,
                38.0,
                38.0
        );
        TexturedCueBrand.Segment conePart = TexturedCueBrand.Segment.fromPureColor(
                socketBaseColor,
                30.0,
                38.0,
                25.0
        );
        TexturedCueBrand.Segment screwPart = TexturedCueBrand.Segment.fromPureColor(
                Color.BLUE,
                30.0,
                32.0,
                32.0
        );
        TexturedCueBrand.Segment backPart = TexturedCueBrand.Segment.fromPureColor(
                socketBaseColor,
                isLast ? 300.0 : 300.0 - PlayerHand.CueExtension.SOCKET_COVER_LENGTH,
                29.0,
                29.0
        );

        double remLen = socketLen +
                PlayerHand.CueExtension.SOCKET_COVER_LENGTH -
                socketPart.length() -
                conePart.length() -
                screwPart.length() -
                backPart.length();
        TexturedCueBrand.Segment shenSuoPart = TexturedCueBrand.Segment.fromPureColor(
                socketBaseColor,
                remLen,
                25.0,
                25.0
        );

        cem.getSegments().add(socketPart);
        cem.getSegments().add(conePart);
        cem.getSegments().add(shenSuoPart);
        cem.getSegments().add(screwPart);
        cem.getSegments().add(backPart);
    }
}