package tech.chenh.outlast;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

@Getter
@Setter
@ConfigurationProperties(prefix = "outlast")
public class Properties {

    @Name("mode")
    private String mode;

    @Name("proxy-port")
    private int proxyPort;

    @Name("agent-proxy-host")
    private String agentProxyHost;

    @Name("agent-proxy-port")
    private int agentProxyPort;

    @Name("data-size")
    private int dataSize;

    @Name("batch-size")
    private int batchSize;

    @Name("encryption-key")
    private String encryptionKey;

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
            ", batch-size: " + batchSize;
    }
}