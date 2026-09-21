package cn.iocoder.sva.module.ai.controller.admin.music;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.iocoder.sva.framework.common.pojo.CommonResult;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.common.util.object.BeanUtils;
import cn.iocoder.sva.module.ai.controller.admin.music.vo.*;
import cn.iocoder.sva.module.ai.dal.dataobject.music.AiMusicDO;
import cn.iocoder.sva.module.ai.dal.dataobject.user.AdminUserDO;
import cn.iocoder.sva.module.ai.dal.mysql.user.AdminUserMapper;
import cn.iocoder.sva.module.ai.service.music.AiMusicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.sva.framework.common.pojo.CommonResult.success;
import static cn.iocoder.sva.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - AI 音乐")
@RestController
@RequestMapping("/ai/music")
public class AiMusicController {

    @Resource
    private AiMusicService musicService;

    @Resource
    private AdminUserMapper adminUserMapper;

    @GetMapping("/my-page")
    @Operation(summary = "获得【我的】音乐分页")
    public CommonResult<PageResult<AiMusicRespVO>> getMusicMyPage(@Valid AiMusicPageReqVO pageReqVO) {
        PageResult<AiMusicDO> pageResult = musicService.getMusicMyPage(pageReqVO, getLoginUserId());
        return success(BeanUtils.toBean(pageResult, AiMusicRespVO.class));
    }

    @PostMapping("/generate")
    @Operation(summary = "音乐生成")
    public CommonResult<List<Long>> generateMusic(@RequestBody @Valid AiSunoGenerateReqVO reqVO) {
        return success(musicService.generateMusic(getLoginUserId(), reqVO));
    }

    @Operation(summary = "删除【我的】音乐记录")
    @DeleteMapping("/delete-my")
    @Parameter(name = "id", required = true, description = "音乐编号", example = "1024")
    public CommonResult<Boolean> deleteMusicMy(@RequestParam("id") Long id) {
        musicService.deleteMusicMy(id, getLoginUserId());
        return success(true);
    }

    @GetMapping("/get-my")
    @Operation(summary = "获取【我的】音乐")
    @Parameter(name = "id", required = true, description = "音乐编号", example = "1024")
    public CommonResult<AiMusicRespVO> getMusicMy(@RequestParam("id") Long id) {
        AiMusicDO music = musicService.getMusic(id);
        if (music == null || ObjUtil.notEqual(getLoginUserId(), music.getUserId())) {
            return success(null);
        }
        return success(BeanUtils.toBean(music, AiMusicRespVO.class));
    }

    @PostMapping("/update-my")
    @Operation(summary = "修改【我的】音乐 目前只支持修改标题")
    @Parameter(name = "title", required = true, description = "音乐名称", example = "夜空中最亮的星")
    public CommonResult<Boolean> updateMy(AiMusicUpdateMyReqVO updateReqVO) {
        musicService.updateMyMusic(updateReqVO, getLoginUserId());
        return success(true);
    }

    // ================ 音乐管理 ================

    @GetMapping("/page")
    @Operation(summary = "获得音乐分页")
    @PreAuthorize("@ss.hasPermission('ai:music:query')")
    public CommonResult<PageResult<AiMusicRespVO>> getMusicPage(@Valid AiMusicPageReqVO pageReqVO) {
        // 如果传入了用户名，先查询用户ID
        if (pageReqVO.getUsername() != null && !pageReqVO.getUsername().isEmpty()) {
            AdminUserDO user = adminUserMapper.selectOne(AdminUserDO::getUsername, pageReqVO.getUsername());
            if (user == null) {
                // 用户不存在，返回空结果
                return success(PageResult.empty());
            }
            pageReqVO.setUserId(user.getId());
        }

        PageResult<AiMusicDO> pageResult = musicService.getMusicPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty());
        }

        // 获取所有用户ID，批量查询用户信息
        List<Long> userIds = convertList(pageResult.getList(), AiMusicDO::getUserId);
        Map<Long, String> userNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(userIds)) {
            List<AdminUserDO> users = adminUserMapper.selectBatchIds(userIds);
            userNameMap = users.stream()
                    .collect(Collectors.toMap(AdminUserDO::getId, AdminUserDO::getNickname));
        }

        // 填充用户姓名
        Map<Long, String> finalUserNameMap = userNameMap;
        return success(BeanUtils.toBean(pageResult, AiMusicRespVO.class,
                music -> music.setUserName(finalUserNameMap.get(music.getUserId()))));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除音乐")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('ai:music:delete')")
    public CommonResult<Boolean> deleteMusic(@RequestParam("id") Long id) {
        musicService.deleteMusic(id);
        return success(true);
    }

    @PutMapping("/update")
    @Operation(summary = "更新音乐")
    @PreAuthorize("@ss.hasPermission('ai:music:update')")
    public CommonResult<Boolean> updateMusic(@Valid @RequestBody AiMusicUpdateReqVO updateReqVO) {
        musicService.updateMusic(updateReqVO);
        return success(true);
    }

}
