package fun.dodo.verticle.data.dictionary;

import fun.dodo.common.meta.Dictionary;
import fun.dodo.common.meta.DictionaryList;
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
        boolean result = keeper.add(entity);
        if (result) {
            CompletableFuture.runAsync(() -> redis.add(entity.getOwnerId(), entity));
            return true;
        }
        return false;
    }

    /**
     * 更新
     *
     * @param entity: Entity
     */

    public boolean update(final Dictionary entity) {
        boolean result = keeper.update(entity);
        if (result) {
            CompletableFuture.runAsync(() -> redis.add(entity.getOwnerId(), entity));
            return true;
        }
        return false;
    }

    /**
     * 读取
     *
     * @param entityId
     */
    public Dictionary get(final long ownerId, final long entityId, final int refresh) {
        if (refresh == 0) {
            Optional<Dictionary> optRedis = redis.get(ownerId, entityId);
            if (optRedis.isPresent()) return optRedis.get();
        }
        final Optional<Dictionary> optMySQL = keeper.get(entityId);
        CompletableFuture.runAsync(() -> optMySQL.ifPresent(it -> redis.add(it.getOwnerId(), it)));
        return optMySQL.orElse(null);
    }

    /**
     * 读取 List 分页
     *
     * @param ownerId
     */
    public DictionaryList getList(final long ownerId, final int index, final int size, final int refresh) {
        DictionaryList.Builder builder = DictionaryList.newBuilder();
        builder.setIndex(index).setSize(size);
        if (refresh == 0) {
            final Optional<List> optRedis = redis.getList(ownerId, index, size);
            if (optRedis.isPresent()) {
                final Optional<Long> count = redis.getCount(ownerId);
                count.ifPresent(value -> builder.setCount(value.intValue()));
                builder.addAllObject(optRedis.get());
                return builder.build();
            }
        }

        final Optional<ArrayList<Dictionary>> optMySQL = keeper.getList(ownerId);
        if (optMySQL.isPresent()) {
            ArrayList list = optMySQL.get();
            CompletableFuture.runAsync(() -> optMySQL.ifPresent(it -> redis.addList(ownerId, list)));
            int start = index * size;
            int end = (index + 1) * size;
            if (start < list.size()) {
                builder.setCount(list.size());
                if (end < list.size())
                    builder.addAllObject(list.subList(start, end)).build();
                else
                    builder.addAllObject(list.subList(start, list.size())).build();
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
        boolean result = keeper.delete(entityId);
        if (result) {
            redis.delete(ownerId, entityId);
            return true;
        }
        return false;
    }

}
