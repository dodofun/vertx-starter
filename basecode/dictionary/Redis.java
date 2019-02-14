package fun.dodo.verticle.data.dictionary;

import fun.dodo.common.interfaces.RedisService;
import fun.dodo.common.meta.Dictionary;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Singleton
public final class Redis implements RedisService<Long, Long, Dictionary> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Redis.class);
    // 过期时间设置
    private static final long EXPIRE_SECONDS = 60 * 20;

    private final RedisReactiveCommands<byte[], byte[]> commands;

    @Inject
    public Redis(final RedisClient client) {
        commands = client.connect(ByteArrayCodec.INSTANCE).reactive();
    }

    // ----------------------------------------------------------------------------------
    // Utils
    //
    // 用于生成系统内的 Key
    // ----------------------------------------------------------------------------------
    private static final class KeyUtil {
        private static final String ENTITY_PREFIX = "dictionary:";
        private static final String MASTER_PREFIX = "owner:";
        private static final String LIST_PREFIX = "list";

        /**
         * Serial's Key
         */
        static byte[] serialKey() {
            return new StringBuilder(MASTER_PREFIX).append(ENTITY_PREFIX).append(SERIAL_NUMBER_SUFFIX).toString().getBytes();
        }

        /**
         * Entity's Key
         */
        static byte[] entityKey(final long id, final long id2) {
            return new StringBuilder(MASTER_PREFIX).append(id).append(":").append(ENTITY_PREFIX).append(id2).toString().getBytes();
        }

        /**
         * List Key
         */
        static byte[] listKey(final long id) {
            return new StringBuilder(MASTER_PREFIX).append(id).append(":").append(ENTITY_PREFIX).append(LIST_PREFIX).toString().getBytes();
        }
    }

    /**
     * 标识
     */
    @Override
    public long id() {
        try {
            return commands.incr(KeyUtil.serialKey()).block().longValue();
        } catch (final Exception e) {
            LOGGER.error("生成ID失败", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
        return -1L;
    }

    @Override
    public void add(final Long id, final Dictionary obj) {

        addObj(id ,obj);

        final byte[] listKey = KeyUtil.listKey(id);
        if (commands.exists(listKey).block().intValue() == 1) {
            addZset(id, obj);
        }

    }

    @Override
    public void addObj(final Long id, final Dictionary obj) {

        final byte[] entityKey = KeyUtil.entityKey(id, obj.getId());
        commands.set(entityKey, obj.toByteArray()).subscribe();
        commands.expire(entityKey, EXPIRE_SECONDS).subscribe();

    }

    @Override
    public void addZset(final Long id, final Dictionary obj) {

        final byte[] listKey = KeyUtil.listKey(id);
        commands.zadd(listKey, Long.valueOf(obj.getCreatedAt()).doubleValue(), String.valueOf(obj.getId()).getBytes()).subscribe();
        commands.expire(listKey, EXPIRE_SECONDS).subscribe();

    }

    @Override
    public void addMap(final Long id, final Map<Long, Dictionary> map) {
        map.forEach((key, value) -> add(id, value));
    }

    @Override
    public void addList(final Long id, final List<Dictionary> list) {
        list.forEach(value -> {
            addObj(id, value);
            addZset(id, value);
        });
    }

    @Override
    public Optional<Dictionary> get(final Long id, final Long id2) {
        try {
            final byte[] bytes = commands.get(KeyUtil.entityKey(id, id2)).block();
            if (Objects.nonNull(bytes) && (bytes.length > 0)) {
                return Optional.of(Dictionary.parseFrom(bytes));
            }
        } catch (final Exception e) {
            LOGGER.error("查询失败", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<List<byte[]>> getList(final Long id, long index, long size) {
        try {
            final List<byte[]> idList = commands.zrevrange(KeyUtil.listKey(id), index * size, (index + 1) * size - 1).collectList().block();
            if (Objects.nonNull(idList) && (!idList.isEmpty())) {
                return Optional.of(idList);
            }
        } catch (final Exception e) {
            LOGGER.error("查询失败", e.getMessage(), Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Long> getCount(final Long id) {
        try {
            return Optional.of(commands.zcard(KeyUtil.listKey(id)).block().longValue());
        } catch (final Exception e) {
            LOGGER.error("查询失败", e.getMessage(), Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        return Optional.of(0l);
    }

    @Override
    public void update(final Long id, final Long key, final Dictionary obj) {
        add(id, obj);
    }

    @Override
    public void delete(final Long id, final Long key) {
        commands.del(KeyUtil.entityKey(id, key)).subscribe();
        commands.zrem(KeyUtil.listKey(id), key.toString().getBytes()).subscribe();
    }

}
