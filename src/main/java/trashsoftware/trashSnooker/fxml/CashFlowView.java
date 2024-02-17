package trashsoftware.trashSnooker.fxml;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.ChampionshipData;
import trashsoftware.trashSnooker.core.career.ChampionshipScore;
import trashsoftware.trashSnooker.core.career.HumanCareer;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
import trashsoftware.trashSnooker.core.career.challenge.ChallengeManager;
import trashsoftware.trashSnooker.core.career.challenge.ChallengeSet;
import trashsoftware.trashSnooker.core.career.championship.MatchTreeNode;
import trashsoftware.trashSnooker.core.career.championship.MetaMatchInfo;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueTip;
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
    
    private final List<InvoiceObject> invoiceObjects = new ArrayList<>();
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
    GridPane listPane;
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

        fill(true);
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
        fill(false);
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
        List<JSONObject> invoices = humanCareer.getInvoices();
        Calendar current = CareerManager.getInstance().getBeginTimestamp();
        for (JSONObject object : invoices) {
            try {
                String type = object.getString("type");
                if (object.has("inGameDate")) {
                    current = CareerManager.stringToCalendar(object.getString("inGameDate"));
                } else if ("participation".equals(type)) {
                    String champInsId = object.getString("match");
                    MetaMatchInfo mmi = MatchTreeNode.analyzeMatchId(champInsId);
                    
                    current.set(Calendar.YEAR, mmi.year);
                    current.set(Calendar.MONTH, mmi.data.getMonth() - 1);
                    current.set(Calendar.DAY_OF_MONTH, mmi.data.getDay());
                } else if ("championshipEarn".equals(type)) {
                    int year = object.getInt("year");
                    String match = object.getString("match");
                    ChampionshipData data = CareerManager.getInstance().getChampDataManager().findDataById(match);
                    current.set(Calendar.YEAR, year);
                    current.set(Calendar.MONTH, data.getMonth() - 1);
                    current.set(Calendar.DAY_OF_MONTH, data.getDay());
                }

                InvoiceObject io = new InvoiceObject(type, (Calendar) current.clone(), object);

                invoiceObjects.add(io);
            } catch (RuntimeException e) {
                EventLogger.warning(e);
            }
        }
    }

    private void fill() {
        fill(false);
    }

    private void fill(boolean initFill) {
        listPane.getChildren().clear();

        Map<String, Integer> incomes;
        Map<String, Integer> expenditures;
        SortedMap<Calendar, Integer> dateMoneyMap = new TreeMap<>();

        if (initFill) {
            incomes = this.incomes;
            expenditures = this.expenditures;
        } else {
            incomes = new HashMap<>();
            expenditures = new HashMap<>();
        }

        int cumIncome = 0;
        int cumExpenditure = 0;

        int row = 0;
        Calendar last = CareerManager.getInstance().getBeginTimestamp();
        
        for (int idx = 0; idx < invoiceObjects.size(); idx++) {
            InvoiceObject io = invoiceObjects.get(idx);
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
                
                if (io.inGameDate.get(Calendar.YEAR) != last.get(Calendar.YEAR) ||
                        io.inGameDate.get(Calendar.MONTH) != last.get(Calendar.MONTH)) {
                    String month = String.format("%s.%s",
                            io.inGameDate.get(Calendar.YEAR),
                            io.inGameDate.get(Calendar.MONTH) + 1);
                    Label monthLabel = new Label(month);
                    monthLabel.setFont(new Font(App.FONT.getName(), 16));

                    listPane.add(new Separator(Orientation.HORIZONTAL), 0, row++, 3, 1);
                    listPane.add(monthLabel, 0, row++);
                }

                if (!isTypeSelected(io.type)) {
                    last = io.inGameDate;
                    continue;
                }

                Integer typeExpend = expenditures.get(io.type);
                if (typeExpend != null) {
                    expenditures.put(io.type, typeExpend - io.getMoneyChange());
                }
                Integer typeIncome = incomes.get(io.type);
                if (typeIncome != null) {
                    incomes.put(io.type, typeIncome + io.getMoneyChange());
                }

                String date = CareerManager.calendarToString(io.inGameDate);
                listPane.add(new Label(date), 0, row++);
                listPane.add(new Label(io.getShownType()), 0, row);

                Label desLabel = new Label(io.getItemDes());
                desLabel.setWrapText(true);
                desLabel.setMaxWidth(180.0);
                listPane.add(desLabel, 1, row++);

                listPane.add(new Separator(Orientation.HORIZONTAL), 1, row++, 2, 1);

                int mb = io.getMoneyBefore();
                Label moneyBefore = new Label(Util.moneyToReadable(mb));
                if (mb < 0) {
                    moneyBefore.setTextFill(CareerView.SPEND_MONEY_COLOR);
                }
                listPane.add(moneyBefore, 2, row++);

                for (String key : io.others.keySet()) {
                    Object obj = io.others.get(key);
                    if (obj instanceof JSONObject subObj) {
                        if ("championshipEarn".equals(io.type)) {
                            int taxes = 0;
                            for (String subKey : subObj.keySet()) {
                                ChampionshipScore.Rank cs = ChampionshipScore.Rank.valueOf(subKey);
                                listPane.add(new Label(cs.getShown()), 1, row);
                                JSONObject cEarn = subObj.getJSONObject(subKey);
                                int raw = cEarn.getInt("raw");
                                int actual = cEarn.getInt("actual");
                                taxes += (actual - raw);
                                Label rawAwd = new Label(Util.moneyToReadable(raw, true));
                                if (raw > 0) {
                                    rawAwd.setTextFill(CareerView.EARN_MONEY_COLOR);
                                }
                                listPane.add(rawAwd, 2, row++);
                            }
                            if (taxes < 0) {
                                listPane.add(new Label(strings.getString("taxes")), 1, row);
                                Label taxLabel = new Label(Util.moneyToReadable(taxes));
                                taxLabel.setTextFill(CareerView.SPEND_MONEY_COLOR);
                                listPane.add(taxLabel, 2, row++);
                            }
                        }
                    } else if (obj instanceof JSONArray subArr) {
                        for (int i = 0; i < subArr.length(); i++) {
                            JSONObject subObj = subArr.getJSONObject(i);
                            if (!subObj.has("item")) continue;

                            String item = subObj.getString("item");

                            String shownItem = formatType(item);

                            int subChange;
                            if (subObj.has("moneyCost")) {
                                subChange = -subObj.getInt("moneyCost");
                                Integer subExpend = expenditures.get(item);
                                if (subExpend != null) {
                                    expenditures.put(item, subExpend - subChange);
                                }
                            } else if (subObj.has("moneyEarned")) {
                                subChange = subObj.getInt("moneyEarned");
                            } else {
                                continue;
                            }

                            String subChangeStr = Util.moneyToReadable(subChange, true);

                            listPane.add(new Label(shownItem), 1, row);
                            Label subChangeLabel = new Label(subChangeStr);
                            if (subChange < 0)
                                subChangeLabel.setTextFill(CareerView.SPEND_MONEY_COLOR);

                            listPane.add(subChangeLabel, 2, row++);
                        }
                    }
                }

                int mc = io.getMoneyChange();
                String mcs = Util.moneyToReadable(mc, true);
                int ma = io.getMoneyAfter();
                Label moneyChange = new Label(mcs);
                if (mc > 0) {
                    moneyChange.setTextFill(CareerView.EARN_MONEY_COLOR);
                } else if (mc < 0) {
                    moneyChange.setTextFill(CareerView.SPEND_MONEY_COLOR);
                }
                listPane.add(new Separator(Orientation.HORIZONTAL), 1, row++, 2, 1);
                listPane.add(new Label(strings.getString("subtotal")), 1, row);
                listPane.add(moneyChange, 2, row++);
                Label moneyAfter = new Label(Util.moneyToReadable(ma));
                if (ma < 0) {
                    moneyAfter.setTextFill(CareerView.SPEND_MONEY_COLOR);
                }
                listPane.add(new Label(strings.getString("balanceAfter")), 1, row);
                listPane.add(moneyAfter, 2, row++);

                listPane.add(new Separator(Orientation.HORIZONTAL), 0, row++, 3, 1);

                last = io.inGameDate;

                if (mc > 0) cumIncome += mc;
                else if (mc < 0) cumExpenditure += mc;
            } catch (RuntimeException e) {
                EventLogger.warning(e);
            }
        }

        if (initFill) {
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

    class InvoiceObject {
        final Calendar inGameDate;
        final String type;
        JSONObject others;

        InvoiceObject(String type, Calendar inGameDate, JSONObject others) {
            this.inGameDate = inGameDate;
            this.type = type;
            this.others = others;
        }

        public String getShownType() {
            String upper = "INVOICE_" + Util.toAllCapsUnderscoreCase(type);
            String key = Util.toLowerCamelCase(upper);
            if (strings.containsKey(key)) return strings.getString(key);
            else return type;
        }

        public String getItemDes() {
            return switch (type) {
                case "championshipEarn" -> {
                    ChampionshipData data = CareerManager.getInstance().getChampDataManager()
                            .findDataById(others.getString("match"));
                    yield data.getName();
                }
                case "challengeEarn" -> {
                    ChallengeSet cs = ChallengeManager.getInstance().getById(others.getString("match"));
                    if (cs == null) yield "";
                    yield cs.getName();
                }
                case "invitation", "participation" -> {
                    String champInsId = others.getString("match");
                    MetaMatchInfo mmi = MatchTreeNode.analyzeMatchId(champInsId);
                    yield mmi.data.getName();
                }
                case "purchase" -> {
                    String item = others.getString("item");
                    if (others.has("itemType")) {
                        String itemType = others.getString("itemType");
//                    String typeStr = "";
                        String itemStr = "";
                        if ("cue".equals(itemType)) {
//                        typeStr = strings.getString("inventoryCues");
                            itemStr = humanCareer.getInventory().getCueByInstanceId(item).getName();
                        } else if ("tip".equals(itemType)) {
//                        typeStr = strings.getString("inventoryTips");
                            itemStr = humanCareer.getInventory().getTipByInstanceId(item).getBrand().shownName();
                        }
                        yield itemStr;
                    } else {
                        Cue cue;
                        CueTip tip;
                        if ((cue = humanCareer.getInventory().getCueByInstanceId(item)) != null) {
                            yield cue.getName();
                        } else if ((tip = humanCareer.getInventory().getTipByInstanceId(item)) != null) {
                            yield tip.getBrand().shownName();
                        } else {
                            yield "";
                        }
                    }
                }
                case "fees" -> strings.getString("fixedExpenditure");
                case "achievementAward" -> {
                    int level = others.getInt("level");
                    Achievement achievement = Achievement.valueOf(others.getString("item"));
                    yield achievement.getDescriptionOfLevel(level);
                }
                default -> "";
            };
        }

        public int getMoneyBefore() {
            return others.getInt("moneyBefore");
        }

        public int getMoneyAfter() {
            return others.getInt("moneyAfter");
        }

        public int getMoneyChange() {
            return getMoneyAfter() - getMoneyBefore();
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
