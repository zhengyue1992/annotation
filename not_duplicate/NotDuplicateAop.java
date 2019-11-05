package face_gate_server.annotation.not_duplicate;

import face_gate_server.entity.response.ResponseBase;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
@Slf4j
public class NotDuplicateAop {
    private ConcurrentHashMap<String, Long> redisTemplate = new ConcurrentHashMap<String, Long>();

    @Around("@annotation(notDuplicate)")
    public Object requestLimit(ProceedingJoinPoint joinPoint, NotDuplicate notDuplicate) throws Exception {
        Object[] args = joinPoint.getArgs();
        HttpServletRequest request = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof HttpServletRequest) {
                request = (HttpServletRequest) args[i];
                break;
            }
        }
        if (request == null) {
            throw new RuntimeException("方法中缺失HttpServletRequest参数");
        }
        StringBuffer keyBuffer = new StringBuffer();
        Enumeration enu = request.getParameterNames();//获取所有参数
        while (enu.hasMoreElements()) {
            String paraName = (String) enu.nextElement();
            keyBuffer.append(paraName);
            keyBuffer.append(request.getParameter(paraName));
        }
        String key = request.getRequestURI() + keyBuffer.toString();
        Object responseBase = null;
        if (redisTemplate.get(key) == null || redisTemplate.get(key) == 0) {
            redisTemplate.put(key, System.currentTimeMillis());
            try {
                responseBase = joinPoint.proceed(args);
            } catch (Throwable throwable) {
                return ResponseBase.serverError(throwable.getMessage());
            }
        } else {
            return ResponseBase.expectationFailed(notDuplicate.time() / 1000 + "秒内请不可发送重复请求！");
        }
//        Long count = redisTemplate.get(key);
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {    //创建一个新的计时器任务。
            @Override
            public void run() {
                redisTemplate.remove(key);
            }
        };
        timer.schedule(task, notDuplicate.time());
        return responseBase;
    }
}
