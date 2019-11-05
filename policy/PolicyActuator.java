package face_gate_server.annotation.policy;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.ognl.MethodFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import javax.el.MethodNotFoundException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhengyue
 * @date 2019-10-26 22:21
 */
@Component
public class PolicyActuator {

    /**
     * 执行方法的实例集合，为方法执行做准备
     */
    private Map<String, Map<String, Object>> groupObjectMap;

    /**
     * 被注解的方法的集合
     */
    private Map<String, Map<String, Method>> groupMethodMap;

    /**
     * 默认分组名称
     */
    private static final String DEFAULT_MAP_NAME = "defaultMap";

    @Autowired
    public PolicyActuator(ApplicationContext applicationContext) {
        groupObjectMap = new HashMap<>();
        groupMethodMap = new HashMap<>();
        //从容器中获取所有实现类
        Map<String, PolicyInterface> beans = applicationContext.getBeansOfType(PolicyInterface.class);
        //循环获取类
        for (Map.Entry<String, PolicyInterface> entry : beans.entrySet()) {
            Class<? extends PolicyInterface> thisClass = entry.getValue().getClass();
            //获取当前类中的所有方法
            Method[] methods = thisClass.getDeclaredMethods();
            for (Method method : methods) {
                //判断方法上是否包含PolicyType注解
                if (method.isAnnotationPresent(PolicyType.class)) {
                    PolicyType policyType = method.getAnnotation(PolicyType.class);
                    String group = policyType.group();
                    Map<String, Object> objectMap = getObjectMap(group, ReadOrWriteType.WRITE);
                    Map<String, Method> methodMap = getMethodMap(group, ReadOrWriteType.WRITE);
                    methodMap.put(policyType.policy(), method);
                    objectMap.put(policyType.policy(), applicationContext.getBean(thisClass));
                }
            }
        }
    }

    /**
     * 默认分组 不带参数的方法执行
     * @param policy
     * @throws ClassNotFoundException
     * @throws MethodFailedException
     */
    public void runPolicy(String policy)
            throws ClassNotFoundException, MethodFailedException, IllegalAccessException {
        runPolicy(null, policy, null);
    }

    /**
     * 默认分组的 带参数的方法执行
     * @param policy
     * @param args
     * @throws ClassNotFoundException
     * @throws MethodFailedException
     */
    public void runPolicy(String policy, Object[] args)
            throws ClassNotFoundException, MethodFailedException, IllegalAccessException {
        runPolicy(null, policy, args);
    }

    /**
     * 带分组的 不带参数的方法执行
     * @param group
     * @param policy
     * @throws ClassNotFoundException
     * @throws MethodFailedException
     */
    public void runPolicy(String group, String policy)
            throws ClassNotFoundException, MethodFailedException, IllegalAccessException {
        runPolicy(group, policy, null);
    }

    /**
     * 带分组 带参数的方法执行
     * @param group
     * @param policy
     * @param args
     * @throws ClassNotFoundException
     * @throws MethodFailedException
     */
    public void runPolicy(String group, String policy, Object[] args)
            throws ClassNotFoundException, MethodFailedException, IllegalAccessException {
        if (StringUtils.isBlank(policy)) {
            throw new IllegalAccessException("参数policy不能为null或者空字符串！");
        }
        Object obj = getObject(group, policy);
        Method method = getMethod(group,policy);
        if (null == obj || null == method) {
            throw new MethodNotFoundException("没有发现策略：" + policy);
        }
        try {
            if (null == args) {
                method.invoke(obj);
            } else {
                method.invoke(obj, args);
            }
        } catch (Exception e) {
            throw new MethodFailedException(obj, method.getName());
        }
    }

    /**
     * 获取执行对象
     * @param policy
     * @return
     * @throws ClassNotFoundException
     */
    public Object getObject(String group, String policy) throws ClassNotFoundException {
        Map<String, Object> objectMap = getObjectMap(group, ReadOrWriteType.READ);
        if (null == objectMap || objectMap.size() <= 0) {
            throw new ClassNotFoundException("在分组" + group +"里，spring容器中没有找到" + PolicyInterface.class.getName() + "的继承类；"
                    + "请检查类是否继承了" + PolicyInterface.class.getName() + ",是否添加@Component相关注解");
        }
        Object obj = objectMap.get(policy);
        if (obj == null) {
            return expressionParser(objectMap, policy);
        } else {
            return obj;
        }
    }

    /**
     * 获取要执行的方法
     * @param policy
     * @return
     */
    public Method getMethod(String group, String policy) {
        Map<String, Method> methodMap = getMethodMap(group, ReadOrWriteType.READ);
        if (null == methodMap || methodMap.size() <= 0) {
            throw new MethodNotFoundException("在分组" + group +"中没有找到被"
                    + PolicyType.class.getName() + "注解的方法；");
        }
        Method method = methodMap.get(policy);
        if (null == method) {
            return  (Method) expressionParser(methodMap, policy);
        } else {
            return method;
        }
    }

    /**
     * 获取根据正则获取map中对应的对象
     * @param epMap
     * @param policy
     * @return
     */
    public Object expressionParser(Map epMap, String policy) {
        ExpressionParser ep = new SpelExpressionParser();
        for (Object keyO : epMap.keySet()) {
            String key = (String) keyO;
            if (key.indexOf("@") != -1) {
                key = key.replaceAll("@", policy);
                if (ep.parseExpression(key).getValue(Boolean.class)) {
                    return epMap.get(keyO);
                }
            }
        }
        return null;
    }

    /**
     * 获取分组对象map
     * @param group
     * @return
     */
    public Map<String, Object> getObjectMap(String group, ReadOrWriteType type) {
        if (null == groupObjectMap) {
            groupObjectMap = new HashMap<>();
        }
        if (StringUtils.isBlank(group)) {
            group = DEFAULT_MAP_NAME;
        }
        Map<String, Object> objectMap = groupObjectMap.get(group);
        if (null == objectMap && ReadOrWriteType.WRITE.equals(type)) {
            objectMap = new HashMap<>();
            groupObjectMap.put(group, objectMap);
        }
        return objectMap;
    }

    /**
     * 获取分组方法map
     * @param group
     * @return
     */
    public Map<String, Method> getMethodMap(String group, ReadOrWriteType type) {
        if (null == groupMethodMap) {
            groupMethodMap = new HashMap<>();
        }
        if (StringUtils.isBlank(group)) {
            group = DEFAULT_MAP_NAME;
        }
        Map<String, Method> methodMap = groupMethodMap.get(group);
        if (null == methodMap && ReadOrWriteType.WRITE.equals(type)) {
            methodMap = new HashMap<>();
            groupMethodMap.put(group, methodMap);
        }
        return methodMap;
    }

}

enum ReadOrWriteType {
    READ, WRITE;
}