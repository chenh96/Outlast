package tech.chenh.outlast.start;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Getter
@Setter
@Accessors(chain = true)
public class Config {

    private String mode;
    private int proxyPort;
    private String agentProxyHost;
    private int agentProxyPort;
    private int dataSize;
    private int batchSize;
    private int idleTimeout;
    private int parkMaximum;
    private double parkMultiplier;
    private String encryptionKey;

    // 数据库配置
    private String datasourceUrl;
    private String datasourceUsername;
    private String datasourcePassword;
    private String datasourceDriverClassName;
    private int datasourceMaximumPoolSize;
    private int datasourceMinimumIdle;

    public Config() throws IOException {
        load();
    }

    private void load() throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties");

        Properties props = new Properties();
        props.load(input);

        // 加载outlast配置
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

        // 加载数据库配置
        this.datasourceUrl = props.getProperty("datasource.url");
        this.datasourceUsername = props.getProperty("datasource.username");
        this.datasourcePassword = props.getProperty("datasource.password");
        this.datasourceDriverClassName = props.getProperty("datasource.driver-class-name");
        this.datasourceMaximumPoolSize = Integer.parseInt(props.getProperty("datasource.hikari.maximum-pool-size"));
        this.datasourceMinimumIdle = Integer.parseInt(props.getProperty("datasource.hikari.minimum-idle"));
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