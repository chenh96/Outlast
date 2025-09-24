package tech.chenh.outlast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Tunnel {

    private static final Logger LOG = LoggerFactory.getLogger(Tunnel.class);

    private final ExecutorService pollingPool = Executors.newCachedThreadPool();
    private final List<String> listening = new CopyOnWriteArrayList<>();

    private final String source;
    private final String target;
    private final Consumer<String> onConnect;

    public Tunnel(String source, String target, Consumer<String> onConnect) {
        this.source = source;
        this.target = target;
        this.onConnect = onConnect;
    }

    public void start() {
        try {
            Repository.instance().deleteByRole(source);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }

        Thread.ofPlatform().start(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Set<String> channels = Repository.instance().findNewChannels(source, new ArrayList<>(listening));
                    for (String channel : channels) {
                        if (listening.contains(channel)) {
                            continue;
                        }
                        pollingPool.submit(() -> {
                            try {
                                onConnect.accept(channel);
                            } catch (Exception e) {
                                LOG.error(e.getMessage(), e);
                            }
                        });
                    }
                    LockSupport.parkNanos(Config.instance().getPollingInterval() * 1000_000L);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                    return;
                }
            }
        });
    }

    public void sendData(String channel, byte[] content) throws Exception {
        send(channel, Data.Type.DATA, content);
    }

    public void sendClose(String channel) throws Exception {
        send(channel, Data.Type.CLOSE, new byte[0]);
    }

    private void send(String channel, Data.Type type, byte[] content) throws Exception {
        List<Data> dataList = splitPacks(channel, type, content);
        Repository.instance().saveAll(dataList);
    }

    private List<Data> splitPacks(String channel, Data.Type type, byte[] content) {
        List<Data> dataList = new ArrayList<>();
        int packSize = Config.instance().getEncryptableDataSize();
        int total = (int) Math.ceil(content.length * 1.0 / packSize);
        for (int i = 0; i < total; i++) {
            byte[] pack = Arrays.copyOfRange(content, i * packSize, Math.min((i + 1) * packSize, content.length));
            String encrypted = Encryption.encrypt(pack, Config.instance().getEncryptionKey());
            dataList.add(
                new Data()
                    .setSource(source)
                    .setTarget(target)
                    .setChannel(channel)
                    .setType(type)
                    .setContent(encrypted)
            );
        }
        return dataList;
    }

    public synchronized void listen(String channel, BiConsumer<Data.Type, byte[]> listener) {
        if (listening.contains(channel)) {
            return;
        }
        listening.add(channel);

        pollingPool.submit(() -> {
            long lastReceivedAt = System.currentTimeMillis();
            while (!Thread.currentThread().isInterrupted() && listening.contains(channel)) {
                try {
                    List<Data> dataList = Repository.instance().popReceivable(source, channel, Config.instance().getReadBatchSize());
                    for (Data data : dataList) {
                        if (!listening.contains(channel)) {
                            break;
                        }

                        String pack = data.getContent();
                        byte[] decrypted = Encryption.decrypt(pack, Config.instance().getEncryptionKey());
                        listener.accept(data.getType(), decrypted);

                        lastReceivedAt = System.currentTimeMillis();
                    }
                    LockSupport.parkNanos(Config.instance().getPollingInterval() * 1000_000L);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                    remove(channel);
                    return;
                }
                if (System.currentTimeMillis() - lastReceivedAt > Config.instance().getIdleTimeout()) {
                    remove(channel);
                }
            }
        });
    }

    public void remove(String channel) {
        listening.remove(channel);
    }

}