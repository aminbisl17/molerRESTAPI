
package com.example.moler.repository;

import com.example.moler.model.ProjektiPrezencaMenaxheret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjektiPrezencaMenaxheretRepository
        extends JpaRepository<ProjektiPrezencaMenaxheret, Long> {

    List<ProjektiPrezencaMenaxheret> findByProjekti_Id(Long idProjekti);

    List<ProjektiPrezencaMenaxheret> findByMenaxheri_Id(Long idMenaxherit);

    boolean existsByProjekti_IdAndMenaxheri_Id(
            Long idProjekti,
            Long idMenaxherit
    );

    void deleteByProjekti_Id(Long idProjekti);
}
