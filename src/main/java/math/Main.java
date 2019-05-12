package math;

/**
 * author yg
 * description
 * date 2019/5/12
 */
public class Main {

    /**
     * 是否2的次方
     * @param val
     * @return
     */
    private static boolean isPowerOfTwo(int val) {
        return (val & -val) == val;
    }
}
