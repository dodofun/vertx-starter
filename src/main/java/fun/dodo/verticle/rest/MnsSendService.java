package fun.dodo.verticle.rest;

import com.aliyun.mns.client.CloudAccount;
import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.client.MNSClient;
import com.aliyun.mns.model.Message;
import fun.dodo.common.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;

public class MnsSendService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MnsSendService.class);

  // 消息发送服务
  private final MNSClient msnClient;

  // 发送访问日志记录队列
  private final String apiLogQueue;


  // 默认构造函数
  @Inject
  public MnsSendService(final Options options) {
    final CloudAccount account = new CloudAccount(options.getMnsAccessKey()
      , options.getMnsAccessSecret()
      , options.getMnsEndpoint());

    msnClient = account.getMNSClient();

    apiLogQueue = options.getMnsApiLogQueue();
  }


  /**
   * 发送访问日志记录请求
   */
  public final void sendMNS(final com.google.protobuf.Message request, String queueName) {
    try {
      final CloudQueue queue = msnClient.getQueueRef(queueName);

      // 以二级制方式装载
      final Message message = new Message();
      message.setMessageBody(request.toByteArray());

      // 发送到消息服务
      queue.putMessage(message);

    } catch (final Exception e) {
      LOGGER.error("发送MNS消息失败: {};  {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
    }
  }

}
