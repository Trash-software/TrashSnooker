package trashsoftware.trashSnooker.fxml;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import org.controlsfx.control.WorldMapView;
import trashsoftware.trashSnooker.core.career.transporation.City;
import trashsoftware.trashSnooker.core.career.transporation.Route;
import trashsoftware.trashSnooker.core.career.transporation.RouteResult;
import trashsoftware.trashSnooker.core.career.transporation.TransportationManager;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.net.URL;
import java.util.*;

public class GeographicView extends ChildInitializable {

    private static final double MIN_ZOOM = 1.0;
    private static final double MAX_ZOOM = 10.0;
    private static final double ZOOM_STEP = 1.15;

    private static final Color INTERNATIONAL_FLIGHT_COLOR =
            Color.DARKORANGE;

    private static final Color DOMESTIC_FLIGHT_COLOR =
            Color.DODGERBLUE;

    private static final Color TRAIN_COLOR =
            Color.FORESTGREEN;

    @FXML
    StackPane mapViewport;
    @FXML
    WorldMapView worldMapView;
    @FXML
    Pane routeLayer, cityLayer;
    @FXML
    CheckBox internationalFlightsBox, domesticFlightsBox, trainsBox;

    private TransportationManager manager;
    private ResourceBundle strings;

    /*
     * 我们自己的地图状态。
     *
     * 不再使用 WorldMapView.zoomFactor，
     * 否则会和 ControlsFX 内部的 WorldMapViewSkin 冲突。
     */
    private double zoom = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;

    private double dragStartX;
    private double dragStartY;
    private double dragStartPanX;
    private double dragStartPanY;

    private final RouteRenderer straightRouteRenderer =
            new StraightRouteRenderer();
    private final RouteRenderer curvedRouteRenderer =
            new CurveRouteRenderer();

    private boolean routeRedrawPending = false;

    private final Map<WorldMapView.Location, City> locationCityMap =
            new IdentityHashMap<>();

    private final List<CityMarker> cityMarkers =
            new ArrayList<>();

