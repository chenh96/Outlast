package tech.chenh.outlast.data;

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