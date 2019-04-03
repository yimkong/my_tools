package expression.tool;

/**
 * author yg
 * description
 * date 2019/4/3
 */
public class A {
    private int a = 1;
    private int b = 2;

    public int get(String s) {
        if (s.equals("a")) {
            return a;
        } else {
            return b;
        }
    }
}
