
package com.example.moler.repository;

import com.example.moler.model.ProjektiMateriali;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjektiMaterialiRepository extends JpaRepository<ProjektiMateriali, Long> {

    List<ProjektiMateriali> findByProjekti_Id(Long idProjekti);

    List<ProjektiMateriali> findByMateriali_Id(Long idMaterialit);

    void deleteByProjekti_Id(Long idProjekti);
}
