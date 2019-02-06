package fun.dodo.verticle.data.demo;

import fun.dodo.common.meta.Demo;
import com.zaxxer.hikari.HikariDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static fun.dodo.common.Constants.DATABASE_ACCESS_ERROR;


@Singleton
public final class DemoKeeper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoKeeper.class);


    // JDBI 访问接口
    private final DBI dbi;


    @Inject
    public DemoKeeper(final HikariDataSource dataSource) {
        dbi = new DBI(dataSource);
    }


    /**
     * 写入
     *
     * @param demo : 模型定义
     */
    public void add(final Demo demo) {
        try (Handle handle = dbi.open()) {
            checkArgument(null != demo, "不能为空");

            final DemoDAO dao = handle.attach(DemoDAO.class);

            dao.add(demo.getId(), demo.getOwnerId(), demo.getName(), demo.getNotes(), demo.toByteArray(), demo.getCreatedAt());

        } catch (final Exception e) {
            LOGGER.error(DATABASE_ACCESS_ERROR + ", {};  {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }


    /**
     * 更新
     *
     * @param demo : 模型定义
     */
    public void update(final Demo demo) {
        try (Handle handle = dbi.open()) {
            checkArgument(null != demo, "不能为空");

            final DemoDAO dao = handle.attach(DemoDAO.class);

            dao.update(demo.getId(), demo.getOwnerId(), demo.getName(), demo.getNotes(), demo.toByteArray(), demo.getEnabled(), demo.getUpdatedAt());
        } catch (final Exception e) {
            LOGGER.error(DATABASE_ACCESS_ERROR + ", {};  {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
    }


    /***
     * 读取 Demo
     * @param demoId
     * @return
     */
    public Optional<Demo> get(final long demoId) {
        Optional<Demo> result = Optional.empty();

        try (Handle handle = dbi.open()) {
            checkArgument(0 < demoId, "ID 应该是大于零的整数");

            final DemoDAO dao = handle.attach(DemoDAO.class);

            Demo demo = dao.get(demoId);

            if (null != demo) {
                result = Optional.of(demo);
            }

        } catch (final Exception e) {
            LOGGER.error(DATABASE_ACCESS_ERROR + ", {};  {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
        return result;
    }


    /**
     * 读取 List
     */
    public Optional<List<Demo>> getList(long ownerId) {

        checkArgument(0 < ownerId, "ownerId 应该是大于零的整数");

        Optional<List<Demo>> result = Optional.empty();

        try (Handle handle = dbi.open()) {

            final DemoDAO dao = handle.attach(DemoDAO.class);

            List<Demo> list = dao.getList(ownerId);

            if (null != list) {
                result = Optional.of(list);
            }

        } catch (final Exception e) {
            LOGGER.error(DATABASE_ACCESS_ERROR + ", {};  {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }

        return result;
    }


    /**
     * 删除 Demo
     *
     * @param demoId
     */
    public void delete(final long demoId) {
        try (Handle handle = dbi.open()) {
            checkArgument(0 < demoId, "ID 应该是大于零的整数");

            final DemoDAO dao = handle.attach(DemoDAO.class);

            dao.delete(demoId);
        } catch (final Exception e) {
            LOGGER.error(DATABASE_ACCESS_ERROR + ", {};  {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
    }

}
