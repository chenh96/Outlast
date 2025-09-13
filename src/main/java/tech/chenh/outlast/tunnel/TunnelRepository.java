package tech.chenh.outlast.tunnel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface TunnelRepository extends JpaRepository<TunnelData, Long> {

    @Query("FROM TunnelData WHERE target = :target AND channel IS NOT NULL AND batch IS NOT NULL AND serial IS NOT NULL AND total IS NOT NULL AND content IS NOT NULL")
    List<TunnelData> findByTarget(@Param("target") String target);

    @Transactional
    @Modifying
    @Query("DELETE FROM TunnelData WHERE batch IN :batches")
    void deleteByBatch(@Param("batches") Collection<String> batches);

}
