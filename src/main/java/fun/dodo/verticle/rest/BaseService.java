package fun.dodo.verticle.rest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fun.dodo.common.help.StringUtil;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;
import retrofit2.Retrofit;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class BaseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseService.class);

    private Retrofit retrofit;

    // 默认构造函数
    @Inject
    public BaseService(Retrofit retrofit) {

        this.retrofit = retrofit;

    }

    /**
     * GET
     * @return
     */
    public Optional<JsonObject> get() {
        Optional<JsonObject> opt = Optional.empty();

        final BaseClient baseClient = retrofit.create(BaseClient.class);

        try {

            StringBuilder path = new StringBuilder();

            // 组装路径
            path.append("/");

            // 设置header参数
            Map<String, String> headers = new HashMap();

            // 设置query参数
            Map<String, String> querys = new HashMap();

            final Observable<Response<String>> remoteCall = baseClient.get(path.toString(), headers, querys);
            // 同步调用, 等待返回结果
            final Response<String> callResult = remoteCall.blockingSingle();

            if (callResult.isSuccessful()) {
                final String callBody = callResult.body();

                // 处理返回结果
                if (StringUtil.isNullOrEmpty(callBody)) {
                    LOGGER.info("结果: 消息体长度 0");
                } else {
                    opt = Optional.of(new JsonParser().parse(callBody).getAsJsonObject());
                }
            } else {
                LOGGER.info("读取信息失败: {}", callResult.message());
            }
        } catch (final Exception ex) {
            LOGGER.error("读取信息错误: {}, {}", ex.getMessage(), Arrays.toString(ex.getStackTrace()));
            ex.printStackTrace();
        }

        return opt;
    }

}
