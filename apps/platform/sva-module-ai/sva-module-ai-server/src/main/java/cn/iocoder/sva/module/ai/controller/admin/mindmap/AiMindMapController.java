package cn.iocoder.sva.module.ai.controller.admin.mindmap;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.module.ai.controller.admin.mindmap.vo.AiMindMapGenerateReqVO;
import cn.iocoder.sva.module.ai.controller.admin.mindmap.vo.AiMindMapPageReqVO;
import cn.iocoder.sva.module.ai.controller.admin.mindmap.vo.AiMindMapRespVO;
import cn.iocoder.sva.module.ai.dal.dataobject.mindmap.AiMindMapDO;
import cn.iocoder.sva.module.ai.dal.dataobject.user.AdminUserDO;
import cn.iocoder.sva.module.ai.dal.mysql.user.AdminUserMapper;
import cn.iocoder.sva.module.ai.service.mindmap.AiMindMapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.sva.framework.common.pojo.CommonResult.success;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - AI 思维导图")
@RestController
@RequestMapping("/ai/mind-map")
public class AiMindMapController {

    @Resource
    private AiMindMapService mindMapService;

    @Resource
    private AdminUserMapper adminUserMapper;

    @PostMapping(value = "/generate-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "导图生成（流式）", description = "流式返回，响应较快")
    public Flux<CommonResult<String>> generateMindMap(@RequestBody @Valid AiMindMapGenerateReqVO generateReqVO) {
        return mindMapService.generateMindMap(generateReqVO, getLoginUserId());
    }

    // ================ 导图管理 ================

    @DeleteMapping("/delete")
    @Operation(summary = "删除思维导图")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('ai:mind-map:delete')")
    public CommonResult<Boolean> deleteMindMap(@RequestParam("id") Long id) {
        mindMapService.deleteMindMap(id);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得思维导图分页")
    @PreAuthorize("@ss.hasPermission('ai:mind-map:query')")
    public CommonResult<PageResult<AiMindMapRespVO>> getMindMapPage(@Valid AiMindMapPageReqVO pageReqVO) {
        // 如果传入了用户名，先查询用户ID
        if (pageReqVO.getUsername() != null && !pageReqVO.getUsername().isEmpty()) {
            AdminUserDO user = adminUserMapper.selectOne(AdminUserDO::getUsername, pageReqVO.getUsername());
            if (user == null) {
                // 用户不存在，返回空结果
                return success(PageResult.empty());
            }
            pageReqVO.setUserId(user.getId());
        }

        PageResult<AiMindMapDO> pageResult = mindMapService.getMindMapPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty());
        }

        // 获取所有用户ID，批量查询用户信息
        List<Long> userIds = convertList(pageResult.getList(), AiMindMapDO::getUserId);
        Map<Long, String> userNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(userIds)) {
            List<AdminUserDO> users = adminUserMapper.selectBatchIds(userIds);
            userNameMap = users.stream()
                    .collect(Collectors.toMap(AdminUserDO::getId, AdminUserDO::getNickname));
        }

        // 填充用户姓名
        Map<Long, String> finalUserNameMap = userNameMap;
        return success(BeanUtils.toBean(pageResult, AiMindMapRespVO.class,
                mindMap -> mindMap.setUserName(finalUserNameMap.get(mindMap.getUserId()))));
    }

}
