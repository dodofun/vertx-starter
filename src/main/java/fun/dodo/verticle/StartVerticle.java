package fun.dodo.verticle;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.VertxOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartVerticle.class);

    @Override
    public void start(Promise<Void> startFuture) throws Exception {

        VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.setBlockedThreadCheckInterval(100000)
                .setMaxEventLoopExecuteTime(2000)
                .setMaxWorkerExecuteTime(60l * 1000 * 1000000)
                .setHAEnabled(true).setWarningExceptionTime(2000);

        vertx = Vertx.vertx(vertxOptions);

        DeploymentOptions options = new DeploymentOptions().setInstances(1)
                .setWorker(true)
                .setWorkerPoolName("base-worker")
                .setWorkerPoolSize(512);

        vertx.deployVerticle(DemoVerticle.class.getName(), options);

        vertx.deployVerticle(RpcVerticle.class.getName(), new DeploymentOptions().setInstances(1));
    }
}
