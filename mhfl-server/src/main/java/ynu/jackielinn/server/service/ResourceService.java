package ynu.jackielinn.server.service;

import ynu.jackielinn.server.dto.response.SystemResourcesVO;

public interface ResourceService {

    /**
     * 获取系统资源实时信息（CPU、内存、GPU 使用率及用量）。
     *
     * @return 封装后的系统资源 VO
     */
    SystemResourcesVO getSystemResources();
}
