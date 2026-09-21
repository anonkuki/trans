package cn.iocoder.sva.module.ai.controller.admin.translation;

import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranTextHistoryPageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranTextHistoryRespVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranTextTranslateReqVO;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranTextTranslateRespVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranTextHistoryDO;
import cn.iocoder.sva.module.ai.service.translation.TranTextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.sva.framework.common.pojo.CommonResult.success;

/**
 * AI词句翻译 REST API 控制器
 * <p>
 * 提供“词句翻译”页面所需的接口：
 * <ul>
 *   <li>POST /ai/tran/text/translate - 翻译文本</li>
 *   <li>GET /ai/tran/text/history/page - 翻译历史分页（支持模糊搜索）</li>
 *   <li>DELETE /ai/tran/text/history/delete - 删除单条历史</li>
 *   <li>DELETE /ai/tran/text/history/clear - 清空当前用户历史</li>
 * </ul>
 *
 * @author like
 */
@Tag(name = "管理后台 - AI词句翻译")
@Slf4j
@RestController
@RequestMapping("/ai/tran/text")
@Validated
public class TranTextController {

    @Resource
    private TranTextService tranTextService;

    @PostMapping("/translate")
    @Operation(summary = "翻译文本")
    public CommonResult<TranTextTranslateRespVO> translate(@Valid @RequestBody TranTextTranslateReqVO reqVO) {
        return success(tranTextService.translateText(reqVO));
    }

    @GetMapping("/history/page")
    @Operation(summary = "获得词句翻译历史分页")
    public CommonResult<PageResult<TranTextHistoryRespVO>> getHistoryPage(@Valid TranTextHistoryPageReqVO pageReqVO) {
        PageResult<TranTextHistoryDO> pageResult = tranTextService.getHistoryPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TranTextHistoryRespVO.class));
    }

    @DeleteMapping("/history/delete")
    @Operation(summary = "删除词句翻译历史")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteHistory(@RequestParam("id") Long id) {
        tranTextService.deleteHistory(id);
        return success(true);
    }

    @DeleteMapping("/history/clear")
    @Operation(summary = "清空当前用户的词句翻译历史")
    public CommonResult<Integer> clearHistory() {
        return success(tranTextService.clearHistory());
    }

}
