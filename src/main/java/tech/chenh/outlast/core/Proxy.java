package tech.chenh.outlast.core;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.chenh.outlast.Properties;
import tech.chenh.outlast.tunnel.TunnelRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Proxy {

    private static final Logger LOG = LoggerFactory.getLogger(Proxy.class);

    private final ExecutorService serverPool = Executors.newSingleThreadExecutor();
    private final ExecutorService clientPool = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, Socket> clients = new ConcurrentHashMap<>();

    private final Connector connector;
    private final Properties properties;

    private ServerSocket server;

    public Proxy(Properties properties, TunnelRepository repository) {
        this.connector = new Connector("PROXY", "AGENT", properties, repository, this::onAgentConnect);
        this.properties = properties;
    }

    public void start() throws IOException {
        connector.start();

        server = new ServerSocket(properties.getProxyPort());
        serverPool.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket client = server.accept();
                    clientPool.submit(() -> readClientData(client));
                } catch (Exception e) {
                    LOG.debug(ExceptionUtils.getStackTrace(e));
                }
            }
        });
    }

    private void readClientData(Socket client) {
        String channel = UUID.randomUUID().toString();
        clients.put(channel, client);

        try {
            InputStream input = client.getInputStream();
            byte[] buffer = new byte[properties.getBufferSize()];
            int bytesRead;
            while (!Thread.currentThread().isInterrupted() && (bytesRead = input.read(buffer)) != -1) {
                String content = Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, bytesRead));
                connector.send(Message.data(channel, content));
            }
        } catch (Exception e) {
            LOG.debug(ExceptionUtils.getStackTrace(e));
            sendAgentClose(channel);
        }
    }

    private void onAgentConnect(String channel) {
        connector.listen(channel, message -> {
            switch (message.getType()) {
                case DATA:
                    onAgentData(message);
                    break;
                case CLOSE:
                    onAgentClose(message);
                    break;
            }
        });
    }

    private void onAgentData(Message message) {
        Socket client = clients.get(message.getChannel());
        if (client == null) {
            sendAgentClose(message.getChannel());
            return;
        }
        try {
            byte[] content = Base64.getDecoder().decode(message.getContent());
            OutputStream output = client.getOutputStream();
            output.write(content);
            output.flush();
        } catch (Exception e) {
            LOG.debug(ExceptionUtils.getStackTrace(e));
            sendAgentClose(message.getChannel());
        }
    }

    private void onAgentClose(Message message) {
        closeSocket(clients.remove(message.getChannel()));
        connector.remove(message.getChannel());
    }

    private void sendAgentClose(String channel) {
        closeSocket(clients.remove(channel));
        connector.send(Message.close(channel));
        connector.remove(channel);
    }

    private void closeSocket(Socket socket) {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception ignored) {
        }
    }

}