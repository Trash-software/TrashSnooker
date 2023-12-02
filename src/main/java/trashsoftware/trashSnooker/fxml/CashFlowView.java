package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.career.*;
import trashsoftware.trashSnooker.core.career.championship.MatchTreeNode;
import trashsoftware.trashSnooker.core.career.championship.MetaMatchInfo;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueTip;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.ResourceBundle;

public class CashFlowView extends ChildInitializable {
    
    @FXML
    GridPane listPane;

    private Stage stage;
    private final List<InvoiceObject> invoiceObjects = new ArrayList<>();

    private HumanCareer humanCareer;
    private ResourceBundle strings;

    public void setup(Stage stage, HumanCareer humanCareer) {
        this.stage = stage;
        this.humanCareer = humanCareer;
        
        createObjects(humanCareer);

        fill();
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;
    }
    
    private void createObjects(HumanCareer humanCareer) {
        List<JSONObject> invoices = humanCareer.getInvoices();
        Calendar current = CareerManager.getInstance().getBeginTimestamp();
        for (JSONObject object : invoices) {
            try {
                boolean timeIsReal = false;
                if (object.has("inGameDate")) {
                    current = CareerManager.stringToCalendar(object.getString("inGameDate"));
                    timeIsReal = true;
                }

                String type = object.getString("type");
                if (!timeIsReal && "championshipEarn".equals(type)) {
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
        listPane.getChildren().clear();
        
        int row = 0;
        Calendar last = null;
        for (InvoiceObject io : invoiceObjects) {
            try {
                if (last == null ||
                        io.inGameDate.get(Calendar.YEAR) != last.get(Calendar.YEAR) ||
                        io.inGameDate.get(Calendar.MONTH) != last.get(Calendar.MONTH)) {
                    String month = String.format("%s.%s",
                            io.inGameDate.get(Calendar.YEAR),
                            io.inGameDate.get(Calendar.MONTH) + 1);
                    Label monthLabel = new Label(month);
                    monthLabel.setFont(new Font(App.FONT.getName(), 16));

                    listPane.add(new Separator(Orientation.HORIZONTAL), 0, row++, 3, 1);
                    listPane.add(monthLabel, 0, row++);
                }

                String date = CareerManager.calendarToString(io.inGameDate);
                listPane.add(new Label(date), 0, row++);
                listPane.add(new Label(io.getType()), 0, row);
                listPane.add(new Label(io.getItemDes()), 1, row++);

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
                            
                            String sKey = Util.toLowerCamelCase("SUB_INVOICE_" + 
                                    Util.toAllCapsUnderscoreCase(subObj.getString("item")));
                            String shownItem = sKey;
                            if (strings.containsKey(sKey)) {
                                shownItem = strings.getString(sKey);
                            }
                            
                            int subChange;
                            if (subObj.has("moneyCost")) {
                                subChange = -subObj.getInt("moneyCost");
                            } else if (subObj.has("moneyEarned")) {
                                subChange = subObj.getInt("moneyEarned");
                            } else {
                                continue;
                            }
                            
                            String subChangeStr = Util.moneyToReadable(subChange, true);
                            
                            listPane.add(new Label(shownItem), 1, row);
                            Label subChangeLabel = new Label(subChangeStr);
                            if (subChange < 0) subChangeLabel.setTextFill(CareerView.SPEND_MONEY_COLOR);
                            
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
            } catch (RuntimeException e) {
                EventLogger.warning(e);
            }
        }
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
        
        public String getType() {
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
                    // todo
                    yield "";
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
                case "participation" -> {
                    String champInsId = others.getString("match");
                    MetaMatchInfo mmi = MatchTreeNode.analyzeMatchId(champInsId);
                    yield mmi.data.getName();
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
}
