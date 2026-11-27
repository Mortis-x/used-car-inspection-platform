package com.usedcar.platform.scoring;

import com.usedcar.platform.inspection.InspectionItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 车况评分引擎：先算六大项得分，再按固定权重汇总为总分。
 * 纯函数实现，便于单元测试，也不依赖数据库。
 */
public final class ScoringEngine {

    /** 六大项权重，合计 100 */
    private static final Map<String, Integer> CATEGORY_WEIGHTS = new LinkedHashMap<>();

    static {
        CATEGORY_WEIGHTS.put("EXTERIOR", 15);
        CATEGORY_WEIGHTS.put("INTERIOR", 10);
        CATEGORY_WEIGHTS.put("ENGINE", 25);
        CATEGORY_WEIGHTS.put("CHASSIS", 20);
        CATEGORY_WEIGHTS.put("ACCIDENT", 20);
        CATEGORY_WEIGHTS.put("WATER_FIRE", 10);
    }

    private ScoringEngine() {
    }

    public record Result(int totalScore, String grade, Map<String, Integer> categoryScores) {
    }

    public static Result score(List<InspectionItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("没有检测项，无法评分");
        }

        Map<String, long[]> accumulators = new LinkedHashMap<>();
        for (InspectionItem item : items) {
            long[] acc = accumulators.computeIfAbsent(item.getCategory(), key -> new long[]{0, 0});
            int weight = item.getWeight() == null || item.getWeight() <= 0 ? 1 : item.getWeight();
            int score = item.getScore() == null ? 0 : item.getScore();
            acc[0] += (long) score * weight;
            acc[1] += weight;
        }

        Map<String, Integer> categoryScores = new LinkedHashMap<>();
        long weightedSum = 0;
        long weightSum = 0;
        for (String category : CATEGORY_WEIGHTS.keySet()) {
            long[] acc = accumulators.get(category);
            if (acc == null || acc[1] == 0) {
                continue;
            }
            int categoryScore = (int) Math.round((double) acc[0] / acc[1]);
            categoryScores.put(category, categoryScore);
            int categoryWeight = CATEGORY_WEIGHTS.get(category);
            weightedSum += (long) categoryScore * categoryWeight;
            weightSum += categoryWeight;
        }
        if (weightSum == 0) {
            throw new IllegalStateException("检测项分类无法匹配评分权重");
        }

        int totalScore = (int) Math.round((double) weightedSum / weightSum);
        return new Result(totalScore, gradeOf(totalScore), categoryScores);
    }

    public static String gradeOf(int totalScore) {
        if (totalScore >= 90) {
            return "S";
        }
        if (totalScore >= 80) {
            return "A";
        }
        if (totalScore >= 70) {
            return "B";
        }
        if (totalScore >= 60) {
            return "C";
        }
        return "D";
    }
}
