package tech.chenh.outlast;

import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Getter
public class Config {

    private static Config INSTANCE;

    private final String datasourceUrl;
    private final String datasourceUsername;
    private final String datasourcePassword;
    private final String datasourceDriverClassName;
    private final int datasourceMaximumPoolSize;
    private final int datasourceMinimumIdle;

    private final String mode;
    private final int proxyPort;
    private final String agentProxyHost;
    private final int agentProxyPort;
    private final int dataSize;
    private final int batchSize;
    private final int idleTimeout;
    private final int parkMaximum;
    private final double parkMultiplier;
    private final String encryptionKey;

    private Config() throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties");

        Properties props = new Properties();
        props.load(input);

        this.datasourceUrl = props.getProperty("datasource.url");
        this.datasourceUsername = props.getProperty("datasource.username");
        this.datasourcePassword = props.getProperty("datasource.password");
        this.datasourceDriverClassName = props.getProperty("datasource.driver-class-name");
        this.datasourceMaximumPoolSize = Integer.parseInt(props.getProperty("datasource.hikari.maximum-pool-size"));
        this.datasourceMinimumIdle = Integer.parseInt(props.getProperty("datasource.hikari.minimum-idle"));

        this.mode = props.getProperty("outlast.mode");
        this.proxyPort = Integer.parseInt(props.getProperty("outlast.proxy-port"));
        this.agentProxyHost = props.getProperty("outlast.agent-proxy-host");
        this.agentProxyPort = Integer.parseInt(props.getProperty("outlast.agent-proxy-port"));
        this.dataSize = Integer.parseInt(props.getProperty("outlast.data-size"));
        this.batchSize = Integer.parseInt(props.getProperty("outlast.batch-size"));
        this.idleTimeout = Integer.parseInt(props.getProperty("outlast.idle-timeout"));
        this.parkMaximum = Integer.parseInt(props.getProperty("outlast.park-maximum"));
        this.parkMultiplier = Double.parseDouble(props.getProperty("outlast.park-multiplier"));
        this.encryptionKey = props.getProperty("outlast.encryption-key");

        INSTANCE = this;
    }

    public static synchronized Config instance() {
        if (INSTANCE == null) {
            try {
                INSTANCE = new Config();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return INSTANCE;
    }

    public int getEncryptableDataSize() {
        return dataSize / 4 * 3 - 1;
    }

    public int getSocketBufferSize() {
        return getEncryptableDataSize() * batchSize;
    }

    public String display() {
        return "mode: " + mode +
            ", proxy-port: " + proxyPort +
            ", agent-proxy-host: " + agentProxyHost +
            ", agent-proxy-port: " + agentProxyPort +
            ", data-size: " + dataSize +
            ", batch-size: " + batchSize +
            ", idle-timeout: " + idleTimeout +
            ", park-max: " + parkMaximum +
            ", park-increment: " + parkMultiplier;
    }

}