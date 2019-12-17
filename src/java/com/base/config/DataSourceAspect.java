package com.base.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 定义数据源的AOP切面，该类控制了使用Master还是Slave。
 * 如果事务管理中配置了事务策略，则采用配置的事务策略中的标记了ReadOnly的方法是用Slave，其它使用Master。
 * 如果没有配置事务管理的策略，则采用方法名匹配的原则，以query、find、get开头方法用Slave，其它用Master。
 * @author qishuo
 * @date 2019年4月22日 下午2:56:14
 */
public class DataSourceAspect {

    private List<String> slaveMethodPattern = new ArrayList<String>();
    
    private static final String[] defaultSlaveMethodStart = new String[]{"query", "find", "get"};
    
    private String[] slaveMethodStart;
    
    /**
     * @function 读取事务管理中的策略
     * @param txAdvice
     * @author qishuo
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     * @date 2019年4月22日 下午3:00:46
     */
    public void setTxAdvice(TransactionInterceptor txAdvice) throws IllegalArgumentException, IllegalAccessException {
        if (txAdvice == null) {
            // 没有配置事务管理策略
            return;
        }
        // 从txAdvice获取到策略配置信息
        TransactionAttributeSource transactionAttributeSource = txAdvice.getTransactionAttributeSource();
        if (!(transactionAttributeSource instanceof NameMatchTransactionAttributeSource)) {
            return;
        }
        // 使用反射技术获取到NameMatchTransactionAttributeSource对象中的nameMap属性值
        NameMatchTransactionAttributeSource matchTransactionAttributeSource = (NameMatchTransactionAttributeSource) transactionAttributeSource;
        Field nameMapField = ReflectionUtils.findField(NameMatchTransactionAttributeSource.class, "nameMap");
        nameMapField.setAccessible(true); // 设置改字段可访问
        // 获取nameMap的值
        @SuppressWarnings("unchecked")
        Map<String, TransactionAttribute>  map = (Map<String, TransactionAttribute>) nameMapField.get(matchTransactionAttributeSource);
        // 遍历nameMap
        for (Entry<String, TransactionAttribute> entry : map.entrySet()) {
            if (!entry.getValue().isReadOnly()) {
                // 判断之后定义了ReadOnly的策略才加入到slaveMethodPattern
                continue;
            }
            slaveMethodPattern.add(entry.getKey());
        }
    }
    
    /**
     * @function 在进入Service方法之前执行
     * @param point 切面对象
     * @author qishuo
     * @date 2019年4月22日 下午3:09:36
     */
    public void before(JoinPoint point) {
        // 获取到当前执行的方法名
        String methodName = point.getSignature().getName();
        boolean isSlave = false;
        if (slaveMethodPattern.isEmpty()) {
            // 当前Spring容器中和没有配置事务策略，采用方法名匹配方式
            isSlave = isSlave(methodName);
        } else {
            // 使用策略规则匹配
            for (String mappedName : slaveMethodPattern) {
                if (isMatch(methodName, mappedName)) {
                    isSlave = true;
                    break;
                }
            }
        }
        
        if (isSlave) {
            // 标记为读库
            DynamicDataSourceHolder.markSlave();
        } else {
            // 标记为写库
            DynamicDataSourceHolder.markMaster();
        }
        System.out.println(methodName + "        " + isSlave);
    }
    
    /**
     * @function 判断是否为读库
     * @param methodName
     * @return
     * @author qishuo
     * @date 2019年4月22日 下午3:13:26
     */
    private Boolean isSlave(String methodName) {
        return StringUtils.startsWithAny(methodName, getSlaveMethodStart());
    }
    
    /**
     * 通配符匹配
     * Return if the given method name matches the mapped name.
     * <p>
     * The default implementation checks for "xxx*", "*xxx" and "*xxx*" matches, as well as direct
     * equality. Can be overridden in subclasses.
     * 
     * @param methodName
     * @param mappedName
     * @return
     * @author qishuo
     * @date 2019年4月22日 下午3:15:30
     */
    protected boolean isMatch(String methodName, String mappedName) {
        return PatternMatchUtils.simpleMatch(mappedName, methodName);
    }
    
    /**
     * 用户指定slave的方法名前缀
     * @param slaveMethodStart
     * @author qishuo
     * @date 2019年4月22日 下午3:18:36
     */
    public void setSlaveMethodStart(String[] slaveMethodStart) {
        this.slaveMethodStart = slaveMethodStart;
    }
    
    public String[] getSlaveMethodStart() {
        if (this.slaveMethodStart == null) {
            // 没有指定，使用默认
            return defaultSlaveMethodStart;
        }
        return slaveMethodStart;
    }
}
