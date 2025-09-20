package tech.chenh.outlast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Agent {

    private static final Logger LOG = LoggerFactory.getLogger(Agent.class);

    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private final Map<String, Socket> clients = new ConcurrentHashMap<>();

    private final Tunnel tunnel;

    public Agent() {
        this.tunnel = new Tunnel("AGENT", "PROXY", this::onProxyConnect);
    }

    public void start() {
        tunnel.start();
    }

    private void onProxyConnect(String channel) {
        tunnel.listen(channel, (type, data) -> {
            switch (type) {
                case DATA:
                    onProxyData(channel, data);
                    break;
                case CLOSE:
                    onProxyClose(channel);
                    break;
            }
        });
    }

    private void readClientData(String channel, Socket client) {
        try (InputStream input = client.getInputStream()) {
            byte[] buffer = new byte[Config.instance().getSocketBufferSize()];
            int bytesRead;
            while (!Thread.currentThread().isInterrupted() && (bytesRead = input.read(buffer)) != -1) {
                tunnel.sendData(channel, Arrays.copyOf(buffer, bytesRead));
            }
        } catch (Exception e) {
            LOG.debug(e.getMessage(), e);
        } finally {
            sendProxyClose(channel);
        }
    }

    private void onProxyData(String channel, byte[] data) {
        try {
            Socket client = clients.get(channel);
            if (client == null) {
                Socket newClient = new Socket(Config.instance().getAgentProxyHost(), Config.instance().getAgentProxyPort());
                clients.put(channel, newClient);
                clientPool.submit(() -> readClientData(channel, newClient));

                client = newClient;
            }

            OutputStream output = client.getOutputStream();
            output.write(data);
            output.flush();
        } catch (Exception e) {
            LOG.debug(e.getMessage(), e);
            sendProxyClose(channel);
        }
    }

    private void onProxyClose(String channel) {
        closeSocket(clients.remove(channel));
        tunnel.remove(channel);
    }

    private void sendProxyClose(String channel) {
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