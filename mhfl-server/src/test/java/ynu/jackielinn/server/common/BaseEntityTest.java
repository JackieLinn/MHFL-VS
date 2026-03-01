package ynu.jackielinn.server.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BaseEntity 单元测试：字段访问及继承自 BaseData 的 asViewObject。
 */
class BaseEntityTest {

    @Test
    void concreteEntityShouldCopyToViewObject() {
        ConcreteEntity entity = new ConcreteEntity();
        entity.setId(1L);
        entity.setCreateTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        entity.setUpdateTime(LocalDateTime.of(2026, 1, 2, 0, 0));
        entity.setDeleted(0);

        ConcreteEntityVO vo = entity.asViewObject(ConcreteEntityVO.class);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertNotNull(vo.getCreateTime());
        assertEquals(2026, vo.getCreateTime().getYear());
        assertEquals(0, vo.getDeleted());
    }

    @Test
    void concreteEntityAsViewObjectWithConsumerShouldApplyConsumer() {
        ConcreteEntity entity = new ConcreteEntity();
        entity.setId(3L);

        ConcreteEntityVO vo = entity.asViewObject(ConcreteEntityVO.class, v -> v.setId(999L));

        assertEquals(999L, vo.getId());
    }

    /**
     * 具体实体子类，用于测试 BaseEntity。
     */
    public static class ConcreteEntity extends BaseEntity {
        // 无额外字段，仅用于测试基类行为
    }

    /**
     * 对应 VO，用于 asViewObject 目标。
     */
    @SuppressWarnings("unused")
    public static class ConcreteEntityVO {
        private Long id;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private LocalDateTime deleteTime;
        private Integer deleted;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }

        public LocalDateTime getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
        }

        public LocalDateTime getDeleteTime() {
            return deleteTime;
        }

        public void setDeleteTime(LocalDateTime deleteTime) {
            this.deleteTime = deleteTime;
        }

        public Integer getDeleted() {
            return deleted;
        }

        public void setDeleted(Integer deleted) {
            this.deleted = deleted;
        }
    }
}
