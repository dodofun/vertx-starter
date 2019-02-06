package fun.dodo.verticle.loghubClient;

import com.aliyun.openservices.log.common.*;
import com.aliyun.openservices.loghub.client.ILogHubCheckPointTracker;
import com.aliyun.openservices.loghub.client.exceptions.LogHubCheckPointException;
import com.aliyun.openservices.loghub.client.interfaces.ILogHubProcessor;
import com.aliyun.openservices.loghub.client.interfaces.ILogHubProcessorFactory;

import java.util.List;

/***
 * 日志消费示例
 */
public class SampleLogHubProcessor implements ILogHubProcessor {
    private int mShardId;
    // 记录上次持久化 check point 的时间
    private long mLastCheckTime = 0;

    public void initialize(int shardId) {
        mShardId = shardId;
    }

    // 消费数据的主逻辑，这里面的所有异常都需要捕获，不能抛出去。
    public String process(List<LogGroupData> logGroups,
                          ILogHubCheckPointTracker checkPointTracker) {
        // 这里简单的将获取到的数据打印出来
        for (LogGroupData logGroup : logGroups) {

            FastLogGroup flg = logGroup.GetFastLogGroup();
            System.out.println(String.format("\tcategory\t:\t%s\n\tsource\t:\t%s\n\ttopic\t:\t%s\n\tmachineUUID\t:\t%s",
                    flg.getCategory(), flg.getSource(), flg.getTopic(), flg.getMachineUUID()));
            System.out.println("Tags");
            for (int tagIdx = 0; tagIdx < flg.getLogTagsCount(); ++tagIdx) {
                FastLogTag logtag = flg.getLogTags(tagIdx);
                System.out.println(String.format("\t%s\t:\t%s", logtag.getKey(), logtag.getValue()));
            }
            for (int lIdx = 0; lIdx < flg.getLogsCount(); ++lIdx) {
                FastLog log = flg.getLogs(lIdx);

                System.out.println("--------\nLog: " + lIdx + ", time: " + log.getTime() + ", GetContentCount: " + log.getContentsCount());
                for (int cIdx = 0; cIdx < log.getContentsCount(); ++cIdx) {
                    FastLogContent content = log.getContents(cIdx);
                    System.out.println(content.getKey() + "\t:\t" + content.getValue());
                }
            }
        }
        long curTime = System.currentTimeMillis();
        // 每隔 30 秒，写一次 check point 到服务端，如果 30 秒内，worker crash，
        // 新启动的 worker 会从上一个 checkpoint 其消费数据，有可能有少量的重复数据
        if (curTime - mLastCheckTime > 30 * 1000) {
            try {
                //参数true表示立即将checkpoint更新到服务端，为false会将checkpoint缓存在本地，后台默认隔60s会将checkpoint刷新到服务端。
                checkPointTracker.saveCheckPoint(true);
            } catch (LogHubCheckPointException e) {
                e.printStackTrace();
            }
            mLastCheckTime = curTime;
        }
        return null;
    }

    // 当 worker 退出的时候，会调用该函数，用户可以在此处做些清理工作。
    public void shutdown(ILogHubCheckPointTracker checkPointTracker) {
        //将消费断点保存到服务端。
        try {
            checkPointTracker.saveCheckPoint(false);
        } catch (LogHubCheckPointException e) {
            e.printStackTrace();
        }
    }
}

class SampleLogHubProcessorFactory implements ILogHubProcessorFactory {
    public ILogHubProcessor generatorProcessor() {
        // 生成一个消费实例
        return new SampleLogHubProcessor();
    }
}
