package cn.iocoder.sva.module.system.service.auth;

/**
 * LDAP域控认证服务接口
 *
 * @author 闫理想
 * @since 2026/1/11
 */
public interface ILdapService {

    /**
     * 域控登录校验
     *
     * @param employeeId 员工工号（对应LDAP中的cn属性，如SVA00024）
     * @param password   用户密码
     * @return 认证通过返回true；用户不存在或密码错误返回false
     * @throws RuntimeException 当LDAP服务器不可达、管理员认证失败等基础设施异常时抛出
     */
    boolean loginVerify(String employeeId, String password);
}
