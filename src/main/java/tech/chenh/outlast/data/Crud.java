package tech.chenh.outlast.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Crud extends JpaRepository<Data, Long> {

    @Query("""
        FROM Data
        WHERE target = :target
        AND channel = :channel
        AND (type != tech.chenh.outlast.data.Data.Type.DATA OR content IS NOT NULL)
        ORDER BY id ASC
        LIMIT :limit
        """)
    List<Data> findReceivableData(
        @Param("target") String target,
        @Param("channel") String channel,
        @Param("limit") int limit
    );

    @Query("""
        SELECT DISTINCT channel
        FROM Data
        WHERE target = :target
        AND channel NOT IN :existedChannels
        """)
    List<String> findNewChannels(@Param("target") String target, @Param("existedChannels") Iterable<String> existedChannels);

    @Modifying
    @Query("""
        DELETE FROM Data
        WHERE source = :role
        OR target = :role
        """)
    int deleteByRole(@Param("role") String role);

}