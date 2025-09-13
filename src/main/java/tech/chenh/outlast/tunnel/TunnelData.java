package tech.chenh.outlast.tunnel;

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
@Table(name = "OUTLAST_TUNNEL_DATA")
@Entity
public class TunnelData {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OUTLAST_TUNNEL_DATA_ID")
    @SequenceGenerator(name = "SEQ_OUTLAST_TUNNEL_DATA_ID", allocationSize = 1024)
    @Column(name = "ID")
    private long id;

    @Column(name = "SOURCE")
    private String source;

    @Column(name = "TARGET")
    private String target;

    @Column(name = "CHANNEL")
    private String channel;

    @Column(name = "BATCH")
    private String batch;

    @Column(name = "SERIAL")
    private int serial;

    @Column(name = "TOTAL")
    private int total;

    @Column(name = "CONTENT")
    private String content;

}
