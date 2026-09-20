package cn.iocoder.sva.module.system.service.auth;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Hashtable;

/**
 * LDAP域控认证服务实现
 * <p>
 * 认证流程（Bind-Search-Bind模式）：
 * <ol>
 *   <li>使用管理员账号绑定（Bind）LDAP目录服务器，建立管理员连接</li>
 *   <li>以员工工号（cn）为条件，在Base DN下进行子树搜索（Search），定位用户的完整DN</li>
 *   <li>使用搜索到的用户DN + 用户输入的密码进行二次绑定（Bind），验证密码正确性</li>
 * </ol>
 * </p>
 * <p>
 * 配置项（application.properties）：
 * <ul>
 *   <li>{@code ldap.url} - LDAP服务器地址，如 ldap://192.168.2.16:389</li>
 *   <li>{@code ldap.admin.username} - 管理员账号（用于搜索用户）</li>
 *   <li>{@code ldap.admin.password} - 管理员密码</li>
 * </ul>
 * <p>Base DN 以常量形式定义在类中：{@code ou=科兴控股,ou=cpsinovac,dc=cpsinovac,dc=com}</p>
 * </p>
 *
 * @author 闫理想
 * @since 2026/1/11
 */
@Slf4j
@Service
public class LdapServiceImpl implements ILdapService {

    /** JNDI LDAP上下文工厂类名 */
    private static final String LDAP_CTX_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";

    /** LDAP简单认证方式（明文用户名+密码） */
    private static final String LDAP_AUTH_SIMPLE = "simple";

    /** LDAP搜索基础DN（Distinguished Name），所有用户搜索在此节点下进行 */
    private static final String LDAP_BASE_DN = "ou=科兴控股,ou=cpsinovac,dc=cpsinovac,dc=com";

    /** LDAP服务器连接地址，如 ldap://192.168.2.16:389 */
    @Value("${ldap.url}")
    private String ldapUrl;

    /** LDAP管理员账号，用于绑定目录服务器执行用户搜索操作 */
    @Value("${ldap.admin.username}")
    private String ldapAdminUsername;

    /** LDAP管理员密码 */
    @Value("${ldap.admin.password}")
    private String ldapAdminPassword;

    /**
     * 域控登录校验
     * <p>
     * 执行步骤：
     * <ol>
     *   <li>参数空值校验</li>
     *   <li>使用管理员账号绑定LDAP服务器</li>
     *   <li>根据员工工号（cn属性）搜索用户，获取用户完整DN</li>
     *   <li>使用用户DN+密码进行二次绑定，验证密码</li>
     * </ol>
     * </p>
     *
     * @param employeeId 员工工号（对应LDAP中的cn属性，如SVA00024）
     * @param password   用户密码
     * @return 认证通过返回true；用户不存在或密码错误返回false
     * @throws RuntimeException 当LDAP服务器不可达、管理员认证失败等基础设施异常时抛出
     */
    @Override
    public boolean loginVerify(String employeeId, String password) {
        // 参数空值校验
        if (StrUtil.hasBlank(employeeId, password)) {
            log.warn("LDAP登录校验参数为空, employeeId={}", employeeId);
            return false;
        }

        DirContext adminCtx = null;
        NamingEnumeration<SearchResult> results = null;
        try {
            // 第一步：使用管理员账号绑定LDAP服务器
            adminCtx = createDirContext(ldapAdminUsername, ldapAdminPassword);

            // 第二步：构造搜索过滤器，对工号进行特殊字符转义防止LDAP注入
            String searchFilter = "(cn=" + escapeLdapFilter(employeeId) + ")";
            SearchControls searchControls = new SearchControls();
            // 在Base DN下进行子树搜索（包含所有子OU）
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            // 只需要返回DN，无需其他属性，减少网络传输
            searchControls.setReturningAttributes(new String[]{"dn"});
            // 最多返回1条结果即可
            searchControls.setCountLimit(1);

            // 执行搜索
            results = adminCtx.search(LDAP_BASE_DN, searchFilter, searchControls);

            // 未找到匹配用户
            if (!results.hasMore()) {
                log.warn("LDAP中未找到工号为[{}]的员工", employeeId);
                return false;
            }

            // 获取用户的完整DN（如：CN=SVA00024,OU=某部门,OU=科兴控股,DC=cpsinovac,DC=com）
            String userDN = results.next().getNameInNamespace();
            log.info("LDAP用户匹配成功, employeeId={}", employeeId);

            // 第三步：使用用户DN+密码进行二次绑定，验证密码
            boolean authenticated = verifyPassword(userDN, password);
            if (!authenticated) {
                log.warn("LDAP密码校验失败, employeeId={}", employeeId);
            }
            return authenticated;

        } catch (AuthenticationException e) {
            // 管理员账号认证失败，属于配置错误，必须抛出异常
            log.error("LDAP管理员认证失败, 请检查管理员账号配置", e);
            throw new RuntimeException("LDAP管理员认证失败", e);
        } catch (NamingException e) {
            // LDAP连接异常、搜索异常等基础设施错误
            log.error("LDAP操作异常", e);
            throw new RuntimeException("LDAP服务异常", e);
        } finally {
            // 确保搜索结果和管理员连接被正确关闭
            closeQuietly(results);
            closeQuietly(adminCtx);
        }
    }

