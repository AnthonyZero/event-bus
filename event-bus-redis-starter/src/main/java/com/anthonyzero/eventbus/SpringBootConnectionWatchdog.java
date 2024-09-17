package com.anthonyzero.eventbus;


import com.anthonyzero.eventbus.core.base.Lifecycle;
import com.anthonyzero.eventbus.core.base.NodeTestConnect;
import com.anthonyzero.eventbus.core.config.GlobalConfig;
import com.anthonyzero.eventbus.core.part.ConnectionWatchdog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.Collection;


/**
 * 启动
 * @date 2024/1/17
 **/
@Slf4j
public class SpringBootConnectionWatchdog
        extends ConnectionWatchdog implements ApplicationRunner, DisposableBean {
    public SpringBootConnectionWatchdog(NodeTestConnect testConnect,
                                        GlobalConfig.TestConnect testConnectProperties,
                                        Collection<Lifecycle> listeners) {
        super(testConnect, testConnectProperties, listeners);
    }

    @Override
    public void run(ApplicationArguments args) {
        super.startup();
    }

    @Override
    public void destroy() {
        super.shutdown();
    }
}
