package fun.dodo.verticle.acts;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import fun.dodo.common.meta.Log;
import fun.dodo.verticle.acts.handler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;

@Singleton
public final class DataStream {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataStream.class);

  // Buffer 的尺寸
  private static final int BUFFER_SIZE = 4096;

  // 保证读写安全
  private final Object syncObject = new Object();

  // 事件分发器
  private Disruptor<DataEvent> disruptor;
  // 事件缓存池
  private RingBuffer<DataEvent> ringBuffer;


  // 默认构造函数
  @Inject
  public DataStream(final DataPreparer dataPreparer, final LogHandler logHandler) {

    try {
      // 事件工厂 - 用于生成每一个事件的初始对象
      final DataFactory eventFactory = new DataFactory();
      // 等待策略 - 进程等待时, 使用哪种方式来观察新的事件
      final WaitStrategy waitStrategy = new BlockingWaitStrategy();
      // 线程管理 - 没有使用原始的 JDK 线程管理, 做了一定程度的优化
      final ThreadPool threadPool = new ThreadPool();

      // 使用单生成着模式, 意味着只有唯一的事件源
      disruptor = new Disruptor<>(eventFactory, BUFFER_SIZE, threadPool, ProducerType.SINGLE, waitStrategy);

      ringBuffer = disruptor.getRingBuffer();


      // 注册内部处理器
      disruptor.handleEventsWith(dataPreparer)
        .then(logHandler);

      System.out.println("DataStream 运行环境初始化已完成");
    } catch (final Exception ex) {
      LOGGER.error("!!! DataStream 创建失败: {}", ex.getMessage());
    }
  }


  /**
   * 数据注入: 从外部数据源, 向 Disruptor 发送数据
   */
  public void inject(final Log message) {
    try {
      synchronized (syncObject) {
        final long sequence = ringBuffer.next();
        final DataEvent event = ringBuffer.get(sequence);

        event.setLog(message);

        // 完成状态
        event.setSuccess(false);

        // 出现错误
        event.setFoundError(false);


        // 发布数据
        ringBuffer.publish(sequence);
      }
    } catch (final Exception ex) {
      LOGGER.error("!!! 数据注入失败: {}\n{}", ex.getMessage(), Arrays.toString(ex.getStackTrace()));
    }
  }


  /**
   * 读取 RingBuffer 的填充率
   */
  public float getFillingRate() {
    return (ringBuffer.remainingCapacity() * 1.0f) / BUFFER_SIZE;
  }


  /**
   * 读取 Disruptor 实例, 用于启动/终止它
   */
  public Disruptor<DataEvent> getDisruptor() {
    return disruptor;
  }

}
