package fun.dodo.verticle.data.user;

import com.google.protobuf.Message;
import fun.dodo.common.meta.User;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

@RegisterMapper(Mapper.class)
public interface Dao {

    /**
     * 写入
     */
    @SqlUpdate("INSERT INTO user "
            + "(id, ownerId, type, name, avatar, mobile, username, password, notes, entity, enabled, createdAt, updatedAt) values "
            + "(:e.id, :e.ownerId, :e.type, :e.name, :e.avatar, :e.mobile, :e.username, :e.password, :e.notes, :bytes, :e.enabled, :e.createdAt, :e.updatedAt)")
    void add(@BindBean("e") User entity, @Bind("bytes") byte[] bytes);

    /**
     * 更新
     */
    @SqlUpdate("UPDATE user SET "
            + "entity = :bytes, enabled = :e.enabled, updatedAt = :e.updatedAt, name = :e.name, notes = :e.notes, avatar = :e.avatar, mobile = :e.mobile, username = :e.username, password = :e.password, type = :e.type "
            + "WHERE id = :e.id")
    void update(@BindBean("e") User entity, @Bind("bytes") byte[] bytes);

    /**
     * 读取 - by ID
     */
    @SqlQuery("SELECT entity FROM user WHERE id = :id")
    User get(@Bind("id") long id);

    /**
     * 读取 - list
     */
    @SqlQuery("SELECT entity FROM user WHERE ownerId = :ownerId order by createdAt desc")
    List<User> getList(@Bind("ownerId") long ownerId);

    /**
     * 删除 - by ID
     */
    @SqlUpdate("DELETE FROM user WHERE id = :id")
    void delete(@Bind("id") long id);

}
