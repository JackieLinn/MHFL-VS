package ynu.jackielinn.server.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gender 枚举单元测试：fromCode、getCode、getDescription。
 */
class GenderTest {

    @Test
    void fromCodeShouldReturnCorrectGenderForEachCode() {
        assertEquals(Gender.UNKNOWN, Gender.fromCode(0));
        assertEquals(Gender.MALE, Gender.fromCode(1));
        assertEquals(Gender.FEMALE, Gender.fromCode(2));
    }

    @Test
    void fromCodeShouldReturnUnknownForUnknownCode() {
        assertEquals(Gender.UNKNOWN, Gender.fromCode(-1));
        assertEquals(Gender.UNKNOWN, Gender.fromCode(3));
    }

    @Test
    void getCodeShouldMatchEnumValue() {
        assertEquals(0, Gender.UNKNOWN.getCode());
        assertEquals(1, Gender.MALE.getCode());
        assertEquals(2, Gender.FEMALE.getCode());
    }

    @Test
    void getDescriptionShouldBeNonEmpty() {
        assertNotNull(Gender.MALE.getDescription());
        assertFalse(Gender.MALE.getDescription().isEmpty());
    }

    @Test
    void valuesShouldReturnAllThreeGenders() {
        Gender[] values = Gender.values();
        assertEquals(3, values.length);
    }
}