    // ======================== private methods ========================

    /**
     * 以用户DN+密码进行LDAP绑定，验证密码是否正确
     * <p>
     * 原理：LDAP的简单绑定（Simple Bind）会用提供的DN和密码向服务器发起认证，
     * 如果密码正确则绑定成功，密码错误则抛出{@link AuthenticationException}。
     * </p>
     *
     * @param userDN   用户的完整DN（Distinguished Name）
     * @param password 用户输入的密码
     * @return 密码正确返回true，密码错误返回false
     * @throws RuntimeException LDAP连接异常等非认证错误时抛出
     */
    private boolean verifyPassword(String userDN, String password) {
        DirContext userCtx = null;
        try {
            userCtx = createDirContext(userDN, password);
            // 绑定成功，密码正确
            return true;
        } catch (AuthenticationException e) {
            // 绑定失败，密码错误（这是正常业务场景，不需要打印堆栈）
            return false;
        } catch (NamingException e) {
            // 非密码错误的连接异常，属于基础设施故障
            log.error("LDAP密码验证时发生连接异常, userDN={}", userDN, e);
            throw new RuntimeException("LDAP服务异常", e);
        } finally {
            closeQuietly(userCtx);
        }
    }

    /**
     * 创建LDAP目录上下文（简单绑定认证）
     * <p>
     * 使用JNDI的{@link InitialDirContext}建立与LDAP服务器的连接。
     * 该方法同时用于管理员绑定和用户密码验证绑定，通过不同的principal和credentials区分。
     * </p>
     *
     * @param principal   绑定主体（管理员用户名 或 用户完整DN）
     * @param credentials 绑定凭证（密码）
     * @return 已建立连接的目录上下文
     * @throws NamingException 连接失败或认证失败时抛出
     */
    private DirContext createDirContext(String principal, String credentials) throws NamingException {
        Hashtable<String, String> env = new Hashtable<>(8);
        env.put(Context.INITIAL_CONTEXT_FACTORY, LDAP_CTX_FACTORY);
        env.put(Context.PROVIDER_URL, ldapUrl);
        env.put(Context.SECURITY_AUTHENTICATION, LDAP_AUTH_SIMPLE);
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, credentials);
        return new InitialDirContext(env);
    }

    /**
     * 转义LDAP搜索过滤器中的特殊字符，防止LDAP注入攻击
     * <p>
     * 根据RFC 4515规范，以下字符在LDAP过滤器中具有特殊含义，必须转义：
     * <ul>
     *   <li>{@code \} → {@code \5c}</li>
     *   <li>{@code *} → {@code \2a}（通配符）</li>
     *   <li>{@code (} → {@code \28}（过滤器左括号）</li>
     *   <li>{@code )} → {@code \29}（过滤器右括号）</li>
     *   <li>{@code \0} → {@code \00}（空字符）</li>
     * </ul>
     * </p>
     *
     * @param input 原始输入字符串（如员工工号）
     * @return 转义后的安全字符串
     */
    private static String escapeLdapFilter(String input) {
        StringBuilder sb = new StringBuilder(input.length() + 10);
        for (char c : input.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\5c"); break;
                case '*':  sb.append("\\2a"); break;
                case '(':  sb.append("\\28"); break;
                case ')':  sb.append("\\29"); break;
                case '\0': sb.append("\\00"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 静默关闭LDAP搜索结果枚举，忽略关闭时的异常
     *
     * @param enumeration LDAP搜索结果枚举，允许为null
     */
    private static void closeQuietly(NamingEnumeration<?> enumeration) {
        if (enumeration != null) {
            try {
                enumeration.close();
            } catch (NamingException ignored) {
            }
        }
    }

    /**
     * 静默关闭LDAP目录上下文连接，忽略关闭时的异常
     *
     * @param context LDAP目录上下文，允许为null
     */
    private static void closeQuietly(DirContext context) {
        if (context != null) {
            try {
                context.close();
            } catch (NamingException ignored) {
            }
        }
    }
}

