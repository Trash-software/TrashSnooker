package trashsoftware.trashSnooker.fxml;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.*;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.WorldMapView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.ChampionshipData;
import trashsoftware.trashSnooker.core.career.HumanCareer;
import trashsoftware.trashSnooker.core.career.transporation.*;
import trashsoftware.trashSnooker.fxml.alert.AlertShower;
import trashsoftware.trashSnooker.fxml.widgets.LabelTable;
import trashsoftware.trashSnooker.fxml.widgets.LabelTableColumn;
import trashsoftware.trashSnooker.res.ResourcesLoader;
import trashsoftware.trashSnooker.util.Util;

import java.net.URL;
import java.util.*;

public class GeographicView extends ChildInitializable {

    private static final double MIN_ZOOM = 1.0;
    private static final double MAX_ZOOM = 10.0;
    private static final double ZOOM_STEP = 1.15;

    private static final Color INTERNATIONAL_FLIGHT_COLOR =
            Color.DARKORANGE;

    private static final Color DOMESTIC_FLIGHT_COLOR =
            Color.DARKGOLDENROD;

    private static final Color TRAIN_COLOR =
            Color.FORESTGREEN;

    @FXML
    VBox controlsBox;
    @FXML
    StackPane mapViewport;
    @FXML
    WorldMapView worldMapView;
    @FXML
    Pane routeLayer, cityLayer;
    @FXML
    CheckBox internationalFlightsBox, domesticFlightsBox, trainsBox, residencesBox;
    @FXML
    ComboBox<City> departureBox, destinationBox;
    @FXML
    ComboBox<RouteResultSort> resultSortBox;
    @FXML
    ListView<RouteResult> searchResultsView;
    @FXML
    CheckBox directOnlyBox, flightsOnlyBox;
    @FXML
    Label currentDateLabel, currentLocationLabel, nextChampLocationLabel, nextChampDateLabel, nextChampLateDepartureLabel;

    private TransportationManager manager;
    private CareerManager careerManager;
    private @Nullable ChampionshipData.WithYear nextChampionship;
    private ResourceBundle strings;
    private Stage stage;
    private CareerView careerView;

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
    private boolean dragging;

    private final StraightRouteRenderer straightRouteRenderer =
            new StraightRouteRenderer();
    private final CurveRouteRenderer curvedRouteRenderer =
            new CurveRouteRenderer();

    private boolean overlayUpdatePending = false;

    private final Map<WorldMapView.Location, City> locationCityMap =
            new IdentityHashMap<>();

    private final List<CityMarker> cityMarkers =
            new ArrayList<>();

    private final Map<City, CityMarker> cityMarkerMap =
            new HashMap<>();

    private final Map<City, Node> cityAnchorMap =
            new HashMap<>();

    private final Map<String, Node> locationAnchors =
            new HashMap<>();

