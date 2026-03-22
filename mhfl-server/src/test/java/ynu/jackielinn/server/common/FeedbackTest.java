package ynu.jackielinn.server.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Feedback 枚举单元测试：fromCode、getCode、getDescription。
 */
class FeedbackTest {

    @Test
    void fromCodeShouldReturnCorrectFeedbackForEachCode() {
        assertEquals(Feedback.NONE, Feedback.fromCode(0));
        assertEquals(Feedback.LIKED, Feedback.fromCode(1));
        assertEquals(Feedback.DISLIKED, Feedback.fromCode(-1));
    }

    @Test
    void fromCodeShouldReturnNoneForUnknownCode() {
        assertEquals(Feedback.NONE, Feedback.fromCode(2));
        assertEquals(Feedback.NONE, Feedback.fromCode(-2));
    }

    @Test
    void getCodeShouldMatchEnumValue() {
        assertEquals(0, Feedback.NONE.getCode());
        assertEquals(1, Feedback.LIKED.getCode());
        assertEquals(-1, Feedback.DISLIKED.getCode());
    }

    @Test
    void getDescriptionShouldBeNonEmpty() {
        assertNotNull(Feedback.LIKED.getDescription());
        assertFalse(Feedback.LIKED.getDescription().isEmpty());
    }

    @Test
    void valuesShouldReturnAllThreeFeedbackStates() {
        Feedback[] values = Feedback.values();
        assertEquals(3, values.length);
    }
}
