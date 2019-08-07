package fun.dodo.verticle;

import dagger.Component;
import fun.dodo.common.Options;
import fun.dodo.verticle.bots.BotDictionary;
import io.vertx.core.Promise;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import io.vertx.reactivex.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Arrays;

public class RpcVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcVerticle.class);

    @Singleton
    @Component(modules = Modules.class)
    interface ComponentBuilder {

        Options options();

        BotDictionary botDictionary();

    }

    @Override
    public void start(Promise<Void> startFuture) throws Exception {

        // 构建关系链
        final ComponentBuilder builder = DaggerRpcVerticle_ComponentBuilder.create();

        // 检查环境 -----------------------------------------------------------------------------------

        final Options options = builder.options();

        BotDictionary botDictionary = builder.botDictionary();

        // RPC server
        VertxServer server = VertxServerBuilder.forPort(vertx.getDelegate(),  options.getRpcPort())
                .addService(botDictionary.getService())
                .build();
        server.start(ar -> {
            if (ar.succeeded()) {
                System.out.println("gRPC service started");
            } else {
                System.out.println("Could not start server " + ar.cause().getMessage());
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
