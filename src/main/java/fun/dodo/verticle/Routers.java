package fun.dodo.verticle;

import com.aliyun.openservices.aliyun.log.producer.Producer;
import com.aliyun.openservices.log.common.LogItem;
import com.google.gson.Gson;
import fun.dodo.common.echo.EchoOne;
import fun.dodo.common.help.*;
import fun.dodo.common.Options;

import fun.dodo.common.log.AliyunLogService;
import fun.dodo.common.log.AliyunLogUtils;
import fun.dodo.verticle.bots.BotDictionary;
import fun.dodo.verticle.bots.BotLog;
import fun.dodo.verticle.bots.BotUser;
import io.vertx.core.http.HttpMethod;
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
import java.util.ArrayList;
import java.util.List;

import static fun.dodo.common.help.ReqHelper.printRequest;

@Singleton
public final class Routers {

    private static final Logger LOGGER = LoggerFactory.getLogger(Routers.class);

    private final Options options;
    private final AliyunLogService logService;
    private final Producer producer;
    private final Gson gson;

    @Inject
    public Routers(final Options options, final AliyunLogService logService, final Gson gson) {
        this.options = options;
        this.logService = logService;
        this.gson = gson;
        // 获取 producer
        this.producer = AliyunLogUtils.createProducer(options.getLogProjectName(), options.getLogEndpoint(), options.getLogAccessKey(), options.getLogAccessSecret());

    }

    public void routerList(final Router router, final DemoVerticle.ComponentBuilder builder) {

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
                .handler(TimeoutHandler.create(30000))
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
        botDictionary.register(router);

        final BotLog botLog = builder.botLog();
        botLog.register(router);

        final BotUser botUser = builder.botUser();
        botUser.register(router);

        router.route().failureHandler(ctx -> {
            EchoOne echo = new EchoOne();
            echo.getHead().setCode(-2).setItemCount(0).setMessage("调用出错");
            echo.getBody().setData("");
            ctx.response().putHeader("Content-Type", "application/json;chatset=utf-8").putHeader("Access-Control-Allow-Origin", "*").setStatusCode(400).end(gson.toJson(echo));

        });
    }

    /***
     * 记录日志
     * @param context
     */
    public void insertLog(final RoutingContext context) {
        final HttpServerRequest request = context.request();

        if (options.getRunMode().equals("dev")) {
            // 打印日志，并通过log4j写入日志系统
            printRequest(context);
        } else {
            // 通过SDK写入日志系统
            List<LogItem> logItemList = new ArrayList<>();
            LogItem logItem = new LogItem();
            logItem.PushBack("scheme", request.scheme());
            logItem.PushBack("method", request.method().name());
            logItem.PushBack("Access", context.normalisedPath());
            logItem.PushBack("Complete:", context.request().absoluteURI());
            logItem.PushBack("Client", request.remoteAddress().host());
            logItem.PushBack("Server", request.localAddress().host());
            logItem.PushBack("Headers", StringUtil.expressMultiMap(request.headers()));
            logItem.PushBack("Params", StringUtil.expressMultiMap(request.params()));
            logItem.PushBack("__topic__", "API_LOG");
            logItem.PushBack("level", "API");
            if (!StringUtil.isNullOrEmpty(context.currentRoute().getPath())) {
                logItem.PushBack("path", context.currentRoute().getPath());
            }
            String bodyString = context.getBodyAsString();
            if (!StringUtil.isNullOrEmpty(bodyString)) {
                logItem.PushBack("Body", bodyString);
            }
            logItemList.add(logItem);
            logService.send(producer, options.getLogProjectName(), options.getLogstore(), logItemList);

        }

    }
}
