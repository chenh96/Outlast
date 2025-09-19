package tech.chenh.outlast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Proxy {

    private static final Logger LOG = LoggerFactory.getLogger(Proxy.class);

    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private final Map<String, Socket> clients = new ConcurrentHashMap<>();

    private final Tunnel tunnel;

    private ServerSocket server;

    public Proxy() {
        this.tunnel = new Tunnel("PROXY", "AGENT", this::onAgentConnect);
    }

    public void start() throws IOException {
        tunnel.start();

        server = new ServerSocket(Config.instance().getProxyPort());
        Thread.ofPlatform().start(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket client = server.accept();
                    clientPool.submit(() -> readClientData(client));
                } catch (Exception e) {
                    LOG.debug(e.getMessage(), e);
                    break;
                }
            }
        });
    }

    private void readClientData(Socket client) {
        String channel = UUID.randomUUID().toString();
        clients.put(channel, client);

        try {
            InputStream input = client.getInputStream();
            byte[] buffer = new byte[Config.instance().getSocketBufferSize()];
            int bytesRead;
            while (!Thread.currentThread().isInterrupted() && (bytesRead = input.read(buffer)) != -1) {
                tunnel.sendData(channel, Arrays.copyOf(buffer, bytesRead));
            }
        } catch (Exception e) {
            LOG.debug(e.getMessage(), e);
            sendAgentClose(channel);
        }
    }

    private void onAgentConnect(String channel) {
        tunnel.listen(channel, (type, data) -> {
            switch (type) {
                case DATA:
                    onAgentData(channel, data);
                    break;
                case CLOSE:
                    onAgentClose(channel);
                    break;
            }
        });
    }

    private void onAgentData(String channel, byte[] data) {
        Socket client = clients.get(channel);
        if (client == null) {
            sendAgentClose(channel);
            return;
        }
        try {
            OutputStream output = client.getOutputStream();
            output.write(data);
            output.flush();
        } catch (Exception e) {
            LOG.debug(e.getMessage(), e);
            sendAgentClose(channel);
        }
    }

    private void onAgentClose(String channel) {
        closeSocket(clients.remove(channel));
        tunnel.remove(channel);
    }

    private void sendAgentClose(String channel) {
        closeSocket(clients.remove(channel));
        try {
            tunnel.sendClose(channel);
        } catch (Exception e) {
            LOG.debug(e.getMessage(), e);
        }
        tunnel.remove(channel);
    }

    private void closeSocket(Socket socket) {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            LOG.debug(e.getMessage(), e);
        }
    }

}