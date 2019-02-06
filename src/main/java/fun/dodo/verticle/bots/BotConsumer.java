package fun.dodo.verticle.bots;

import fun.dodo.common.meta.Log;
import fun.dodo.verticle.acts.DataStream;
import fun.dodo.verticle.rest.MnsReceiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

import static fun.dodo.common.Constants.DATABASE_ACCESS_ERROR;

@Singleton
public final class BotConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotConsumer.class);

    // 随机数生成器
    private static final Random RANDOM = new Random();
    private static final int WAITING_SECONDS = 32;

    // 关键服务
    private final DataStream dataStream;
    private final MnsReceiveService mnsReceiveService;

    // 定时器: 控制取数据的频率
    private final Timer timer;
    private final ScheduleTask task;


    @Inject
    public BotConsumer(final DataStream dataStream, final MnsReceiveService mnsReceiveService) {
        this.dataStream = dataStream;
        this.mnsReceiveService = mnsReceiveService;

        timer = new Timer();
        task = new ScheduleTask();
    }


    /**
     * 定时器工作
     */
    private class ScheduleTask extends TimerTask {
        @Override
        public final void run() {
            // 使用 Disruptor 的填充率来限速, 避免超出了处理能力
            final float fillingRate = dataStream.getFillingRate();
            // LOGGER.info("当前填充率是: {}", fillingRate);

            if (0.2f > fillingRate) {
                int zoomRate = 4;
                if (0.1f > fillingRate) {
                    zoomRate = 20;
                }
                if (0.05f > fillingRate) {
                    zoomRate = 100;
                }
                if (0.01f > fillingRate) {
                    zoomRate = 2000;
                }

                // 尝试多休眠一定的时间量
                try {
                    Thread.sleep(RANDOM.nextInt(WAITING_SECONDS * zoomRate));
                } catch (final Exception e) {
                    LOGGER.error(DATABASE_ACCESS_ERROR + ", {};  {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
                }
            }

            // 从 MNS 提取消息
            final Optional<Log> optMessage = mnsReceiveService.takeLogMessage();

            // 填充 Disruptor
            optMessage.ifPresent(dataStream::inject);
        }
    }

    /**
     * 启动检测任务
     */
    public void startTask() {
        LOGGER.info("*** 消息队列查询启动");

        timer.schedule(task, WAITING_SECONDS * 100, RANDOM.nextInt(WAITING_SECONDS) + WAITING_SECONDS);
    }


    /**
     * 停止检测任务
     */
    public void stopTask() {
        LOGGER.info("定时器 取消 !");

        timer.cancel();
    }

}
