package tech.chenh.outlast.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Message {

    private String channel;
    private Type type;
    private String content;

    public static Message data(String channel, String content) {
        return new Message(channel, Type.DATA, content);
    }

    public static Message close(String channel) {
        return new Message(channel, Type.CLOSE, "");
    }

    public enum Type {
        DATA,
        CLOSE
    }

}
