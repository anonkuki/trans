package cn.iocoder.sva.module.ai.dal.mysql.translation;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.sva.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranTextHistoryPageReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranTextHistoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI词句翻译历史 Mapper
 *
 * @author like
 */
@Mapper
public interface TranTextHistoryMapper extends BaseMapperX<TranTextHistoryDO> {

    /**
     * 分页查询翻译历史，支持按原文/译文模糊搜索
     *
     * @param reqVO 分页查询条件
     * @return 翻译历史分页
     */
    default PageResult<TranTextHistoryDO> selectPage(TranTextHistoryPageReqVO reqVO) {
        LambdaQueryWrapperX<TranTextHistoryDO> wrapper = new LambdaQueryWrapperX<TranTextHistoryDO>()
                .eqIfPresent(TranTextHistoryDO::getUsername, reqVO.getUsername())
                .betweenIfPresent(TranTextHistoryDO::getCreateTime, reqVO.getCreateTime());
        // 关键字模糊搜索：匹配原文或译文（用括号包裹，避免与其它条件产生 OR 冲突）
        if (StrUtil.isNotBlank(reqVO.getKeyword())) {
            String keyword = reqVO.getKeyword().trim();
            wrapper.and(w -> w.like(TranTextHistoryDO::getSourceText, keyword)
                    .or().like(TranTextHistoryDO::getTargetText, keyword));
        }
        wrapper.orderByDesc(TranTextHistoryDO::getId);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 统计指定用户的历史记录数量
     *
     * @param username 用户名（工号）
     * @return 记录数量
     */
    default Long selectCountByUsername(String username) {
        return selectCount(new LambdaQueryWrapperX<TranTextHistoryDO>()
                .eq(TranTextHistoryDO::getUsername, username));
    }

    /**
     * 查询指定用户最早的历史记录ID（用于裁剪，仅保留最近 N 条）
     *
     * @param username 用户名（工号）
     * @param limit    需要删除的最早记录数量
     * @return 最早的历史记录列表
     */
    default List<TranTextHistoryDO> selectOldestByUsername(String username, long limit) {
        return selectList(new LambdaQueryWrapperX<TranTextHistoryDO>()
                .eq(TranTextHistoryDO::getUsername, username)
                .orderByAsc(TranTextHistoryDO::getId)
                .last("LIMIT " + limit));
    }

}
