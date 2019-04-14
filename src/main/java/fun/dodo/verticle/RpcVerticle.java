package fun.dodo.verticle;

import dagger.Component;
import examples.GreeterGrpc;
import examples.HelloReply;
import examples.HelloRequest;
import fun.dodo.common.Options;
import fun.dodo.verticle.bots.BotDictionary;
import io.vertx.core.Future;
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
    public void start(Future<Void> startFuture) {

        // 构建关系链
        final ComponentBuilder builder = DaggerRpcVerticle_ComponentBuilder.create();

        // 检查环境 -----------------------------------------------------------------------------------

        final Options options = builder.options();

        BotDictionary botDictionary = builder.botDictionary();


        // RPC server
        GreeterGrpc.GreeterVertxImplBase service = new GreeterGrpc.GreeterVertxImplBase() {
            @Override
            public void sayHello(HelloRequest request, Future<HelloReply> future) {
                future.complete(HelloReply.newBuilder().setMessage(request.getName()).build());
            }
        };
        VertxServer server = VertxServerBuilder.forAddress(vertx.getDelegate(), "localhost", 8090)
                .addService(service)
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
    public void stop(Future<Void> stopFuture) {
        try {
        } catch (final Exception e) {
            LOGGER.error("程序退出异常: {}\n {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
    }

}
