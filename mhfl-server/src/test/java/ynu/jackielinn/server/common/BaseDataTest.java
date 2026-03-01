package ynu.jackielinn.server.common;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BaseData 接口默认方法单元测试：asViewObject(Class)、asViewObject(Class, Consumer)。
 */
class BaseDataTest {

    @Test
    void asViewObjectShouldCopyPropertiesToNewInstance() {
        SourceBean source = new SourceBean();
        source.setName("test");
        source.setValue(100);

        TargetVo vo = source.asViewObject(TargetVo.class);

        assertNotNull(vo);
        assertEquals("test", vo.getName());
        assertEquals(100, vo.getValue());
    }

    @Test
    void asViewObjectWithConsumerShouldApplyConsumerAfterCopy() {
        SourceBean source = new SourceBean();
        source.setName("a");
        AtomicReference<String> captured = new AtomicReference<>();

        TargetVo vo = source.asViewObject(TargetVo.class, v -> {
            v.setExtra("extra");
            captured.set(v.getName());
        });

        assertEquals("a", vo.getName());
        assertEquals("extra", vo.getExtra());
        assertEquals("a", captured.get());
    }

    @Test
    void asViewObjectShouldThrowWhenTargetHasNoNoArgConstructor() {
        SourceBean source = new SourceBean();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> source.asViewObject(NoNoArgConstructor.class));
        assertTrue(ex.getMessage().contains("转换失败"));
    }

    /**
     * 实现 BaseData 的简单源对象。
     */
    public static class SourceBean implements BaseData {
        private String name;
        private int value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    /**
     * 目标 VO，需有无参构造。
     */
    public static class TargetVo {
        private String name;
        private int value;
        private String extra;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public String getExtra() {
            return extra;
        }

        public void setExtra(String extra) {
            this.extra = extra;
        }
    }

    /**
     * 无无参构造的类，用于触发反射异常。
     */
    public static class NoNoArgConstructor implements BaseData {
        @SuppressWarnings("unused")
        public NoNoArgConstructor(String required) {
        }
    }
}
