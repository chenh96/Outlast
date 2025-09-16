package tech.chenh.outlast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Tunnel {

    private static final Logger LOG = LoggerFactory.getLogger(Tunnel.class);

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
            Repository.getInstance().deleteByRole(source);
        } catch (SQLException e) {
            LOG.debug(e.getMessage(), e);
        }

        Thread.ofPlatform().start(() -> {
            Parker parker = new Parker(Config.getInstance().getParkMaximum(), Config.getInstance().getParkMultiplier());
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Set<String> channels = Repository.getInstance().findNewChannels(source, new ArrayList<>(listening));
                    parker.increaseIfAbsent(channels.isEmpty());
                    for (String channel : channels) {
                        if (listening.contains(channel)) {
                            continue;
                        }
                        Thread.ofPlatform().start(() -> {
                            try {
                                onConnect.accept(channel);
                            } catch (Exception e) {
                                LOG.debug(e.getMessage(), e);
                            }
                        });
                    }
                } catch (Exception e) {
                    LOG.debug(e.getMessage(), e);
                } finally {
                    parker.park();
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
        Repository.getInstance().saveAll(dataList);
    }

    private List<Data> splitPacks(String channel, Data.Type type, byte[] content) {
        List<Data> dataList = new ArrayList<>();
        int packSize = Config.getInstance().getEncryptableDataSize();
        int total = (int) Math.ceil(content.length * 1.0 / packSize);
        for (int i = 0; i < total; i++) {
            byte[] pack = Arrays.copyOfRange(content, i * packSize, Math.min((i + 1) * packSize, content.length));
            String encrypted = Encryption.encrypt(pack, Config.getInstance().getEncryptionKey());
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

        Thread.ofPlatform().start(() -> {
            Parker parker = new Parker(Config.getInstance().getParkMaximum(), Config.getInstance().getParkMultiplier());
            long lastReceived = System.currentTimeMillis();
            while (!Thread.currentThread().isInterrupted() && listening.contains(channel)) {
                try {
                    List<Data> dataList = Repository.getInstance().popReceivable(source, channel, Config.getInstance().getBatchSize());
                    parker.increaseIfAbsent(dataList.isEmpty());
                    for (Data data : dataList) {
                        if (!listening.contains(channel)) {
                            break;
                        }

                        String pack = data.getContent();
                        byte[] decrypted = Encryption.decrypt(pack, Config.getInstance().getEncryptionKey());
                        listener.accept(data.getType(), decrypted);

                        lastReceived = System.currentTimeMillis();
                    }
                } catch (Exception e) {
                    LOG.debug(e.getMessage(), e);
                } finally {
                    parker.park();
                }
                if (System.currentTimeMillis() - lastReceived > Config.getInstance().getIdleTimeout()) {
                    remove(channel);
                }
            }
        });
    }

    public void remove(String channel) {
        listening.remove(channel);
    }

}