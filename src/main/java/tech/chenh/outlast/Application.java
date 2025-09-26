package tech.chenh.outlast;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String @Nullable [] args) throws Exception {
        Config config = Config.instance();
        switch (config.getMode()) {
            case "proxy":
                new Proxy().start();
                LOG.info("Running as proxy...");
                break;
            case "agent":
                new Agent().start();
                LOG.info("Running as agent...");
                break;
            case "test":
                new Proxy().start();
                new Agent().start();
                LOG.info("Running as test...");
                break;
        }

        new CountDownLatch(1).await();
    }

}