package cn.iocoder.sva.module.ai.service.translation.tran.model;

import lombok.Data;

import java.util.List;

/**
 * 术语库查询响应模型
 * <p>
 * 对应 API GET /api/glossary 的返回格式：
 * <pre>{"items": [{"source": "...", "target": "..."}], "total": N}</pre>
 * <p>
 * items 为术语条目列表，total 为术语总数（用于分页展示）。
 */
@Data
public class GlossaryResponse {

    /** 术语条目列表 */
    private List<GlossaryItem> items;

    /** 术语总数 */
    private int total;

    public GlossaryResponse() {
    }

    public GlossaryResponse(List<GlossaryItem> items, int total) {
        this.items = items;
        this.total = total;
    }
}
