package fun.dodo.verticle.bots;

import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.response.GetLogsResponse;
import com.google.gson.Gson;
import fun.dodo.common.Constants;
import fun.dodo.common.Options;
import fun.dodo.common.echo.EchoOne;
import fun.dodo.common.help.RedisUtils;
import fun.dodo.common.log.AliyunLogService;
import fun.dodo.common.log.AliyunLogUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

import static fun.dodo.common.help.ReqHelper.*;
import static fun.dodo.common.help.ResHelper.*;
import static org.apache.http.HttpStatus.SC_OK;

@Singleton
public final class BotLog {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotLog.class);

    // JSON处理
    private final Gson gson;
    // 数据代理
    private final Options options;
    private final AliyunLogService logService;
    private final Producer producer;
    private final RedisUtils redisUtils;
    private final Client client;

    final static String VERSION = "v1";

    final static String ENTITY = "log";

    final static String getPath = "/" + VERSION + "/get/" + ENTITY;
    final static String addPath = "/" + VERSION + "/add/" + ENTITY;

    @Inject
    public BotLog(final Gson gson, final Options options, final AliyunLogService logService, final RedisUtils redisUtils) {
        this.gson = gson;
        this.options = options;
        this.logService = logService;
        this.redisUtils = redisUtils;
        this.producer = AliyunLogUtils.createProducer(options.getLogProjectName(), options.getLogEndpoint(), options.getLogAccessKey(), options.getLogAccessSecret());
        this.client = new Client(options.getLogEndpoint(), options.getLogAccessKey(), options.getLogAccessSecret());

    }

    public void register(final Router router) {

        // 添加
        router.post(addPath).blockingHandler(this::addLog, false);
        // 获取
        router.post(getPath).blockingHandler(this::getLog, false);

    }

    /**
     * 添加日志 POST /add/log
     *
     * @param context: HTTP 路由上下文
     */
    public void addLog(final RoutingContext context) {

        try {
            final HttpServerRequest request = context.request();

            // 获取body数据
            JsonObject body = context.getBodyAsJson();
            if (null != body) {

                // 通过SDK写入日志系统
                List<LogItem> logItemList = new ArrayList<>();
                LogItem logItem = new LogItem();

                // Todo 日志参数
                body.stream().forEach(item -> {
                    logItem.PushBack(item.getKey(), String.valueOf(item.getValue()));
                });

                logItemList.add(logItem);
                logService.send(producer, options.getLogProjectName(), options.getLogstore(), logItemList);

                echoDoneMessage(context, SC_OK, "success");
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        echoFoundError(context, "fail");
    }

    /***
     * 获取日志数据 /get/log
     * @param context
     */
    public void getLog(final RoutingContext context) {

        printRequest(context);

        // 读取 project
        final String project = getBodyStringValue(context, "project");
        if (null == project || project.isEmpty()) {
            echoParamError(context, "project 不能为空");
            return;
        }

        // 读取 logstore
        final String logstore = getBodyStringValue(context, "logstore");
        if (null == logstore || logstore.isEmpty()) {
            echoParamError(context, "logstore 不能为空");
            return;
        }

        // 读取 query 语句
        final String query = getBodyStringValue(context, "query");
        if (null == query || query.isEmpty()) {
            echoParamError(context, "query 不能为空");
            return;
        }

        // 读取 topic
        final String topic = getBodyStringValue(context, "topic");

        // 读取 from
        long from = getBodySafeLongValue(context, "from");

        // 读取 to
        long to = getBodySafeLongValue(context, "to");
        if (0 >= to) {
            to = new Date().getTime() / 1000;
        }

        // 读取分页 Index
        int index = (int) getBodySafeLongValue(context, "index");
        if (0 > index) {
            index = 0;
        }

        int size = (int) getBodySafeLongValue(context, "size");
        if (0 >= size) {
            size = 20;
        }

        // 缓存时间
        int expires = (int) getBodySafeLongValue(context, "expires");
        if (0 >= expires) {
            expires = 1200;
        }

        // 读取是否反转排序，0：正序，1：倒序
        boolean reverse = getBodyBooleanValue(context, "reverse");

        GetLogsResponse result;

        try {

            // 设置 redis存储key
            String key = new StringBuilder().append(project).append(logstore).append(from).append(to).append(topic).append(query).append(index).append(size).append(reverse).toString();

            // 先从redis中取
            Optional<Object> resultRedis = redisUtils.get(key);
            if (resultRedis.isPresent()) {
                // redis中有缓存数据
                result = (GetLogsResponse) resultRedis.get();
            } else {
                // 远程获取小程序日志列表
                result = logService.getLogs(client, project, logstore, (int) from, (int) to, topic, query, index, size, reverse);

                // 将结果缓存
                redisUtils.set(key, result, expires);
            }

            final HttpServerResponse response = context.response();

            final EchoOne echo = new EchoOne();
            echo.getHead().setMessage("提取日志数据").setItemCount((int) result.getProcessedRow());
            echo.getBody().setData(result);

            response.putHeader(Constants.CONTENT_TYPE, Constants.CONTENT_TYPE_JSON)
                    .putHeader(Constants.CONTENT_CONTROL, Constants.CONTENT_CONTROL_VALUE)
                    .setStatusCode(SC_OK)
                    .end(gson.toJson(echo));

        } catch (final Exception e) {
            echoTransError(context, Arrays.toString(e.getStackTrace()));
        }
    }

}
