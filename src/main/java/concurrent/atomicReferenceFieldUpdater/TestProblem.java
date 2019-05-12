package concurrent.atomicReferenceFieldUpdater;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * author yg
 * description
 * date 2019/5/12
 */
public class TestProblem {
    static Node<Integer> node = new Node<>(0);

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch c = new CountDownLatch(100);
        List<Thread> list = new ArrayList<>();
        for (int i = 1; i < 101; i++) {
            list.add(new Thread(() -> {
                for (int j = 1; j < 101; j++) {
                    Node<Integer> pre = node;
                    do {
                        Node<Integer> next = pre.getNext();
                        if (next == null) {
                            pre.setNext(new Node<>(j));
                            break;
                        } else {
                            pre = next;
                        }
                    } while (true);
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
