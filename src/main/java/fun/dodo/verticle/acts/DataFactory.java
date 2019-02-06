package fun.dodo.verticle.acts;

import com.lmax.disruptor.EventFactory;

import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
final class DataFactory implements EventFactory<DataEvent> {
    private static final AtomicLong ITEM_COUNTER = new AtomicLong(1L);


    @Override
    public DataEvent newInstance() {
        return new DataEvent(ITEM_COUNTER.getAndIncrement());
    }

}
