package fun.dodo.verticle;

import com.aliyun.openservices.log.common.LogItem;
import fun.dodo.common.help.*;
import fun.dodo.common.Options;
import fun.dodo.common.meta.Log;
import fun.dodo.common.meta.LogType;

import fun.dodo.verticle.bots.BotDictionary;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.core.WorkerExecutor;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.CorsHandler;
import io.vertx.reactivex.ext.web.handler.TimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.Vector;

import static fun.dodo.common.help.ReqHelper.printRequest;

@Singleton
public final class Routers {

    private static final Logger LOGGER = LoggerFactory.getLogger(Routers.class);

    private final Options options;
    private final AliyunLogService logService;
    private WorkerExecutor executor;

    @Inject
    public Routers(final Options options, final AliyunLogService logService) {
        this.options = options;
        this.logService = logService;
    }

    public void routerList(final Router router, final DemoVerticle.ComponentBuilder builder, final WorkerExecutor executor) {
        this.executor = executor;

        // 跨域访问设置
        router.route().handler(CorsHandler.create(
                options.getCorsHost())
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.PUT)
                .allowedMethod(HttpMethod.DELETE)
                .allowedMethod(HttpMethod.OPTIONS)
                .allowedMethod(HttpMethod.PATCH)
                .allowedMethod(HttpMethod.CONNECT)
        )
                // Body
                .handler(BodyHandler.create())
                // 超时时间
                .handler(TimeoutHandler.create(10000))
                // 前置操作
                .handler(ctx -> {
                    // 打印并记录日志
                    insertLog(ctx);
                    // TODO 前置操作完成后，进入业务路由模块
                    ctx.next();
                });


        router.get("/id").handler(ctx -> {
           ctx.response().end("TEST");
        });

        final BotDictionary botDictionary = builder.botDictionary();
        botDictionary.register(router, executor);

    }

    /***
     * 记录日志
     * @param context
     */
    public void insertLog(final RoutingContext context) {
        final HttpServerRequest request = context.request();

        executor.executeBlocking(future -> {

            // 打印日志
            printRequest(context);

            final Log.Builder builder = Log.newBuilder();

            builder.setId(IdCreator.newId())
                    .setOwnerId(options.getId())
                    .setType(LogType.LOG_TYPE_DEFAULT)
                    .setContentType(String.valueOf(context.getAcceptableContentType()))
                    .setScheme(String.valueOf(request.scheme()))
                    .setMethod(String.valueOf(request.method().name()))
                    .setPath(String.valueOf(context.currentRoute().getPath()))
                    .setCompletePath(String.valueOf(request.absoluteURI()))
                    .setClient(String.valueOf(request.remoteAddress().path()))
                    .setServer(String.valueOf(request.localAddress().path()))
                    .setHeaders(String.valueOf(StringUtil.expressMultiMap(request.headers())))
                    .setAttributes(String.valueOf(StringUtil.expressMultiMap(request.formAttributes())))
                    .setBody(String.valueOf(context.getBodyAsString()))
                    .setCreatedAt(Instant.now().toEpochMilli());

            // TODO 推送日志到 日志系统
            Vector<LogItem> logGroups = new Vector<>();

            LogItem logItem = new LogItem();
            logGroups.add(logItem);

            logService.send("application-metrics", "metrics", null, null, logGroups);

        }, false, null);

    }
}
