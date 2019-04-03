package expression;

import expression.tool.A;

/**
 * author yg
 * description
 * date 2019/4/3
 */
public class Main {
    public static void main(String[] args) {
        String expression = "get(\"a\")<get(\"b\")";
        Boolean invoke = ExpressionHelper.invoke(expression, Boolean.class, new A());
        System.err.println(invoke);
    }
}

