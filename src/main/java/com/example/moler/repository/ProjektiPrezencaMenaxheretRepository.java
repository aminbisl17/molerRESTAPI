
package com.example.moler.repository;

import com.example.moler.model.ProjektiPrezencaMenaxheret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProjektiPrezencaMenaxheretRepository
        extends JpaRepository<ProjektiPrezencaMenaxheret, Long> {

    List<ProjektiPrezencaMenaxheret> findByProjekti_Id(Long idProjekti);

    List<ProjektiPrezencaMenaxheret> findByMenaxheri_Id(Long idMenaxherit);

    // Distinct managers responsible for any of the given projects
    @Query("""
        SELECT DISTINCT m.menaxheri.id
        FROM ProjektiPrezencaMenaxheret m
        WHERE m.projekti.id IN :idProjektet
    """)
    List<Long> findMenaxheriIdsByProjektiIds(
            @Param("idProjektet") Collection<Long> idProjektet
    );

    boolean existsByProjekti_IdAndMenaxheri_Id(
            Long idProjekti,
            Long idMenaxherit
    );

    void deleteByProjekti_Id(Long idProjekti);
}
