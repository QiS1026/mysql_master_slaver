package com.base.config;

/**
 * 使用ThreadLocal技术来记录当前线程中的数据源的key
 * @author qishuo
 * @date 2019年4月22日 下午1:25:19
 */
public class DynamicDataSourceHolder {

    // 写库对应的数据源key
    private static final String master = "master";
    
    // 读库对应的数据源key
    private static final String slave = "slave";
    
    // 使用ThreadLocal记录当前线程的数据源key
    private static final ThreadLocal<String> holder = new ThreadLocal<String>();
    
    /**
     * @function 设置数据源key
     * @param key
     * @author qishuo
     * @date 2019年4月22日 下午1:30:24
     */
    public static void putDataSourceKey(String key) {
        holder.set(key);
    }
    
    /**
     * @function 获取数据源key
     * @return
     * @author qishuo
     * @date 2019年4月22日 下午1:34:57
     */
    public static String getDataSourceKey() {
        return holder.get();
    }
    
    /**
     * @function 标记写库
     * @author qishuo
     * @date 2019年4月22日 下午1:35:48
     */
    public static void markMaster() {
        putDataSourceKey(master);
    }
    
    /**
     * @function 标记读库
     * @author qishuo
     * @date 2019年4月22日 下午1:36:24
     */
    public static void markSlave() {
        putDataSourceKey(slave);
    }
}
