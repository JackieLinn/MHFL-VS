package ynu.jackielinn.server.service;

import ynu.jackielinn.server.dto.response.SystemResourcesVO;

/**
 * 资源管理服务接口
 */
public interface ResourceService {

    /**
     * 获取系统资源信息
     * 调用Python FastAPI获取CPU、内存、GPU信息
     *
     * @return 系统资源信息
     */
    SystemResourcesVO getSystemResources();
}
