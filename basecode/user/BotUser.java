package fun.dodo.verticle.bots;

import com.google.gson.Gson;
import fun.dodo.common.help.ReqHelper;
import fun.dodo.common.interfaces.BotBase;
import fun.dodo.common.meta.User;
import fun.dodo.common.meta.UserList;
import fun.dodo.common.meta.UserRequest;
import fun.dodo.common.meta.UserRpcGrpc;
import fun.dodo.verticle.data.user.Data;
import io.grpc.stub.StreamObserver;
import io.vertx.ext.web.api.validation.ParameterType;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.api.validation.HTTPRequestValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static fun.dodo.common.Constants.*;
import static fun.dodo.common.help.ReqHelper.*;
import static fun.dodo.common.help.ResHelper.*;
import static io.vertx.reactivex.ext.web.api.validation.ParameterTypeValidator.createLongTypeValidator;
import static org.apache.http.HttpStatus.SC_OK;


@Singleton
public final class BotUser implements BotBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotUser.class);

    final Gson gson;

    // 数据代理
    private final Data data;

    // rpc
    private final UserRpcGrpc.UserRpcImplBase service;

    final static String VERSION = "v1";

    final static String OWNER = "owner";
    final static String ENTITY = "user";

    final static String OWNER_ID = "ownerId";
    final static String ID = "id";
    final static String PROP = "prop";
    final static String INDEX = "index";
    final static String SIZE = "size";
    final static String REFRESH = "refresh";

    final static String mainPath = "/" + VERSION + "/" + OWNER + "/:" + OWNER_ID + "/" + ENTITY + "/:" + ID;
    final static String listPath = "/" + VERSION + "/" + OWNER + "/:" + OWNER_ID + "/" + ENTITY + "s";

    @Inject
    public BotUser(final Gson gson, final Data data) {
        this.gson = gson;
        this.data = data;

        service = new UserRpcGrpc.UserRpcImplBase() {
            @Override
            public void get(UserRequest request, StreamObserver<User> responseObserver) {
                responseObserver.onNext(data.get(request.getOwnerId(), request.getId(), request.getRefresh()));
                responseObserver.onCompleted();
            }
            @Override
            public void getList(UserRequest request, StreamObserver<UserList> responseObserver) {
                responseObserver.onNext(data.getList(request.getOwnerId(), request.getIndex() > 0 ? request.getIndex() : 0, request.getSize() > 0 ? request.getSize() : 20, request.getRefresh()));
                responseObserver.onCompleted();
            }
            @Override
            public void add(User request, StreamObserver<User> responseObserver) {
                responseObserver.onNext(data.add(request) ? request : null);
                responseObserver.onCompleted();
            }
            @Override
            public void update(User request, StreamObserver<User> responseObserver) {
                responseObserver.onNext(data.update(request) ? request : null);
                responseObserver.onCompleted();
            }
            @Override
            public void del(UserRequest request, StreamObserver<UserRequest> responseObserver) {
                responseObserver.onNext(data.delete(request.getOwnerId(), request.getId()) ? request : null);
                responseObserver.onCompleted();
            }
        };

    }

    public UserRpcGrpc.UserRpcImplBase getService() {
        return service;
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

                if (ReqHelper.wrongString(context, message.getName(), "Name 不能为空")) {
                    return;
                }

                User.Builder builder = message.toBuilder();
                builder.setCreatedAt(Instant.now().toEpochMilli())
                        .setUpdatedAt(Instant.now().toEpochMilli())
                        .setId(id).setOwnerId(ownerId);

                // 持久化存储
                boolean result = data.add(builder.build());
                LOGGER.info("result  {}", result);

                if (result) {
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

            User user = data.get(ownerId, id, 1);
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
                if (builder.isInitialized() && data.update(builder.build())) {
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
            // 是否刷新缓存数据
            final int refresh = getParamSafeIntegerValue(context.request(), REFRESH);

            // 提取伙伴数据
            final User user = data.get(ownerId, userId, refresh);

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

            // 是否刷新缓存数据
            final int refresh = getParamSafeIntegerValue(request, REFRESH);

            // 提取清单
            final UserList list = data.getList(ownerId, pageIndex, pageSize, refresh);

            echoList(context, list.getObjectList(), list.getIndex(), list.getSize(), list.getCount());

            return;

        } catch (Exception e) {
            e.printStackTrace();
        }

        echoFoundError(context, "fail");

    }

    /**
     * 向前端反馈数据
     *
     * @param context: HTTP 路由上下文
     */
    public void echoList(final RoutingContext context, final List<User> list, final long index, final long size, final long count) {
        if (desiredJson(context)) {
            echoListJson(context, list, count);
        } else {
            echoListProto(context, list, index, size, count);
        }
    }


    /**
     * 把集合转换为 JSON 格式
     *
     * @param context: HTTP 路由上下文
     * @param list:    对象清单
     */
    private void echoListJson(final RoutingContext context, final List<User> list, final Long count) {
        final HttpServerResponse response = context.response();

        try {
            final fun.dodo.common.echo.EchoList echo = new fun.dodo.common.echo.EchoList();
            echo.getHead().setItemCount(count.intValue()).setMessage("读取清单");

            // 添加到结果集
            echo.getBody().setData(list);

            response.putHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                    .putHeader(CONTENT_CONTROL, CONTENT_CONTROL_VALUE)
                    .setStatusCode(HttpServletResponse.SC_OK)
                    .end(gson.toJson(echo));
        } catch (final Exception e) {
            echoTransError(context, Arrays.toString(e.getStackTrace()));
        }
    }


    /**
     * 把集合转换为 ProtoBuf 格式
     *
     * @param context: HTTP 路由上下文
     * @param list:    对象清单
     */
    private void echoListProto(final RoutingContext context, final List<User> list, final Long index, final Long size, final Long count) {
        final HttpServerResponse response = context.response();

        try {
            final UserList.Builder builder = UserList.newBuilder();
            builder.addAllObject(list);
            builder.setIndex(index.intValue()).setSize(size.intValue()).setCount(count.intValue());
            response.putHeader(CONTENT_TYPE, CONTENT_TYPE_STREAM)
                    .putHeader(CONTENT_CONTROL, CONTENT_CONTROL_VALUE)
                    .setStatusCode(HttpServletResponse.SC_OK)
                    .end(Buffer.buffer(Base64.getEncoder().encode(builder.build().toByteArray())));
        } catch (final Exception e) {
            echoTransError(context, Arrays.toString(e.getStackTrace()));
        }
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
                e.printStackTrace();
                echoTransError(context, "解析出错");
            }
        } else {
            message = (User) parseJsonToProtobuf(body.getBytes(), User.newBuilder());
        }
        return message;
    }

}
