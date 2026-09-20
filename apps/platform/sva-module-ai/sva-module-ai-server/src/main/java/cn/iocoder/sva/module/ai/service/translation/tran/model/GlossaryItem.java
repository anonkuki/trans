package cn.iocoder.sva.module.ai.service.translation.tran.model;

import lombok.Data;

/**
 * 术语条目模型
 * <p>
 * 表示术语库中的一条术语映射，包含原文（源语言）和译文（目标语言）。
 * <p>
 * 对应 API 返回格式：
 * <pre>{"source": "原文术语", "target": "译文术语"}</pre>
 */
@Data
public class GlossaryItem {

    /** 原文术语（源语言） */
    private String source;

    /** 译文术语（目标语言） */
    private String target;

    public GlossaryItem() {
    }

    public GlossaryItem(String source, String target) {
        this.source = source;
        this.target = target;
    }
}
