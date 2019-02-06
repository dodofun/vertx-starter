package fun.dodo.verticle.bots;

import fun.dodo.verticle.data.demo.DemoData;
import fun.dodo.common.help.ReqHelper;
import fun.dodo.common.interfaces.BotBase;
import fun.dodo.common.meta.Demo;
import fun.dodo.common.meta.EchoList;
import fun.dodo.common.meta.ResultType;
import io.vertx.ext.web.api.validation.ParameterType;
import io.vertx.reactivex.core.WorkerExecutor;

import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.api.validation.HTTPRequestValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;

import static fun.dodo.common.Constants.*;
import static fun.dodo.common.help.ReqHelper.getParamSafeIntegerValue;
import static fun.dodo.common.help.ReqHelper.getParamSafeLongValue;
import static fun.dodo.common.help.ResHelper.*;
import static io.vertx.reactivex.ext.web.api.validation.ParameterTypeValidator.createLongTypeValidator;


@Singleton
public final class BotDemoV1 implements BotBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotDemo.class);

    // 数据代理
    private final DemoData demoData;
    private WorkerExecutor executor;

    final static String VERSION = "v1";

    final static String OWNER = "owner";
    final static String ENTITY = "demo";

    final static String OWNER_ID = "owner_id";
    final static String ID = "id";
    final static String INDEX = "index";
    final static String SIZE = "size";

    final static String mainPath = "/" + VERSION + "/" + OWNER + "/:" + OWNER_ID + "/" + ENTITY + "/:" + ID;
    final static String listPath = "/" + VERSION + "/" + OWNER + "/:" + OWNER_ID + "/" + ENTITY + "s";

    @Inject
    public BotDemoV1(final DemoData demoData) {
        this.demoData = demoData;
    }

    @Override
    public void register(final Router router, final WorkerExecutor executor) {

        this.executor = executor;
        // 路径参数验证
        HTTPRequestValidationHandler validationHandler =
                HTTPRequestValidationHandler.create()
                        .addPathParamWithCustomTypeValidator(OWNER_ID, createLongTypeValidator(0l), false)
                        .addPathParamWithCustomTypeValidator(ID, createLongTypeValidator(0l), false);

        HTTPRequestValidationHandler validationListHandler =
                HTTPRequestValidationHandler.create()
                        .addPathParamWithCustomTypeValidator(OWNER_ID, createLongTypeValidator(0l), false)
                        .addQueryParam(INDEX, ParameterType.INT, false)
                        .addQueryParam(SIZE, ParameterType.INT, false);

        // 添加
        router.post(mainPath).handler(validationHandler).handler(this::add);
        // 更新
        router.put(mainPath).handler(validationHandler).handler(this::update);
        // 获取
        router.get(mainPath).handler(validationHandler).handler(this::get);
        // 删除
        router.delete(mainPath).handler(validationHandler).handler(this::delete);
        // 获取列表
        router.get(listPath).handler(validationListHandler).blockingHandler(this::getList, false);

    }

    /**
     * 添加 - POST /owner/:owner_id/demo/:id
     *
     * @param context: HTTP 路由上下文
     */
    @Override
    public void add(final RoutingContext context) {

        executor.executeBlocking(future -> {
            try {
                final HttpServerRequest request = context.request();

                final long ownerId = Long.valueOf(context.pathParam(OWNER_ID));
                final long id = Long.valueOf(context.pathParam(ID));

                final String contentType = request.getHeader(CONTENT_TYPE);

                Demo message = getMessage(contentType, context);

                if (null != message) {

                    // TODO body 参数验证
                    if (ReqHelper.wrongString(context, message.getName(), "Name 不能为空")) {
                        return;
                    }

                    Demo.Builder builder = message.toBuilder();
                    builder.setCreatedAt(Instant.now().toEpochMilli())
                            .setUpdatedAt(Instant.now().toEpochMilli())
                            .setId(id).setOwnerId(ownerId);

                    // 持久化存储
                    demoData.add(builder.build());
                    future.complete("添加成功");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            future.fail("添加失败");

        }, false, asyncResult -> asyncResult(asyncResult, context, ResultType.DONE_VALUE));


    }

    /**
     * 更新 - PUT /owner/:owner_id/demo/:id
     *
     * @param context: HTTP 路由上下文
     */
    @Override
    public void update(final RoutingContext context) {
        executor.executeBlocking(future -> {
            try {
                final HttpServerRequest request = context.request();

                final long ownerId = Long.valueOf(context.pathParam(OWNER_ID));
                final long id = Long.valueOf(context.pathParam(ID));

                final String contentType = request.getHeader(CONTENT_TYPE);

                Demo demo = demoData.get(ownerId, id);
                if (null == demo) {
                    echoTransError(context, "Demo 数据不存在");
                    return;
                }

                Demo message = getMessage(contentType, context);

                if (null != message) {

                    // TODO body 参数验证
                    if (ReqHelper.wrongString(context, message.getName(), "Name 不能为空")) {
                        return;
                    }

                    Demo.Builder builder = message.toBuilder();
                    builder.setCreatedAt(demo.getCreatedAt())
                            .setUpdatedAt(Instant.now().toEpochMilli())
                            .setId(id).setOwnerId(ownerId);

                    // 持久化存储
                    demoData.update(builder.build());

                    future.complete("更新成功");
                    return;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            future.fail("更新失败");

        }, false, asyncResult -> asyncResult(asyncResult, context, ResultType.DONE_VALUE));

    }

    /**
     * 读取 - GET /owner/:owner_id/demo/:id
     */
    @Override
    public void get(final RoutingContext context) {

        executor.executeBlocking(future -> {

            final long ownerId = Long.valueOf(context.pathParam(OWNER_ID));
            final long demoId = Long.valueOf(context.pathParam(ID));

            // 提取伙伴数据
            final Demo demo = demoData.get(ownerId, demoId);

            future.complete(demo);

        }, false, asyncResult -> asyncResult(asyncResult, context, ResultType.ITEM_VALUE));

    }


    /**
     * 删除 - DELETE  /owner/:owner_id/demo/:id
     */
    @Override
    public void delete(final RoutingContext context) {

        executor.executeBlocking(future -> {

            final HttpServerRequest request = context.request();

            // 读取作者的 ID
            final long ownerId = getParamSafeLongValue(request, OWNER_HOLDER);
            if (ReqHelper.wrongLongId(context, ownerId, MESSAGE_OWNERID_CONDITION)) {
                return;
            }

            // 读取 ID
            final long demoId = getParamSafeLongValue(request, ID_HOLDER);
            if (ReqHelper.wrongLongId(context, demoId, MESSAGE_ID_CONDITION)) {
                return;
            }

            // 更新持久化存储
            demoData.delete(ownerId, demoId);

            future.complete("删除成功");

        }, false, asyncResult -> asyncResult(asyncResult, context, ResultType.DONE_VALUE));

    }


    /**
     * 读取 - GET /owner/:owner_id/demos?index=1&size=20
     */
    @Override
    public void getList(final RoutingContext context) {

        executor.executeBlocking(future -> {
            final HttpServerRequest request = context.request();

            // 读取拥有者的 ID
            final long ownerId = getParamSafeLongValue(request, OWNER_HOLDER);
            if (ReqHelper.wrongLongId(context, ownerId, MESSAGE_OWNERID_CONDITION)) {
                return;
            }

            // 读取分页 Index
            int pageIndex = getParamSafeIntegerValue(request, PAGE_INDEX);
            if (0 > pageIndex) {
                pageIndex = 0;
            }

            // 读取分页 Size
            int pageSize = getParamSafeIntegerValue(request, PAGE_SIZE);
            if (0 >= pageSize) {
                pageSize = 20;
            }

            // 提取清单
            final EchoList echoList = demoData.getList(ownerId, pageIndex, pageSize);

            future.complete(echoList);

        }, false, asyncResult -> asyncResult(asyncResult, context, ResultType.LIST_VALUE));

    }

    /***
     * 获取body数据
     * @param contentType
     * @param context
     */
    private Demo getMessage(final String contentType, final RoutingContext context) {
        Demo message = null;
        Buffer body = context.getBody();
        if (CONTENT_TYPE_STREAM.equals(contentType)) {
            if (0 >= body.getDelegate().getBytes().length) {
                echoTransError(context, "没有接收到 Body 数据");
            }
            try {
                message = Demo.parseFrom(body.getDelegate().getBytes());
            } catch (final Exception e) {
                echoTransError(context, "解析出错");
            }
        } else {
            message = (Demo) parseJsonToProtobuf(body.toString(), Demo.newBuilder());
        }
        return message;
    }

}
