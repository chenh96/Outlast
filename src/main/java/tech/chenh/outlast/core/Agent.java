package tech.chenh.outlast.core;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.chenh.outlast.Context;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Agent {

    private static final Logger LOG = LoggerFactory.getLogger(Agent.class);

    private final Map<String, Socket> clients = new ConcurrentHashMap<>();

    private final Connector connector;
    private final Context context;

    public Agent(Context context) {
        this.context = context;
        this.connector = new Connector("AGENT", "PROXY", context, this::onProxyConnect);
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
            byte[] buffer = new byte[context.getProperties().getBufferSize()];
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
                Socket newClient = new Socket(context.getProperties().getAgentProxyHost(), context.getProperties().getAgentProxyPort());
                clients.put(message.getChannel(), newClient);
                Thread.startVirtualThread(() -> readClientData(message.getChannel(), newClient));

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