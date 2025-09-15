package tech.chenh.outlast;

import tech.chenh.outlast.core.Agent;
import tech.chenh.outlast.core.Proxy;
import tech.chenh.outlast.data.Repository;
import tech.chenh.outlast.start.Config;
import tech.chenh.outlast.start.Context;

import java.util.concurrent.CountDownLatch;

public class Application {

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        Repository repository = Repository.getInstance(config);
        Context context = new Context(config, repository);

        switch (config.getMode()) {
            case "proxy":
                new Proxy(context).start();
                System.out.println("Running as proxy -> " + config.display());
                break;
            case "agent":
                new Agent(context).start();
                System.out.println("Running as agent -> " + config.display());
                break;
            case "test":
                new Proxy(context).start();
                new Agent(context).start();
                System.out.println("Running as test -> " + config.display());
                break;
        }

        new CountDownLatch(1).await();
    }

}