package ynu.jackielinn.server.entity;

import org.springframework.beans.BeanUtils;

import java.util.function.Consumer;

/**
 * 对象转换小工具
 */
public interface BaseData {

    default <V> V asViewObject(Class<V> clazz, Consumer<V> consumer) {
        V v = this.asViewObject(clazz);
        consumer.accept(v);
        return v;
    }

    default <V> V asViewObject(Class<V> clazz) {
        try {
            V v = clazz.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(this, v);
            return v;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("转换失败: " + e.getMessage());
        }
    }
}
