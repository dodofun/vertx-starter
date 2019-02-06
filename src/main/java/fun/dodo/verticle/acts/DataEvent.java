package fun.dodo.verticle.acts;

import fun.dodo.common.meta.Log;

import javax.inject.Singleton;
import java.io.Serializable;

@Singleton
public final class DataEvent implements Serializable {
  private static final long serialVersionUID = 6900557611426312314L;

  // 标识
  private long id; // 可以作为事件数的计量

  private Log message;

  // 完成状态
  private boolean success = false;

  // 出现错误
  private boolean foundError = false;


  // 公共构造函数
  public DataEvent(final long id) {
    this.id = id;
  }

  public long getId() {
    return id;
  }

  public Log getLog() {
    return message;
  }

  public void setLog(Log message) {
    this.message = message;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public boolean isFoundError() {
    return foundError;
  }

  public void setFoundError(boolean foundError) {
    this.foundError = foundError;
  }
}
