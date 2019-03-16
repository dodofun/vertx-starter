package fun.dodo.verticle.data.dictionary;

import fun.dodo.common.meta.Dictionary;
import fun.dodo.common.meta.EchoList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


@Singleton
public final class Data {
    private static final Logger LOGGER = LoggerFactory.getLogger(Data.class);

    // Redis
    private final Redis redis;
    // MySQL
    private final Keeper keeper;

    @Inject
    public Data(final Redis redis, final Keeper keeper) {
        this.redis = redis;
        this.keeper = keeper;
    }

    /**
     * 创建
     *
     * @param entity: Entity
     */
    public boolean add(final Dictionary entity) {
        return CompletableFuture
                .supplyAsync(() -> keeper.add(entity))
                .thenAcceptAsync(r -> {
                    if (r) redis.add(entity.getOwnerId(), entity);
                }).isDone();
    }

    /**
     * 更新
     *
     * @param entity: Entity
     */

    public boolean update(final Dictionary entity) {

        return CompletableFuture
                .supplyAsync(() -> keeper.update(entity))
                .thenAcceptAsync(r -> {
                    if (r) redis.add(entity.getOwnerId(), entity);
                }).isDone();
    }

    /**
     * 读取
     *
     * @param entityId
     */
    public Dictionary get(final long ownerId, final long entityId) {
        return redis.get(ownerId, entityId)
                .orElseGet(() -> {
                    LOGGER.info("Redis 缺乏数据 (Dictionary), 尝试从 MySQL 内获取 ***");

                    final Optional<Dictionary> optMySQL = keeper.get(entityId);
                    optMySQL.ifPresent(it -> redis.add(it.getOwnerId(), it));

                    return optMySQL.orElse(null);
                });
    }

    /**
     * 读取 List 分页
     *
     * @param ownerId
     */
    public EchoList getList(final long ownerId, final int index, final int size) {
        final Optional<List<byte[]>> optRedis = redis.getList(ownerId, index, size);

        EchoList.Builder builder = EchoList.newBuilder();

        builder.setIndex(index).setSize(size);

        if (optRedis.isPresent()) {
            final Optional<Long> count = redis.getCount(ownerId);
            count.ifPresent(value -> builder.setCount(value.intValue()));
            List<Dictionary> list = new ArrayList<>();
            optRedis.get().forEach(id -> {
                Dictionary entity = get(ownerId, Long.parseLong(new String(id)));
                if (null != entity) {
                    list.add(entity);
                }
            });
            builder.addAllObject((ArrayList) list);

        } else {
            LOGGER.info("Redis 缺乏数据 (Dictionary List), 尝试从 MySQL 内获取 ***");

            final Optional<List<Dictionary>> optMySQL = keeper.getList(ownerId);

            if (optMySQL.isPresent()) {
                List<Dictionary> list = optMySQL.get();
                redis.addList(ownerId, list);
                int start = index * size;
                int end = (index + 1) * size;
                if (start < list.size()) {
                    builder.setCount(list.size());
                    if (end < list.size())
                        builder.addAllObject((ArrayList) list.subList(start, end)).build();
                    else
                        builder.addAllObject((ArrayList) list.subList(start, list.size())).build();
                }
            }
        }
        return builder.build();
    }

    /**
     * 删除
     *
     * @param entityId: 伙伴的 ID
     */
    public boolean delete(final long ownerId, final long entityId) {

        return CompletableFuture
                .supplyAsync(() -> keeper.delete(entityId))
                .thenAcceptAsync(r -> {
                    if (r) redis.delete(ownerId, entityId);
                }).isDone();
    }

}
