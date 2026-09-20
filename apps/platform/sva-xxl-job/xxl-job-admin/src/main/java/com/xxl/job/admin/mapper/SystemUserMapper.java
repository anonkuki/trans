package com.xxl.job.admin.mapper;

import com.xxl.job.admin.model.SystemUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统用户 Mapper 接口
 * 参照 XxlJobUserMapper 风格生成
 *
 * @author system
 * @date 2026-04-08
 */
@Mapper
public interface SystemUserMapper {

    /**
     * 分页查询用户列表
     */
    List<SystemUser> pageList(@Param("offset") int offset,
                              @Param("pagesize") int pagesize,
                              @Param("username") String username,
                              @Param("nickname") String nickname,
                              @Param("mobile") String mobile,
                              @Param("status") Integer status,
                              @Param("deptId") Long deptId);

    /**
     * 分页查询用户总数
     */
    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("username") String username,
                      @Param("nickname") String nickname,
                      @Param("mobile") String mobile,
                      @Param("status") Integer status,
                      @Param("deptId") Long deptId);

    /**
     * 根据用户名加载用户
     */
    SystemUser loadByUserName(@Param("username") String username);

    /**
     * 根据ID加载用户
     */
    SystemUser loadById(@Param("id") Long id);

    /**
     * 保存用户
     */
    int save(SystemUser systemUser);

    /**
     * 更新用户
     */
    int update(SystemUser systemUser);

    /**
     * 删除用户（逻辑删除）
     */
    int delete(@Param("id") Long id);

    /**
     * 更新最后登录信息
     */
    int updateLoginInfo(@Param("id") Long id,
                        @Param("loginIp") String loginIp,
                        @Param("loginDate") java.util.Date loginDate);

    /**
     * 批量删除用户
     */
    int batchDelete(@Param("ids") List<Long> ids);

    /**
     * 更新用户状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}