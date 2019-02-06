package fun.dodo.verticle.acts.handler;

import com.lmax.disruptor.EventHandler;
import fun.dodo.common.Options;
import fun.dodo.verticle.acts.DataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 数据前期准备
 */
@Singleton
public final class DataPreparer implements EventHandler<DataEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataPreparer.class);

    // 一般选项
    private final Options options;

    @Inject
    public DataPreparer(final Options options) {
        this.options = options;
    }


    /*
     * 此 Handler 是 RingBuffer 的第一个处理器, 为之后的 Handlers 准备相关的数据, 因此, 它不能包含异步的方法
     */
    @Override
    public void onEvent(final DataEvent event, final long sequence, final boolean endOfBatch) throws Exception {

        try {
            // 数据前期准备

        } catch (final Exception e) {
            LOGGER.error("数据提取失败: {}", e.getMessage());
        }

    }

}
