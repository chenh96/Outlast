package tech.chenh.outlast.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.transaction.support.TransactionTemplate;
import tech.chenh.outlast.Properties;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Context {

    private Properties properties;

    private Crud crud;

    private TransactionTemplate transaction;

}