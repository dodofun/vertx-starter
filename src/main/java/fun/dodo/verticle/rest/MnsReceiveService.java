package fun.dodo.verticle.rest;

import com.aliyun.mns.client.CloudAccount;
import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.client.MNSClient;
import com.aliyun.mns.model.Message;
import fun.dodo.common.meta.Log;
import fun.dodo.common.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Optional;

import static fun.dodo.common.Constants.PROTOBUF_PARSE_ERROR;


public final class MnsReceiveService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MnsReceiveService.class);

  private final Options options;

  // 客户端
  private final MNSClient msnClient;

  // 队列名称
  private final String apiLogQueue;


  @Inject
  public MnsReceiveService(final Options options) {
    this.options = options;

    final CloudAccount account = new CloudAccount(options.getMnsAccessKey(), options.getMnsAccessSecret(), options.getMnsEndpoint());

    // 在程序中，CloudAccount以及MNSClient单例实现即可，多线程安全
    msnClient = account.getMNSClient();

    // 读取默认的队列名
    apiLogQueue = options.getMnsApiLogQueue();
  }


  public final Optional<Log> takeLogMessage() {
    Optional<Log> result = Optional.empty();

    try {
      final CloudQueue queue = msnClient.getQueueRef(apiLogQueue);

      // 以二级制方式转载
      final Message takeMsg = queue.popMessage();

      if (null != takeMsg) {
        // 读取消息的二级制内容
        final byte[] bytes = takeMsg.getMessageBodyAsBytes();

        if ((null != bytes) && (0 < bytes.length)) {
          try {

            result = Optional.of(Log.parseFrom(bytes));

          } catch (final Exception ex) {
            LOGGER.error(PROTOBUF_PARSE_ERROR + ", {};  {}", ex.getMessage(), Arrays.toString(ex.getStackTrace()));
          }
        }

        // 删除已经取出消费的消息
        queue.deleteMessage(takeMsg.getReceiptHandle());
      }
    } catch (final Exception e) {
      LOGGER.error("消息服务 MNS: {};  {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
    }
    return result;
  }

}