    private final Map<City, CityMarker> cityMarkerMap =
            new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.strings = resources;
    }

    @Override
    public void backAction() {
        super.backAction();
    }

    public void setup() {
        manager = TransportationManager.getInstance();

        setupViewport();
        setupCountryViews();
        setupLocationViews();
        loadCities();

        setCheckboxes();

        /*
         * ControlsFX 自己的 zoom 永远保持 1。
         *
         * 我们缩放 WorldMapView Node 本身，
         * 而不是缩放它内部的地图 Group。
         */
        worldMapView.setZoomFactor(1.0);

        worldMapView.setShowLocations(true);

        setupMapDragging();
        setupMapZoom();

        mapViewport.widthProperty().addListener(
                (obs, oldValue, newValue) ->
                        Platform.runLater(this::resolveLabelOverlaps)
        );

        mapViewport.heightProperty().addListener(
                (obs, oldValue, newValue) ->
                        Platform.runLater(this::resolveLabelOverlaps)
        );

        Platform.runLater(() -> {
            /*
             * WorldMapViewSkin 会给 WorldMapView 自己安装一个 clip。
             * 这里去掉，真正的 viewport clip 由外层 StackPane 提供。
             */
            worldMapView.applyCss();
            worldMapView.setClip(null);

            applyTransform();
            resolveLabelOverlaps();
        });
    }

    private void setCheckboxes() {
        trainsBox.selectedProperty().addListener((_, oldValue, newValue) -> {
            if (oldValue != newValue) scheduleRouteRedraw();
        });
        internationalFlightsBox.selectedProperty().addListener((_, oldValue, newValue) -> {
            if (oldValue != newValue) scheduleRouteRedraw();
        });
        domesticFlightsBox.selectedProperty().addListener((_, oldValue, newValue) -> {
            if (oldValue != newValue) scheduleRouteRedraw();
        });
    }

    private void setupViewport() {
        mapViewport.setStyle(
                "-fx-background-color: #8ecae6;"
        );

        mapViewport.setPickOnBounds(true);

        Rectangle clip = new Rectangle();

        clip.widthProperty().bind(
                mapViewport.widthProperty()
        );

        clip.heightProperty().bind(
                mapViewport.heightProperty()
        );

        mapViewport.setClip(clip);

        worldMapView.setStyle(
                "-fx-background-color: transparent;"
        );
        
        routeLayer.setMouseTransparent(false);
        routeLayer.setPickOnBounds(false);
        cityLayer.setMouseTransparent(false);
        cityLayer.setPickOnBounds(false);

        mapViewport.widthProperty().addListener(
                (obs, oldValue, newValue) -> {
                    Platform.runLater(() -> {
                        resolveLabelOverlaps();
                        redrawRoutes();
                    });
                }
        );

        mapViewport.heightProperty().addListener(
                (obs, oldValue, newValue) -> {
                    Platform.runLater(() -> {
                        resolveLabelOverlaps();
                        redrawRoutes();
                    });
                }
        );
    }

    private void setupCountryViews() {
        /*
         * 禁用国家点击。
         */
        worldMapView.setCountryViewFactory(country -> {
            WorldMapView.CountryView view =
                    new WorldMapView.CountryView(country);
            view.setStyle("-fx-stroke: transparent; -fx-stroke-width: 0;");
            view.setMouseTransparent(true);

            return view;
        });
    }

    private void setupLocationViews() {
        worldMapView.setLocationViewFactory(location -> {
            City city = locationCityMap.get(location);

            CityMarker marker =
                    new CityMarker(
                            city,
                            location.getName()
                    );

            marker.updateZoom(zoom);

            cityMarkers.add(marker);

            cityMarkerMap.put(
                    city,
                    marker
            );

            return marker;
        });
    }

    private void loadCities() {
        Locale locale =
                ConfigLoader.getInstance().getLocale();

        for (City city : manager.getCities().values()) {
            WorldMapView.Location location =
                    new WorldMapView.Location(
                            city.getName(locale),
                            city.getLatitude(),
                            city.getLongitude()
                    );

            locationCityMap.put(
                    location,
                    city
            );

            worldMapView.getLocations().add(
                    location
            );
        }
    }

    // ------------------------------------------------------------
    // Drag
    // ------------------------------------------------------------

    private void setupMapDragging() {

        /*
         * IMPORTANT:
         *
         * 必须使用 EVENT FILTER，而不是 setOnMouseDragged。
         *
         * Filter 属于事件捕获阶段，
         * 会在 WorldMapViewSkin 自己的 handler 之前执行。
         *
         * MOUSE_PRESSED 不 consume：
         * 以后还可以正常点击城市。
         */
        mapViewport.addEventFilter(
                MouseEvent.MOUSE_PRESSED,
                event -> {
                    if (event.getButton()
                            != MouseButton.PRIMARY) {
                        return;
                    }

                    dragStartX = event.getSceneX();
                    dragStartY = event.getSceneY();

                    dragStartPanX = panX;
                    dragStartPanY = panY;
                }
        );

        mapViewport.addEventFilter(
                MouseEvent.MOUSE_DRAGGED,
                event -> {
                    if (!event.isPrimaryButtonDown()) {
                        return;
                    }

                    panX =
                            dragStartPanX
                                    + event.getSceneX()
                                    - dragStartX;

                    panY =
                            dragStartPanY
                                    + event.getSceneY()
                                    - dragStartY;

                    applyTransform();

                    resolveLabelOverlaps();

                    /*
                     * 关键：
                     *
                     * 阻止这个 MOUSE_DRAGGED 到达
                     * WorldMapViewSkin。
                     *
                     * 否则 ControlsFX 会同时修改自己的
                     * group.translateX/Y。
                     */
                    event.consume();
                }
        );

        /*
         * WorldMapView 原生双击会：
         *
         * zoomFactor = 1
         * group.translate = 0
         *
         * 我们不需要这个行为。
         */
        mapViewport.addEventFilter(
                MouseEvent.MOUSE_CLICKED,
                event -> {
                    if (event.getClickCount() >= 2) {
                        event.consume();
                    }
                }
        );
    }

    // ------------------------------------------------------------
    // Zoom
    // ------------------------------------------------------------

    private void setupMapZoom() {

        /*
         * 鼠标滚轮。
         *
         * 同样必须用 filter，让事件不要进入
         * WorldMapViewSkin。
         */
        mapViewport.addEventFilter(
                ScrollEvent.SCROLL,
                event -> {
                    if (event.getDeltaY() == 0.0) {
                        return;
                    }

                    double factor =
                            event.getDeltaY() > 0
                                    ? ZOOM_STEP
                                    : 1.0 / ZOOM_STEP;

                    zoomAtCenter(
                            zoom * factor
                    );

                    event.consume();
                }
        );

        /*
         * 顺便支持触摸板 pinch zoom。
         *
         * 同样不允许 ControlsFX 自己处理。
         */
        mapViewport.addEventFilter(
                ZoomEvent.ZOOM,
                event -> {
                    zoomAtCenter(
                            zoom * event.getZoomFactor()
                    );

                    event.consume();
                }
        );
    }

    /**
     * 以当前 viewport 中心作为缩放中心。
     *
     * 因为现在缩放的是整个 WorldMapView Node，
     * JavaFX 的 scaleX / scaleY 会围绕 Node 中心缩放。
     *
     * 因此只需让当前 pan 同比例变化，
     * 即可保持当前 viewport 中心对应的地图位置不变。
     */
    private void zoomAtCenter(double newZoom) {
        newZoom = Math.clamp(newZoom,
                MIN_ZOOM, MAX_ZOOM);

        if (Math.abs(newZoom - zoom) < 0.00001) {
            return;
        }

        double ratio =
                newZoom / zoom;

        /*
         * 例如：
         *
         * zoom = 2
         * panX = 100
         *
         * zoom -> 4
         *
         * panX 必须 -> 200
         *
         * 当前屏幕中心指向的地理位置才不会变化。
         */
        panX *= ratio;
        panY *= ratio;

        zoom = newZoom;

        applyTransform();
        resolveLabelOverlaps();
    }

    /**
     * 所有地图变换只从这里应用。
     */
    private void applyTransform() {
        worldMapView.setScaleX(zoom);
        worldMapView.setScaleY(zoom);

        worldMapView.setTranslateX(panX);
        worldMapView.setTranslateY(panY);

        /*
         * 城市图标和文字不应该跟着地图变大。
         */
        for (CityMarker marker : cityMarkers) {
            marker.updateZoom(zoom);
        }

        scheduleRouteRedraw();
    }

    private void scheduleRouteRedraw() {
        /*
         * drag / zoom 时可能一帧调用很多次，
         * 合并成下一次 JavaFX pulse 只画一次。
         */
        if (routeRedrawPending) {
            return;
        }

        routeRedrawPending = true;

        Platform.runLater(() -> {
            routeRedrawPending = false;
            redrawRoutes();
        });
    }

    private void redrawRoutes() {
        routeLayer.getChildren().clear();

        for (Route route : manager.getRoutes()) {
            if (!shouldShowRoute(route)) {
                continue;
            }

            City city1 = route.getCity1();

            City city2 = route.getCity2();

            CityMarker marker1 =
                    cityMarkerMap.get(
                            route.getCity1()
                    );

            CityMarker marker2 =
                    cityMarkerMap.get(
                            route.getCity2()
                    );

            if (city1 == null ||
                    city2 == null ||
                    marker1 == null ||
                    marker2 == null) {
                continue;
            }

            /*
             * marker 的 (0,0) 就是城市实际位置。
             *
             * 先转成 scene 坐标，
             * 再转回 routeLayer 的本地坐标。
             */
            Point2D p1 =
                    routeLayer.sceneToLocal(
                            marker1.localToScene(
                                    0,
                                    0
                            )
                    );

            Point2D p2 =
                    routeLayer.sceneToLocal(
                            marker2.localToScene(
                                    0,
                                    0
                            )
                    );

            Color color =
                    getRouteColor(route);

            RouteRenderer renderer;
            if (route.isFlight()) {
                renderer = curvedRouteRenderer;
            } else {
                renderer = straightRouteRenderer;
            }
            List<Node> nodes =
                    renderer.create(
                            route,
                            city1,
                            city2,
                            p1,
                            p2,
                            color
                    );

            routeLayer.getChildren().addAll(
                    nodes
            );
        }
    }

    private Color getRouteColor(Route route) {
        if (!route.isFlight()) {
            return TRAIN_COLOR;
        }

        if ("international".equals(route.getType())) {
            return INTERNATIONAL_FLIGHT_COLOR;
        }

        return DOMESTIC_FLIGHT_COLOR;
    }

    private boolean shouldShowRoute(Route route) {
        if (!route.isFlight()) {
            return trainsBox.isSelected();
        }

        return switch (route.getType()) {
            case "international" -> internationalFlightsBox.isSelected();

            case "domestic" -> domesticFlightsBox.isSelected();

            default -> false;
        };
    }

    // ------------------------------------------------------------
    // Labels
    // ------------------------------------------------------------

    /**
     * 解决城市名重叠。
     *
     * 优先级：
     *
     * capital > normal city
     *
     * 同级发生碰撞时，
     * 按当前遍历顺序保留一个即可。
     */
    private void resolveLabelOverlaps() {
        worldMapView.applyCss();
        worldMapView.layout();

        List<CityMarker> sorted =
                new ArrayList<>(cityMarkers);

        /*
         * capital 在前。
         */
        sorted.sort(
                Comparator.comparing(
                        marker -> !marker.city.isCapital()
                )
        );

        /*
         * 先全部恢复，否则之前隐藏的 label
         * 永远没有机会重新显示。
         */
        for (CityMarker marker : sorted) {
            marker.label.setVisible(true);
        }

        Bounds viewportBounds =
                mapViewport.localToScene(
                        mapViewport.getBoundsInLocal()
                );

        List<Bounds> occupied =
                new ArrayList<>();

        for (CityMarker marker : sorted) {
            Label label = marker.label;

            Bounds bounds =
                    label.localToScene(
                            label.getBoundsInLocal()
                    );

            Bounds padded =
                    new BoundingBox(
                            bounds.getMinX() - 2,
                            bounds.getMinY() - 1,
                            bounds.getWidth() + 4,
                            bounds.getHeight() + 2
                    );

            /*
             * 已经完全离开 viewport 的 label
             * 直接隐藏。
             *
             * 这样它们也不会参与碰撞判断。
             */
            if (!viewportBounds.intersects(padded)) {
                label.setVisible(false);
                continue;
            }

            boolean overlaps =
                    occupied.stream()
                            .anyMatch(
                                    existing ->
                                            existing.intersects(padded)
                            );

            if (overlaps) {
                label.setVisible(false);
            } else {
                occupied.add(padded);
            }
        }
    }

    // ------------------------------------------------------------
    // City marker
    // ------------------------------------------------------------

    private class CityMarker extends Pane {

        private static final double NORMAL_STROKE_WIDTH = 1.5;
        private static final double HIGHLIGHT_STROKE_WIDTH = 2.5;

        private final City city;
        private final Label label;

        private final Circle outer;
        private final Circle inner;
        private final Circle hitCircle;

        private final Scale inverseScale =
                new Scale(
                        1.0,
                        1.0,
                        0.0,
                        0.0
                );

        CityMarker(
                City city,
                String displayName
        ) {
            this.city = city;

            label = new Label(displayName);

            setPrefSize(1, 1);
            setMinSize(1, 1);
            setMaxSize(1, 1);

            getTransforms().add(inverseScale);

            /*
             * 城市外圈。
             */
            outer = new Circle(
                    0,
                    0,
                    4
            );

            outer.setFill(Color.TRANSPARENT);
            outer.setStroke(Color.BLACK);
            outer.setStrokeWidth(
                    NORMAL_STROKE_WIDTH
            );

            /*
             * 首都中心点。
             *
             * 非首都仍创建，但隐藏。
             * 这样 highlight 逻辑不用分两套。
             */
            inner = new Circle(
                    0,
                    0,
                    1.7
            );

            inner.setFill(Color.BLACK);
            inner.setVisible(city.isCapital());

            hitCircle = new Circle(
                    0,
                    0,
                    8
            );

            hitCircle.setFill(Color.TRANSPARENT);
            hitCircle.setStroke(null);

            label.setLayoutX(7);
            label.setLayoutY(-9);

            if (city.isCapital()) {
                label.setStyle("""
                    -fx-font-size: 11px;
                    -fx-font-weight: bold;
                    """);
            } else {
                label.setStyle("""
                    -fx-font-size: 10px;
                    """);
            }

            getChildren().addAll(
                    hitCircle,
                    outer,
                    inner,
                    label
            );

            setupInteraction();
        }

        private void setupInteraction() {
            /*
             * Marker 本身是 1x1，
             * 但我们希望实际的圆和 label 都能被 pick。
             */
            setPickOnBounds(false);

            /*
             * 之前 label 是 mouseTransparent=true。
             *
             * 现在需要 label 也能够触发整个 CityMarker，
             * 所以改成 false。
             */
            label.setMouseTransparent(false);

            setCursor(Cursor.HAND);

            Tooltip tooltip =
                    new Tooltip(
                            createCityTooltipText(city)
                    );

            tooltip.setShowDelay(Duration.ZERO);
            tooltip.setShowDuration(Duration.INDEFINITE);

            /*
             * 安装在整个 marker 上。
             */
            Tooltip.install(this, tooltip);

            setOnMouseEntered(event ->
                    setHighlighted(true)
            );

            setOnMouseExited(event ->
                    setHighlighted(false)
            );

            setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    onCityClicked(city);

                    /*
                     * 不让城市点击继续冒泡到地图。
                     */
                    event.consume();
                }
            });
        }

        private void setHighlighted(boolean highlighted) {
            outer.setStrokeWidth(
                    highlighted
                            ? HIGHLIGHT_STROKE_WIDTH
                            : NORMAL_STROKE_WIDTH
            );

            /*
             * 首都中心点稍微变大。
             */
            if (city.isCapital()) {
                inner.setRadius(
                        highlighted
                                ? 2.2
                                : 1.7
                );
            }

            /*
             * Label hover 时简单强调。
             *
             * 不覆盖原有字体大小/首都粗体，
             * 用 opacity 和 scale 即可。
             */
            label.setOpacity(
                    highlighted
                            ? 1.0
                            : 0.9
            );

            label.setScaleX(
                    highlighted
                            ? 1.08
                            : 1.0
            );

            label.setScaleY(
                    highlighted
                            ? 1.08
                            : 1.0
            );
        }

        void updateZoom(double zoom) {
            double inverse =
                    1.0 / zoom;

            inverseScale.setX(inverse);
            inverseScale.setY(inverse);
        }
    }

    private void onRouteClicked(Route route) {
        System.out.println(
                "Clicked route: " + route.getId()
        );
    }

    private void onCityClicked(City city) {
        System.out.println(
                "Clicked city: " + city.getId()
        );
    }

    private String createCityTooltipText(City city) {
        return city.getId();
    }

    private String createRouteTooltipText(Route route) {
        String transport;

        if (route.isFlight()) {
            transport = "international".equals(route.getType())
                    ? "intlFlight"
                    : "domesticFlight";
        } else {
            transport = "trainTransport";
        }
        
        Locale locale = strings.getLocale();

        return String.format(
                "%s ↔ %s\n%s\n%s: %s\n%s: %.0f km\n%s: %d",
                route.getCity1().getName(locale),
                route.getCity2().getName(locale),
                strings.getString(transport),
                strings.getString("routeTime"),
                RouteResult.formatTime(route.getTimeMinutes()),
                strings.getString("routeDistance"),
                route.getDistance(),
                strings.getString("routeLowPrice"),
                route.getEconomyPrice()
        );
    }

    private interface RouteRenderer {

        List<Node> create(
                Route route,
                City city1,
                City city2,
                Point2D p1,
                Point2D p2,
                Color color
        );
    }

    private abstract class BaseRouteRenderer
            implements RouteRenderer {

        private static final double HIT_WIDTH = 10.0;

        @Override
        public List<Node> create(
                Route route,
                City city1,
                City city2,
                Point2D p1,
                Point2D p2,
                Color color
        ) {
            RouteView routeView =
                    new RouteView(route);

            List<RouteSegment> segments =
                    splitWrappedSegments(
                            city1,
                            city2,
                            p1,
                            p2
                    );

            List<Node> result =
                    new ArrayList<>();

            for (RouteSegment segment : segments) {
                result.add(
                        createInteractiveSegment(
                                routeView,
                                segment,
                                color
                        )
                );
            }

            return result;
        }

        /**
         * 子类只需要决定一个 segment 长什么样。
         */
        protected abstract Shape createShape(
                RouteSegment segment
        );

        private Node createInteractiveSegment(
                RouteView routeView,
                RouteSegment segment,
                Color color
        ) {
            Shape visualShape =
                    createShape(segment);

            visualShape.setFill(
                    Color.TRANSPARENT
            );

            visualShape.setStroke(color);
            visualShape.setStrokeWidth(1.5);
            visualShape.setOpacity(0.75);
            visualShape.setMouseTransparent(true);

            routeView.addVisualShape(
                    visualShape
            );

            /*
             * 创建一份完全相同几何形状的 hit shape。
             */
            Shape hitShape =
                    createShape(segment);

            hitShape.setFill(
                    Color.TRANSPARENT
            );

            hitShape.setStroke(
                    Color.TRANSPARENT
            );

            hitShape.setStrokeWidth(
                    HIT_WIDTH
            );

            Group group =
                    new Group(
                            visualShape,
                            hitShape
                    );

            group.setPickOnBounds(false);
            group.setCursor(Cursor.HAND);

            Tooltip tooltip =
                    new Tooltip(
                            createRouteTooltipText(
                                    routeView.route
                            )
                    );

            tooltip.setShowDelay(Duration.ZERO);
            tooltip.setShowDuration(
                    Duration.INDEFINITE
            );

            Tooltip.install(
                    group,
                    tooltip
            );

            group.setOnMouseEntered(event ->
                    routeView.setHighlighted(true)
            );

            group.setOnMouseExited(event ->
                    routeView.setHighlighted(false)
            );

            group.setOnMouseClicked(event -> {
                if (event.getButton()
                        == MouseButton.PRIMARY) {

                    onRouteClicked(
                            routeView.route
                    );

                    event.consume();
                }
            });

            return group;
        }

        /**
         * 将普通线路返回一个 segment，
         * 跨 ±180° 的线路拆成两个 segment。
         */
        private List<RouteSegment> splitWrappedSegments(
                City city1,
                City city2,
                Point2D p1,
                Point2D p2
        ) {
            double lon1 =
                    city1.getLongitude();

            double lon2 =
                    city2.getLongitude();

            if (Math.abs(lon1 - lon2) <= 180.0) {
                return List.of(
                        new RouteSegment(
                                p1,
                                p2
                        )
                );
            }

            double longitudeDifference =
                    Math.abs(lon1 - lon2);

            double screenDifference =
                    Math.abs(
                            p1.getX()
                                    - p2.getX()
                    );

            if (longitudeDifference < 0.0001) {
                return List.of();
            }

            double pixelsPerDegree =
                    screenDifference
                            / longitudeDifference;

            double worldWidth =
                    pixelsPerDegree
                            * 360.0;

            double worldLeft =
                    p1.getX()
                            - (lon1 + 180.0)
                            * pixelsPerDegree;

            double worldRight =
                    worldLeft
                            + worldWidth;

            double dx =
                    p2.getX()
                            - p1.getX();

            double p2AdjustedX;

            if (dx > worldWidth / 2.0) {
                p2AdjustedX =
                        p2.getX()
                                - worldWidth;
            } else {
                p2AdjustedX =
                        p2.getX()
                                + worldWidth;
            }

            double boundaryX =
                    p2AdjustedX < p1.getX()
                            ? worldLeft
                            : worldRight;

            double t =
                    (boundaryX - p1.getX())
                            /
                            (p2AdjustedX - p1.getX());

            double boundaryY =
                    p1.getY()
                            + (p2.getY() - p1.getY())
                            * t;

            double oppositeBoundaryX =
                    boundaryX == worldLeft
                            ? worldRight
                            : worldLeft;

            return List.of(
                    new RouteSegment(
                            p1,
                            new Point2D(
                                    boundaryX,
                                    boundaryY
                            )
                    ),

                    new RouteSegment(
                            new Point2D(
                                    oppositeBoundaryX,
                                    boundaryY
                            ),
                            p2
                    )
            );
        }
    }

    private class StraightRouteRenderer
            extends BaseRouteRenderer {

        @Override
        protected Shape createShape(
                RouteSegment segment
        ) {
            return new Line(
                    segment.start().getX(),
                    segment.start().getY(),
                    segment.end().getX(),
                    segment.end().getY()
            );
        }
    }

    private class CurveRouteRenderer
            implements RouteRenderer {

        /*
         * 大约每 2° 的球面夹角取一个采样点。
         *
         * 长途航线会自然拥有更多点，
         * 短途航线不会生成大量无意义节点。
         */
        private static final double SAMPLE_ANGLE_DEG = 2.0;

        /*
         * Mercator 在 ±90° 发散。
         * 世界地图一般也不会真正显示到极点。
         */
        private static final double MAX_MERCATOR_LAT = 85.0;

        @Override
        public List<Node> create(
                Route route,
                City city1,
                City city2,
                Point2D p1,
                Point2D p2,
                Color color
        ) {
            RouteView routeView =
                    new RouteView(route);

            Projection projection =
                    createProjection(
                            city1,
                            p1,
                            city2,
                            p2
                    );

            if (projection == null) {
                return List.of();
            }

            List<GeoPoint> greatCircle =
                    createGreatCirclePoints(
                            city1.getLatitude(),
                            city1.getLongitude(),
                            city2.getLatitude(),
                            city2.getLongitude()
                    );

            /*
             * 在 ±180° 处拆成多个 Path。
             *
             * 正常情况下只有 1 或 2 个。
             */
            List<List<GeoPoint>> parts =
                    splitAtDateLine(greatCircle);

            List<Node> result =
                    new ArrayList<>();

            for (List<GeoPoint> part : parts) {
                if (part.size() < 2) {
                    continue;
                }

                Path visualPath =
                        createProjectedPath(
                                part,
                                projection
                        );

                Path hitPath =
                        createProjectedPath(
                                part,
                                projection
                        );

                result.add(
                        createInteractiveShape(
                                routeView,
                                visualPath,
                                hitPath,
                                color
                        )
                );
            }

            return result;
        }

        protected Node createInteractiveShape(
                RouteView routeView,
                Shape visualShape,
                Shape hitShape,
                Color color
        ) {
            /*
             * 线路是开放路径，不应该有 fill。
             *
             * 特别注意：
             * Color.TRANSPARENT 依然会参与 JavaFX pick 检测；
             * null 才表示完全没有填充区域。
             */
            visualShape.setFill(null);
            visualShape.setStroke(color);
            visualShape.setStrokeWidth(1.5);
            visualShape.setOpacity(0.75);
            visualShape.setMouseTransparent(true);

            routeView.addVisualShape(visualShape);

            /*
             * 隐形 hit shape：
             * 只允许 stroke 区域接收鼠标，
             * 不允许 Path 内部区域参与 pick。
             */
            hitShape.setFill(null);
            hitShape.setStroke(Color.TRANSPARENT);
            hitShape.setStrokeWidth(8.0);

            Group group = new Group(
                    visualShape,
                    hitShape
            );

            group.setPickOnBounds(false);
            group.setCursor(Cursor.HAND);

            Tooltip tooltip =
                    new Tooltip(
                            createRouteTooltipText(routeView.route)
                    );

            tooltip.setShowDelay(Duration.ZERO);
            tooltip.setShowDuration(Duration.INDEFINITE);

            Tooltip.install(group, tooltip);

            group.setOnMouseEntered(_ ->
                    routeView.setHighlighted(true)
            );

            group.setOnMouseExited(_ ->
                    routeView.setHighlighted(false)
            );

            group.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    onRouteClicked(routeView.route);
                    event.consume();
                }
            });

            return group;
        }

        /**
         * 使用球面线性插值 SLERP 生成真正的大圆路径。
         */
        private List<GeoPoint> createGreatCirclePoints(
                double lat1,
                double lon1,
                double lat2,
                double lon2
        ) {
            Vector3 a =
                    latLonToVector(lat1, lon1);

            Vector3 b =
                    latLonToVector(lat2, lon2);

            double dot =
                    clamp(
                            a.dot(b),
                            -1.0,
                            1.0
                    );

            double omega =
                    Math.acos(dot);

            if (omega < 1e-10) {
                return List.of(
                        new GeoPoint(lat1, lon1),
                        new GeoPoint(lat2, lon2)
                );
            }

            int segments =
                    Math.max(
                            8,
                            (int) Math.ceil(
                                    Math.toDegrees(omega)
                                            / SAMPLE_ANGLE_DEG
                            )
                    );

            List<GeoPoint> result =
                    new ArrayList<>(segments + 1);

            double sinOmega =
                    Math.sin(omega);

            for (int i = 0; i <= segments; i++) {
                double t =
                        (double) i / segments;

                double k1 =
                        Math.sin((1.0 - t) * omega)
                                / sinOmega;

                double k2 =
                        Math.sin(t * omega)
                                / sinOmega;

                Vector3 v =
                        new Vector3(
                                a.x * k1 + b.x * k2,
                                a.y * k1 + b.y * k2,
                                a.z * k1 + b.z * k2
                        ).normalized();

                result.add(
                        vectorToLatLon(v)
                );
            }

            /*
             * 强制保证首尾完全使用原始坐标。
             * 避免浮点误差导致 marker 端点差几个像素。
             */
            result.set(
                    0,
                    new GeoPoint(lat1, lon1)
            );

            result.set(
                    result.size() - 1,
                    new GeoPoint(lat2, lon2)
            );

            return result;
        }

        private Vector3 latLonToVector(
                double latitude,
                double longitude
        ) {
            double lat =
                    Math.toRadians(latitude);

            double lon =
                    Math.toRadians(longitude);

            double cosLat =
                    Math.cos(lat);

            return new Vector3(
                    cosLat * Math.cos(lon),
                    cosLat * Math.sin(lon),
                    Math.sin(lat)
            );
        }

        private GeoPoint vectorToLatLon(Vector3 v) {
            double latitude =
                    Math.toDegrees(
                            Math.asin(
                                    clamp(v.z, -1.0, 1.0)
                            )
                    );

            double longitude =
                    Math.toDegrees(
                            Math.atan2(v.y, v.x)
                    );

            return new GeoPoint(
                    latitude,
                    longitude
            );
        }

        /**
         * 将大圆路径在国际日期变更线拆开。
         *
         * 例如：
         *
         * Sydney --------> +180
         *
         * -180 --------> Los Angeles
         */
        private List<List<GeoPoint>> splitAtDateLine(
                List<GeoPoint> points
        ) {
            if (points.isEmpty()) {
                return List.of();
            }

            List<List<GeoPoint>> result =
                    new ArrayList<>();

            List<GeoPoint> current =
                    new ArrayList<>();

            current.add(points.get(0));

            for (int i = 1; i < points.size(); i++) {
                GeoPoint a =
                        points.get(i - 1);

                GeoPoint b =
                        points.get(i);

                double lonA = a.longitude();
                double lonB = b.longitude();

                double delta =
                        lonB - lonA;

                if (Math.abs(delta) <= 180.0) {
                    current.add(b);
                    continue;
                }

                /*
                 * 把 b 的 longitude 调整到相邻的“复制世界”，
                 * 从而能连续插值到 ±180°。
                 */
                double adjustedLonB;

                double boundaryA;
                double boundaryB;

                if (delta > 180.0) {
                    /*
                     * 例如：
                     *
                     * -170 -> +170
                     *
                     * 实际最短路线是：
                     *
                     * -170 -> -180 | +180 -> +170
                     */
                    adjustedLonB =
                            lonB - 360.0;

                    boundaryA = -180.0;
                    boundaryB = 180.0;
                } else {
                    /*
                     * +170 -> -170
                     */
                    adjustedLonB =
                            lonB + 360.0;

                    boundaryA = 180.0;
                    boundaryB = -180.0;
                }

                double t =
                        (boundaryA - lonA)
                                /
                                (adjustedLonB - lonA);

                /*
                 * 两个采样点本来已经非常接近，
                 * 所以这里纬度做线性插值即可。
                 *
                 * 真正的大圆几何是在前面的 SLERP 完成的。
                 */
                double boundaryLat =
                        a.latitude()
                                + (b.latitude() - a.latitude())
                                * t;

                current.add(
                        new GeoPoint(
                                boundaryLat,
                                boundaryA
                        )
                );

                result.add(current);

                current =
                        new ArrayList<>();

                current.add(
                        new GeoPoint(
                                boundaryLat,
                                boundaryB
                        )
                );

                current.add(b);
            }

            if (!current.isEmpty()) {
                result.add(current);
            }

            return result;
        }

        private Path createProjectedPath(
                List<GeoPoint> points,
                Projection projection
        ) {
            Path path = new Path();

            boolean first = true;

            for (GeoPoint geo : points) {
                Point2D point =
                        projection.project(
                                geo.latitude(),
                                geo.longitude()
                        );

                if (first) {
                    path.getElements().add(
                            new MoveTo(
                                    point.getX(),
                                    point.getY()
                            )
                    );

                    first = false;
                } else {
                    path.getElements().add(
                            new LineTo(
                                    point.getX(),
                                    point.getY()
                            )
                    );
                }
            }

            return path;
        }

        /**
         * 从当前两个城市在 routeLayer 中的位置，
         * 反推出 ControlsFX Mercator 投影当前的
         * scale / offset。
         *
         * 这样完全兼容当前 zoom + pan。
         */
        private Projection createProjection(
                City city1,
                Point2D p1,
                City city2,
                Point2D p2
        ) {
            double lon1 =
                    city1.getLongitude();

            double lon2 =
                    city2.getLongitude();

            double longitudeDiff =
                    lon2 - lon1;

            /*
             * 绝大多数 route 都不会出现两个城市
             * longitude 完全相同。
             *
             * 但做一个 fallback。
             */
            if (Math.abs(longitudeDiff) < 1e-8) {
                return createProjectionFromCurrentMap(
                        city1,
                        p1
                );
            }

            /*
             * ControlsFX:
             *
             * x = (longitude + 180) * width / 360 + offset
             *
             * 所以屏幕上的 pixelsPerDegree 可以直接
             * 从两个 Location 推出来。
             */
            double pixelsPerDegree =
                    (p2.getX() - p1.getX())
                            / longitudeDiff;

            double worldWidth =
                    pixelsPerDegree * 360.0;

            double worldLeft =
                    p1.getX()
                            - (lon1 + 180.0)
                            * pixelsPerDegree;

            /*
             * Mercator:
             *
             * y = offsetY
             *     - width/(2π)
             *       * ln(tan(π/4 + lat/2))
             */
            double mercatorScale =
                    worldWidth
                            / (2.0 * Math.PI);

            double mercator1 =
                    mercatorY(
                            city1.getLatitude()
                    );

            double yCenter =
                    p1.getY()
                            + mercatorScale
                            * mercator1;

            return new Projection(
                    worldLeft,
                    pixelsPerDegree,
                    yCenter,
                    mercatorScale
            );
        }

        /**
         * 极少数同经度线路的 fallback。
         *
         * 可以利用当前 worldMapView 的宽度与 zoom。
         */
        private Projection createProjectionFromCurrentMap(
                City city,
                Point2D point
        ) {
            /*
             * 更推荐这里用现有任意两个不同经度的 CityMarker
             * 来校准。
             *
             * 当前简单 fallback 使用 WorldMapView 的实际宽度。
             */
            double worldWidth =
                    worldMapView.getWidth() * zoom;

            double pixelsPerDegree =
                    worldWidth / 360.0;

            double worldLeft =
                    point.getX()
                            - (city.getLongitude() + 180.0)
                            * pixelsPerDegree;

            double mercatorScale =
                    worldWidth
                            / (2.0 * Math.PI);

            double yCenter =
                    point.getY()
                            + mercatorScale
                            * mercatorY(
                            city.getLatitude()
                    );

            return new Projection(
                    worldLeft,
                    pixelsPerDegree,
                    yCenter,
                    mercatorScale
            );
        }

        private double mercatorY(double latitude) {
            double lat =
                    clamp(
                            latitude,
                            -MAX_MERCATOR_LAT,
                            MAX_MERCATOR_LAT
                    );

            double radians =
                    Math.toRadians(lat);

            return Math.log(
                    Math.tan(
                            Math.PI / 4.0
                                    + radians / 2.0
                    )
            );
        }

        private double clamp(
                double value,
                double min,
                double max
        ) {
            return Math.max(
                    min,
                    Math.min(max, value)
            );
        }

        private record GeoPoint(
                double latitude,
                double longitude
        ) {
        }

        private record Projection(
                double worldLeft,
                double pixelsPerDegree,
                double yCenter,
                double mercatorScale
        ) {

            Point2D project(
                    double latitude,
                    double longitude
            ) {
                double x =
                        worldLeft
                                + (longitude + 180.0)
                                * pixelsPerDegree;

                double lat =
                        Math.clamp(
                                latitude
                                ,
                                -MAX_MERCATOR_LAT,
                                MAX_MERCATOR_LAT);

                double latRad =
                        Math.toRadians(lat);

                double mercator =
                        Math.log(
                                Math.tan(
                                        Math.PI / 4.0
                                                + latRad / 2.0
                                )
                        );

                double y =
                        yCenter
                                - mercatorScale
                                * mercator;

                return new Point2D(
                        x,
                        y
                );
            }
        }

        private record Vector3(
                double x,
                double y,
                double z
        ) {

            double dot(Vector3 other) {
                return x * other.x
                        + y * other.y
                        + z * other.z;
            }

            Vector3 normalized() {
                double length =
                        Math.sqrt(
                                x * x
                                        + y * y
                                        + z * z
                        );

                if (length < 1e-12) {
                    return this;
                }

                return new Vector3(
                        x / length,
                        y / length,
                        z / length
                );
            }
        }
    }

    private record RouteSegment(
            Point2D start,
            Point2D end
    ) {
    }

    private static class RouteView {

        private static final double NORMAL_WIDTH = 1.5;
        private static final double HIGHLIGHT_WIDTH = 3.0;

        private final Route route;
        private final List<Shape> visualShapes = new ArrayList<>();

        RouteView(Route route) {
            this.route = route;
        }

        void addVisualShape(Shape shape) {
            visualShapes.add(shape);
        }

        void setHighlighted(boolean highlighted) {
            for (Shape shape : visualShapes) {
                shape.setStrokeWidth(
                        highlighted
                                ? HIGHLIGHT_WIDTH
                                : NORMAL_WIDTH
                );

                shape.setOpacity(
                        highlighted
                                ? 1.0
                                : 0.75
                );
            }
        }
    }
}