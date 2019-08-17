package fun.dodo.verticle.data.user;

import com.zaxxer.hikari.HikariDataSource;
import fun.dodo.common.meta.User;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static fun.dodo.common.Constants.DATABASE_ACCESS_ERROR;


@Singleton
public final class Keeper {
    private static final Logger LOGGER = LoggerFactory.getLogger(Keeper.class);

    // JDBI 访问接口
    private final DBI dbi;

    @Inject
    public Keeper(final HikariDataSource dataSource) {
        dbi = new DBI(dataSource);
    }

    /**
     * 写入
     *
     * @param entity : 模型定义
     */
    public boolean add(final User entity){
        try (Handle handle = dbi.open()) {
            checkArgument(null != entity, "不能为空");

            final Dao dao = handle.attach(Dao.class);

            int result = dao.add(entity, entity.toByteArray());
            return result > 0;
        } catch (final Exception e) {
            LOGGER.error(DATABASE_ACCESS_ERROR + ", {};  {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
        return false;
    }

    /**
     * 更新
     *
     * @param entity : 模型定义
     */
    public boolean update(final User entity) {
        try (Handle handle = dbi.open()) {
            checkArgument(null != entity, "不能为空");

            final Dao dao = handle.attach(Dao.class);

            int result = dao.update(entity, entity.toByteArray());

            return result > 0;
        } catch (final Exception e) {
            LOGGER.error(DATABASE_ACCESS_ERROR + ", {};  {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
        return false;
    }

    /***
     * 读取 User
     * @param entityId
     * @return
     */
    public Optional<User> get(final long entityId) {
        Optional<User> result = Optional.empty();

        try (Handle handle = dbi.open()) {
            checkArgument(0 < entityId, "ID 应该是大于零的整数");

            final Dao dao = handle.attach(Dao.class);

            User entity = dao.get(entityId);

            if (null != entity) {
                result = Optional.of(entity);
            }

        } catch (final Exception e) {
            LOGGER.error(DATABASE_ACCESS_ERROR + ", {};  {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
        return result;
    }

    /**
     * 读取 List
     */
    public Optional<ArrayList<User>> getList(long ownerId) {

        checkArgument(0 < ownerId, "ownerId 应该是大于零的整数");

        Optional<ArrayList<User>> result = Optional.empty();

        try (Handle handle = dbi.open()) {

            final Dao dao = handle.attach(Dao.class);

            ArrayList<User> list = (ArrayList)dao.getList(ownerId);

            if (null != list) {
                result = Optional.of(list);
            }

        } catch (final Exception e) {
            LOGGER.error(DATABASE_ACCESS_ERROR + ", {};  {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }

        return result;
    }

    /**
     * 删除 User
     *
     * @param entityId
     */
    public boolean delete(final long entityId) {
        try (Handle handle = dbi.open()) {
            checkArgument(0 < entityId, "ID 应该是大于零的整数");

            final Dao dao = handle.attach(Dao.class);

            int result = dao.delete(entityId);

            return result > 0;
        } catch (final Exception e) {
            LOGGER.error(DATABASE_ACCESS_ERROR + ", {};  {}", e.getMessage(), Arrays.toString(e.getStackTrace()));
        }
        return false;
    }

}
