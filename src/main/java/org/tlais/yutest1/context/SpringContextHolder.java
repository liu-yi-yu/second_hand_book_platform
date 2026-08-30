package org.tlais.yutest1.context;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 容器持有器
 *
 * 小白理解：
 * 普通的类（比如 Controller、Service）可以用 @Autowired 注入别的 Bean。
 * 但 WebSocket 端点类（@ServerEndpoint）由 WebSocket 容器管理，不是 Spring 创建的，
 * 所以 @Autowired 在它里面是无效的（注入进来是 null）。
 *
 * 这个类的目的：在 Spring 启动时把 ApplicationContext 存到静态变量里，
 * 之后任何地方（包括 WebSocket 端点）都能通过 getBean() 拿到 Spring 管理的 Bean。
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    // 静态变量：保存 Spring 容器的引用，供全局访问
    private static ApplicationContext applicationContext;

    /**
     * Spring 启动时会自动调用这个方法，把容器传进来
     */
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * 根据类型获取 Spring 管理的 Bean
     *
     * @param clazz 要获取的 Bean 的类型，例如 JwtProperties.class
     */
    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }
}