    private RouteSearchResult routeSearchResult;
    private PopOver cityPopOver;
    private PopOver ticketPopOver;
    private AnimationTimer travelAnimation;
    private TravelAnimationPlayer travelAnimationPlayer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.strings = resources;
    }

    @Override
    public void backAction() {
        if (careerView != null) careerView.refreshGui();
        super.backAction();
    }

    public void setup(Stage stage,
                      @Nullable CareerView careerView,
                      @Nullable CareerManager careerManager) {
        this.stage = stage;
        this.careerView = careerView;
        manager = TransportationManager.getInstance();
        this.careerManager = careerManager;
        
        if (careerManager == null) {
            residencesBox.setVisible(false);
            residencesBox.setManaged(false);
        }

        setupViewport();
        setupCountryViews();
        setupLocationViews();
        loadCities();

        setupControls();

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

    void searchRouteAction() {
        City a = departureBox.getValue();
        City b = destinationBox.getValue();
        if (a == null || b == null || a.equals(b)) {
            routeSearchResult = null;
            updateRouteResultList();
            System.out.println("Cannot");
            return;
        }
        List<RouteResult> allRoutes = manager.findAllFeasibleRoutes(a, b);
        routeSearchResult = new RouteSearchResult(allRoutes);
        updateRouteResultList();
    }

    public void setNextChampionship(Calendar current, City location,
                                    @Nullable ChampionshipData.WithYear next) {
        nextChampionship = next;
        City to = next == null ? null : next.data.getLocation().city();
        setSearchCities(location, to);

        currentDateLabel.setText(CareerManager.calendarToString(current));

        boolean hasNext = nextChampionship != null;
        nextChampDateLabel.setVisible(hasNext);
        nextChampDateLabel.setManaged(hasNext);
        nextChampLocationLabel.setVisible(hasNext);
        nextChampLocationLabel.setManaged(hasNext);
        nextChampLateDepartureLabel.setVisible(hasNext);
        nextChampLateDepartureLabel.setManaged(hasNext);

        if (nextChampionship != null) {
            Calendar[] se = nextChampionship.toCalendarStartEndInclusive();
            nextChampDateLabel.setText(CareerManager.calendarDurationToString(se[0], se[1]));
            nextChampLocationLabel.setText(nextChampionship.data.getLocation().city().getName(strings.getLocale()));
        }
    }

    public void setSearchCities(City from, @Nullable City to) {
        Platform.runLater(() -> {
            departureBox.getSelectionModel().select(from);
            if (to != null) {
                destinationBox.getSelectionModel().select(to);
            }
            searchRouteAction();
            if (!searchResultsView.getItems().isEmpty()) {
                searchResultsView.getSelectionModel().select(0);
            }

            CityMarker marker =
                    cityMarkerMap.get(from);

            if (marker != null) {
                marker.showLocationBubble(true);
            }

            currentLocationLabel.setText(String.format(strings.getString("currentLocationFmt"),
                    from.getName(strings.getLocale())));
        });
    }

    private void updateRequiredDepartureTime() {
        if (nextChampionship != null) {
            RouteResult first = searchResultsView.getSelectionModel().getSelectedItem();
            if (first != null) {
                Calendar latestDeparture = nextChampionship.latestDeparture(first);
                nextChampLateDepartureLabel.setText(String.format(
                        strings.getString("latestDepartureTimeFmt"),
                        CareerManager.calendarToString(latestDeparture)));
            }
        }
    }

    private void updateRouteResultList() {
        searchResultsView.getItems().clear();
        if (routeSearchResult == null || routeSearchResult.routeResults.isEmpty()) {
            return;
        }
        routeSearchResult.filterAndSort(
                directOnlyBox.isSelected(),
                flightsOnlyBox.isSelected(),
                resultSortBox.getValue());

        searchResultsView.getItems().addAll(routeSearchResult.shownResults);
    }

    private void setupControls() {
        trainsBox.selectedProperty().addListener((_, oldValue, newValue) -> {
            if (oldValue != newValue) scheduleOverlayUpdate();
        });
        internationalFlightsBox.selectedProperty().addListener((_, oldValue, newValue) -> {
            if (oldValue != newValue) scheduleOverlayUpdate();
        });
        domesticFlightsBox.selectedProperty().addListener((_, oldValue, newValue) -> {
            if (oldValue != newValue) scheduleOverlayUpdate();
        });
        residencesBox.selectedProperty().addListener((_, oldValue, newValue) -> {
            if (oldValue != newValue) showResidencesBubbles(newValue); 
        });

        setCityBoxFactory(departureBox);
        setCityBoxFactory(destinationBox);

        List<City> allCities = manager.getCityList();
        departureBox.getItems().addAll(allCities);
        destinationBox.getItems().addAll(allCities);

        directOnlyBox.selectedProperty().addListener(((_, oldValue, newValue) -> {
            if (oldValue != newValue) {
                updateRouteResultList();
            }
        }));
        flightsOnlyBox.selectedProperty().addListener(((_, oldValue, newValue) -> {
            if (oldValue != newValue) {
                updateRouteResultList();
            }
        }));
        resultSortBox.getSelectionModel().selectedItemProperty().addListener(((_, oldValue, newValue) -> {
            if (newValue != null && newValue != oldValue) {
                updateRouteResultList();
            }
        }));

        resultSortBox.getItems().addAll(RouteResultSort.values());
        resultSortBox.getSelectionModel().select(0);

        searchResultsView.setCellFactory(_ -> new RouteResultListCell());
        searchResultsView.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if (routeSearchResult != null) {
                routeSearchResult.selected = newValue;
                scheduleOverlayUpdate();
                updateRequiredDepartureTime();
            }
        }));
    }

    private void setCityBoxFactory(ComboBox<City> comboBox) {
        comboBox.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(City item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName(strings.getLocale()) + ", " + item.getCountryDisplay(strings));
                }
            }
        });

        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(City item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName(strings.getLocale()) + ", " + item.getCountryDisplay(strings));
                }
            }
        });

        comboBox.getSelectionModel().selectedItemProperty().addListener((_, _, _) -> {
            searchRouteAction();
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

            Pane anchor = new Pane();

            anchor.setPrefSize(1, 1);
            anchor.setMinSize(1, 1);
            anchor.setMaxSize(1, 1);

            anchor.setMouseTransparent(true);

            locationAnchors.put(
                    location.getName(),
                    anchor
            );

            cityAnchorMap.put(
                    city,
                    anchor
            );

            return anchor;
        });
    }

    private void loadCities() {
        Locale locale = strings.getLocale();

        for (City city : manager.getCities().values()) {
            WorldMapView.Location location =
                    new WorldMapView.Location(
                            city.getId(),
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

            CityMarker marker =
                    new CityMarker(
                            city,
                            city.getName(locale)
                    );

            cityMarkers.add(marker);

            cityMarkerMap.put(
                    city,
                    marker
            );

            cityLayer.getChildren().add(
                    marker
            );
        }

        worldMapView.setShowLocations(true);

        Platform.runLater(
                this::updateCityPositions
        );
    }

    private void updateCityPositions() {
        for (City city : manager.getCities().values()) {
            Node anchor =
                    locationAnchors.get(
                            city.getId()
                    );

            CityMarker marker =
                    cityMarkerMap.get(
                            city
                    );

            if (anchor == null || marker == null) {
                continue;
            }

            /*
             * anchor 的 (0,0) 就是 ControlsFX 算出来的城市位置。
             */
            Point2D scenePoint =
                    anchor.localToScene(
                            0,
                            0
                    );

            Point2D localPoint =
                    cityLayer.sceneToLocal(
                            scenePoint
                    );

            marker.relocate(
                    localPoint.getX(),
                    localPoint.getY()
            );
        }
    }
    
    private void showResidencesBubbles(boolean show) {
        if (!show) {
            for (CityMarker cm : cityMarkers) {
                cm.showHousesBubble(false);
            }
        } else {
            if (careerManager != null) {
                for (Residence residence : careerManager.getInventory().getResidences()) {
                    if (residence.getOwnership().available) {
                        CityMarker cm = cityMarkerMap.get(residence.getCity());
                        if (cm != null) {
                            cm.showHousesBubble(true);
                        }
                    }
                }
            }
        }
    }

    private void travelTo(RouteResult.Ticket ticket) {
        careerManager.getHumanPlayerCareer().payTravelFees(ticket);

        startTravelling(ticket);
    }

    private void startTravelling(RouteResult.Ticket ticket) {
        controlsBox.setDisable(true);

        CityMarker marker =
                cityMarkerMap.get(careerManager.getHumanPlayerCareer().getCurrentLocation());

        if (marker != null) {
            // 消掉当前的标
            marker.showLocationBubble(false);
        }

        travelAnimationPlayer = new TravelAnimationPlayer(ticket,
                ticket.route(),
                ticket.date(),
                1.0);
        routeLayer.getChildren().add(travelAnimationPlayer.movingGraphics);
        careerManager.getHumanPlayerCareer().startTravelling();
        travelAnimation = new AnimationTimer() {
            @Override
            public void handle(long now) {
                boolean hasNext = travelAnimationPlayer.oneFrame();
                if (!hasNext) {
                    endTravelling();
                    return;
                }

                Calendar lastDate = careerManager.getTimestamp();
                Calendar newDate = travelAnimationPlayer.getDate();
                if (!lastDate.equals(newDate)) {
                    careerManager.pushDateTo(newDate);
                }
                currentDateLabel.setText(CareerManager.calendarToString(careerManager.getTimestamp()));

                Point2D mapPoint = travelAnimationPlayer.getPoint();
                if (mapPoint == null) return;
                travelAnimationPlayer.movingGraphics.setTranslateX(mapPoint.getX());
                travelAnimationPlayer.movingGraphics.setTranslateY(mapPoint.getY());
            }
        };
        travelAnimation.start();
    }

    private void endTravelling() {
        RouteResult.Ticket ticket = travelAnimationPlayer.ticket;
        RouteResult routeResult = travelAnimationPlayer.routeResult;
        Calendar dateArrival = routeResult.computeArrivalDate(travelAnimationPlayer.departureDate);
        travelAnimation.stop();
        travelAnimation = null;
        routeLayer.getChildren().remove(travelAnimationPlayer.movingGraphics);
        travelAnimationPlayer = null;

        // 移动
        careerManager.pushDateTo(dateArrival);  // 只是为了预防bug，本身应该没问题
        careerManager.getHumanPlayerCareer().endTravelling(ticket);
        careerManager.getHumanPlayerCareer().setCurrentLocation(routeResult.getEndCity());
        careerManager.saveToDisk();

        setSearchCities(careerManager.getHumanPlayerCareer().getCurrentLocation(), null);
        // 确保日期没有意外bug
        currentDateLabel.setText(CareerManager.calendarToString(careerManager.getTimestamp()));

        controlsBox.setDisable(false);
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

                    dragging = false;
                }
        );

        mapViewport.addEventFilter(
                MouseEvent.MOUSE_DRAGGED,
                event -> {
                    if (!event.isPrimaryButtonDown()) {
                        return;
                    }

                    double dx = event.getSceneX() - dragStartX;
                    double dy = event.getSceneY() - dragStartY;

                    if (Math.hypot(dx, dy) >= 5.0) {
                        dragging = true;
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
                    if (dragging) {
                        event.consume();
                        return;
                    }
                    if (event.getClickCount() == 1) {
                        updateRouteResultList();
                    }
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

        scheduleOverlayUpdate();
    }

    private void scheduleOverlayUpdate() {
        if (overlayUpdatePending) {
            return;
        }

        overlayUpdatePending = true;

        Platform.runLater(() -> {
            overlayUpdatePending = false;

            updateCityPositions();
            redrawRoutes();
            resolveLabelOverlaps();
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

            Node anchor1 =
                    cityAnchorMap.get(
                            route.getCity1()
                    );

            Node anchor2 =
                    cityAnchorMap.get(
                            route.getCity2()
                    );

            if (city1 == null ||
                    city2 == null ||
                    anchor1 == null ||
                    anchor2 == null) {
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
                            anchor1.localToScene(
                                    0,
                                    0
                            )
                    );

            Point2D p2 =
                    routeLayer.sceneToLocal(
                            anchor2.localToScene(
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

        if (travelAnimationPlayer != null) {
            routeLayer.getChildren().add(travelAnimationPlayer.movingGraphics);
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
        if (routeSearchResult != null && routeSearchResult.selected != null) {
            if (routeSearchResult.selected.containsRoute(route)) return true;
        }
        if (travelAnimationPlayer != null && travelAnimationPlayer.routeResult.containsRoute(route))
            return true;

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
        private LocationBubble locationBubble;
        private HouseBubble houseBubble;

        CityMarker(
                City city,
                String displayName
        ) {
            this.city = city;

            label = new Label(displayName);

            setPrefSize(1, 1);
            setMinSize(1, 1);
            setMaxSize(1, 1);

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
                    onCityClicked(city, this);

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

        void showLocationBubble(boolean show) {
            if (show) {
                if (locationBubble == null) {
                    locationBubble =
                            new LocationBubble();
                    locationBubble.setOnMouseClicked(getOnMouseClicked());

                    getChildren().add(
                            locationBubble
                    );
                }
            } else {
                if (locationBubble != null) {
                    getChildren().remove(
                            locationBubble
                    );

                    locationBubble = null;
                }
            }
        }

        void showHousesBubble(boolean show) {
            if (show) {
                if (houseBubble == null) {
                    houseBubble =
                            new HouseBubble();
                    houseBubble.setOnMouseClicked(getOnMouseClicked());

                    getChildren().add(
                            houseBubble
                    );
                }
            } else {
                if (houseBubble != null) {
                    getChildren().remove(
                            houseBubble
                    );

                    houseBubble = null;
                }
            }
        }
    }

    private void onRouteClicked(Route route) {
        RouteResult.RouteStep routeStep = new RouteResult.RouteStep(route, route.getCity1(), route.getCity2());
        RouteResult single = new RouteResult(route.getCity1(), route.getCity2(), List.of(routeStep));

        searchResultsView.getItems().clear();
        searchResultsView.getItems().add(single);
    }

    private void onCityClicked(City city, CityMarker cityMarker) {
        showCityPopOver(city, cityMarker);
    }

    private void showCityPopOver(
            City city,
            CityMarker marker
    ) {
        /*
         * 同一时间只显示一个城市窗口。
         */
        if (cityPopOver != null) {
            cityPopOver.hide();
        }

        VBox content = new VBox(8);
        content.setPrefWidth(220);
        content.setStyle("""
                -fx-padding: 12px;
                """);

        Label cityName = new Label(
                city.getName(
                        strings.getLocale()
                )
        );

        cityName.setStyle("""
                -fx-font-size: 16px;
                -fx-font-weight: bold;
                """);

        Label countryLabel = new Label(
                city.getCountry().shownName(strings)
        );

        countryLabel.setStyle("""
                -fx-text-fill: #777777;
                """);

        Separator separator =
                new Separator();

        GridPane housesPane = new GridPane();
        housesPane.setVgap(5.0);
        housesPane.setHgap(10.0);
        housesPane.getColumnConstraints().add(new ColumnConstraints());
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHalignment(HPos.RIGHT);

        Label housePriceLabel = new Label(
                String.format(
                        strings.getString("housePriceFmt"),
                        Util.moneyToReadable((int) (city.getHousePriceM2() * Residence.DEFAULT_AREA))
                )
        );
        Label houseRentalPriceLabel = new Label(
                String.format(
                        strings.getString("houseMonthlyRentalPriceFmt"),
                        Util.moneyToReadable(city.getHouseMonthlyRentalPrice(Residence.DEFAULT_AREA))
                )
        );

        Button buyHouseBtn = new Button(strings.getString("buyHouse"));
        Button rentHouseBtn = new Button(strings.getString("rentHouse"));

        int rowIndex = 0;

        HumanCareer humanCareer = careerManager.getHumanPlayerCareer();

        List<Residence> residences = careerManager.getHumanPlayerCareer().getInventory().getAliveResidenceAt(city);
        if (!residences.isEmpty()) {
            housesPane.add(new Label(strings.getString("houseProperties")), 0, rowIndex++);
            for (Residence residence : residences) {
                Residence.Ownership ownership = residence.getOwnership();
                String formatted = String.format(strings.getString("owningHouseWithAreaFmt"),
                        ownership.getShown(strings), residence.getArea());
                Label label = new Label(formatted);
                housesPane.add(label, 0, rowIndex);

                Button btn = switch (ownership) {
                    case OWN -> {
                        Button button = new Button(strings.getString("sellHouse") +
                                String.format(" %s", Util.moneyToReadable(residence.getTotalPrice())));
                        button.setOnAction(_ -> {
                            int curMoney = humanCareer.getMoney();
                            int price = residence.getTotalPrice();
                            AlertShower.askConfirmation(stage,
                                    String.format(strings.getString("balanceAfterSellFmt"),
                                            Util.moneyToReadable(curMoney),
                                            Util.moneyToReadable(price),
                                            Util.moneyToReadable(curMoney + price)
                                    ),
                                    strings.getString("confirmSell"),
                                    () -> {
                                        humanCareer.sellHouse(residence);
                                        cityPopOver.hide();
                                        cityPopOver = null;
                                        showCityPopOver(city, marker);
                                    },
                                    () -> showCityPopOver(city, marker));
                        });
                        yield button;
                    }
                    case RENT -> {
                        Button button = new Button(strings.getString("cancelRentHouse"));
                        button.setOnAction(_ -> {
                            AlertShower.askConfirmation(stage,
                                    strings.getString("cancelRentHouseInfo"),
                                    strings.getString("confirmCancelRent"),
                                    () -> {
                                        humanCareer.cancelRentHouse(residence);
                                        cityPopOver.hide();
                                        cityPopOver = null;
                                        showCityPopOver(city, marker);
                                    },
                                    () -> showCityPopOver(city, marker));
                        });
                        yield button;
                    }
                    default -> null;
                };
                if (btn != null) {
                    housesPane.add(btn, 1, rowIndex);
                }
                rowIndex++;
            }

            housesPane.add(new Separator(), 0, rowIndex++, 2, 1);
        }

        buyHouseBtn.setDisable(!humanCareer.canBuyHouse(city, Residence.DEFAULT_AREA));
        buyHouseBtn.setOnAction(_ -> {
            int curMoney = humanCareer.getMoney();
            int price = (int) (city.getHousePriceM2() * Residence.DEFAULT_AREA);
            AlertShower.askConfirmation(stage,
                    String.format(strings.getString("balanceAfterPurchase"),
                            Util.moneyToReadable(curMoney),
                            Util.moneyToReadable(price),
                            Util.moneyToReadable(curMoney - price)
                    ),
                    strings.getString("confirmPurchase"),
                    () -> {
                        humanCareer.buyHouse(Residence.createForCareer(
                                city,
                                Residence.DEFAULT_AREA,
                                careerManager,
                                Residence.Ownership.OWN
                        ));
                        cityPopOver.hide();
                        cityPopOver = null;
                        showCityPopOver(city, marker);
                    },
                    () -> showCityPopOver(city, marker));
        });
        
        // 租房也可以租到负的钱
        rentHouseBtn.setOnAction(_ -> {
//            int curMoney = humanCareer.getMoney();
            int price = city.getHouseMonthlyRentalPrice(Residence.DEFAULT_AREA);
            AlertShower.askConfirmation(stage,
                    String.format(strings.getString("rentHouseDesFmt"),
                            Util.moneyToReadable(price)
                    ),
                    strings.getString("confirmRentHouse"),
                    () -> {
                        humanCareer.startRentHouse(Residence.createForCareer(
                                city,
                                Residence.DEFAULT_AREA,
                                careerManager,
                                Residence.Ownership.RENT
                        ));
                        cityPopOver.hide();
                        cityPopOver = null;
                        showCityPopOver(city, marker);
                    },
                    () -> showCityPopOver(city, marker));
        });

        housesPane.add(housePriceLabel, 0, rowIndex);
        housesPane.add(buyHouseBtn, 1, rowIndex);
        rowIndex++;
        housesPane.add(houseRentalPriceLabel, 0, rowIndex);
        housesPane.add(rentHouseBtn, 1, rowIndex);

        rowIndex++;
        housesPane.add(new Label(
                String.format(
                        strings.getString("hotelDailyPriceFmt"),
                        Util.moneyToReadable(city.hotelPricePerDay())
                )
        ), 0, rowIndex++);

        housesPane.add(new Label(
                String.format(
                        strings.getString("dailyLifeFeeFmt"),
                        Util.moneyToReadable(city.livingCostPerDay())
                )
        ), 0, rowIndex++);

        Button travelButton =
                new Button(strings.getString("goto"));

        HBox buttonBox =
                new HBox(travelButton);

        buttonBox.setAlignment(
                Pos.CENTER_RIGHT
        );

        /*
         * 暂时不实现功能。
         */
        travelButton.setOnAction(_ -> {
            setSearchCities(careerManager.getHumanPlayerCareer().getCurrentLocation(), city);
            cityPopOver.hide();
            cityPopOver = null;
        });
        
        if (careerManager == null) {
            travelButton.setDisable(true);
        }

        content.getChildren().addAll(
                cityName,
                countryLabel,
                separator,
                housesPane,
                buttonBox
        );

        PopOver popOver =
                new PopOver(content);

        popOver.setDetachable(false);
        popOver.setAutoHide(true);
        popOver.setHeaderAlwaysVisible(false);
        popOver.setCloseButtonEnabled(false);

        /*
         * 根据城市位于屏幕左半还是右半，
         * 决定 PopOver 放在哪一边。
         *
         * 这样靠近地图右边的城市不会让窗口跑出屏幕。
         */
        Point2D markerInViewport =
                mapViewport.sceneToLocal(
                        marker.localToScene(0, 0)
                );

        if (markerInViewport.getX()
                < mapViewport.getWidth() / 2.0) {

            /*
             * 箭头在 PopOver 左侧，
             * 因此窗口出现在城市右边。
             */
            popOver.setArrowLocation(
                    PopOver.ArrowLocation.LEFT_CENTER
            );

        } else {

            /*
             * 窗口出现在城市左边。
             */
            popOver.setArrowLocation(
                    PopOver.ArrowLocation.RIGHT_CENTER
            );
        }

        cityPopOver = popOver;

        /*
         * marker 就是箭头所指向的 owner。
         */
        popOver.show(marker);
    }

    private String createCityTooltipText(City city) {
        StringBuilder builder = new StringBuilder();
        builder.append(city.getName(strings.getLocale())).append('\n');

        if (careerManager != null) {
            List<ChampionshipData> cityChamps = careerManager.getChampDataManager().getByCity(city);
            if (!cityChamps.isEmpty()) {
                StringJoiner joiner = new StringJoiner("\n");
                for (ChampionshipData cd : cityChamps) {
                    joiner.add(cd.getName());
                }
                builder.append(joiner);
            }
        }
        return builder.toString();
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


    private void updateRouteHighlight(RouteView routeView, boolean mouseSelected) {
        routeView.setHighlighted(mouseSelected, routeSearchResult == null ? null : routeSearchResult.selected);
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

            updateRouteHighlight(routeView, false);

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

            group.setOnMouseEntered(_ ->
                    updateRouteHighlight(routeView, true)
            );

            group.setOnMouseExited(_ ->
                    updateRouteHighlight(routeView, false)
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

            updateRouteHighlight(routeView, false);

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
                    updateRouteHighlight(routeView, true)
            );

            group.setOnMouseExited(_ ->
                    updateRouteHighlight(routeView, false)
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
        protected List<GeoPoint> createGreatCirclePoints(
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

        protected Vector3 latLonToVector(
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
        Projection createProjection(
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

        private Projection createCurrentProjection() {
            City city1 = manager.getCities().get("London");
            City city2 = manager.getCities().get("Beijing");

            if (city1 == null || city2 == null) {
                return null;
            }

            CityMarker marker1 =
                    cityMarkerMap.get(city1);

            CityMarker marker2 =
                    cityMarkerMap.get(city2);

            if (marker1 == null || marker2 == null) {
                return null;
            }

            Point2D p1 =
                    routeLayer.sceneToLocal(
                            marker1.localToScene(0, 0)
                    );

            Point2D p2 =
                    routeLayer.sceneToLocal(
                            marker2.localToScene(0, 0)
                    );

            return createProjection(
                    city1,
                    p1,
                    city2,
                    p2
            );
        }

        private Point2D greatCirclePointOnView(
                double lat1,
                double lon1,
                double lat2,
                double lon2,
                double progress
        ) {
            progress = Math.clamp(progress,
                    0.0, 1.0);

            GeoPoint geoPoint =
                    greatCirclePoint(
                            lat1,
                            lon1,
                            lat2,
                            lon2,
                            progress
                    );

            CurveRouteRenderer.Projection projection = curvedRouteRenderer.createCurrentProjection();
            if (projection == null) {
                return null;
            }

            return projection.project(
                    geoPoint.latitude(),
                    geoPoint.longitude()
            );
        }

        GeoPoint greatCirclePoint(
                double lat1,
                double lon1,
                double lat2,
                double lon2,
                double progress
        ) {
            Vector3 a =
                    latLonToVector(
                            lat1,
                            lon1
                    );

            Vector3 b =
                    latLonToVector(
                            lat2,
                            lon2
                    );

            double dot =
                    Math.clamp(
                            a.dot(b)
                            ,
                            -1.0,
                            1.0);

            double omega =
                    Math.acos(dot);

            /*
             * 两点几乎相同。
             */
            if (omega < 1e-10) {
                return new GeoPoint(
                        lat1,
                        lon1
                );
            }

            double sinOmega =
                    Math.sin(omega);

            /*
             * 正常的大圆 SLERP。
             */
            double k1 =
                    Math.sin(
                            (1.0 - progress) * omega
                    ) / sinOmega;

            double k2 =
                    Math.sin(
                            progress * omega
                    ) / sinOmega;

            Vector3 v =
                    new Vector3(
                            a.x() * k1
                                    + b.x() * k2,

                            a.y() * k1
                                    + b.y() * k2,

                            a.z() * k1
                                    + b.z() * k2
                    ).normalized();

            return vectorToLatLon(v);
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
        private static final double TARGETED_WIDTH = 2.5;
        private static final double SELECTED_WIDTH = 3.5;
        private static final double TARGETED_SELECTED_WIDTH = 5.0;

        private final Route route;
        private final List<Shape> visualShapes = new ArrayList<>();

        RouteView(Route route) {
            this.route = route;
        }

        void addVisualShape(Shape shape) {
            visualShapes.add(shape);
        }

        /**
         * @param selected 是否被点击选中
         * @param selectedSearchResult 选中的搜索结果
         */
        void setHighlighted(boolean selected, RouteResult selectedSearchResult) {
            boolean targeted = false;

            if (selectedSearchResult != null) {
                for (RouteResult.RouteStep rs : selectedSearchResult.getSteps()) {
                    if (rs.route().equals(route)) {
                        targeted = true;
                        break;
                    }
                }
            }

            for (Shape shape : visualShapes) {
                double strokeWidth;
                double opacity;
                if (selected) {
                    if (targeted) {
                        strokeWidth = TARGETED_SELECTED_WIDTH;
                        opacity = 1.0;
                    } else {
                        strokeWidth = SELECTED_WIDTH;
                        opacity = 0.9;
                    }
                } else {
                    if (targeted) {
                        strokeWidth = TARGETED_WIDTH;
                        opacity = 0.9;
                    } else {
                        strokeWidth = NORMAL_WIDTH;
                        opacity = 0.75;
                    }
                }
                shape.setStrokeWidth(strokeWidth);
                shape.setOpacity(opacity);
            }
        }
    }

    enum RouteResultSort {
        PRICE("sortByPrice"),
        TIME("sortByTime");

        final String stringKey;

        RouteResultSort(String stringKey) {
            this.stringKey = stringKey;
        }

        @Override
        public String toString() {
            return App.getStrings().getString(stringKey);
        }
    }

    static class RouteSearchResult {
        final List<RouteResult> routeResults;
        final List<RouteResult> shownResults = new ArrayList<>();
        RouteResult selected;

        RouteSearchResult(List<RouteResult> routeResults) {
            this.routeResults = routeResults;
        }

        public void filterAndSort(boolean directOnly, boolean flightOnly, RouteResultSort sort) {
            shownResults.clear();
            for (RouteResult rr : routeResults) {
                if (directOnly && rr.getTransitCount() != 0) continue;
                if (flightOnly && !rr.isAllFlight()) continue;
                shownResults.add(rr);
            }

            shownResults.sort((o1, o2) -> {
                int priceCmp = Double.compare(o1.getTotalEconomyPrice(), o2.getTotalEconomyPrice());
                int totalTimeCmp = Double.compare(o1.getTotalTimeMinutes(), o2.getTotalTimeMinutes());
                if (sort == RouteResultSort.PRICE) {
                    if (priceCmp != 0) return priceCmp;
                    else return totalTimeCmp;
                } else if (sort == RouteResultSort.TIME) {
                    if (totalTimeCmp != 0) return totalTimeCmp;
                    else return priceCmp;
                } else {
                    throw new RuntimeException("Unsupported comparison " + sort);
                }
            });
        }
    }

    class RouteResultListCell extends ListCell<RouteResult> {

        private final GridPane basePane = new GridPane();
        private final Label routeCitiesLabel = new Label();
        private final Label transitsLabel = new Label();
        private final Label totalTimeTextLabel = new Label();
        private final Label totalTimeLabel = new Label();
        private final Label onboardTimeLabel = new Label();
        private final Label distanceLabel = new Label();
        private final LabelTable<double[]> priceTable;
        private RouteResult routeResult;

        RouteResultListCell() {
            basePane.setVgap(5.0);
            basePane.setHgap(5.0);

            basePane.setPrefWidth(220.0);
            basePane.setMaxWidth(220.0);

            int row = 0;
            routeCitiesLabel.setWrapText(true);
            routeCitiesLabel.setStyle("""
                    -fx-font-size: 14px;
                    -fx-font-weight: bold;
                    """);
            basePane.add(routeCitiesLabel, 0, row++, 4, 1);

            basePane.add(new Label(strings.getString("routeOnboardTime")), 0, row);
            basePane.add(onboardTimeLabel, 1, row);

            totalTimeTextLabel.setText(strings.getString("routeTotalTime"));
            basePane.add(totalTimeTextLabel, 2, row);
            basePane.add(totalTimeLabel, 3, row++);

            basePane.add(new Label(strings.getString("routeDistance")), 0, row);
            basePane.add(distanceLabel, 1, row);

            basePane.add(transitsLabel, 2, row++);

            priceTable = new LabelTable<>();
            priceTable.addColumns(
                    new LabelTableColumn<>(priceTable, strings.getString("economyClass"),
                            value -> new ReadOnlyStringWrapper(String.format("%.0f", value[0]))),
                    new LabelTableColumn<>(priceTable, strings.getString("businessClass"),
                            value -> new ReadOnlyStringWrapper(String.format("%.0f", value[1]))),
                    new LabelTableColumn<>(priceTable, strings.getString("firstClass"),
                            value -> new ReadOnlyStringWrapper(String.format("%.0f", value[2])))
            );
            priceTable.getColumns().get(0).setOnClick(param -> {
                showBuyTicketPopOver(param, 0, priceTable);
                return null;
            });
            priceTable.getColumns().get(1).setOnClick(param -> {
                showBuyTicketPopOver(param, 1, priceTable);
                return null;
            });
            priceTable.getColumns().get(2).setOnClick(param -> {
                showBuyTicketPopOver(param, 2, priceTable);
                return null;
            });

            basePane.add(priceTable, 0, row++, 4, 1);
        }

        private void showBuyTicketPopOver(double[] prices,
                                          int classIndex,
                                          Node parent) {
            if (ticketPopOver != null) {
                ticketPopOver.hide();
            }
            if (careerManager == null) return;
            if (routeResult == null) return;

            RouteResult.SeatClass classType = RouteResult.SeatClass.fromIndex(classIndex);

            double price = prices[classIndex];
            Label priceLabel = new Label(String.format("%.0f", price));
            Label classLabel = new Label(classType.getShown(strings));

            VBox content = new VBox(8);
            content.setPadding(new Insets(12));

            Button travelButton =
                    new Button(strings.getString("travelNow"));
            Button scheduleTravelButton =
                    new Button(strings.getString("scheduleTravelBeforeChamp"));

            if (careerManager != null
                    && careerManager.getHumanPlayerCareer().getCurrentLocation().equals(departureBox.getValue()) 
                    && careerManager.getChampionshipInProgress() == null) {
                travelButton.setDisable(false);
                travelButton.setOnAction(_ -> {
                    travelTo(new RouteResult.Ticket(
                            routeResult,
                            classType,
                            careerManager.getTimestamp(),
                            routeResult.computeArrivalDate(careerManager.getTimestamp())
                    ));
                    ticketPopOver.hide();
                });
            } else {
                travelButton.setDisable(true);
            }

            if (nextChampionship == null
                    || !nextChampionship.data.getLocation().city().equals(destinationBox.getValue())
                    || careerManager == null
                    || !careerManager.getHumanPlayerCareer().getCurrentLocation().equals(departureBox.getValue())) {
                scheduleTravelButton.setDisable(true);
            } else {
                scheduleTravelButton.setDisable(false);
                Calendar latestDeparture = nextChampionship.latestDeparture(routeResult);
                scheduleTravelButton.setOnAction(_ -> {
                    careerManager.setNextScheduleTravel(new RouteResult.Ticket(
                            routeResult,
                            classType,
                            latestDeparture,
                            routeResult.computeArrivalDate(latestDeparture)
                    ));
                    ticketPopOver.hide();
                });
            }

            HBox buttonBox =
                    new HBox(travelButton, scheduleTravelButton);
            buttonBox.setSpacing(8.0);

            buttonBox.setAlignment(
                    Pos.CENTER_RIGHT
            );

            content.getChildren().addAll(
                    classLabel,
                    priceLabel,
                    buttonBox
            );

            PopOver popOver =
                    new PopOver(content);

            popOver.setDetachable(false);
            popOver.setAutoHide(true);
            popOver.setHeaderAlwaysVisible(false);
            popOver.setCloseButtonEnabled(false);

            popOver.setArrowLocation(
                    PopOver.ArrowLocation.LEFT_CENTER
            );

            ticketPopOver = popOver;

            /*
             * marker 就是箭头所指向的 owner。
             */
            popOver.show(parent);
        }

        @Override
        protected void updateItem(RouteResult item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                routeResult = null;
                setGraphic(null);
            } else {
                routeResult = item;
                routeCitiesLabel.setText(item.cityNamesOnUi(strings));

                int nTrans = item.getTransitCount();
                if (nTrans == 0) {
                    transitsLabel.setText(strings.getString("directRoute"));
                    totalTimeTextLabel.setVisible(false);
                    totalTimeTextLabel.setManaged(false);
                    totalTimeLabel.setVisible(false);
                    totalTimeLabel.setManaged(false);
                } else {
                    transitsLabel.setText(String.format(strings.getString("transitsFmt"), nTrans));
                    totalTimeTextLabel.setVisible(true);
                    totalTimeTextLabel.setManaged(true);
                    totalTimeLabel.setVisible(true);
                    totalTimeLabel.setManaged(true);
                }

                totalTimeLabel.setText(RouteResult.formatTime(item.getTotalTimeMinutes()));
                onboardTimeLabel.setText(RouteResult.formatTime(item.getOnBoardTimeMinutes()));
                distanceLabel.setText(String.format("%.0f km", item.getTotalDistance()));

                priceTable.clearItems();
                priceTable.addItem(new double[]{item.getTotalEconomyPrice(), item.getTotalBusinessPrice(), item.getTotalFirstPrice()});

                setGraphic(basePane);
            }
        }
    }

    private static class LocationBubble extends Group {

        LocationBubble() {
            ResourcesLoader rl = ResourcesLoader.getInstance();
            ImageView iv = new ImageView();
            rl.setIconImage1x1(rl.getLocationIcon(), iv, 1.773);
            iv.setTranslateX(-iv.getFitWidth() / 2);
            iv.setTranslateY(-iv.getFitHeight());
            getChildren().add(iv);

//            setMouseTransparent(true);
        }
    }

    private static class HouseBubble extends Group {

        HouseBubble() {
            ResourcesLoader rl = ResourcesLoader.getInstance();
            ImageView iv = new ImageView();
            rl.setIconImage1x1(rl.getHouseIcon(), iv, 1.333);
//            iv.setTranslateX(-iv.getFitWidth() / 2);
            iv.setTranslateY(-iv.getFitHeight());
            getChildren().add(iv);

//            setMouseTransparent(true);
        }
    }

    private class TravelAnimationPlayer {

        static final double SPEED_MUL = 11520;

        private final RouteResult.Ticket ticket;
        private final RouteResult routeResult;
        private final Calendar departureDate;
        private double curSegmentTravelledDt;
        private final double speed;
        private final Group movingGraphics;

        private long lastFrameTime;
        private double minutesSpent;

        private final NavigableMap<Double, Calendar> dates = new TreeMap<>();  // 时间和date
        private final NavigableMap<Double, Segment> routeSegments = new TreeMap<>();

        TravelAnimationPlayer(RouteResult.Ticket ticket,
                              RouteResult routeResult, Calendar departureDate, double speed) {
            this.routeResult = routeResult;
            this.ticket = ticket;
            this.departureDate = departureDate;
            this.speed = speed;

            movingGraphics = new Group();
            Shape dot = new Circle(5);
            dot.setFill(Color.RED);
            movingGraphics.getChildren().add(dot);

            computeDates();
        }

        private void computeDates() {
            double totalMinutes = routeResult.getTotalTimeMinutes();
            double remMinutes = (1440 - totalMinutes % 1440) % 1440;
            // 如果时间充裕，就10点出发
            // 如果不充裕，就卡在 23:59能到的时间出发
            double departureMinutes = Math.min(10 * 60, remMinutes);
            int daysCount = 0;
            for (double mins = departureMinutes; mins < departureMinutes + totalMinutes; mins += 1440) {
                Calendar cal = (Calendar) departureDate.clone();
                cal.add(Calendar.DAY_OF_MONTH, daysCount);
                dates.put(mins - departureMinutes, cal);
                daysCount++;
            }

            double minutes = 0;
            List<RouteResult.RouteStep> steps = routeResult.getSteps();
            for (int i = 0; i < steps.size(); i++) {
                RouteResult.RouteStep step = steps.get(i);
                if (i != 0) {
                    routeSegments.put(minutes, new Segment(Status.TRANSITING, step));
                    minutes += step.getFromCityTransitTime();
                }

                routeSegments.put(minutes, new Segment(Status.MOVING, step));
                minutes += step.route().getTimeMinutes();
            }
        }

        /**
         * @return true if has next frame, false if ends
         */
        boolean oneFrame() {
            if (minutesSpent >= routeResult.getTotalTimeMinutes()) {
                return false;
            }
            long curTime = System.currentTimeMillis();
            long frameTime;
            if (lastFrameTime == 0) {
                frameTime = 20;
            } else {
                frameTime = curTime - lastFrameTime;
            }
            var entry = routeSegments.floorEntry(minutesSpent);
            Segment currentSegment = entry.getValue();
            double frameMinutes = frameTime * speed * SPEED_MUL / 60000;
            minutesSpent += frameMinutes;

            if (currentSegment.status() == Status.MOVING) {
                double frameKm = currentSegment.step().route().averageSpeedKmh() * frameMinutes / 60;
                curSegmentTravelledDt += frameKm;
                if (curSegmentTravelledDt > currentSegment.step().route().getDistance()) {
                    // 特殊case: 已经到达了
                    // 如果不设为0，这一帧会渲染到下一段航程里去
                    curSegmentTravelledDt = 0;
                }
            } else {
                curSegmentTravelledDt = 0;
            }

            lastFrameTime = curTime;
            return true;
        }

        Calendar getDate() {
            return dates.floorEntry(minutesSpent).getValue();
        }

        Point2D getPoint() {
            Segment currentSegment = routeSegments.floorEntry(minutesSpent).getValue();
            RouteResult.RouteStep routeStep = currentSegment.step();
            double progress = curSegmentTravelledDt / routeStep.route().getDistance();
            return curvedRouteRenderer.greatCirclePointOnView(
                    routeStep.fromCity().getLatitude(),
                    routeStep.fromCity().getLongitude(),
                    routeStep.toCity().getLatitude(),
                    routeStep.toCity().getLongitude(),
                    progress
            );
        }

        private enum Status {
            MOVING,
            TRANSITING
        }

        private record Segment(Status status, RouteResult.RouteStep step) {
            @Override
            public @NotNull String toString() {
                return "Segment{" +
                        "status=" + status +
                        ", step=" + step +
                        '}';
            }
        }
    }
}