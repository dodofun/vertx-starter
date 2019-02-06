package fun.dodo.verticle.acts;

import com.lmax.disruptor.util.DaemonThreadFactory;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadPool implements ThreadFactory {
    private final DaemonThreadFactory threadFactory = DaemonThreadFactory.INSTANCE;
    private final Collection<Thread> threadList = new CopyOnWriteArrayList<>();
    private final AtomicBoolean ignoreExecutions = new AtomicBoolean(false);
    private final AtomicInteger executionCount = new AtomicInteger(0);


    @Override
    public Thread newThread(final Runnable command) {
        executionCount.getAndIncrement();

        Runnable toExecute = command;
        if (ignoreExecutions.get()) {
            toExecute = new NoOpRunnable();
        }

        final Thread thread = threadFactory.newThread(toExecute);
        thread.setName(command.toString());
        threadList.add(thread);

        return thread;
    }


    public void joinAllThreads() {
        threadList.stream().filter(Thread::isAlive).forEach(thread -> {
            try {
                thread.interrupt();
                thread.join(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        threadList.clear();
    }


    public void ignoreExecutions() {
        ignoreExecutions.set(true);
    }


    public int getExecutionCount() {
        return executionCount.get();
    }


    private static final class NoOpRunnable implements Runnable {
        @Override
        public void run() {
        }
    }

}
