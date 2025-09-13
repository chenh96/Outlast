package tech.chenh.outlast.core;

import com.alibaba.fastjson2.JSON;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.chenh.outlast.Context;
import tech.chenh.outlast.tunnel.TunnelManager;

import java.util.function.Consumer;

public class Connector {

    private static final Logger LOG = LoggerFactory.getLogger(Connector.class);

    private final TunnelManager connector;

    public Connector(String source, String target, Context context, Consumer<String> onConnect) {
        this.connector = new TunnelManager(source, target, context, onConnect);
    }

    public void start() {
        connector.start();
    }

    public void send(Message message) {
        connector.send(message.getChannel(), JSON.toJSONString(message));
    }

    public void listen(String channel, Consumer<Message> consumer) {
        connector.listen(channel, message -> {
            try {
                consumer.accept(JSON.parseObject(message, Message.class));
            } catch (Exception e) {
                LOG.debug(ExceptionUtils.getStackTrace(e));
            }
        });
    }

    public void remove(String channel) {
        connector.remove(channel);
    }

}
