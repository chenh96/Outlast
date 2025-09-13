package tech.chenh.outlast.core;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.chenh.outlast.Properties;
import tech.chenh.outlast.tunnel.TunnelRepository;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Agent {

    private static final Logger LOG = LoggerFactory.getLogger(Agent.class);

    private final ExecutorService clientPool = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, Socket> clients = new ConcurrentHashMap<>();

    private final Connector connector;
    private final Properties properties;

    public Agent(Properties properties, TunnelRepository repository) {
        this.connector = new Connector("AGENT", "PROXY", properties, repository, this::onProxyConnect);
        this.properties = properties;
    }

    public void start() {
        connector.start();
    }

    private void onProxyConnect(String channel) {
        connector.listen(channel, message -> {
            switch (message.getType()) {
                case DATA:
                    onProxyData(message);
                    break;
                case CLOSE:
                    onProxyClose(message);
                    break;
            }
        });
    }

    private void readClientData(String channel, Socket client) {
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
            sendProxyClose(channel);
        }
    }

    private void onProxyData(Message message) {
        try {
            Socket client = clients.get(message.getChannel());
            if (client == null) {
                Socket newClient = new Socket(properties.getAgentProxyHost(), properties.getAgentProxyPort());
                clients.put(message.getChannel(), newClient);
                clientPool.submit(() -> readClientData(message.getChannel(), newClient));

                client = newClient;
            }

            byte[] content = Base64.getDecoder().decode(message.getContent());
            OutputStream output = client.getOutputStream();
            output.write(content);
            output.flush();
        } catch (Exception e) {
            LOG.debug(ExceptionUtils.getStackTrace(e));
            sendProxyClose(message.getChannel());
        }
    }

    private void onProxyClose(Message message) {
        closeSocket(clients.remove(message.getChannel()));
        connector.remove(message.getChannel());
    }

    private void sendProxyClose(String channel) {
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