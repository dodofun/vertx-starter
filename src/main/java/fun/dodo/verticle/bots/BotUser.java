package fun.dodo.verticle.bots;

import fun.dodo.common.help.ReqHelper;
import fun.dodo.common.interfaces.BotBase;
import fun.dodo.common.meta.User;
import fun.dodo.common.meta.EchoList;
import fun.dodo.verticle.data.user.Data;
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
import java.lang.reflect.Method;
import java.time.Instant;

import static fun.dodo.common.Constants.*;
import static fun.dodo.common.help.ReqHelper.getParamSafeIntegerValue;
import static fun.dodo.common.help.ReqHelper.getParamSafeLongValue;
import static fun.dodo.common.help.ReqHelper.getParamStringValue;
import static fun.dodo.common.help.ResHelper.*;
import static io.vertx.reactivex.ext.web.api.validation.ParameterTypeValidator.createLongTypeValidator;
import static org.apache.http.HttpStatus.SC_OK;


@Singleton
public final class BotUser implements BotBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotUser.class);

    // 数据代理
    private final Data data;

    final static String VERSION = "v1";

    final static String OWNER = "owner";
    final static String ENTITY = "user";

    final static String OWNER_ID = "ownerId";
    final static String ID = "id";
    final static String PROP = "prop";
    final static String INDEX = "index";
    final static String SIZE = "size";

    final static String mainPath = "/" + VERSION + "/" + OWNER + "/:" + OWNER_ID + "/" + ENTITY + "/:" + ID;
    final static String listPath = "/" + VERSION + "/" + OWNER + "/:" + OWNER_ID + "/" + ENTITY + "s";

    @Inject
    public BotUser(final Data data) {
        this.data = data;
    }

    @Override
    public void register(final Router router) {

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
        router.get(listPath).handler(validationListHandler).handler(this::getList);

    }

    /**
     * 添加 - POST /owner/:ownerId/user/:id
     *
     * @param context: HTTP 路由上下文
     */
    @Override
    public void add(final RoutingContext context) {

        try {
            final HttpServerRequest request = context.request();

            final long ownerId = Long.valueOf(context.pathParam(OWNER_ID));
            final long id = Long.valueOf(context.pathParam(ID));

            // body 参数格式
            final String contentType = request.getHeader(CONTENT_TYPE);

            User message = getMessage(contentType, context);

            if (null != message) {

                // TODO body 参数验证
                if (ReqHelper.wrongString(context, message.getName(), "Name 不能为空")) {
                    return;
                }

                User.Builder builder = message.toBuilder();
                builder.setCreatedAt(Instant.now().toEpochMilli())
                        .setUpdatedAt(Instant.now().toEpochMilli())
                        .setId(id).setOwnerId(ownerId);

                // 持久化存储
                if (data.add(builder.build())) {
                    echoDoneMessage(context, SC_OK, "success");
                    return;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        echoFoundError(context, "fail");
    }

    /**
     * 更新 - PUT /owner/:ownerId/user/:id?prop=type
     * prop 不为空时，只更新指定属性值
     *
     * @param context: HTTP 路由上下文
     */
    @Override
    public void update(final RoutingContext context) {
        try {
            final HttpServerRequest request = context.request();

            final long ownerId = Long.valueOf(context.pathParam(OWNER_ID));
            final long id = Long.valueOf(context.pathParam(ID));

            final String prop = getParamStringValue(request, PROP);

            // body 参数格式
            final String contentType = request.getHeader(CONTENT_TYPE);

            User user = data.get(ownerId, id);
            if (null == user) {
                echoTransError(context, "User 数据不存在");
                return;
            }

            User message = getMessage(contentType, context);

            if (null != message) {

                User.Builder builder;

                if (null != prop && !prop.isEmpty()) {

                    // 更新指定属性
                    builder = user.toBuilder();
                    builder.setUpdatedAt(Instant.now().toEpochMilli());

                    // 属性首字母大写
                    String titleCaseProp = (new StringBuilder()).append(Character.toUpperCase(prop.charAt(0))).append(prop.substring(1)).toString();

                    Method getField = builder.getClass().getMethod("get" + titleCaseProp);
                    Method setField = builder.getClass().getMethod("set" + titleCaseProp, getField.getReturnType());

                    Object value = getField.invoke(message.toBuilder());

                    // 设置属性值
                    setField.invoke(builder, value);

                } else {

                    // 更新全部
                    builder = message.toBuilder();
                    builder.setCreatedAt(user.getCreatedAt())
                            .setUpdatedAt(Instant.now().toEpochMilli())
                            .setId(id).setOwnerId(ownerId);

                }

                // 持久化存储
                if (data.update(builder.build())) {
                    echoDoneMessage(context, SC_OK, "success");
                    return;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        echoFoundError(context, "fail");
    }

    /**
     * 删除 - DELETE  /owner/:ownerId/user/:id
     */
    @Override
    public void delete(final RoutingContext context) {

        try {
            final HttpServerRequest request = context.request();

            // 读取作者的 ID
            final long ownerId = getParamSafeLongValue(request, OWNER_HOLDER);
            if (ReqHelper.wrongLongId(context, ownerId, MESSAGE_OWNERID_CONDITION)) {
                return;
            }

            // 读取 ID
            final long userId = getParamSafeLongValue(request, ID_HOLDER);
            if (ReqHelper.wrongLongId(context, userId, MESSAGE_ID_CONDITION)) {
                return;
            }

            // 更新持久化存储
            if (data.delete(ownerId, userId)) {
                echoDoneMessage(context, SC_OK, "success");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        echoFoundError(context, "fail");

    }

    /**
     * 读取 - GET /owner/:ownerId/user/:id
     */
    @Override
    public void get(final RoutingContext context) {

        try {
            final long ownerId = Long.valueOf(context.pathParam(OWNER_ID));
            final long userId = Long.valueOf(context.pathParam(ID));

            // 提取伙伴数据
            final User user = data.get(ownerId, userId);

            echoItem(context, user);
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }

        echoFoundError(context, "fail");

    }

    /**
     * 读取 - GET /owner/:ownerId/users?index=1&size=20
     */
    @Override
    public void getList(final RoutingContext context) {

        try {

            final HttpServerRequest request = context.request();

            // 读取拥有者的 ID
            final long ownerId = getParamSafeLongValue(request, OWNER_ID);

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
            final EchoList echoList = data.getList(ownerId, pageIndex, pageSize);

            echoList(context, echoList.getObjectList(), echoList.getIndex(), echoList.getSize(), echoList.getCount());

            return;

        } catch (Exception e) {
            e.printStackTrace();
        }

        echoFoundError(context, "fail");

    }

    /***
     * 获取body数据
     * @param contentType
     * @param context
     */
    private User getMessage(final String contentType, final RoutingContext context) {
        User message = null;
        Buffer body = context.getBody();
        if (CONTENT_TYPE_STREAM.equals(contentType)) {
            if (0 >= body.getDelegate().getBytes().length) {
                echoTransError(context, "没有接收到 Body 数据");
                return null;
            }
            try {
                message = User.parseFrom(body.getDelegate().getBytes());
            } catch (final Exception e) {
                echoTransError(context, "解析出错");
            }
        } else {
            message = (User) parseJsonToProtobuf(body.toString(), User.newBuilder());
        }
        return message;
    }

}
