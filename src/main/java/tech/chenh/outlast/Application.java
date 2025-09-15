package tech.chenh.outlast;

import java.util.concurrent.CountDownLatch;

public class Application {

    public static void main(String[] args) throws Exception {
        Config config = Config.getInstance();
        switch (config.getMode()) {
            case "proxy":
                new Proxy().start();
                System.out.println("Running as proxy -> " + config.display());
                break;
            case "agent":
                new Agent().start();
                System.out.println("Running as agent -> " + config.display());
                break;
            case "test":
                new Proxy().start();
                new Agent().start();
                System.out.println("Running as test -> " + config.display());
                break;
        }

        new CountDownLatch(1).await();
    }

}