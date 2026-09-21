package cn.iocoder.sva.module.ai.service.translation;

import cn.iocoder.sva.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.sva.framework.common.exception.ServiceException;
import cn.iocoder.sva.framework.common.pojo.PageResult;
import cn.iocoder.sva.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.sva.module.ai.controller.admin.translation.vo.TranFilePageReqVO;
import cn.iocoder.sva.module.ai.dal.dataobject.translation.TranFileDO;
import cn.iocoder.sva.module.ai.dal.mysql.translation.TranFileMapper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.List;

import static cn.iocoder.sva.framework.common.pojo.CommonResult.success;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class TranFileServiceImplTest {

    @Test
    void rejectsDocumentOwnedByAnotherUser() {
        Fixture fixture = fixture("owner");
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9L);
            security.when(SecurityFrameworkUtils::getLoginUserUsername).thenReturn("other-user");
            when(fixture.permissionApi.getLoginUserAllRoleIds(9L)).thenReturn(success(Set.of(2L)));

            assertThrows(ServiceException.class, () -> fixture.service.getTranFileForCurrentUser(1L));
        }
    }

    @Test
    void allowsOwnerAndSuperAdministrator() {
        Fixture ownerFixture = fixture("owner");
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9L);
            security.when(SecurityFrameworkUtils::getLoginUserUsername).thenReturn("owner");
            when(ownerFixture.permissionApi.getLoginUserAllRoleIds(9L)).thenReturn(success(Set.of(2L)));
            assertSame(ownerFixture.file, ownerFixture.service.getTranFileForCurrentUser(1L));
        }

        Fixture adminFixture = fixture("owner");
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(1L);
            security.when(SecurityFrameworkUtils::getLoginUserUsername).thenReturn("admin");
            when(adminFixture.permissionApi.getLoginUserAllRoleIds(1L)).thenReturn(success(Set.of(1L)));
            assertSame(adminFixture.file, adminFixture.service.getTranFileForCurrentUser(1L));
        }
    }

    @Test
    void forcesCurrentUsernameWhenNonAdminSuppliesAnotherUsername() {
        Fixture fixture = fixture("owner");
        TranFilePageReqVO request = new TranFilePageReqVO();
        request.setUsername("victim");
        when(fixture.mapper.selectPage(request)).thenReturn(new PageResult<>(List.of(), 0L));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9L);
            security.when(SecurityFrameworkUtils::getLoginUserUsername).thenReturn("current-user");
            when(fixture.permissionApi.getLoginUserAllRoleIds(9L)).thenReturn(success(Set.of(2L)));

            fixture.service.getTranFilePage(request);
        }

        org.junit.jupiter.api.Assertions.assertEquals("current-user", request.getUsername());
    }

    private Fixture fixture(String owner) {
        TranFileMapper mapper = mock(TranFileMapper.class);
        PermissionCommonApi permissionApi = mock(PermissionCommonApi.class);
        TranFileDO file = new TranFileDO();
        file.setId(1L);
        file.setUsername(owner);
        when(mapper.selectById(1L)).thenReturn(file);

        TranFileServiceImpl service = new TranFileServiceImpl();
        ReflectionTestUtils.setField(service, "tranFileMapper", mapper);
        ReflectionTestUtils.setField(service, "permissionApi", permissionApi);
        return new Fixture(service, mapper, permissionApi, file);
    }

    private record Fixture(TranFileServiceImpl service, TranFileMapper mapper,
                           PermissionCommonApi permissionApi, TranFileDO file) {
    }
}
