package cn.iocoder.sva.module.ai.service.translation.tran;

/**
 * 翻译进度回调接口
 * <p>
 * 用于报告翻译进度的函数式接口，支持 Lambda 表达式。
 */
@FunctionalInterface
public interface ProgressCallback {

    /**
     * 报告翻译进度
     *
     * @param current 当前已完成数量
     * @param total   总数量
     * @param message 进度消息
     */
    void onProgress(int current, int total, String message);
}
