
package com.example.moler.repository;

import com.example.moler.model.ProjektiPrezencaPuntoret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProjektiPrezencaPuntoretRepository
        extends JpaRepository<ProjektiPrezencaPuntoret, Long> {

    List<ProjektiPrezencaPuntoret> findByProjekti_Id(Long idProjekti);

    List<ProjektiPrezencaPuntoret> findByPuntori_Id(Long idPuntorit);

    boolean existsByProjekti_IdAndPuntori_Id(
            Long idProjekti,
            Long idPuntorit
    );

      List<ProjektiPrezencaPuntoret>
    findAllByPuntori_IdAndProjekti_StatusiNot(
            Long idPuntorit,
            String statusi
    );
    
    void deleteByProjekti_Id(Long idProjekti);
}
