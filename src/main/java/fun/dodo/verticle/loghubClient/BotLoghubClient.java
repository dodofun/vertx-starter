package fun.dodo.verticle.loghubClient;

import com.aliyun.openservices.loghub.client.ClientWorker;
import com.aliyun.openservices.loghub.client.config.LogHubConfig;
import com.aliyun.openservices.loghub.client.exceptions.LogHubClientWorkerException;
import fun.dodo.verticle.loghubClient.SampleLogHubProcessorFactory;

public class BotLoghubClient {

    public static void main(String []args) throws LogHubClientWorkerException, InterruptedException
    {
        // 第二个参数是消费者名称，同一个消费组下面的消费者名称必须不同，可以使用相同的消费组名称，不同的消费者名称在多台机器上启动多个进程，来均衡消费一个Logstore, 这个时候消费者名称可以使用机器ip来区分。第9个参数（maxFetchLogGroupSize）是每次从服务端获取的LogGroup数目，使用默认值即可，如有调整请注意取值范围(0,1000]
        LogHubConfig config = new LogHubConfig("testGroup", "metrics2", "cn-beijing.log.aliyuncs.com", "dodo-demo", "application-metrics", "LTAICWWsWX6u2qJ9", "1YsL1rKknuAwoRDSskG1VwBELgFuLc", LogHubConfig.ConsumePosition.BEGIN_CURSOR);
        ClientWorker worker = new ClientWorker(new SampleLogHubProcessorFactory(), config);
        Thread thread = new Thread(worker);
        //Thread运行之后，Client Worker会自动运行，ClientWorker扩展了Runnable接口。
        thread.start();
        Thread.sleep(60 * 60 * 1000);
        //调用worker的Shutdown函数，退出消费实例，关联的线程也会自动停止。
        worker.shutdown();
        //ClientWorker运行过程中会生成多个异步的Task，Shutdown之后最好等待还在执行的Task安全退出，建议sleep 30s。
        Thread.sleep(30 * 1000);
    }
}
