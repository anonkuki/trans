package cn.iocoder.sva.module.bpm.service.oa.listener;

import cn.iocoder.sva.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.sva.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.sva.module.bpm.service.oa.BpmOALeaveService;
import cn.iocoder.sva.module.bpm.service.oa.BpmOALeaveServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * OA 请假单的结果的监听器实现类
 *
 * @author 科兴源码
 */
@Component
public class BpmOALeaveStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private BpmOALeaveService leaveService;

    @Override
    protected String getProcessDefinitionKey() {
        return BpmOALeaveServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        leaveService.updateLeaveStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());
    }

}
