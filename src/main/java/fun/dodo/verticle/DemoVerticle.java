package fun.dodo.verticle;

import dagger.Component;
import fun.dodo.common.Options;
import fun.dodo.verticle.acts.DataStream;
import fun.dodo.verticle.bots.BotConsumer;
import fun.dodo.verticle.bots.BotDictionary;
import fun.dodo.verticle.bots.BotLog;
import io.vertx.core.Promise;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Arrays;

public class DemoVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoVerticle.class);

    @Singleton
    @Component(modules = Modules.class)
    interface ComponentBuilder {

        Options options();

        Routers routers();

        DataStream dataStream();

        BotConsumer botConsumer();

        BotDictionary botDictionary();

        BotLog botLog();

    }

    @Override
    public void start(Promise<Void> startFuture) throws Exception {

        // 构建关系链
        final ComponentBuilder builder = DaggerDemoVerticle_ComponentBuilder.create();

        // 检查环境 -----------------------------------------------------------------------------------

        final Options options = builder.options();

        // 消息处理
//        DataStream dataStream = builder.dataStream();
//        dataStream.getDisruptor().start();
//
//        // 消息消费
//        BotConsumer botConsumer = builder.botConsumer();
//        botConsumer.startTask();


        final Router router = Router.router(vertx);

        // 路由库
        Routers routers = builder.routers();

        // 路由列表
        routers.routerList(router, builder);

        // 启动服务
        vertx.createHttpServer().requestHandler(router::handle).listen(options.getServerPort(), res -> {
            if (res.succeeded()) {
                System.out.println("启动成功");
            } else {
                System.out.println("启动失败");
            }
        });
    }

    /**
     * 停止服务
     */
    @Override
    public void stop(Promise<Void> startFuture) throws Exception {
        try {
        } catch (final Exception e) {
            LOGGER.error("程序退出异常: {}\n {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
    }
}
