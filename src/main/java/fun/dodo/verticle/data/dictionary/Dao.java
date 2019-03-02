package fun.dodo.verticle.data.dictionary;

import com.google.protobuf.Message;
import fun.dodo.common.meta.Dictionary;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

@RegisterMapper(Mapper.class)
public interface Dao {

    /**
     * 写入
     */
    @SqlUpdate("INSERT INTO dictionary "
            + "(id, ownerId, type, name, notes, entity, enabled, createdAt, updatedAt) values "
            + "(:e.id, :e.ownerId, :e.type, :e.name, :e.notes, :bytes, :e.enabled, :e.createdAt, :e.updatedAt)")
    void add(@BindBean("e") Dictionary entity,  @Bind("bytes") byte[] bytes);

    /**
     * 更新
     */
    @SqlUpdate("UPDATE dictionary SET "
            + "entity = :bytes, enabled = :e.enabled, updatedAt = :e.updatedAt, name = :e.name, notes = :e.notes, ownerId = :e.ownerId, type = :e.type "
            + "WHERE id = :e.id")
    void update(@BindBean("e") Dictionary entity,  @Bind("bytes") byte[] bytes);

    /**
     * 读取 - by ID
     */
    @SqlQuery("SELECT entity FROM dictionary WHERE id = :id")
    Dictionary get(@Bind("id") long id);

    /**
     * 读取 - list
     */
    @SqlQuery("SELECT entity FROM dictionary WHERE ownerId = :ownerId order by createdAt desc")
    List<Dictionary> getList(@Bind("ownerId") long ownerId);

    /**
     * 删除 - by ID
     */
    @SqlUpdate("DELETE FROM dictionary WHERE id = :id")
    void delete(@Bind("id") long id);

}
