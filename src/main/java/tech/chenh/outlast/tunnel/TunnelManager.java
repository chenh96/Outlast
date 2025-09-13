package tech.chenh.outlast.tunnel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.chenh.outlast.Context;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TunnelManager {

    private static final Logger LOG = LoggerFactory.getLogger(TunnelManager.class);

    private final List<String> listeningChannels = new CopyOnWriteArrayList<>();

    private final String source;
    private final String target;
    private final Context context;
    private final Consumer<String> onConnect;

    public TunnelManager(String source, String target, Context context, Consumer<String> onConnect) {
        this.source = source;
        this.target = target;
        this.context = context;
        this.onConnect = onConnect;
    }

    public void start() {
        Thread.startVirtualThread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<String> newChannels = context.getRepository().findNewChannels(listeningChannels)
                        .stream().filter(newChannel -> !listeningChannels.contains(newChannel)).toList();
                    for (String newChannel : newChannels) {
                        Thread.startVirtualThread(() -> {
                            try {
                                onConnect.accept(newChannel);
                            } catch (Exception e) {
                                LOG.debug(ExceptionUtils.getStackTrace(e));
                            }
                        });
                    }
                } catch (Exception e) {
                    LOG.debug(ExceptionUtils.getStackTrace(e));
                } finally {
                    LockSupport.parkNanos(1000);
                }
            }
        });
    }

    public void send(String channel, String message) {
        List<TunnelData> sendingDataList = new ArrayList<>();
        message = encrypt(message, context.getProperties().getEncryptionKey());
        String batch = UUID.randomUUID().toString();
        int total = (int) Math.ceil(message.length() * 1.0 / context.getProperties().getPackageSize());
        for (int serial = 0; serial < total; serial++) {
            sendingDataList.add(
                new TunnelData()
                    .setSource(source)
                    .setTarget(target)
                    .setChannel(channel)
                    .setBatch(batch)
                    .setSerial(serial)
                    .setTotal(total)
                    .setContent(
                        message.substring(
                            serial * context.getProperties().getPackageSize(),
                            Math.min((serial + 1) * context.getProperties().getPackageSize(), message.length())
                        )
                    )
            );
        }
        context.getRepository().saveAll(sendingDataList);
    }

    public void listen(String channel, Consumer<String> listener) {
        if (listeningChannels.contains(channel)) {
            return;
        }
        listeningChannels.add(channel);
        Thread.startVirtualThread(() -> {
            while (!Thread.currentThread().isInterrupted() && listeningChannels.contains(channel)) {
                try {
                    List<TunnelData> receivedDataList = context.getRepository()
                        .findReceivableData(source, channel, context.getProperties().getPollSize());
                    if (CollectionUtils.isEmpty(receivedDataList)) {
                        continue;
                    }
                    context.getRepository().deleteAll(receivedDataList);

                    Map<String, List<TunnelData>> dataListByBatch = receivedDataList.stream()
                        .collect(Collectors.groupingBy(TunnelData::getBatch, LinkedHashMap::new, Collectors.toList()));
                    for (Map.Entry<String, List<TunnelData>> dataListByBatchEntry : dataListByBatch.entrySet()) {
                        List<TunnelData> dataListOfBatch = dataListByBatchEntry.getValue();
                        String encryptedContent = dataListOfBatch.stream()
                            .sorted(Comparator.comparingInt(TunnelData::getSerial))
                            .map(TunnelData::getContent)
                            .collect(Collectors.joining(""));
                        String decryptedContent = decrypt(encryptedContent, context.getProperties().getEncryptionKey());

                        listener.accept(decryptedContent);
                    }
                } catch (Exception e) {
                    LOG.debug(ExceptionUtils.getStackTrace(e));
                } finally {
                    LockSupport.parkNanos(1000);
                }
            }
        });
    }

    public void remove(String channel) {
        listeningChannels.remove(channel);
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
