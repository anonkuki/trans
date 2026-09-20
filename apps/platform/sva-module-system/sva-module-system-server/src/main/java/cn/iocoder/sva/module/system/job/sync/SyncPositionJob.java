package cn.iocoder.sva.module.system.job.sync;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.sva.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncPositionDO;
import cn.iocoder.sva.module.system.dal.mysql.dept.PostMapper;
import cn.iocoder.sva.module.system.dal.mysql.sync.SyncPositionMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 岗位信息同步定时任务
 *
 * @author 李可
 */
@Slf4j
@Component
public class SyncPositionJob {

    @Autowired
    private SyncPositionMapper syncPositionMapper;

    @Autowired
    private PostMapper postMapper;

    @Value("${syncPosiUrl}")
    private String syncPositionUrl;

    @Value("${syncAuth.username}")
    private String basicAuthUsername;

    @Value("${syncAuth.password}")
    private String basicAuthPassword;

    @Value("${syncAuth.userid}")
    private String syncUserId;

    @Autowired
    private RestTemplate restTemplate;

    // 岗位同步的 INF_ID
    private static final String INF_ID = "DC_POSITION";
    private static final int PAGE_SIZE = 2000;

    private RestTemplate getSimpleRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(30000);
        return new RestTemplate(factory);
    }

    @XxlJob(value = "syncPosition", init = "init", destroy = "destroy")
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> execute() {
//        RestTemplate restTemplate = getSimpleRestTemplate();
        log.info("==================== 开始同步岗位信息 ====================");
//        log.info("请求URL: {}", syncPositionUrl);
//        log.info("Basic Auth用户名: {}", basicAuthUsername);
//        log.info("Basic Auth密码: {}", basicAuthPassword);
//        log.info("INF_ID: {}", INF_ID);
//        log.info("USERID参数: {}", getUserId());
        long startTime = System.currentTimeMillis();

        try {
            int totalCount = getTotalCount(restTemplate);
            log.info("需要同步的岗位总数: {}", totalCount);

            if (totalCount == 0) {
                log.info("没有需要同步的数据");
                return ReturnT.SUCCESS;
            }

            int totalPages = (totalCount + PAGE_SIZE - 1) / PAGE_SIZE;
            log.info("总页数: {}", totalPages);

            List<SyncPositionDO> allDataList = new ArrayList<>();
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                log.info("正在同步第 {}/{} 页", pageNum, totalPages);
                List<SyncPositionDO> pageData = fetchPageData(restTemplate, pageNum);
                allDataList.addAll(pageData);
                log.info("第 {} 页同步完成，本页数据量: {}", pageNum, pageData.size());
            }

            if (!allDataList.isEmpty()) {
                // 删除同步表旧数据
                syncPositionMapper.delete(null);
                log.info("已删除同步表所有旧数据");

                // 批量插入同步表新数据
                int insertCount = 0;
                int batchSize = 1000;
                for (int i = 0; i < allDataList.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, allDataList.size());
                    List<SyncPositionDO> batchList = allDataList.subList(i, end);
                    for (SyncPositionDO syncPositionDO : batchList) {
                        syncPositionMapper.insert(syncPositionDO);
                        insertCount++;
                    }
                    log.info("已插入 {} 条数据到同步表", insertCount);
                }

                // 根据code新增或更新岗位数据（更新时保持id不变）
                savePostData(convertToPostDOList(allDataList));

                log.info("数据同步完成，共处理 {} 条记录", allDataList.size());
            } else {
                log.warn("未获取到任何数据");
            }

            long endTime = System.currentTimeMillis();
            log.info("岗位信息同步完成，总耗时: {} ms", (endTime - startTime));
            log.info("==================== 同步结束 ====================");
            return ReturnT.SUCCESS;

        } catch (Exception e) {
            log.error("岗位信息同步失败", e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "同步失败: " + e.getMessage());
        }
    }

    //将转换后的岗位数据根据code新增或更新到数据库（更新时保持id不变）
    private void savePostData(List<PostDO> postList) {
        for (PostDO postDO : postList) {
            String code = postDO.getCode();
            // 根据code查询已存在的记录
            PostDO existingPost = postMapper.selectOne(new QueryWrapper<PostDO>().eq("code", code));

            if (existingPost == null) {
                // 新增：使用传入的id
                postMapper.insert(postDO);
                log.debug("新增岗位：CODE={}, ID={}, 名称={}", code, postDO.getId(), postDO.getName());
            } else {
                // 更新：保持数据库中的原id不变
                postDO.setId(existingPost.getId());  // 关键：使用数据库中的id，确保id不变
                postMapper.update(postDO, new QueryWrapper<PostDO>().eq("code", code));
                log.debug("更新岗位：CODE={}, ID={}, 名称={}", code, existingPost.getId(), postDO.getName());
            }
        }
    }

    // 岗位信息同步数据转换为岗位数据
    private List<PostDO> convertToPostDOList(List<SyncPositionDO> syncPositionDOList) {
        List<PostDO> postDOList = new ArrayList<>();
        int sort = 0;
        for (SyncPositionDO syncPositionDO : syncPositionDOList) {
            String deptid = syncPositionDO.getDeptid();
            if (deptid == null) {
                log.info("岗位信息同步数据中，deptid为空，请检查数据");
                deptid="0";
            }
            String positionNbr = syncPositionDO.getPositionNbr();
            if (StrUtil.isBlank(positionNbr)) {
                log.info("岗位信息同步数据中，positionNbr为空，请检查数据");
                continue;
            }

            PostDO postDO = new PostDO();

            try {
                postDO.setId(Long.parseLong(positionNbr));
            } catch (Exception e) {
                log.error("出现异常岗位ID: {}", positionNbr);
                continue;
            }
            postDO.setName(syncPositionDO.getDescr());
            postDO.setCode(syncPositionDO.getPositionNbr());
            postDO.setSort(++sort);
            if(!StrUtil.isBlank(syncPositionDO.getDcInfDtStatus())){
                postDO.setStatus(syncPositionDO.getDcInfDtStatus().equals("D") ? 1 : 0);
            } else {
                postDO.setStatus(1);
            }

            postDO.setGuid(syncPositionDO.getGuid());
            postDO.setCreateTime(LocalDateTime.now());
            postDO.setDescrshort(syncPositionDO.getDescrshort());
            postDO.setDcJobLevel(syncPositionDO.getDcJobLevel());
            postDO.setDcJobLevelDescr(syncPositionDO.getDcJobLevelDescr());
            postDO.setDcJobGrade(syncPositionDO.getDcJobGrade());
            postDO.setDcJobGradeDescr(syncPositionDO.getDcJobGradeDescr());
            postDO.setDcJobStage(syncPositionDO.getDcJobStage());
            postDO.setDcJobStageDescr(syncPositionDO.getDcJobStageDescr());
            postDO.setReportsTo(syncPositionDO.getReportsTo());
            postDO.setReportsToDescr(syncPositionDO.getReportsToDescr());
            postDO.setDcPositionDescra(syncPositionDO.getDcPositionDescra());
            try {
                postDO.setDeptid(Long.parseLong(deptid));
            } catch (Exception e){
                postDO.setDeptid(0L);
            }
            postDO.setDcDeptDescr50(syncPositionDO.getDcDeptDescr50());
            postDO.setBusinessUnit(syncPositionDO.getBusinessUnit());
            postDO.setBusinessDescr(syncPositionDO.getBusinessDescr());

            postDOList.add(postDO);
        }
        return postDOList;
    }

    private String getUserId() {
        return StringUtils.hasText(syncUserId) ? syncUserId : basicAuthUsername;
    }

    private int getTotalCount(RestTemplate restTemplate) {
        try {
            Map<String, String> requestBody = buildRequestBody(1, 1);
            ResponseEntity<Map> response = sendRequest(restTemplate, requestBody);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("TOTALROWCOUNT")) {
                Object totalRowCount = responseBody.get("TOTALROWCOUNT");
                if (totalRowCount instanceof Integer) {
                    return (int) totalRowCount;
                } else if (totalRowCount instanceof Number) {
                    return ((Number) totalRowCount).intValue();
                }
            }
            return 0;
        } catch (Exception e) {
            log.error("获取总记录数失败", e);
            throw new RuntimeException("获取总记录数失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<SyncPositionDO> fetchPageData(RestTemplate restTemplate, int pageNum) {
        try {
            Map<String, String> requestBody = buildRequestBody(pageNum, PAGE_SIZE);
            ResponseEntity<Map> response = sendRequest(restTemplate, requestBody);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null || !responseBody.containsKey("DATA")) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> dataList = (List<Map<String, Object>>) responseBody.get("DATA");
            if (dataList == null || dataList.isEmpty()) {
                return new ArrayList<>();
            }

            List<SyncPositionDO> syncPositionDOList = new ArrayList<>();
            for (Map<String, Object> item : dataList) {
                SyncPositionDO syncPositionDO = convertToSyncPositionDO(item);
                syncPositionDOList.add(syncPositionDO);
            }

            return syncPositionDOList;
        } catch (Exception e) {
            log.error("获取第 {} 页数据失败", pageNum, e);
            throw new RuntimeException("获取第 " + pageNum + " 页数据失败", e);
        }
    }

    private Map<String, String> buildRequestBody(int pageNum, int pageSize) {
        return Map.of(
                "INF_ID", INF_ID,
                "PAGENBR", String.valueOf(pageNum),
                "PAGESIZE", String.valueOf(pageSize),
                "BGNDTTM", "2020-03-27 14:30:25",
                "ENDDTTM", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "USERID", getUserId()
        );
    }

    private ResponseEntity<Map> sendRequest(RestTemplate restTemplate, Map<String, String> requestBody) {
        try {
            String auth = basicAuthUsername + ":" + basicAuthPassword;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            String authorization = "Basic " + encodedAuth;

            String jsonBody = String.format(
                    "{\"INF_ID\":\"%s\",\"PAGENBR\":\"%s\",\"PAGESIZE\":\"%s\",\"BGNDTTM\":\"%s\",\"ENDDTTM\":\"%s\",\"USERID\":\"%s\"}",
                    requestBody.get("INF_ID"),
                    requestBody.get("PAGENBR"),
                    requestBody.get("PAGESIZE"),
                    requestBody.get("BGNDTTM"),
                    requestBody.get("ENDDTTM"),
                    requestBody.get("USERID")
            );

            log.info("========== 发送HTTP请求 ==========");
            log.info("请求URL: {}", syncPositionUrl);
            log.info("Authorization: {}", authorization);
            log.info("请求体: {}", jsonBody);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", authorization);
            headers.set("User-Agent", "PostmanRuntime/7.15.2");
            headers.set("Accept", "*/*");
            headers.set("Cache-Control", "no-cache");
            headers.set("Connection", "keep-alive");

            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    syncPositionUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            log.info("响应状态码: {}", response.getStatusCode());
            log.info("响应体: {}", response.getBody());
            log.info("=================================");

            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("请求失败，状态码: {}", response.getStatusCode());
                throw new RuntimeException("请求失败，状态码: " + response.getStatusCode());
            }

            return response;

        } catch (Exception e) {
            log.error("发送HTTP请求失败，URL: {}", syncPositionUrl, e);
            throw new RuntimeException("发送HTTP请求失败", e);
        }
    }

    private SyncPositionDO convertToSyncPositionDO(Map<String, Object> map) {
        return SyncPositionDO.builder()
                .guid(getStringValue(map, "GUID"))
                .dcInfDtStatus(getStringValue(map, "DC_INF_DT_STATUS"))
                .dcInfDtStatusDescr(getStringValue(map, "DC_INF_DT_STATUS_DESCR"))
                .positionNbr(getStringValue(map, "POSITION_NBR"))
                .effdt(parseLocalDate(getStringValue(map, "EFFDT")))
                .effStatus(getStringValue(map, "EFF_STATUS"))
                .effStatusDescr(getStringValue(map, "EFF_STATUS_DESCR"))
                .descr(getStringValue(map, "DESCR"))
                .descrshort(getStringValue(map, "DESCRSHORT"))
                .businessUnit(getStringValue(map, "BUSINESS_UNIT"))
                .businessDescr(getStringValue(map, "BUSINESS_DESCR"))
                .regRegion(getStringValue(map, "REG_REGION"))
                .regRegionDescr(getStringValue(map, "REG_REGION_DESCR"))
                .location(getStringValue(map, "LOCATION"))
                .dcLocationDescr(getStringValue(map, "DC_LOCATION_DESCR"))
                .deptid(getStringValue(map, "DEPTID"))
                .dcDeptDescr50(getStringValue(map, "DC_DEPT_DESCR50"))
                .dcDeptDescrshort(getStringValue(map, "DC_DEPT_DESCRSHORT"))
                .jobcode(getStringValue(map, "JOBCODE"))
                .dcJobcodeDescr(getStringValue(map, "DC_JOBCODE_DESCR"))
                .dcJobcodeDescrs(getStringValue(map, "DC_JOBCODE_DESCRS"))
                .dcJobGroup(getStringValue(map, "DC_JOB_GROUP"))
                .dcJobGroupDescr(getStringValue(map, "DC_JOB_GROUP_DESCR"))
                .dcJobSequence(getStringValue(map, "DC_JOB_SEQUENCE"))
                .dcJobSeqDescr(getStringValue(map, "DC_JOB_SEQ_DESCR"))
                .dcFirstJobCate(getStringValue(map, "DC_FIRST_JOB_CATE"))
                .dcJobcateDescr(getStringValue(map, "DC_JOBCATE_DESCR"))
                .dcJobLevel(getStringValue(map, "DC_JOB_LEVEL"))
                .dcJobLevelDescr(getStringValue(map, "DC_JOB_LEVEL_DESCR"))
                .dcJobGrade(getStringValue(map, "DC_JOB_GRADE"))
                .dcJobGradeDescr(getStringValue(map, "DC_JOB_GRADE_DESCR"))
                .dcJobStage(getStringValue(map, "DC_JOB_STAGE"))
                .dcJobStageDescr(getStringValue(map, "DC_JOB_STAGE_DESCR"))
                .reportsTo(getStringValue(map, "REPORTS_TO"))
                .reportsToDescr(getStringValue(map, "REPORTS_TO_DESCR"))
                .dcPositionDescra(getStringValue(map, "DC_POSITION_DESCRA"))
                .build();
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private LocalDate parseLocalDate(String dateStr) {
        if (!StringUtils.hasText(dateStr) || "".equals(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("日期解析失败: {}", dateStr);
            return null;
        }
    }

    public void init() {
        log.info("SyncPositionJob 初始化");
        log.info("配置信息:");
        log.info("  - syncPositionUrl: {}", syncPositionUrl);
        log.info("  - basicAuthUsername: {}", basicAuthUsername);
        log.info("  - basicAuthPassword: {}", basicAuthPassword);
        log.info("  - syncUserId: {}", syncUserId);
        log.info("  - INF_ID: {}", INF_ID);
    }

    public void destroy() {
        log.info("SyncPositionJob 销毁");
    }

}