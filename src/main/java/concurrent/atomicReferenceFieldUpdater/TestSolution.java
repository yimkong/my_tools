package concurrent.atomicReferenceFieldUpdater;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * author yg
 * description
 * date 2019/5/12
 */
public class TestSolution {
    static Node<Integer> node = new Node<>(0);
    static AtomicReferenceFieldUpdater<Node, Node>
            bufUpdater = AtomicReferenceFieldUpdater.newUpdater
            (Node.class, Node.class, "next");

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch c = new CountDownLatch(100);
        List<Thread> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(new Thread(() -> {
                for (int j = 1; j < 101; j++) {
                    Node<Integer> pre = node;
                    while (!bufUpdater.compareAndSet(pre, null, new Node<>(j))) {
                        pre = pre.getNext();
                    }
                }
                c.countDown();
            }));
        }
        for (Thread thread : list) {
            thread.start();
        }
        c.await();
        LinkedList<Integer> val = new LinkedList<>();
        do {
            Integer val1 = node.getVal();
            val.add(val1);
            node = node.next;
        } while (node != null);
        for (int i = 1; i < 101; i++) {
            for (int j = 0; j < 100; j++) {
                boolean remove = val.remove((Integer) i);
                if (!remove) {
                    System.err.println("error");
                }
            }
        }
        System.err.println(new Gson().toJson(val));
    }
}
