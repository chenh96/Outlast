package tech.chenh.outlast.core;

import jakarta.persistence.*;
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
@Table(name = "OUTLAST_DATA")
@Entity
public class Data {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OUTLAST_DATA_ID")
    @SequenceGenerator(name = "SEQ_OUTLAST_DATA_ID", allocationSize = 1024)
    @Column(name = "ID")
    private long id;

    @Column(name = "SOURCE")
    private String source;

    @Column(name = "TARGET")
    private String target;

    @Column(name = "CHANNEL")
    private String channel;

    @Column(name = "TYPE")
    private Type type;

    @Column(name = "CONTENT")
    private String content;

    public enum Type {

        DATA,
        CLOSE,

    }

}