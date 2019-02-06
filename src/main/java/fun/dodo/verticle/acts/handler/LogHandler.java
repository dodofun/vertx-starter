package fun.dodo.verticle.acts.handler;

import com.lmax.disruptor.EventHandler;
import fun.dodo.verticle.acts.DataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;

@Singleton
public final class LogHandler implements EventHandler<DataEvent> {
  private static final Logger LOGGER = LoggerFactory.getLogger(LogHandler.class);

  // 数据代理


  @Inject
  public LogHandler() {

  }


  @Override
  public void onEvent(final DataEvent event, final long sequence, final boolean endOfBatch) throws Exception {
    if (event.isFoundError()) {
      return;
    }

    // 数据处理
    handlerEvent(event);

  }

  /*
   * 在持久化数据存储内, 做相关的数据项登记, 有可能做简单的统计处理
   */
  private void handlerEvent(final DataEvent event) {

    try {
      // TODO 数据处理

      // 数据处理完毕
      event.setSuccess(true);
    } catch (final Exception e) {
      LOGGER.error("数据处理失败, {};  {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
    }
  }

}
