package com.briup.cms.common.cache;

import com.briup.cms.common.util.ObjectUtil;
import com.briup.cms.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.Joinpoint;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author YuYan
 * @date 2023-12-13 11:04:43
 */
@Component
@Aspect
@RequiredArgsConstructor
public class CacheAspect {

    private final RedisUtil redisUtil;

    /**
     * 这个Map集合用来保存所有写入到Redis中的Key，
     * 按照Class进行分类。
     * Slideshow.class
     */
    private Map<Class<?>, List<String>> cacheRecord;

    {
        cacheRecord = new HashMap<>();
    }

    /**
     * 切入点：配置需要使用缓存机制的方法
     */
    @Pointcut("execution(* com.briup.cms.service..*.*(..)) " +
            "&& @annotation(Cached)")
    public void cachePointCut() {
    }

    /**
     * 切入点：配置需要清空缓存的方法
     */
    @Pointcut("execution(* com.briup.cms.service..*.*(..)) " +
            "&& @annotation(FlushCache)")
    public void flushCachePointCut() {

    }

    @AfterReturning("flushCachePointCut()")
    public void afterReturning(JoinPoint joinPoint) {
        // 当前目标方法的执行，应该触发哪个类型实体数据缓存的清空？
        /* 先通过连接点对象，获取到方法镜像 */
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        /* 获取到方法上定义的FlushCache注解 */
        FlushCache flushCache = method.getAnnotation(FlushCache.class);
        /* 获取注解中的flushEntityTypes属性 */
        Class<?>[] flushEntityTypes = flushCache.flushEntityTypes();
        System.out.println(method.getName() + "方法被执行，" +
                "需要被清空的缓存实体种类有：");
        for (Class<?> flushEntityType : flushEntityTypes) {
            System.out.print(flushEntityType.getName() + " ");
        }
        System.out.println();
        for (Class<?> flushEntityType : flushEntityTypes) {
            List<String> cacheKeys = cacheRecord.get(flushEntityType);
            if (ObjectUtil.isEmpty(cacheKeys)) {
                continue;
            }
            System.out.println("该实体的缓存中需要被删除的缓存的key有：");
            for (String cacheKey : cacheKeys) {
                System.out.print(cacheKey + " ");
            }
            System.out.println();
            redisUtil.delete(cacheKeys);
            // 清空List集合中记录的所有已缓存的key
            cacheKeys.clear();
        }
    }

    // 环绕通知
    // 1、前置+后置
    // 前置：尝试查询缓存
    // 后置：后置操作
    // 2、如果缓存命中，需要控制直接返回结果，
    // 而不执行目标方法
    @Around("cachePointCut()")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        /* 通过连接点对象获取到方法的签名对象 */
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        /* 通过方法的签名对象获取到方法的镜像对象 */
        Method method = signature.getMethod();
        /* 获取到方法名称 */
        String methodName = method.getName();
        /* 通过方法镜像获取到所在类的镜像对象 */
        Class<?> declaringClass = method.getDeclaringClass();
        /* 读取出类的名称 */
        // String className = declaringClass.getName();
        /* 获取到该方法所有参数的类型 */
        Class<?>[] parameterTypes = method.getParameterTypes();
        /* 通过所有的Class对象拿到每一个参数类型的简略类名，拼接成字符串 */
        String argType = null;;
        if (ObjectUtil.nonEmpty(parameterTypes)) {
            argType = Arrays.stream(parameterTypes)
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining("-"));
        }
        /* 通过连接点获取到本次调用方法传入的参数 */
        Object[] args = proceedingJoinPoint.getArgs();

        /* 根据上述信息，生成缓存Key */
        String cacheKey = redisUtil.generateKey(declaringClass, methodName, argType, args);

        /* 尝试从Redis缓存中取数据 */
        Object cacheData = redisUtil.get(cacheKey);

        /* 如果数据存在，则直接返回，不调用目标方法 */
        if (ObjectUtil.nonEmpty(cacheData)) {
            System.out.println("缓存命中……");
            return cacheData;
        }
        System.out.println("缓存未命中……");
        /* 如果数据不存在，则调用目标方法，查询数据库 */
        Object returnValue = proceedingJoinPoint.proceed();
        System.out.println(returnValue);
        /* 查询到的结果，置入到Redis缓存中 */
        redisUtil.set(cacheKey, returnValue);

        /* 将写入缓存的Key记录下来 */
        // 先拿到目标方法所在的Service类的镜像，获取类上的CacheEntity注解
        CacheEntity cacheEntity = declaringClass.getAnnotation(CacheEntity.class);
        // 读取该注解中的entityTypes属性值（该Service负责处理的实体种类）
        Class<?>[] classes = cacheEntity.entityTypes();
        for (Class<?> aClass : classes) {
            // 这部分数据缓存之后，要在Map集合中标记为是哪些实体种类的数据被缓存了
            List<String> cacheKeys = cacheRecord.get(aClass);
            if (ObjectUtil.isNull(cacheKeys)) {
                System.out.println("类型：" + aClass.getName() + "， 该类型第一次被缓存。");
                cacheKeys = new ArrayList<>();
                cacheRecord.put(aClass, cacheKeys);
            }
            cacheKeys.add(cacheKey);
            System.out.println("已被记录：" + cacheKey);
        }

        /* 返回目标方法返回的结果 */
        return returnValue;
    }

}
