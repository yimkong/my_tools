import org.springframework.context.ApplicationListener;
import org.springframework.core.GenericTypeResolver;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

/**
 * author yg
 * description
 * date 2020/4/7
 */
public class SpringTools {
    public static void main(String[] args) {
        getGenericClass();
    }

    //获得泛型的class
    private static void getGenericClass() {
        Class<?> aClass = GenericTypeResolver.resolveTypeArgument(ScheduledAnnotationBeanPostProcessor.class, ApplicationListener.class);
        System.err.println(aClass);
    }
}
