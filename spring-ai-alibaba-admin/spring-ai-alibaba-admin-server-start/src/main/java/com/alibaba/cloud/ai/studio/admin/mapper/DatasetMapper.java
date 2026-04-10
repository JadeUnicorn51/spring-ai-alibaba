package com.alibaba.cloud.ai.studio.admin.mapper;

import com.alibaba.cloud.ai.studio.admin.entity.DatasetDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;


import java.util.List;


@Mapper
public interface DatasetMapper {

    /**
     * 创建评测集
     *
     * @param dataset 评测集实体
     * @return 影响的行数
     */
    int insert(DatasetDO dataset);

    /**
     * 根据ID删除评测集（逻辑删除）
     *
     * @param id 评测集ID
     * @param tenantId 租户ID
     * @return 影响的行数
     */
    int deleteById(@Param("id") Long id, @Param("tenantId") String tenantId);

    /**
     * 根据ID获取评测集
     *
     * @param id 评测集ID
     * @param tenantId 租户ID
     * @return 评测集实体
     */
    DatasetDO selectById(@Param("id") Long id, @Param("tenantId") String tenantId);

    /**
     * 查询评测集列表
     *
     * @param name    评测集名称（模糊查询）
     * @param tenantId 租户ID
     * @param offset  偏移量
     * @param limit   数量限制
     * @return 评测集列表
     */
    List<DatasetDO> selectList(@Param("name") String name,
                               @Param("tenantId") String tenantId,
                               @Param("offset") Long offset,
                               @Param("limit") int limit);

    /**
     * 查询评测集总数
     *
     * @param name    评测集名称（模糊查询）
     * @param tenantId 租户ID
     * @return 总数
     */
    int selectCount(@Param("name") String name, @Param("tenantId") String tenantId);

    /**
     * 更新评测集
     *
     * @param id          评测集ID
     * @param name        评测集名称
     * @param description 评测集描述
     * @return 影响的行数
     */
    int update(@Param("id") Long id,
               @Param("tenantId") String tenantId,
               @Param("name") String name,
               @Param("description") String description);
}
