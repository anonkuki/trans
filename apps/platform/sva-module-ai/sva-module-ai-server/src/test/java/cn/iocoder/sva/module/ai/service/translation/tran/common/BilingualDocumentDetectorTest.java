package cn.iocoder.sva.module.ai.service.translation.tran.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BilingualDocumentDetectorTest {

    @Test
    void detectsAlternatingChineseEnglishDocument() {
        assertTrue(BilingualDocumentDetector.isAlternatingBilingual(List.of(
                "1. 范围", "1. Scope",
                "本附录规定无菌产品的生产要求。", "This annex specifies requirements for sterile products.",
                "2. 原则", "2. Principle",
                "应采用质量风险管理原则。", "Quality risk management principles should be applied.",
                "3. 厂房", "3. Premises",
                "关键区域应保持清洁。", "Critical areas should be kept clean."
        )));
    }

    @Test
    void doesNotMisclassifyMonolingualDocumentWithEnglishAbbreviations() {
        assertFalse(BilingualDocumentDetector.isAlternatingBilingual(List.of(
                "水痘减毒活疫苗生产使用SOP。",
                "质量部门负责GMP符合性审核。",
                "取样后进行HPLC检测。",
                "结果记录在批生产记录中。",
                "偏差按照CAPA程序处理。",
                "文件编号为BJ-SOP-TM-0201-05。"
        )));
    }
}
