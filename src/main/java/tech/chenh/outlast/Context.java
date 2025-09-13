package tech.chenh.outlast;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import tech.chenh.outlast.tunnel.TunnelRepository;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Context {

    private Properties properties;

    private TunnelRepository repository;

}
