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
    private final String datasourceDriverClass;
    private final int datasourceMaxPoolSize;
    private final int datasourceIdleTimeout;

    private final String mode;
    private final int proxyPort;
    private final String agentProxyHost;
    private final int agentProxyPort;
    private final int dataSize;
    private final int readBatchSize;
    private final int writeBatchSize;
    private final int pollingInterval;
    private final int idleTimeout;
    private final String encryptionKey;
    private final String dataTable;

    public int getEncryptableDataSize() {
        return dataSize / 4 * 3 - 1;
    }

    public int getSocketBufferSize() {
        return getEncryptableDataSize() * writeBatchSize;
    }

    private Config() throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties");

        Properties props = new Properties();
        props.load(input);

        this.datasourceUrl = props.getProperty("datasource.url");
        this.datasourceUsername = props.getProperty("datasource.username");
        this.datasourcePassword = props.getProperty("datasource.password");
        this.datasourceDriverClass = props.getProperty("datasource.driver-class");
        this.datasourceMaxPoolSize = Integer.parseInt(props.getProperty("datasource.max-pool-size"));
        this.datasourceIdleTimeout = Integer.parseInt(props.getProperty("datasource.idle-timeout"));

        this.mode = props.getProperty("outlast.mode");
        this.proxyPort = Integer.parseInt(props.getProperty("outlast.proxy-port"));
        this.agentProxyHost = props.getProperty("outlast.agent-proxy-host");
        this.agentProxyPort = Integer.parseInt(props.getProperty("outlast.agent-proxy-port"));
        this.dataSize = Integer.parseInt(props.getProperty("outlast.data-size"));
        this.readBatchSize = Integer.parseInt(props.getProperty("outlast.read-batch-size"));
        this.writeBatchSize = Integer.parseInt(props.getProperty("outlast.write-batch-size"));
        this.pollingInterval = Integer.parseInt(props.getProperty("outlast.polling-interval"));
        this.idleTimeout = Integer.parseInt(props.getProperty("outlast.idle-timeout"));
        this.encryptionKey = props.getProperty("outlast.encryption-key");
        this.dataTable = props.getProperty("outlast.data-table");

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

}