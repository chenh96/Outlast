package tech.chenh.outlast.tunnel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TunnelRepository extends JpaRepository<TunnelData, Long> {

    @Query("""
        FROM TunnelData
        WHERE batch IN (
            SELECT batch
            FROM TunnelData
            WHERE target = :target
            AND channel = :channel
            AND batch IS NOT NULL
            AND serial IS NOT NULL
            AND total IS NOT NULL
            AND content IS NOT NULL
            GROUP BY batch
            HAVING COUNT(serial) = MAX(total)
        )
        ORDER BY id ASC LIMIT :limit
        """)
    List<TunnelData> findReceivableData(
        @Param("target") String target,
        @Param("channel") String channel,
        @Param("limit") int limit
    );

    @Query("""
        SELECT DISTINCT channel
        FROM TunnelData
        WHERE channel NOT IN :existedChannels
        """)
    List<String> findNewChannels(@Param("existedChannels") Collection<String> existedChannels);

}