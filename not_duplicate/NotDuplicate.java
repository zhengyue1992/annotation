package face_gate_server.annotation.not_duplicate;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import java.lang.annotation.*;
/**
 * 防止重复注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
@Order(Ordered.HIGHEST_PRECEDENCE)
public @interface NotDuplicate {
    /**
     *
     * 时间段，单位为毫秒，默认值一分钟
     */
    long time() default 60000;
}