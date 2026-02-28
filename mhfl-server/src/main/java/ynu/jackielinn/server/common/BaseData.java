package ynu.jackielinn.server.common;

import org.springframework.beans.BeanUtils;

import java.util.function.Consumer;

/**
 * 实体与 VO 转换接口。通过 BeanUtils 拷贝属性将当前对象转为指定类型的视图对象，支持链式回调。
 */
public interface BaseData {

    /**
     * 转为指定类型视图对象，并在返回前用 consumer 进行额外设置（如 token、expire）。
     *
     * @param clazz    目标 VO 类型
     * @param consumer 对生成的 VO 进行二次处理的回调
     * @param <V>      视图类型
     * @return 填充后的视图对象
     */
    default <V> V asViewObject(Class<V> clazz, Consumer<V> consumer) {
        V v = this.asViewObject(clazz);
        consumer.accept(v);
        return v;
    }

    /**
     * 将当前对象属性拷贝为指定类型的视图对象。
     *
     * @param clazz 目标 VO 类型，需有无参构造
     * @param <V>   视图类型
     * @return 新创建的视图对象
     * @throws RuntimeException 反射创建或拷贝失败时抛出
     */
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
