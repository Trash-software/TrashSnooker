package trashsoftware.trashSnooker.fxml;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.ChampionshipScore;
import trashsoftware.trashSnooker.core.career.HumanCareer;
import trashsoftware.trashSnooker.core.career.Invoice;
import trashsoftware.trashSnooker.res.ResourcesLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;

import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CashFlowView extends ChildInitializable {

    public static final String[] ALL_TYPES = {
            "all",
            "championshipEarn", "challengeEarn", "achievementAward", "invitation",
            "participation", "purchase", "upgrade", "fees",
//            "lifeFee", "oweInterest"
    };
    public static final DateFormat MONTH_FMT = new SimpleDateFormat("yyyy-MM");

    private final List<Invoice> invoiceObjects = new ArrayList<>();
    private final Map<String, Integer> incomes = new HashMap<>(
            Map.of("initMoney", CareerManager.INIT_MONEY,
                    "championshipEarn", 0,
                    "challengeEarn", 0,
                    "invitation", 0,
                    "achievementAward", 0)
    );
    private final Map<String, Integer> expenditures = new HashMap<>(
            Map.of("registry", 0,
                    "travel", 0,
                    "hotel", 0,
                    "purchase", 0,
                    "upgrade", 0,
                    "lifeFee", 0,
                    "oweInterest", 0)
    );
    @FXML
    ListView<Invoice> listPane;
    @FXML
    Label moneyLabel;
    @FXML
    ImageView moneyImage;
    @FXML
    MenuButton typeFilterMenu;
    @FXML
    Button filterButton;
    @FXML
    Label cumIncomeLabel, cumExpenditureLabel;
    @FXML
    PieChart incomeChart, expenditureChart;
    @FXML
    LineChart<Number, Number> moneyHistoryChart;
    @FXML
    NumberAxis dateAxis;
    private Stage stage;
    private HumanCareer humanCareer;
    private ResourceBundle strings;

    public void setup(Stage stage, HumanCareer humanCareer) {
        this.stage = stage;
        this.humanCareer = humanCareer;

        listPane.setCellFactory(param -> new InvoiceListCell());
        createObjects(humanCareer);

        dateAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number object) {
                return MONTH_FMT.format(new Date(object.longValue()));
            }

            @Override
            public Number fromString(String string) {
                try {
                    return MONTH_FMT.parse(string).getTime();
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        loadInvoices(true);
    }

    public void renderInvoiceList() {
        listPane.getItems().clear();
        InvoiceListCell.last = CareerManager.getInstance().getBeginTimestamp();

        for (Invoice invoice : invoiceObjects) {
            if (isTypeSelected(invoice.type)) {
                listPane.getItems().add(invoice);
            } else {
                InvoiceListCell.last = invoice.inGameDate;
            }
        }
    }

//    @Override
//    public Stage getStage() {
//        return stage;
//    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;

        ResourcesLoader rl = ResourcesLoader.getInstance();
        rl.setIconImage(rl.getMoneyImg(), moneyImage);

        ImageView btnImg = new ImageView();
        rl.setIconImage1x1(rl.getFilterImage(), btnImg);

        filterButton.setGraphic(btnImg);

        setFilterMenu();
    }

    @FXML
    void typeFilterAction() {
        if (getAllCheckMenu().isSelected()) {
            typeFilterMenu.setText(strings.getString("all"));
        } else {
            List<String> selectedNames = selectedTypeNames();
            if (selectedNames.isEmpty()) {
                typeFilterMenu.setText(strings.getString("none"));
            } else if (selectedNames.size() == 1) {
                typeFilterMenu.setText(selectedNames.getFirst());
            } else {
                typeFilterMenu.setText(strings.getString("multipleSelection"));
            }
        }
        loadInvoices(false);
        renderInvoiceList();
    }

    private void setFilterMenu() {
        typeFilterMenu.setText(strings.getString("all"));
        for (String type : ALL_TYPES) {
            TypeMenuItem tmi = new TypeMenuItem(type);
            CustomMenuItem cmi = new CustomMenuItem(tmi);
            cmi.setHideOnClick(false);
            typeFilterMenu.getItems().add(cmi);
        }
        for (var v : typeFilterMenu.getItems()) {
            TypeMenuItem tmi = (TypeMenuItem) ((CustomMenuItem) v).getContent();
            tmi.setSelected(true);
        }
    }

    private TypeMenuItem getAllCheckMenu() {
        CustomMenuItem cmi = (CustomMenuItem) typeFilterMenu.getItems().get(0);
        return (TypeMenuItem) cmi.getContent();
    }

    private List<String> selectedTypeNames() {
        List<String> result = new ArrayList<>();
        for (MenuItem mi : typeFilterMenu.getItems()) {
            CustomMenuItem cmi = (CustomMenuItem) mi;
            TypeMenuItem tmi = (TypeMenuItem) cmi.getContent();
            if (!tmi.isAll()) {
                if (tmi.isSelected()) {
                    result.add(tmi.getText());
                }
            }
        }
        return result;
    }

    private boolean isTypeSelected(String typeKey) {
        for (MenuItem mi : typeFilterMenu.getItems()) {
            CustomMenuItem cmi = (CustomMenuItem) mi;
            TypeMenuItem tmi = (TypeMenuItem) cmi.getContent();
            if (tmi.isAll() && tmi.isSelected()) return true;
            if (tmi.key.equals(typeKey) && tmi.isSelected()) return true;
        }
        return false;
    }

    private void createObjects(HumanCareer humanCareer) {
        List<Invoice> invoices = humanCareer.getInvoices();
        this.invoiceObjects.addAll(invoices);
    }

    private void loadInvoices(boolean firstFill) {
        Map<String, Integer> incomes;
        Map<String, Integer> expenditures;
        SortedMap<Calendar, Integer> dateMoneyMap = new TreeMap<>();

        Calendar last = CareerManager.getInstance().getBeginTimestamp();

        if (firstFill) {
            incomes = this.incomes;
            expenditures = this.expenditures;
        } else {
            incomes = new HashMap<>();
            expenditures = new HashMap<>();
        }

        int cumIncome = 0;
        int cumExpenditure = 0;

        for (int idx = 0; idx < invoiceObjects.size(); idx++) {
            Invoice io = invoiceObjects.get(idx);
            try {
                if (dateMoneyMap.isEmpty()) {
                    // 初始资金
                    dateMoneyMap.put(last, io.getMoneyBefore());
                }
                if ("fees".equals(io.type)) {
                    // 因为一些早期失误，fees的时间是上一场比赛的时间
                    if (idx < invoiceObjects.size() - 1) {
                        dateMoneyMap.put(invoiceObjects.get(idx + 1).inGameDate, io.getMoneyAfter());
                    } else {
                        dateMoneyMap.put(io.inGameDate, io.getMoneyAfter());
                    }
                } else {
                    dateMoneyMap.put(io.inGameDate, io.getMoneyAfter());
                }

                if (!isTypeSelected(io.type)) {
                    last = io.inGameDate;
                    continue;
                }

                if (io instanceof Invoice.Fees fees) {
                    // Special case for fees
                    for (Map.Entry<String, Integer> entry : fees.getItems().entrySet()) {
                        Integer typeExpend = expenditures.get(entry.getKey());
                        if (typeExpend != null) {
                            expenditures.put(entry.getKey(), typeExpend + entry.getValue());
                        }
                    }
                } else if (io instanceof Invoice.Participation par) {
                    for (Map.Entry<String, Integer> entry : par.getItems().entrySet()) {
                        Integer typeExpend = expenditures.get(entry.getKey());
                        if (typeExpend != null) {
                            expenditures.put(entry.getKey(), typeExpend + entry.getValue());
                        }
                    }
                } else {
                    Integer typeExpend = expenditures.get(io.type);
                    if (typeExpend != null) {
                        expenditures.put(io.type, typeExpend - io.getMoneyChange());
                    }
                }

                Integer typeIncome = incomes.get(io.type);
                if (typeIncome != null) {
                    incomes.put(io.type, typeIncome + io.getMoneyChange());
                }

                int mc = io.getMoneyChange();

                if (mc > 0) cumIncome += mc;
                else if (mc < 0) cumExpenditure += mc;
            } catch (RuntimeException e) {
                EventLogger.warning(e);
            }
        }

        if (firstFill) {
            int money = humanCareer.getMoney();
            moneyLabel.setText(Util.moneyToReadable(money));
            if (money < 0) {
                moneyLabel.setTextFill(CareerView.SPEND_MONEY_COLOR);
            } else {
                moneyLabel.setTextFill(Color.BLACK);
            }
            cumIncomeLabel.setTextFill(CareerView.EARN_MONEY_COLOR);
            cumIncomeLabel.setText(Util.moneyToReadable(cumIncome, true));
            cumExpenditureLabel.setTextFill(CareerView.SPEND_MONEY_COLOR);
            cumExpenditureLabel.setText(Util.moneyToReadable(cumExpenditure, true));

            drawPieChart(incomeChart, incomes);
            drawPieChart(expenditureChart, expenditures);

            drawLineChart(dateMoneyMap);
        }
//        System.out.println("Draw time: " + (System.currentTimeMillis() - beginTime2));
    }

    private String formatType(String typeKey) {
        String sKey = Util.toLowerCamelCase("SUB_INVOICE_" +
                Util.toAllCapsUnderscoreCase(typeKey));
        if (strings.containsKey(sKey)) {
            return strings.getString(sKey);
        } else {
            String upper = "INVOICE_" + Util.toAllCapsUnderscoreCase(typeKey);
            String key = Util.toLowerCamelCase(upper);
            if (strings.containsKey(key)) return strings.getString(key);
        }
        return sKey;
    }

    private String typeMenuItemToString(String key) {
        if ("all".equals(key)) {
            return strings.getString("all");
        } else {
            return formatType(key);
        }
    }

    private void drawPieChart(PieChart chart, Map<String, Integer> map) {
        ObservableList<PieChart.Data> pieChartData =
                FXCollections.observableArrayList();

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            pieChartData.add(new PieChart.Data(
                    formatType(entry.getKey()) +
                            " " + Util.moneyToReadable(entry.getValue()),
                    entry.getValue()));
        }

        chart.setData(pieChartData);
    }

    private void drawLineChart(SortedMap<Calendar, Integer> dateMoneyMap) {
        dateAxis.setLowerBound(dateMoneyMap.firstKey().getTime().getTime());
        dateAxis.setUpperBound(dateMoneyMap.lastKey().getTime().getTime());
        dateAxis.setTickUnit(365.25 * 24 * 60 * 60 * 1000 / 12);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();

        for (Map.Entry<Calendar, Integer> entry : dateMoneyMap.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey().getTime().getTime(), entry.getValue()));
        }

        moneyHistoryChart.getData().add(series);
    }

    class InvoiceListCell extends ListCell<Invoice> {

        GridPane basePane = new GridPane();
        Label monthLabel = new Label();
        Label dateLabel = new Label();
        Label typeLabel = new Label();
        Label desLabel = new Label();
        Label moneyBefore = new Label();

        VBox expandableColumn1 = new VBox();
        VBox expandableColumn2 = new VBox();

        Label moneyChange = new Label();
        Label moneyAfter = new Label();

        static Calendar last;

        InvoiceListCell() {
            basePane.setVgap(5.0);
            basePane.setHgap(10.0);
            basePane.getColumnConstraints().add(new ColumnConstraints());
            basePane.getColumnConstraints().add(new ColumnConstraints());
            ColumnConstraints col2 = new ColumnConstraints();
            col2.setHalignment(HPos.RIGHT);
            basePane.getColumnConstraints().add(col2);

            monthLabel.setFont(new Font(App.FONT.getName(), 16));

            int row = 0;
//            basePane.add(new Separator(Orientation.HORIZONTAL), 0, row++, 3, 1);
            basePane.add(monthLabel, 0, row++);

            basePane.add(dateLabel, 0, row++);
            basePane.add(typeLabel, 0, row);

            desLabel.setWrapText(true);
            desLabel.setPrefWidth(180.0);
            desLabel.setMaxWidth(180.0);
            basePane.add(desLabel, 1, row++);
            basePane.add(new Separator(Orientation.HORIZONTAL), 1, row++, 2, 1);

            moneyBefore.setTextAlignment(TextAlignment.RIGHT);
            basePane.add(moneyBefore, 2, row++);

            expandableColumn1.setSpacing(5.0);
            basePane.add(expandableColumn1, 1, row);
            expandableColumn2.setSpacing(5.0);
            expandableColumn2.setAlignment(Pos.TOP_RIGHT);
            basePane.add(expandableColumn2, 2, row++);

            basePane.add(new Separator(Orientation.HORIZONTAL), 1, row++, 2, 1);
            basePane.add(new Label(strings.getString("subtotal")), 1, row);
            moneyChange.setTextAlignment(TextAlignment.RIGHT);
            basePane.add(moneyChange, 2, row++);

            basePane.add(new Label(strings.getString("balanceAfter")), 1, row);
            moneyAfter.setTextAlignment(TextAlignment.RIGHT);
            basePane.add(moneyAfter, 2, row++);

//            basePane.add(new Separator(Orientation.HORIZONTAL), 0, row++, 3, 1);
        }

        @Override
        protected void updateItem(Invoice item, boolean empty) {
            super.updateItem(item, empty);

            expandableColumn1.setVisible(false);
            expandableColumn1.setManaged(false);
            expandableColumn1.getChildren().clear();
            expandableColumn2.setVisible(false);
            expandableColumn2.setManaged(false);
            expandableColumn2.getChildren().clear();

            if (empty || item == null) {
                setGraphic(null);
            } else {
                if (item.inGameDate.get(Calendar.YEAR) != last.get(Calendar.YEAR) ||
                        item.inGameDate.get(Calendar.MONTH) != last.get(Calendar.MONTH)) {
                    String month = String.format("%s.%s",
                            item.inGameDate.get(Calendar.YEAR),
                            item.inGameDate.get(Calendar.MONTH) + 1);
                    monthLabel.setText(month);
                } else {
                    monthLabel.setVisible(false);
                    monthLabel.setManaged(false);
                }

                String date = CareerManager.calendarToString(item.inGameDate);
                dateLabel.setText(date);
                typeLabel.setText(item.getShownType(strings));

                desLabel.setText(item.getItemDes(strings, humanCareer));

                int mb = item.getMoneyBefore();
                moneyBefore.setText(Util.moneyToReadable(mb));
                if (mb < 0) {
                    moneyBefore.setTextFill(CareerView.SPEND_MONEY_COLOR);
                } else {
                    moneyBefore.setTextFill(CareerView.REGULAR_TEXT_COLOR);
                }

                if (item instanceof Invoice.ChampionshipEarn ce) {
                    expandableColumn1.setVisible(true);
                    expandableColumn1.setManaged(true);
                    expandableColumn2.setVisible(true);
                    expandableColumn2.setManaged(true);
                    int taxes = 0;
                    for (Map.Entry<String, Invoice.TaxedIncome> entry : ce.getItems().entrySet()) {
                        ChampionshipScore.Rank cs = ChampionshipScore.Rank.valueOf(entry.getKey());
                        expandableColumn1.getChildren().add(new Label(cs.getShown()));
                        int raw = entry.getValue().raw();
                        int actual = entry.getValue().actual();
                        taxes += (actual - raw);
                        Label rawAwd = new Label(Util.moneyToReadable(raw, true));
                        rawAwd.setTextAlignment(TextAlignment.RIGHT);
                        if (raw > 0) {
                            rawAwd.setTextFill(CareerView.EARN_MONEY_COLOR);
                        }
                        expandableColumn2.getChildren().add(rawAwd);
                    }
                    if (taxes < 0) {
                        expandableColumn1.getChildren().add(new Label(strings.getString("taxes")));
                        Label taxLabel = new Label(Util.moneyToReadable(taxes));
                        taxLabel.setTextAlignment(TextAlignment.RIGHT);
                        taxLabel.setTextFill(CareerView.SPEND_MONEY_COLOR);
                        expandableColumn2.getChildren().add(taxLabel);
                    }
                } else if (item instanceof Invoice.CostItemsHolder iih) {
                    expandableColumn1.setVisible(true);
                    expandableColumn1.setManaged(true);
                    expandableColumn2.setVisible(true);
                    expandableColumn2.setManaged(true);
                    for (Map.Entry<String, Integer> entry : iih.getItems().entrySet()) {
                        String itemKey = entry.getKey();
                        String shownItem = formatType(itemKey);
                        int subChange = -entry.getValue();

                        String subChangeStr = Util.moneyToReadable(subChange, true);

                        expandableColumn1.getChildren().add(new Label(shownItem));
                        Label subChangeLabel = new Label(subChangeStr);
                        if (subChange < 0)
                            subChangeLabel.setTextFill(CareerView.SPEND_MONEY_COLOR);

                        expandableColumn2.getChildren().add(subChangeLabel);
                    }
                }

                int mc = item.getMoneyChange();
                String mcs = Util.moneyToReadable(mc, true);
                int ma = item.getMoneyAfter();
                if (mc > 0) {
                    moneyChange.setTextFill(CareerView.EARN_MONEY_COLOR);
                } else if (mc < 0) {
                    moneyChange.setTextFill(CareerView.SPEND_MONEY_COLOR);
                } else {
                    moneyChange.setTextFill(CareerView.REGULAR_TEXT_COLOR);
                }
                moneyChange.setText(mcs);

                if (ma < 0) {
                    moneyAfter.setTextFill(CareerView.SPEND_MONEY_COLOR);
                } else {
                    moneyAfter.setTextFill(CareerView.REGULAR_TEXT_COLOR);
                }
                moneyAfter.setText(Util.moneyToReadable(ma));

                last = item.inGameDate;

                setGraphic(basePane);
            }
        }
    }

    public class TypeMenuItem extends CheckBox {
        private final String key;

        TypeMenuItem(String key) {
            super(typeMenuItemToString(key));

            this.key = key;

            selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != oldValue) {
                    if (newValue) {
                        if (isAll()) {
                            for (MenuItem mi : typeFilterMenu.getItems()) {
                                CustomMenuItem cmi = (CustomMenuItem) mi;
                                TypeMenuItem tmi = (TypeMenuItem) cmi.getContent();
                                if (!tmi.isAll() && !tmi.isSelected()) tmi.setSelected(true);
                            }
                        } else {
                            for (MenuItem mi : typeFilterMenu.getItems()) {
                                CustomMenuItem cmi = (CustomMenuItem) mi;
                                TypeMenuItem tmi = (TypeMenuItem) cmi.getContent();
                                if (!tmi.isAll())
                                    if (!tmi.isSelected()) return;
                            }
                            getAllCheckMenu().setSelected(true);
                        }
                    } else {
                        if (isAll()) {
                            for (MenuItem mi : typeFilterMenu.getItems()) {
                                CustomMenuItem cmi = (CustomMenuItem) mi;
                                TypeMenuItem tmi = (TypeMenuItem) cmi.getContent();
                                if (!tmi.isAll())
                                    tmi.setSelected(false);
                            }
                        } else {
                            getAllCheckMenu().setSelected(false);
                        }
                    }
                }
            });
        }

        boolean isAll() {
            return "all".equals(key);
        }
    }
}
