package com.example.moler.repository;

import com.example.moler.model.projektidetajet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ProjektiDetajetRepository
        extends JpaRepository<projektidetajet, Long> {

    Optional<projektidetajet> findByProjektiId(Long projektiId);

    boolean existsByProjektiId(Long projektiId);

    @Query("""
        SELECT COALESCE(SUM(d.pagesa), 0)
        FROM projektidetajet d
        WHERE d.projekti.statusi = :statusi
        """)
    BigDecimal sumPagesaByProjektiStatusi(@Param("statusi") String statusi);

    // Sum of pagesa for projects with the given status completed in [nga, deri)
    @Query("""
        SELECT COALESCE(SUM(d.pagesa), 0)
        FROM projektidetajet d
        WHERE d.projekti.statusi = :statusi
          AND d.ditaPerfundimit >= :nga
          AND d.ditaPerfundimit < :deri
        """)
    BigDecimal sumPagesaByProjektiStatusiAndPeriudha(
            @Param("statusi") String statusi,
            @Param("nga") LocalDateTime nga,
            @Param("deri") LocalDateTime deri
    );

}
