package com.example.moler.repository;

import com.example.moler.model.projekti;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjektiRepository extends JpaRepository<projekti, Long> {

    List<projekti> findByStatusi(String statusi);

    List<projekti> findByPrioriteti(String prioriteti);

    List<projekti> findByStatusiProjektit(String statusiProjektit);

    List<projekti> findByStatusiAndStatusiProjektit(
            String statusi,
            String statusiProjektit
    );

    List<projekti> findByEmriContainingIgnoreCase(String emri);
}