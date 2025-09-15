package tech.chenh.outlast.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.NonNull;
import tech.chenh.outlast.start.Config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Repository {

    private static Repository INSTANCE;

    private final HikariDataSource dataSource;

    private Repository(Config properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getDatasourceUrl());
        config.setUsername(properties.getDatasourceUsername());
        config.setPassword(properties.getDatasourcePassword());
        config.setDriverClassName(properties.getDatasourceDriverClassName());
        config.setMaximumPoolSize(properties.getDatasourceMaximumPoolSize());
        config.setMinimumIdle(properties.getDatasourceMinimumIdle());

        dataSource = new HikariDataSource(config);
    }

    public static synchronized Repository getInstance(Config properties) {
        if (INSTANCE == null) {
            INSTANCE = new Repository(properties);
        }
        return INSTANCE;
    }

    public void saveAll(@NonNull List<Data> dataList) throws SQLException {
        if (dataList.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO OUTLAST_DATA (ID, SOURCE, TARGET, CHANNEL, TYPE, CONTENT)
            VALUES (SEQ_OUTLAST_DATA_ID.NEXTVAL, ?, ?, ?, ?, ?)
            """;
        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            for (Data data : dataList) {
                statement.setString(1, data.getSource());
                statement.setString(2, data.getTarget());
                statement.setString(3, data.getChannel());
                statement.setString(4, data.getType().name());
                statement.setString(5, data.getContent());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public @NonNull List<Data> popReceivable(@NonNull String target, @NonNull String channel, int limit) throws SQLException {
        String querySql = """
            SELECT ID, SOURCE, TARGET, CHANNEL, TYPE, CONTENT
            FROM OUTLAST_DATA
            WHERE TARGET = ? AND CHANNEL = ?
            AND (TYPE != 'DATA' OR CONTENT IS NOT NULL)
            ORDER BY ID ASC
            LIMIT ?
            """;
        List<Data> dataList = new ArrayList<>();
        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(querySql)
        ) {
            statement.setString(1, target);
            statement.setString(2, channel);
            statement.setInt(3, limit);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Data data = new Data()
                        .setId(result.getLong("ID"))
                        .setSource(result.getString("SOURCE"))
                        .setTarget(result.getString("TARGET"))
                        .setChannel(result.getString("CHANNEL"))
                        .setType(Data.Type.valueOf(result.getString("TYPE")))
                        .setContent(result.getString("CONTENT"));
                    dataList.add(data);
                }
            }
        }
        if (dataList.isEmpty()) {
            return new ArrayList<>();
        }

        String deleteSql = """
            DELETE FROM OUTLAST_DATA WHERE ID = ?
            """;
        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(deleteSql)
        ) {
            for (Data data : dataList) {
                statement.setLong(1, data.getId());
                statement.addBatch();
            }
            statement.executeBatch();
        }

        return dataList;
    }

    public @NonNull Set<String> findNewChannels(@NonNull String target, @NonNull List<String> existedChannels) throws SQLException {
        String sql = """
            SELECT DISTINCT CHANNEL
            FROM OUTLAST_DATA
            WHERE TARGET = ?
            """;
        if (!existedChannels.isEmpty()) {
            sql += " AND CHANNEL NOT IN (" + String.join(", ", "?".repeat(existedChannels.size())) + ")";
        }
        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, target);
            if (!existedChannels.isEmpty()) {
                for (int i = 0; i < existedChannels.size(); i++) {
                    statement.setString(i + 2, existedChannels.get(i));
                }
            }

            Set<String> newChannels = new LinkedHashSet<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    newChannels.add(result.getString("CHANNEL"));
                }
            }
            return newChannels;
        }
    }

    public void deleteByRole(@NonNull String role) throws SQLException {
        String sql = """
            DELETE FROM OUTLAST_DATA WHERE SOURCE = ? OR TARGET = ?
            """;
        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, role);
            statement.setString(2, role);
            statement.executeUpdate();
        }
    }

}