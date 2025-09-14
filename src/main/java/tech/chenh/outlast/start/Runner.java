package tech.chenh.outlast.start;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tech.chenh.outlast.core.Agent;
import tech.chenh.outlast.core.Proxy;
import tech.chenh.outlast.data.Crud;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@Component
public class Runner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(Runner.class);

    private final Context context;

    @Autowired
    public Runner(Properties properties, Crud crud, TransactionTemplate transaction) {
        this.context = new Context(properties, crud, transaction);
    }

    @Override
    public void run(ApplicationArguments args) throws InterruptedException, IOException {
        switch (context.getProperties().getMode()) {
            case "proxy":
                new Proxy(context).start();
                LOG.info("Running as proxy -> {}", context.getProperties().display());
                break;
            case "agent":
                new Agent(context).start();
                LOG.info("Running as agent -> {}", context.getProperties().display());
                break;
            case "test":
                new Proxy(context).start();
                new Agent(context).start();
                LOG.info("Running as test -> {}", context.getProperties().display());
                break;
        }
        new CountDownLatch(1).await();
    }

}