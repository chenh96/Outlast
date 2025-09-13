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
    private Integer proxyPort;

    @Name("agent-proxy-host")
    private String agentProxyHost;

    @Name("agent-proxy-port")
    private Integer agentProxyPort;

    @Name("package-size")
    private Integer packageSize;

    @Name("buffer-size")
    private Integer bufferSize;

    @Name("encryption-key")
    private String encryptionKey;

    public String display() {
        return "mode: " + mode +
            ", proxy-port: " + proxyPort +
            ", agent-proxy-host: " + agentProxyHost +
            ", agent-proxy-port: " + agentProxyPort +
            ", package-size: " + packageSize +
            ", buffer-size: " + bufferSize;
    }
}