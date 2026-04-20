package com.fps.svmes.repositories.jpaRepo.alert;

import com.fps.svmes.models.sql.alert.AlertProduct;
import com.fps.svmes.models.sql.alert.AlertProductId;
import com.fps.svmes.repositories.projection.alert.IdCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlertProductRepository extends JpaRepository<AlertProduct, AlertProductId> {

    @Query(value = """
        select ap.product_id as id, count(*) as cnt
        from quality_management.qc_alert_product ap
        join quality_management.qc_alert_record ar on ar.id = ap.alert_id
        where ar.status = :status and ap.product_id is not null
        group by ap.product_id
        """, nativeQuery = true)
    List<IdCountProjection> countByProduct(@Param("status") int status);
}
