package rank;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * author yg
 * description
 * date 2019/4/3
 */
public class Rank {

    private volatile List<RankItem> lists = new LinkedList<>();
    private transient volatile ReadWriteLock lock = new ReentrantReadWriteLock();
    /**
     * 返回并清空排行榜
     * @return
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            // CLEAR
            lists.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 增加排行
     * @param item 排行榜对象
     * @param limit 排行榜最大容量
     */
    public void add(RankItem item, int limit) {
        lock.writeLock().lock();
        try {
            int index = lists.indexOf(item);
            if (index >= 0) {
                // 添加
                lists.remove(index);
            }

            int addTo = binarySearch(lists, item);
            if (addTo < 0) {
                lists.add(item);
            } else {
                lists.add(addTo, item);
            }

            // 截断
            while (lists.size() > limit) {
                lists.remove(lists.size() - 1);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 排行位置
     * @param owner owner
     * @return
     */
    public int indexOf(long owner) {
        RankItem item = RankItem.valueOf(owner, 0);
        lock.readLock().lock();
        try {
            return lists.indexOf(item);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 排行位置
     * @param index
     * @return
     */
    public RankItem get(int index) {
        if (index < 0) {
            return null;
        }
        lock.readLock().lock();
        try {
            if (index >= lists.size()) {
                return null;
            }
            return lists.get(index);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取子列表
     * @param fromIndex 开始位置, 包含
     * @param toIndex 结束位置, 包含
     * @return
     */
    public List<RankItem> subList(int fromIndex, int toIndex) {
        lock.readLock().lock();
        try {
            int size = lists.size();
            if (fromIndex < 0) {
                // 防止subList前端越界
                fromIndex = 0;
            }
            if (fromIndex > size - 1) {
                return new LinkedList<RankItem>();
            }
            int to = toIndex + 1;
            if (to > size) {
                to = size;
            }
            return new ArrayList<>(lists.subList(fromIndex, to));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取全部排行
     * @return
     */
    public List<RankItem> getLists() {
        lock.readLock().lock();
        try {
            List<RankItem> result = new ArrayList<RankItem>(lists);
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 当前大小
     * @return
     */
    public int size() {
        return lists.size();
    }

    // ----------

    private static <T> int binarySearch(List<? extends Comparable<? super T>> list, T key) {
        int low = 0;
        int high = list.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            Comparable<? super T> midVal = list.get(mid);
            int cmp = midVal.compareTo(key);
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return low; // key not found
    }
}
