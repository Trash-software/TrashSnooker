package trashsoftware.trashSnooker.core.career;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public enum ChampionshipLocation {
    AUS {
        @Override
        double flexTax(double amountGbp) {
            return stageTax(Map.of(10000, 0.0,
                            40000, 0.15,
                            100000, 0.3,
                            Double.MAX_VALUE, 0.46),
                    amountGbp / 0.65) * 0.65;
        }
    },
    CAN {
        @Override
        double flexTax(double amountGbp) {
            return stageTax(Map.of(
                            12000, 0.0,
                            50000, 0.2,
                            120000, 0.35,
                            Double.MAX_VALUE, 0.45
                    ),
                    amountGbp / 0.7) * 0.7;
        }
    },
    CHN {
        @Override
        double flexTax(double amountGbp) {
            return amountGbp * 0.2;
        }
    },
    GP {
        @Override
        double flexTax(double amountGbp) {
            return stageTax(Map.of(
                            10000, 0.125,
                            50000, 0.25,
                            150000, 0.4,
                            250000, 0.55,
                            Double.MAX_VALUE, 0.7
                    ),
                    amountGbp / 0.67) * 0.67;
        }
    },
    KSA {
        @Override
        double flexTax(double amountGbp) {
            return 0;
        }
    },
    TKG {
        @Override
        double flexTax(double amountGbp) {
            return stageTax(Map.of(10000, 0.0,
                            40000, 0.25,
                            120000, 0.35,
                            Double.MAX_VALUE, 0.45),
                    amountGbp / 0.5) * 0.5;
        }
    },
    UK {
        @Override
        double flexTax(double amountGbp) {
            return stageTax(Map.of(
                            Double.MAX_VALUE, 0.45,
                            125140.0, 0.25,
                            12570.0, 0.0),
                    amountGbp);
        }
    },
    USA {
        @Override
        double flexTax(double amountGbp) {
            return stageTax(Map.of(
                    9000, 0.1,
                    40000, 0.22,
                    100000, 0.32,
                    Double.MAX_VALUE, 0.37
            ), amountGbp / 0.8) * 0.8;
        }
    };

    /**
     * 处理那种：多少至多少收多少，超过多少的部分又收多少。低于最低key的，按0处理
     */
    double stageTax(Map<Number, Number> stageTaxRates, double amount) {
        SortedMap<Number, Number> ordered = new TreeMap<>(stageTaxRates);
        double tax = 0;
        double lastStage = 0;

        for (Map.Entry<Number, Number> entry : ordered.entrySet()) {
            double stageLimit = entry.getKey().doubleValue();
            double rate = entry.getValue().doubleValue();

            if (amount > stageLimit) {
                tax += (stageLimit - lastStage) * rate;
                lastStage = stageLimit;
            } else {
                tax += (amount - lastStage) * rate;
                return tax;
            }
        }

        // 如果金额超过了所有阶段，则使用最后一个阶段的税率
        double finalRate = stageTaxRates.get(ordered.lastKey()).doubleValue();
        tax += (amount - lastStage) * finalRate;

        return tax;
    }

    double flexTax(double amountGbp) {
        return amountGbp * 0.175;
    }

    public final double tax(double amountGbp) {
        return amountGbp * 0.025 + flexTax(amountGbp);  // 2.5%的台联费用
    }
}
