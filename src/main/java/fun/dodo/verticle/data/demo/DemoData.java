package fun.dodo.verticle.data.demo;

import fun.dodo.common.meta.Demo;
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
public final class DemoData {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoData.class);

    // Redis
    private final DemoRedis demoRedis;
    // MySQL
    private final DemoKeeper demoKeeper;

    @Inject
    public DemoData(final DemoRedis demoRedis, final DemoKeeper demoKeeper) {
        this.demoRedis = demoRedis;
        this.demoKeeper = demoKeeper;
    }

    /**
     * 创建 Demo
     *
     * @param demo: Entity
     */
    public void add(final Demo demo) {

        CompletableFuture
                .runAsync(() -> demoKeeper.add(demo))
                .thenRunAsync(() -> demoRedis.add(demo.getOwnerId(), demo))
                .whenCompleteAsync((a, e) -> {

                });

    }

    /**
     * 更新 Demo
     *
     * @param demo: Entity
     */

    public void update(final Demo demo) {

        CompletableFuture
                .runAsync(() -> demoKeeper.update(demo))
                .thenRunAsync(() -> demoRedis.add(demo.getOwnerId(), demo))
                .whenComplete((a, e) -> {
                });
    }

    /**
     * 读取 Demo
     *
     * @param demoId
     */
    public Demo get(final long ownerId, final long demoId) {
        return demoRedis.get(ownerId, demoId)
                .orElseGet(() -> {
                    LOGGER.info("Redis 缺乏数据 (Demo), 尝试从 MySQL 内获取 ***");

                    final Optional<Demo> optMySQL = demoKeeper.get(demoId);
                    optMySQL.ifPresent(it -> demoRedis.add(it.getOwnerId(), it));

                    return optMySQL.orElse(null);
                });
    }

    /**
     * 读取 Demo List 分页
     *
     * @param ownerId
     */
    public EchoList getList(final long ownerId, final int index, final int size) {
        final Optional<List<byte[]>> optRedis = demoRedis.getList(ownerId, index, size);

        EchoList.Builder builder = EchoList.newBuilder();

        builder.setIndex(index).setSize(size);

        if (optRedis.isPresent()) {
            final Optional<Long> count = demoRedis.getCount(ownerId);
            count.ifPresent(value -> builder.setCount(value.intValue()));
            List<Demo> list = new ArrayList<>();
            optRedis.get().forEach(id -> {
                Demo demo = get(ownerId, Long.parseLong(new String(id)));
                if (null != demo) {
                    list.add(demo);
                }
            });
            builder.addAllObject((List) list);

        } else {
            LOGGER.info("Redis 缺乏数据 (Demo List), 尝试从 MySQL 内获取 ***");

            final Optional<List<Demo>> optMySQL = demoKeeper.getList(ownerId);

            if (optMySQL.isPresent()) {
                List<Demo> list = optMySQL.get();
                demoRedis.addList(ownerId, list);
                int start = index * size;
                int end = (index + 1) * size;
                if (start < list.size()) {
                    builder.setCount(list.size());
                    if (end < list.size())
                        builder.addAllObject((List) list.subList(start, end)).build();
                    else
                        builder.addAllObject((List) list.subList(start, list.size())).build();
                }
            }
        }
        return builder.build();
    }

    /**
     * 删除 Demo
     *
     * @param demoId: 伙伴的 ID
     */
    public void delete(final long ownerId, final long demoId) {

        CompletableFuture
                .runAsync(() -> demoKeeper.delete(demoId))
                .thenRunAsync(() -> demoRedis.delete(ownerId, demoId))
                .whenComplete((a, e) -> {
                });

    }

}
