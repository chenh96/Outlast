package tech.chenh.outlast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tech.chenh.outlast.core.Agent;
import tech.chenh.outlast.core.Proxy;
import tech.chenh.outlast.tunnel.TunnelRepository;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@Component
public class Runner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(Runner.class);

    private final Properties properties;
    private final TunnelRepository repository;

    @Autowired
    public Runner(Properties properties, TunnelRepository repository) {
        this.properties = properties;
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) throws InterruptedException, IOException {
        switch (properties.getMode()) {
            case "proxy":
                new Proxy(properties, repository).start();
                LOG.info("Running as proxy -> {}", properties.display());
                break;
            case "agent":
                new Agent(properties, repository).start();
                LOG.info("Running as agent -> {}", properties.display());
                break;
            case "test":
                new Proxy(properties, repository).start();
                new Agent(properties, repository).start();
                LOG.info("Running as test -> {}", properties.display());
                break;
        }
        new CountDownLatch(1).await();
    }

}
