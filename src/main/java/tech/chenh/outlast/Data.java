package tech.chenh.outlast;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class Data {

    private long id;
    private String source;
    private String target;
    private String channel;
    private Type type;
    private String content;

    public enum Type {
        DATA,
        CLOSE,
    }

}