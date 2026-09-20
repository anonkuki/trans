package cn.iocoder.sva.module.system.job.sync;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.sva.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.sva.module.system.dal.dataobject.dept.UserPostDO;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncPersonDO;
import cn.iocoder.sva.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.sva.module.system.dal.mysql.dept.PostMapper;
import cn.iocoder.sva.module.system.dal.mysql.dept.UserPostMapper;
import cn.iocoder.sva.module.system.dal.mysql.sync.SyncPersonMapper;
import cn.iocoder.sva.module.system.dal.mysql.user.AdminUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobHelper;
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
import java.util.*;

/**
 * 人员信息同步定时任务
 *
 * @author 李可
 */
@Slf4j
@Component
public class SyncPersonJob {

    @Autowired
    private SyncPersonMapper syncPersonMapper;

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Value("${syncPersonUrl}")
    private String syncPersonUrl;

    @Value("${syncAuth.username}")
    private String basicAuthUsername;

    @Value("${syncAuth.password}")
    private String basicAuthPassword;

    @Value("${syncAuth.userid}")
    private String syncUserId;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserPostMapper userPostMapper;

    // 人员同步的 INF_ID
    private static final String INF_ID = "DC_CURRENT_JOB";
    private static final int PAGE_SIZE = 2000;

    private RestTemplate getSimpleRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(30000);
        return new RestTemplate(factory);
    }

    @XxlJob(value = "syncPerson", init = "init", destroy = "destroy")
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> execute() {
//        RestTemplate restTemplate = getSimpleRestTemplate();
        log.info("==================== 开始同步人员信息 ====================");
//        log.info("请求URL: {}", syncPersonUrl);
//        log.info("Basic Auth用户名: {}", basicAuthUsername);
//        log.info("Basic Auth密码: {}", basicAuthPassword);
//        log.info("INF_ID: {}", INF_ID);
//        log.info("USERID参数: {}", getUserId());
        String flag = XxlJobHelper.getJobParam();
        // 入参为空则默认更新人员信息与岗位关联，0为关闭任何更新,1只保留更新人员信息,2开启更新人员和岗位
        log.info("flag入参是: {}", flag);
        long startTime = System.currentTimeMillis();

        try {
            int totalCount = getTotalCount(restTemplate);
            log.info("需要同步的人员总数: {}", totalCount);

            if (totalCount == 0) {
                log.info("没有需要同步的数据");
                return ReturnT.SUCCESS;
            }

            int totalPages = (totalCount + PAGE_SIZE - 1) / PAGE_SIZE;
            log.info("总页数: {}", totalPages);

            List<SyncPersonDO> allDataList = new ArrayList<>();
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                log.info("正在同步第 {}/{} 页", pageNum, totalPages);
                List<SyncPersonDO> pageData = fetchPageData(restTemplate, pageNum);
                allDataList.addAll(pageData);
                log.info("第 {} 页同步完成，本页数据量: {}", pageNum, pageData.size());
            }

            if (!allDataList.isEmpty()) {
                // 删除所有旧数据
                syncPersonMapper.delete(null);
                log.info("已删除所有旧数据");
                List<AdminUserDO> adminUserDOS = new ArrayList<>();
                // 若不传参或者传参包含1，则预备进行人员信息更新的数据
                if(StrUtil.isEmpty(flag) || flag.contains("1")) {
                    adminUserDOS = convertToAdminUserDOList(allDataList);
                }

                // 批量插入新数据
                int insertCount = 0;
                int batchSize = 1000;
                for (int i = 0; i < allDataList.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, allDataList.size());
                    List<SyncPersonDO> batchList = allDataList.subList(i, end);
                    for (SyncPersonDO syncPersonDO : batchList) {
                        syncPersonMapper.insert(syncPersonDO);
                        insertCount++;
                    }
                    log.info("已插入 {} 条数据", insertCount);
                }
                // 若不传参或者传参包含1，则进行人员信息新增或更新
                if(StrUtil.isEmpty(flag) || flag.contains("1")) {
                    savePersonData(adminUserDOS);
                }
                // 若不传参或者传参包含2，则进行人员岗位关联处理
                if(StrUtil.isEmpty(flag) || flag.contains("2")) {
                    setPosiByPerson(adminUserDOS);
                }

                log.info("数据插入完成，共插入 {} 条记录", insertCount);
            } else {
                log.warn("未获取到任何数据");
            }

            long endTime = System.currentTimeMillis();
            log.info("人员信息同步完成，总耗时: {} ms", (endTime - startTime));
            log.info("==================== 同步结束 ====================");
            return ReturnT.SUCCESS;

        } catch (Exception e) {
            log.error("人员信息同步失败", e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "同步失败: " + e.getMessage());
        }
    }

    //将转换后的人员数据新增或更新到数据库
    private void savePersonData(List<AdminUserDO> adminUserDOS) {
        for (AdminUserDO adminUserDO : adminUserDOS){
            String username = adminUserDO.getUsername();
            Long num = adminUserMapper.selectCount(new QueryWrapper<AdminUserDO>().eq("username", username));
            if (num == 0){
                adminUserMapper.insert(adminUserDO);
            } else {
                adminUserMapper.update(adminUserDO, new QueryWrapper<AdminUserDO>().eq("username", username));
            }
        }
    }


    // 将同步表数据转成AdminUserDo列表
    private List<AdminUserDO> convertToAdminUserDOList(List<SyncPersonDO> syncPersonDOList) {
        List<AdminUserDO> adminUserDOS = new ArrayList<>();
        for (SyncPersonDO syncPersonDO : syncPersonDOList){

            String deptid = syncPersonDO.getDeptid();
            if (deptid == null) {
                deptid = "0";
                log.info("人员部门信息不存在：{}", syncPersonDO.getEmplid());
            }
            // 岗位编号数组
            Set<String> postList = new HashSet<>();
            AdminUserDO adminUserDO = new AdminUserDO();
            adminUserDO.setUsername(syncPersonDO.getEmplid());
            adminUserDO.setNickname(syncPersonDO.getNameDisplay());
            try {
                adminUserDO.setDeptId(Long.parseLong(deptid));
            } catch (Exception e){
                adminUserDO.setDeptId(0L);
            }
            if(!StrUtil.isBlank(syncPersonDO.getDcInfDtStatus())){
                adminUserDO.setStatus(syncPersonDO.getDcInfDtStatus().equals("D") ? 1 : 0);
            } else {
                adminUserDO.setStatus(1);
            }
            adminUserDO.setGuid(syncPersonDO.getGuid());
            adminUserDO.setEffdt(syncPersonDO.getEffdt());
            adminUserDO.setEmplClass(syncPersonDO.getEmplClass());
            adminUserDO.setDcEmplClsDescr(syncPersonDO.getDcEmplClsDescr());
            adminUserDO.setHrStatus(syncPersonDO.getHrStatus());
            adminUserDO.setRegTemp(syncPersonDO.getRegTemp());
            adminUserDO.setReportsTo(syncPersonDO.getReportsTo());
            adminUserDO.setJobIndicator(syncPersonDO.getJobIndicator());
            adminUserDO.setJobIndicatorDescr(syncPersonDO.getJobIndicatorDescr());
            adminUserDO.setPositionNbr(syncPersonDO.getPositionNbr());
            adminUserDO.setDcPositionDescr(syncPersonDO.getDcPositionDescr());
            adminUserDO.setDcDeptDescr50(syncPersonDO.getDcDeptDescr50());
            adminUserDO.setManagerPosn(syncPersonDO.getManagerPosn());
            adminUserDO.setDcDirectorPosn(syncPersonDO.getDcDirectorPosn());
            adminUserDO.setProbationDt(syncPersonDO.getProbationDt());
            adminUserDO.setDcJobLevel(syncPersonDO.getDcJobLevel());
            adminUserDO.setDcJobLevelDescr(syncPersonDO.getDcJobLevelDescr());
            adminUserDO.setDcJobGrade(syncPersonDO.getDcJobGrade());
            adminUserDO.setDcJobGradeDescr(syncPersonDO.getDcJobGradeDescr());
            adminUserDO.setDcJobStage(syncPersonDO.getDcJobStage());
            adminUserDO.setDcJobStageDescr(syncPersonDO.getDcJobStageDescr());
            adminUserDO.setLastHireDt(syncPersonDO.getLastHireDt());
            adminUserDO.setCompany(syncPersonDO.getCompany());
            adminUserDO.setDcCompanyDescr(syncPersonDO.getDcCompanyDescr());
            adminUserDO.setBusinessUnit(syncPersonDO.getBusinessUnit());
            adminUserDO.setBusinessDescr(syncPersonDO.getBusinessDescr());

            adminUserDOS.add(adminUserDO);
        }
        return adminUserDOS;
    }


    private void setPosiByPerson(List<AdminUserDO> adminUserDOS) {
        List<String> userIdList = new ArrayList<>();

        for (AdminUserDO adminUserDO : adminUserDOS){
            String username = adminUserDO.getUsername();
            List<AdminUserDO> oneList = adminUserMapper.selectList(new QueryWrapper<AdminUserDO>().eq("username", username));
            if(!oneList.isEmpty()){
                AdminUserDO aud = oneList.get(0);
                Long id = aud.getId();

                // 确保只删除此人的人员岗位关联并且只删一次
                if(!userIdList.contains(username)){
                    int i = userPostMapper.physicalDeleteByUserId(id);
                    log.info("删除数量是：{}",i);
                    userIdList.add(username);
                }

                String positionNbr = adminUserDO.getPositionNbr();
                List<PostDO> code = postMapper.selectList(new QueryWrapper<PostDO>().eq("code", positionNbr));
                if(!code.isEmpty()){
                    PostDO postDO = code.get(0);
                    Long postDOId = postDO.getId();
                    // 拼接人员和岗位关联关系并保存
                    UserPostDO userPostDO = new UserPostDO();
                    userPostDO.setPostId(postDOId);
                    userPostDO.setUserId(id);
                    userPostMapper.insert(userPostDO);
                    // 更新人员表的岗位字段
                    Set<Long> posts = new HashSet<>();
                    List<UserPostDO> userPosiList = userPostMapper.selectList(new QueryWrapper<UserPostDO>().eq("user_id", id));
                    for(UserPostDO userPosi : userPosiList){
                        posts.add(userPosi.getPostId());
                    }
                    // 只更新岗位列表字段
                    aud.setPostIds(posts);
                    adminUserMapper.updateById(aud);
                }
            }
        }
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
    private List<SyncPersonDO> fetchPageData(RestTemplate restTemplate, int pageNum) {
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

            List<SyncPersonDO> syncPersonDOList = new ArrayList<>();
            for (Map<String, Object> item : dataList) {
                SyncPersonDO syncPersonDO = convertToSyncPersonDO(item);
                syncPersonDOList.add(syncPersonDO);
            }

            return syncPersonDOList;
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
            log.info("请求URL: {}", syncPersonUrl);
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
                    syncPersonUrl,
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
            log.error("发送HTTP请求失败，URL: {}", syncPersonUrl, e);
            throw new RuntimeException("发送HTTP请求失败", e);
        }
    }

    private SyncPersonDO convertToSyncPersonDO(Map<String, Object> map) {
        return SyncPersonDO.builder()
                .guid(getStringValue(map, "GUID"))
                .dcInfDtStatus(getStringValue(map, "DC_INF_DT_STATUS"))
                .dcInfDtStatusDescr(getStringValue(map, "DC_INF_DT_STATUS_DESCR"))
                .emplid(getStringValue(map, "EMPLID"))
                .emplRcd(getStringValue(map, "EMPL_RCD"))
                .effdt(parseLocalDate(getStringValue(map, "EFFDT")))
                .effseq(getStringValue(map, "EFFSEQ"))
                .emplClass(getStringValue(map, "EMPL_CLASS"))
                .dcEmplClsDescr(getStringValue(map, "DC_EMPL_CLS_DESCR"))
                .action(getStringValue(map, "ACTION"))
                .actionDescr(getStringValue(map, "ACTION_DESCR"))
                .actionReason(getStringValue(map, "ACTION_REASON"))
                .actionReasnDescr(getStringValue(map, "ACTION_REASN_DESCR"))
                .hrStatus(getStringValue(map, "HR_STATUS"))
                .hrStatusDescr(getStringValue(map, "HR_STATUS_DESCR"))
                .regTemp(getStringValue(map, "REG_TEMP"))
                .regTempDescr(getStringValue(map, "REG_TEMP_DESCR"))
                .reportsTo(getStringValue(map, "REPORTS_TO"))
                .jobIndicator(getStringValue(map, "JOB_INDICATOR"))
                .jobIndicatorDescr(getStringValue(map, "JOB_INDICATOR_DESCR"))
                .positionNbr(getStringValue(map, "POSITION_NBR"))
                .dcPositionDescr(getStringValue(map, "DC_POSITION_DESCR"))
                .regRegion(getStringValue(map, "REG_REGION"))
                .company(getStringValue(map, "COMPANY"))
                .dcCompanyDescr(getStringValue(map, "DC_COMPANY_DESCR"))
                .businessUnit(getStringValue(map, "BUSINESS_UNIT"))
                .businessDescr(getStringValue(map, "BUSINESS_DESCR"))
                .deptid(getStringValue(map, "DEPTID"))
                .dcDeptDescr50(getStringValue(map, "DC_DEPT_DESCR50"))
                .managerPosn(getStringValue(map, "MANAGER_POSN"))
                .dcDirectorPosn(getStringValue(map, "DC_DIRECTOR_POSN"))
                .dcManagerPosn(getStringValue(map, "DC_MANAGER_POSN"))
                .dcCostCenter(getStringValue(map, "DC_COST_CENTER"))
                .probationDt(parseLocalDate(getStringValue(map, "PROBATION_DT")))
                .location(getStringValue(map, "LOCATION"))
                .dcLocationDescr(getStringValue(map, "DC_LOCATION_DESCR"))
                .dcPdhFellowYn(getStringValue(map, "DC_PDH_FELLOW_YN"))
                .dcPdhFellowYnDescr(getStringValue(map, "DC_PDH_FELLOW_YN_DESCR"))
                .dcDisabledYn(getStringValue(map, "DC_DISABLED_YN"))
                .dcDisabledYnDescr(getStringValue(map, "DC_DISABLED_YN_DESCR"))
                .lastHireDt(parseLocalDate(getStringValue(map, "LAST_HIRE_DT")))
                .dcInternHireDt(parseLocalDate(getStringValue(map, "DC_INTERN_HIRE_DT")))
                .jobcode(getStringValue(map, "JOBCODE"))
                .dcJobcodeDescr(getStringValue(map, "DC_JOBCODE_DESCR"))
                .positionEntryDt(parseLocalDate(getStringValue(map, "POSITION_ENTRY_DT")))
                .dcJobGroup(getStringValue(map, "DC_JOB_GROUP"))
                .dcJobGroupDescr(getStringValue(map, "DC_JOB_GROUP_DESCR"))
                .dcJobSequence(getStringValue(map, "DC_JOB_SEQUENCE"))
                .dcJobSeqDescr(getStringValue(map, "DC_JOB_SEQ_DESCR"))
                .dcFirstJobCate(getStringValue(map, "DC_FIRST_JOB_CATE"))
                .dcJobcateDescr(getStringValue(map, "DC_JOBCATE_DESCR"))
                .dcJobStage(getStringValue(map, "DC_JOB_STAGE"))
                .dcJobStageDescr(getStringValue(map, "DC_JOB_STAGE_DESCR"))
                .dcJobLevel(getStringValue(map, "DC_JOB_LEVEL"))
                .dcJobLevelDescr(getStringValue(map, "DC_JOB_LEVEL_DESCR"))
                .dcJobGrade(getStringValue(map, "DC_JOB_GRADE"))
                .dcJobGradeDescr(getStringValue(map, "DC_JOB_GRADE_DESCR"))
                .dcSapCompanyid(getStringValue(map, "DC_SAP_COMPANYID"))
                .nameDisplay(getStringValue(map, "NAME_DISPLAY"))
                .emailAddr(getStringValue(map, "EMAIL_ADDR"))
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
        log.info("SyncPersonJob 初始化");
        log.info("配置信息:");
        log.info("  - syncPersonUrl: {}", syncPersonUrl);
        log.info("  - basicAuthUsername: {}", basicAuthUsername);
        log.info("  - basicAuthPassword: {}", basicAuthPassword);
        log.info("  - syncUserId: {}", syncUserId);
        log.info("  - INF_ID: {}", INF_ID);
    }

    public void destroy() {
        log.info("SyncPersonJob 销毁");
    }
}