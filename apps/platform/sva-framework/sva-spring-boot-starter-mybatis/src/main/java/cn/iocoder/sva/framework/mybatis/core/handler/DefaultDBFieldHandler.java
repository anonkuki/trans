package cn.iocoder.sva.framework.mybatis.core.handler;

import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.sva.framework.mybatis.core.dataobject.BaseCommonDO;
import cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 通用参数填充实现类
 *
 * 如果没有显式的对通用参数进行赋值，这里会对通用参数进行填充、赋值
 *
 * @author hexiaowu
 */
public class DefaultDBFieldHandler implements MetaObjectHandler {

    @Override
    @SuppressWarnings("PatternVariableCanBeUsed")
    public void insertFill(MetaObject metaObject) {
        if (Objects.nonNull(metaObject)) {
            Object originalObject = metaObject.getOriginalObject();

            // ====================== 修改点 1 ======================
            // 支持 BaseDO 和 BaseCommonDO 两种基类
            if (originalObject instanceof BaseDO baseDO) {
                fillBaseDO(baseDO);
            } else if (originalObject instanceof BaseCommonDO baseCommonDO) {
                fillBaseCommonDO(baseCommonDO);
            }
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时间为空，则以当前时间为更新时间
        Object modifyTime = getFieldValByName("updateTime", metaObject);
        if (Objects.isNull(modifyTime)) {
            setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        }

        // 当前登录用户不为空，更新人为空，则当前登录用户为更新人
        Object modifier = getFieldValByName("updater", metaObject);
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (Objects.nonNull(userId) && Objects.isNull(modifier)) {
            setFieldValByName("updater", userId.toString(), metaObject);
        }
    }

    // ====================== 新增：抽取公共填充方法 ======================
    private void fillBaseDO(BaseDO baseDO) {
        LocalDateTime current = LocalDateTime.now();
        if (Objects.isNull(baseDO.getCreateTime())) {
            baseDO.setCreateTime(current);
        }
        if (Objects.isNull(baseDO.getUpdateTime())) {
            baseDO.setUpdateTime(current);
        }

        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (Objects.nonNull(userId) && Objects.isNull(baseDO.getCreator())) {
            baseDO.setCreator(userId.toString());
        }
        if (Objects.nonNull(userId) && Objects.isNull(baseDO.getUpdater())) {
            baseDO.setUpdater(userId.toString());
        }
    }

    private void fillBaseCommonDO(BaseCommonDO baseCommonDO) {
        LocalDateTime current = LocalDateTime.now();
        if (Objects.isNull(baseCommonDO.getCreateTime())) {
            baseCommonDO.setCreateTime(current);
        }
        if (Objects.isNull(baseCommonDO.getUpdateTime())) {
            baseCommonDO.setUpdateTime(current);
        }

        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (Objects.nonNull(userId) && Objects.isNull(baseCommonDO.getCreator())) {
            baseCommonDO.setCreator(userId.toString());
        }
        if (Objects.nonNull(userId) && Objects.isNull(baseCommonDO.getUpdater())) {
            baseCommonDO.setUpdater(userId.toString());
        }
    }
}