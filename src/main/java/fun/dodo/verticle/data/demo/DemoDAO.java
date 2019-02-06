package fun.dodo.verticle.data.demo;

import fun.dodo.common.meta.Demo;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

@RegisterMapper(DemoMapper.class)
public interface DemoDAO {

    /**
     * 写入
     */
    @SqlUpdate("INSERT INTO demo "
            + "(id, owner_id, name, notes, entity, enabled, created_at, updated_at) values "
            + "(:id, :owner_id, :name, :notes, :entity, true, :created_at, :created_at)")
    void add(@Bind("id") long id, @Bind("owner_id") long ownerId, @Bind("name") String name, @Bind("notes") String notes, @Bind("entity") byte[] entity, @Bind("created_at") long createdAt);

    /**
     * 更新
     */
    @SqlUpdate("UPDATE demo SET "
            + "entity = :entity, enabled = :enabled, updated_at = :updated_at, name = :name, notes = :notes,owner_id = :owner_id "
            + "WHERE id = :id")
    void update(@Bind("id") long id, @Bind("owner_id") long owner_id, @Bind("name") String name, @Bind("notes") String notes, @Bind("entity") byte[] entity, @Bind("enabled") boolean enabled, @Bind("updated_at") long updatedAt);

    /**
     * 读取 - by ID
     */
    @SqlQuery("SELECT entity FROM demo WHERE id = :id")
    Demo get(@Bind("id") long id);

    /**
     * 读取 - list
     */
    @SqlQuery("SELECT entity FROM demo WHERE owner_id = :owner_id order by created_at desc")
    List<Demo> getList(@Bind("owner_id") long owner_id);

    /**
     * 删除 - by ID
     */
    @SqlUpdate("DELETE FROM demo WHERE id = :id")
    void delete(@Bind("id") long id);

}
