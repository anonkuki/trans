package cn.iocoder.sva.module.system.job.sync;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.sva.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.sva.framework.tenant.core.job.TenantJob;
import cn.iocoder.sva.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.sva.module.system.dal.dataobject.sync.SyncDeptDO;
import cn.iocoder.sva.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.sva.module.system.dal.mysql.sync.SyncDeptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.util.*;

/**
 * 部门信息同步定时任务
 *
 * @author 李可
 */
@Slf4j
@Component
public class SyncDeptJob {

    @Autowired
    private SyncDeptMapper syncDeptMapper;

    @Autowired
    private DeptMapper deptMapper;

    @Value("${syncDeptUrl}")
    private String syncDeptUrl;

    @Value("${syncAuth.username}")
    private String basicAuthUsername;

    @Value("${syncAuth.password}")
    private String basicAuthPassword;

    @Value("${syncAuth.userid}")
    private String syncUserId;

    @Autowired
    private RestTemplate restTemplate;

    // 根据Postman请求，INF_ID为 DC_DEPT
    private static final String INF_ID = "DC_DEPT";
    private static final int PAGE_SIZE = 2000;

    // 设置默认租户ID（根据您的业务配置）
    private static final Long DEFAULT_TENANT_ID = 1L; // 或从配置中读取

    // 临时测试：创建一个简单的 RestTemplate 替代注入的
    // 如果这个能成功，说明原来的 RestTemplate 配置有问题
    private RestTemplate getSimpleRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(30000);
        return new RestTemplate(factory);
    }

    @XxlJob(value = "syncDept", init = "init", destroy = "destroy")
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> execute() {
//        RestTemplate restTemplate = getSimpleRestTemplate();
//        log.info("==================== 开始同步部门信息 ====================");
//        log.info("请求URL: {}", syncDeptUrl);
//        log.info("Basic Auth用户名: {}", basicAuthUsername);
//        log.info("Basic Auth密码: {}", basicAuthPassword);
//        log.info("INF_ID: {}", INF_ID);
//        log.info("USERID参数: {}", getUserId());
        long startTime = System.currentTimeMillis();

        try {
//            // 设置租户ID（关键修复）
//            TenantContextHolder.setTenantId(DEFAULT_TENANT_ID);
//            log.info("已设置租户ID: {}", DEFAULT_TENANT_ID);

            int totalCount = getTotalCount(restTemplate);
            log.info("需要同步的部门总数: {}", totalCount);

            if (totalCount == 0) {
                log.info("没有需要同步的数据");
                return ReturnT.SUCCESS;
            }

            int totalPages = (totalCount + PAGE_SIZE - 1) / PAGE_SIZE;
            log.info("总页数: {}", totalPages);

            List<SyncDeptDO> allDataList = new ArrayList<>();
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                log.info("正在同步第 {}/{} 页", pageNum, totalPages);
                List<SyncDeptDO> pageData = fetchPageData(restTemplate, pageNum);
                allDataList.addAll(pageData);
                log.info("第 {} 页同步完成，本页数据量: {}", pageNum, pageData.size());
            }

            if (!allDataList.isEmpty()) {
                // 删除同步表旧数据
                syncDeptMapper.delete(null);
                log.info("已删除同步表所有旧数据");

                // 批量插入同步表新数据
                int batchSize = 1000;
                for (int i = 0; i < allDataList.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, allDataList.size());
                    List<SyncDeptDO> batchList = allDataList.subList(i, end);
                    syncDeptMapper.insertBatch(batchList);
                }

                // 根据id新增或更新部门数据
                saveDeptData(convertToDeptDOList(allDataList));

                log.info("数据同步完成，共处理 {} 条记录", allDataList.size());
            } else {
                log.warn("未获取到任何数据");
            }

            long endTime = System.currentTimeMillis();
            log.info("部门信息同步完成，总耗时: {} ms", (endTime - startTime));
            log.info("==================== 同步结束 ====================");
            return ReturnT.SUCCESS;

        } catch (Exception e) {
            log.error("部门信息同步失败", e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "同步失败: " + e.getMessage());
        }
    }

    //将转换后的部门数据根据id新增或更新到数据库
    private void saveDeptData(List<DeptDO> deptList) {
        for (DeptDO deptDO : deptList) {
            Long id = deptDO.getId();
            Long num = deptMapper.selectCount(new QueryWrapper<DeptDO>().eq("id", id));
            if (num == 0) {
                deptMapper.insert(deptDO);
                log.debug("新增部门：ID={}, 名称={}", id, deptDO.getName());
            } else {
                deptMapper.update(deptDO, new QueryWrapper<DeptDO>().eq("id", id));
                log.debug("更新部门：ID={}, 名称={}", id, deptDO.getName());
            }
        }
    }

    // 将同步表数据转换成部门数据
    private List<DeptDO> convertToDeptDOList(List<SyncDeptDO> syncDeptList) {
        List<String> qc = new ArrayList<>();
        List<DeptDO> deptList = new ArrayList<>();
        for (SyncDeptDO syncDeptDO : syncDeptList) {
            String deptid = syncDeptDO.getDeptid();
            if (qc.contains(deptid)){
                continue;
            }
            qc.add(deptid);
            String partDeptidChn = syncDeptDO.getPartDeptidChn();
            if (deptid == null) {
                continue;
            }
            DeptDO deptDO = new DeptDO();
            try {
                deptDO.setId(Long.parseLong(deptid));
            } catch (Exception e) {
                log.error("出现异常部门ID: {}", deptid);
                continue;
            }
            deptDO.setName(syncDeptDO.getDescr());
            try {
                deptDO.setParentId(!StrUtil.isEmpty(partDeptidChn) ? Long.parseLong(partDeptidChn) : null);
            } catch (Exception e) {
                log.error("出现异常父部门ID: {}", partDeptidChn);
                continue;
            }
            if(!StrUtil.isBlank(syncDeptDO.getDcInfDtStatus())){
                deptDO.setStatus(syncDeptDO.getDcInfDtStatus().equals("D") ? 1 : 0);
            } else {
                deptDO.setStatus(1);
            }
            deptDO.setCreateTime(LocalDateTime.now());
            deptDO.setTenantId(DEFAULT_TENANT_ID);
            deptDO.setDcDeptRespon(syncDeptDO.getDcDeptRespon());
            deptDO.setDcDeptFullCode(syncDeptDO.getDcDeptFullCode());
            deptDO.setDcDeptFullDescr(syncDeptDO.getDcDeptFullDescr());
            deptDO.setLocation(syncDeptDO.getLocation());
            deptDO.setDcLocationDescr(syncDeptDO.getDcLocationDescr());
            deptDO.setCompany(syncDeptDO.getCompany());
            deptDO.setDcCompanyDescr(syncDeptDO.getDcCompanyDescr());
            deptDO.setDcOrgType(syncDeptDO.getDcOrgType());
            deptDO.setDcOrgTypeDescr(syncDeptDO.getDcOrgTypeDescr());
            deptDO.setDcParDeptDescr(syncDeptDO.getDcParDeptDescr());
            deptDO.setDcOrgLevel(syncDeptDO.getDcOrgLevel());
            deptDO.setDcOrgLevelDescr(syncDeptDO.getDcOrgLevelDescr());
            deptDO.setDescrshort(syncDeptDO.getDescrshort());
            deptDO.setEffdt(syncDeptDO.getEffdt());
            deptDO.setEffStatus(syncDeptDO.getEffStatus());
            deptDO.setGuid(syncDeptDO.getGuid());
            deptList.add(deptDO);
        }
        return deptList;
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
    private List<SyncDeptDO> fetchPageData(RestTemplate restTemplate, int pageNum) {
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

            List<SyncDeptDO> syncDeptDOList = new ArrayList<>();
            for (Map<String, Object> item : dataList) {
                SyncDeptDO syncDeptDO = convertToSyncDeptDO(item);
                syncDeptDOList.add(syncDeptDO);
            }

            return syncDeptDOList;
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

            // 手动构建 JSON 字符串
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
            log.info("请求URL: {}", syncDeptUrl);
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
                    syncDeptUrl,
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
            log.error("发送HTTP请求失败，URL: {}", syncDeptUrl, e);
            throw new RuntimeException("发送HTTP请求失败", e);
        }
    }

    private SyncDeptDO convertToSyncDeptDO(Map<String, Object> map) {
        return SyncDeptDO.builder()
                .guid(getStringValue(map, "GUID"))
                .dcInfDtStatus(getStringValue(map, "DC_INF_DT_STATUS"))
                .setid(getStringValue(map, "SETID"))
                .deptid(getStringValue(map, "DEPTID"))
                .effdt(parseLocalDate(getStringValue(map, "EFFDT")))
                .effStatus(getStringValue(map, "EFF_STATUS"))
                .effStatusDescr(getStringValue(map, "EFF_STATUS_DESCR"))
                .descr(getStringValue(map, "DESCR"))
                .descrshort(getStringValue(map, "DESCRSHORT"))
                .setidLocation(getStringValue(map, "SETID_LOCATION"))
                .location(getStringValue(map, "LOCATION"))
                .dcLocationDescr(getStringValue(map, "DC_LOCATION_DESCR"))
                .company(getStringValue(map, "COMPANY"))
                .dcCompanyDescr(getStringValue(map, "DC_COMPANY_DESCR"))
                .dcOrgType(getStringValue(map, "DC_ORG_TYPE"))
                .dcOrgTypeDescr(getStringValue(map, "DC_ORG_TYPE_DESCR"))
                .dcOrgLevel(getStringValue(map, "DC_ORG_LEVEL"))
                .dcOrgLevelDescr(getStringValue(map, "DC_ORG_LEVEL_DESCR"))
                .partDeptidChn(getStringValue(map, "PART_DEPTID_CHN"))
                .dcParDeptDescr(getStringValue(map, "DC_PAR_DEPT_DESCR"))
                .managerPosn(getStringValue(map, "MANAGER_POSN"))
                .dcDirectorPosn(getStringValue(map, "DC_DIRECTOR_POSN"))
                .dcManagerPosn(getStringValue(map, "DC_MANAGER_POSN"))
                .dcSetupDate(parseLocalDate(getStringValue(map, "DC_SETUP_DATE")))
                .dcSetupNum(getStringValue(map, "DC_SETUP_NUM"))
                .dcCostCenter(getStringValue(map, "DC_COST_CENTER"))
                .dcOrgBranch(getStringValue(map, "DC_ORG_BRANCH"))
                .dcOrgBranchDescr(getStringValue(map, "DC_ORG_BRANCH_DESCR"))
                .dcSetupReason(getStringValue(map, "DC_SETUP_REASON"))
                .dcDeptRespon(getStringValue(map, "DC_DEPT_RESPON"))
                .dcDeptFullCode(getStringValue(map, "DC_DEPT_FULL_CODE"))
                .dcDeptFullDescr(getStringValue(map, "DC_DEPT_FULL_DESCR"))
                .dcHonghaiDeptid(getStringValue(map, "DC_HONGHAI_DEPTID"))
                .dcSapCompanyid(getStringValue(map, "DC_SAP_COMPANYID"))
                .dcDeptidLv01(getStringValue(map, "DC_DEPTID_LV01"))
                .dcDeptidLv02(getStringValue(map, "DC_DEPTID_LV02"))
                .dcDeptidLv03(getStringValue(map, "DC_DEPTID_LV03"))
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
        log.info("SyncDeptJob 初始化");
        log.info("配置信息:");
        log.info("  - syncDeptUrl: {}", syncDeptUrl);
        log.info("  - basicAuthUsername: {}", basicAuthUsername);
        log.info("  - basicAuthPassword: {}", basicAuthPassword);
        log.info("  - syncUserId: {}", syncUserId);
        log.info("  - INF_ID: {}", INF_ID);
    }

    public void destroy() {
        log.info("SyncDeptJob 销毁");
    }

}