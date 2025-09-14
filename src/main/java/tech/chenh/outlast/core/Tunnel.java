package tech.chenh.outlast.core;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.chenh.outlast.data.Data;
import tech.chenh.outlast.start.Context;
import tech.chenh.outlast.util.Encryption;
import tech.chenh.outlast.util.Parker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Tunnel {

    private static final Logger LOG = LoggerFactory.getLogger(Tunnel.class);

    private final List<String> listening = new CopyOnWriteArrayList<>();

    private final String source;
    private final String target;
    private final Context context;
    private final Consumer<String> onConnect;

    public Tunnel(String source, String target, Context context, Consumer<String> onConnect) {
        this.source = source;
        this.target = target;
        this.context = context;
        this.onConnect = onConnect;
    }

    public void start() {
        context.getTransaction().execute(status ->
            context.getCrud().deleteByRole(source)
        );

        Thread.startVirtualThread(() -> {
            Parker parker = new Parker(context.getProperties().getParkMaximum(), context.getProperties().getParkMultiplier());
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<String> channels = context.getCrud().findNewChannels(source, new ArrayList<>(listening));
                    parker.increaseIfAbsent(CollectionUtils.isEmpty(channels));
                    for (String channel : channels) {
                        if (listening.contains(channel)) {
                            continue;
                        }
                        Thread.startVirtualThread(() -> {
                            try {
                                onConnect.accept(channel);
                            } catch (Exception e) {
                                LOG.debug(ExceptionUtils.getStackTrace(e));
                            }
                        });
                    }
                } catch (Exception e) {
                    LOG.debug(ExceptionUtils.getStackTrace(e));
                } finally {
                    parker.park();
                }
            }
        });
    }

    public void sendData(String channel, byte[] content) {
        send(channel, Data.Type.DATA, content);
    }

    public void sendClose(String channel) {
        send(channel, Data.Type.CLOSE, new byte[0]);
    }

    private void send(String channel, Data.Type type, byte[] content) {
        List<Data> dataList = splitPacks(channel, type, content);
        context.getTransaction().executeWithoutResult(status ->
            context.getCrud().saveAllAndFlush(dataList)
        );
    }

    private List<Data> splitPacks(String channel, Data.Type type, byte[] content) {
        List<Data> dataList = new ArrayList<>();
        int packSize = context.getProperties().getEncryptableDataSize();
        int total = (int) Math.ceil(content.length * 1.0 / packSize);
        for (int i = 0; i < total; i++) {
            byte[] pack = ArrayUtils.subarray(content, i * packSize, Math.min((i + 1) * packSize, content.length));
            String encrypted = Encryption.encrypt(pack, context.getProperties().getEncryptionKey());
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
        Thread.startVirtualThread(() -> {
            Parker parker = new Parker(context.getProperties().getParkMaximum(), context.getProperties().getParkMultiplier());
            long lastReceived = System.currentTimeMillis();
            while (!Thread.currentThread().isInterrupted() && listening.contains(channel)) {
                try {
                    List<Data> dataList = popReceivable(channel);
                    parker.increaseIfAbsent(CollectionUtils.isEmpty(dataList));
                    for (Data data : dataList) {
                        if (!listening.contains(channel)) {
                            break;
                        }

                        String pack = data.getContent();
                        byte[] decrypted = Encryption.decrypt(pack, context.getProperties().getEncryptionKey());
                        listener.accept(data.getType(), decrypted);

                        lastReceived = System.currentTimeMillis();
                    }
                } catch (Exception e) {
                    LOG.debug(ExceptionUtils.getStackTrace(e));
                } finally {
                    parker.park();
                }
                if (System.currentTimeMillis() - lastReceived > context.getProperties().getIdleTimeout()) {
                    remove(channel);
                }
            }
        });
    }

    private List<Data> popReceivable(String channel) {
        return context.getTransaction().execute(status -> {
            List<Data> dataList = context.getCrud()
                .findReceivableData(source, channel, context.getProperties().getBatchSize());
            if (CollectionUtils.isNotEmpty(dataList) && listening.contains(channel)) {
                context.getCrud().deleteAllInBatch(dataList);
            }
            return dataList;
        });
    }

    public void remove(String channel) {
        listening.remove(channel);
    }

}