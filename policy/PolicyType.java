package face_gate_server.annotation.policy;

import java.lang.annotation.*;

/**
 * @author zhengyue
 * @date 2019-10-26 22:18
 */
//@Documented//若使用javadoc生成文档时 将注解也生成文档则加入该注解
//@Order(Ordered.HIGHEST_PRECEDENCE)//spring的注解 会传入一个常数 数字越小优先加载等级会越高
@Inherited
@Target(ElementType.METHOD)//表示该自定义注解只能使用在方法上
@Retention(RetentionPolicy.RUNTIME)//表示该注解的声明周期 将注解保留在运行时
public @interface PolicyType {
    /**
     * 策略，可以使用spel表达式中的返回boolean类型的表达式
     * 使用@占位要传入的值(@也是必须传入的，否则会报错)
     * 判断大于6： @>6 或者 @>2*3 等
     * 判断小于6： @<6 或者 @<2*3 等
     * 判断某个数介于1-100之间： @ between {1,100}
     * 逻辑运算符：@>6 or @<0; @>6 and @<100
     * @return
     */
    String policy();

    /**
     * 分组，即将注解归类，在分组中获取
     * @return
     */
    String group() default "";

}
