package tech.chenh.outlast.tunnel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.chenh.outlast.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TunnelManager {

    private static final Logger LOG = LoggerFactory.getLogger(TunnelManager.class);

    private final ExecutorService writePool = Executors.newSingleThreadExecutor();
    private final ExecutorService readPool = Executors.newSingleThreadExecutor();
    private final ExecutorService listenerPool = Executors.newVirtualThreadPerTaskExecutor();

    private final Map<String, List<String>> sending = new ConcurrentHashMap<>();
    private final Map<String, List<String>> received = new ConcurrentHashMap<>();
    private final Map<String, Consumer<List<String>>> listeners = new ConcurrentHashMap<>();

    private final String source;
    private final String target;
    private final Properties properties;
    private final TunnelRepository repository;
    private final Consumer<String> onConnect;

    public TunnelManager(String source, String target, Properties properties, TunnelRepository repository, Consumer<String> onConnect) {
        this.source = source;
        this.target = target;
        this.properties = properties;
        this.repository = repository;
        this.onConnect = onConnect;
    }

    public void start() {
        writePool.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (MapUtils.isEmpty(sending)) {
                    LockSupport.parkNanos(1_000_000L);
                    continue;
                }
                saveSending();
            }
        });

        readPool.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                readReceived();
            }
        });
    }

    public void send(String channel, String message) {
        sending.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(message);
    }

    public void listen(String channel, Consumer<List<String>> listener) {
        listeners.put(channel, listener);
    }

    public void remove(String channel) {
        listeners.remove(channel);
    }

    private void saveSending() {
        try {
            List<TunnelData> dataList = new ArrayList<>();
            List<String> channels = new ArrayList<>(sending.keySet());

            for (String channel : channels) {
                List<String> messages = new ArrayList<>(sending.remove(channel));
                for (String message : messages) {
                    message = encrypt(message, properties.getEncryptionKey());
                    String batch = UUID.randomUUID().toString();
                    int total = (int) Math.ceil(message.length() * 1.0 / properties.getPackageSize());
                    for (int j = 0; j < total; j++) {
                        String subMessage = message.substring(j * properties.getPackageSize(), Math.min((j + 1) * properties.getPackageSize(), message.length()));
                        dataList.add(
                            new TunnelData()
                                .setSource(source)
                                .setTarget(target)
                                .setChannel(channel)
                                .setBatch(batch)
                                .setSerial(j)
                                .setTotal(total)
                                .setContent(subMessage)
                        );
                    }
                }
            }
            repository.saveAllAndFlush(dataList);
        } catch (Exception e) {
            LOG.debug(ExceptionUtils.getStackTrace(e));
        }
    }

    private void readReceived() {
        try {
            List<TunnelData> dataList = repository.findByTarget(source);
            if (CollectionUtils.isEmpty(dataList)) {
                return;
            }
            List<String> receivedBatches = new ArrayList<>();
            dataList.stream().collect(Collectors.groupingBy(TunnelData::getChannel, LinkedHashMap::new, Collectors.toList())).forEach((channel, thisDataList) -> {
                List<String> channelMessages = new ArrayList<>();
                thisDataList.stream().collect(Collectors.groupingBy(TunnelData::getBatch, LinkedHashMap::new, Collectors.toList())).forEach((batch, subMessages) -> {
                    if (subMessages.size() == subMessages.getFirst().getTotal()) {
                        receivedBatches.add(batch);

                        String message = subMessages.stream().sorted(Comparator.comparingInt(TunnelData::getSerial)).map(TunnelData::getContent).collect(Collectors.joining(""));
                        message = decrypt(message, properties.getEncryptionKey());
                        channelMessages.add(message);
                    }
                });
                if (CollectionUtils.isNotEmpty(channelMessages)) {
                    received.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).addAll(channelMessages);
                }
            });
            if (CollectionUtils.isNotEmpty(receivedBatches)) {
                received.forEach((channel, messages) -> {
                    if (listeners.containsKey(channel)) {
                        List<String> receivedMessages = new ArrayList<>(received.remove(channel));
                        listenerPool.submit(() -> listeners.get(channel).accept(receivedMessages));
                    } else {
                        listenerPool.submit(() -> onConnect.accept(channel));
                    }
                });
                repository.delete(receivedBatches);
            }
        } catch (Exception e) {
            LOG.debug(ExceptionUtils.getStackTrace(e));
        }
    }

    private String encrypt(String plain, String encryptionKey) {
        try {
            SecretKeySpec sk = new SecretKeySpec(encryptionKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, sk);
            byte[] bytes = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return plain;
        }
    }

    private String decrypt(String base64Cipher, String decryptionKey) {
        try {
            SecretKeySpec sk = new SecretKeySpec(decryptionKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, sk);
            byte[] bytes = cipher.doFinal(Base64.getDecoder().decode(base64Cipher));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return base64Cipher;
        }
    }

}
