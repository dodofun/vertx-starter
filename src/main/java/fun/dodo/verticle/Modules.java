package fun.dodo.verticle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.moandjiezana.toml.Toml;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dagger.Module;
import dagger.Provides;
import fun.dodo.common.Options;
import io.grpc.ManagedChannel;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.vertx.core.Vertx;
import io.vertx.grpc.VertxChannelBuilder;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.protobuf.ProtoConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import javax.inject.Singleton;
import java.io.File;
import java.text.DateFormat;
import java.util.concurrent.TimeUnit;

/**
 * 提供依赖对象的实例
 * <p>
 * 先判断 Module 中是否有提供对象实例化的方法, 如果有则返回, 结束 如果没有, 则查找类的构造方法, 是否有带有@Inject的方法, 如过存在, 则返回
 * <p>
 * Created by Leon on 2016-12-06 MailTo: shinetimes@hotmail.com Mobile: 18602100195
 */
@Module
public final class Modules {
    // 默认配置文件
    private static final String configFile = "/etc/conf.toml";

    private static final Logger LOGGER = LoggerFactory.getLogger(Modules.class);

    /**
     * 获取 GSON 全局实例
     */
    @Provides
    @Singleton
    public static Gson provideGson() {
        return new GsonBuilder().enableComplexMapKeySerialization()
                .serializeNulls()
                .setDateFormat(DateFormat.LONG)
                .setPrettyPrinting()
                .setVersion(1.0)
                .create();
    }


    /**
     * 获取 TOML 全局实例
     */
    @Provides
    @Singleton
    public static Toml provideToml() {
        // 启动文件的位置
        final File directory = new File("");

        String config = directory.getAbsolutePath() + configFile;

        // 服务器通常是 ./bin/restart.sh 启动的, 需要调整
        config = config.replace("/bin", "");

        // 确定配置文件
        final File file = new File(config);
        System.out.println("===============================================================");
        System.out.println("配置文件: " + config);

        // 配置读取工具
        final Toml toml;
        if (file.exists()) {
            toml = (new Toml()).read(file);
            System.out.println("使用指定的配置文件, 开始读取相关配置");
        } else {
            toml = new Toml();
            System.out.println("!!! 配置文件不存在, 需要开发人员介入");
        }
        System.out.println("---------------------------------------------------------------");

        return toml;
    }


    /**
     * 获取 TOML 全局实例
     */
    @Provides
    @Singleton
    public static Options provideOptions(final Toml toml) {
        return new Options(toml);
    }

    /***
     * 数据库连接池
     * @param toml
     * @return
     */
    @Provides
    @Singleton
    public static HikariDataSource dataSource(final Toml toml) {
        final String host = toml.getString("mysql.host", "");
        final String port = String.valueOf(toml.getLong("mysql.port", 0l));
        final String username = toml.getString("mysql.username", "");
        final String password = toml.getString("mysql.password", "");
        final String database = toml.getString("mysql.database", "");

        // 设置数据库URL
        final String connUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=CONVERT_TO_NULL&useSSL=false&allowPublicKeyRetrieval=true";


        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(connUrl);
        config.setUsername(username);
        config.setPassword(password);
        // 自动commit
        config.setAutoCommit(true);
        // 数据库连接超时时间
        config.setConnectionTimeout(toml.getLong("mysql.timeout", 30000l));
        // 允许连接在池中空闲的最长时间
        config.setIdleTimeout(toml.getLong("mysql.idleTimeout", 600000l));
        // 池中连接的最长生命周期
        config.setMaxLifetime(toml.getLong("mysql.maxLifetime", 1800000l));
        // 连接池大小
        config.setMinimumIdle(toml.getLong("mysql.minimumIdle", 10l).intValue());
        config.setMaximumPoolSize(toml.getLong("mysql.maximumPoolSize", 10l).intValue());

        config.addDataSourceProperty("cachePrepStmts", true);
        config.addDataSourceProperty("prepStmtCacheSize", 250);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);

        return new HikariDataSource(config);
    }

    /**
     * 获取 Redis 数据源
     */
    @Provides
    @Singleton
    public static RedisClient provideRedisClient(final Toml toml) {
        final String type = toml.getString("redis.type", "single");
        final int database = toml.getLong("redis.database", 1L).intValue();
        final String password = toml.getString("redis.password", "");
        final String host = toml.getString("redis.host", "");
        final int port = toml.getLong("redis.port", 6379L).intValue();

        RedisURI redisUri = RedisURI.Builder.redis(host)
                .withPassword(password)
                .withDatabase(database)
                .withPort(port)
                .build();

        return RedisClient.create(redisUri);
    }


    /**
     * 获取 OkHttp
     */
    @Provides
    public static OkHttpClient provideOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 获取 Retrofit
     */
    @Provides
    public static Retrofit provideRetrofit(final OkHttpClient client, final Toml toml) {

        final String serverType = toml.getString("baseClient.type", "https");
        final String serverHost = toml.getString("baseClient.host", "");
        final int serverPort = toml.getLong("baseClient.port", 80L).intValue();

        final String baseUri = serverType + "://" + serverHost + ":" + serverPort;

        return new Retrofit.Builder()
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(ProtoConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(baseUri)
                .client(client)
                .build();
    }

    /**
     * 获取 RPC连接
     */
    @Provides
    public static ManagedChannel provideRpcManagedChannel(final Toml toml) {

        final String host = toml.getString("rpcClient.host", "");
        final int port = toml.getLong("rpcClient.port", 80L).intValue();

        return VertxChannelBuilder
                .forAddress(Vertx.vertx(), host, port)
                .usePlaintext(true)
                .build();
    }

    /**
     * 获取 RPC连接
     */
    @Provides
    public static ManagedChannel provideRpcManagedChannel2(final Toml toml) {

        final String host = toml.getString("rpcClient.host", "");
        final int port = toml.getLong("rpcClient.port", 80L).intValue();

        return VertxChannelBuilder
                .forAddress(Vertx.vertx(), host, port)
                .usePlaintext(true)
                .build();
    }
}
