package fun.dodo.verticle;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.VertxOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Future<Void> startFuture) {

        VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.setBlockedThreadCheckInterval(100000)
                .setMaxEventLoopExecuteTime(2000)
                .setMaxWorkerExecuteTime(60l * 1000 * 1000000)
                .setHAEnabled(true).setWarningExceptionTime(100000);

        vertx = Vertx.vertx(vertxOptions);
        vertx.exceptionHandler(e -> {
           e.printStackTrace();
        });

        DeploymentOptions options = new DeploymentOptions()
                .setWorker(true).setWorkerPoolName("demo").setWorkerPoolSize(1024).setMaxWorkerExecuteTime(60000).setInstances(1);

        vertx.deployVerticle(new DemoVerticle(), options, res -> {
            if (res.succeeded()) {
                System.out.println("DemoVerticle id is: " + res.result());
            } else {
                System.out.println("DemoVerticle failed!");
            }
        });
    }
}
