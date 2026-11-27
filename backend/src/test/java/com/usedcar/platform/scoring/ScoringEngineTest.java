package com.usedcar.platform.scoring;

import com.usedcar.platform.inspection.InspectionItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoringEngineTest {

    private InspectionItem item(String category, int score, int weight) {
        InspectionItem item = new InspectionItem();
        item.setCategory(category);
        item.setScore(score);
        item.setWeight(weight);
        return item;
    }

    @Test
    void shouldComputeWeightedTotalAcrossCategories() {
        List<InspectionItem> items = new ArrayList<>();
        // 外观 2 项均 90（权重 3、2）→ 90，权重 15
        items.add(item("EXTERIOR", 90, 3));
        items.add(item("EXTERIOR", 90, 2));
        // 发动机 2 项 80 → 80，权重 25
        items.add(item("ENGINE", 80, 5));
        items.add(item("ENGINE", 80, 2));
        // 事故 1 项 60 → 60，权重 20
        items.add(item("ACCIDENT", 60, 5));

        ScoringEngine.Result result = ScoringEngine.score(items);

        // (90*15 + 80*25 + 60*20) / 60 = 75.83，四舍五入为 76
        assertEquals(76, result.totalScore());
        assertEquals("B", result.grade());
        assertEquals(90, result.categoryScores().get("EXTERIOR"));
        assertEquals(80, result.categoryScores().get("ENGINE"));
    }

    @Test
    void gradeBoundariesShouldMatchRules() {
        assertEquals("S", ScoringEngine.gradeOf(90));
        assertEquals("A", ScoringEngine.gradeOf(89));
        assertEquals("B", ScoringEngine.gradeOf(70));
        assertEquals("C", ScoringEngine.gradeOf(60));
        assertEquals("D", ScoringEngine.gradeOf(59));
    }

    @Test
    void emptyItemsShouldNotBeScored() {
        assertThrows(IllegalArgumentException.class, () -> ScoringEngine.score(List.of()));
    }
}
