package expression;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import expression.tool.A;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * author yg
 * description
 * date 2019/4/3
 */
public class Main {
    private static LoadingCache<Long, Lock> locks;

    public static void main(String[] args) throws InterruptedException {
//        String expression = "get(\"a\")<get(\"b\")";
//        Boolean invoke = ExpressionHelper.invoke(expression, Boolean.class, new A());
//        System.err.println(invoke);
        locks = CacheBuilder.newBuilder().expireAfterAccess(1L, TimeUnit.SECONDS).build(new CacheLoader<Long, Lock>() {
            public Lock load(Long id) throws Exception {
                return new ReentrantLock();
            }
        });
        Lock unchecked = locks.getUnchecked(1L);
        Lock unchecked1 = locks.getUnchecked(1L);
        System.err.println(unchecked.equals(unchecked1));
        Thread.sleep(1000);
        Lock unchecked2 = locks.getUnchecked(1L);
        System.err.println(unchecked.equals(unchecked2));

    }
}

