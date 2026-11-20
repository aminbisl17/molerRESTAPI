package com.example.moler.repository;

import com.example.moler.model.prezencapuntoreve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

public interface PrezencaPuntoreveRepository
        extends JpaRepository<prezencapuntoreve, Long> {

    List<prezencapuntoreve> findByDita(LocalDate dita);

    List<prezencapuntoreve> findByPuntoriId(Long puntoriId);

    List<prezencapuntoreve> findByProjektiId(Long projektiId);

    List<prezencapuntoreve> findByStatusi(String statusi);

    List<prezencapuntoreve> findByDitaAndStatusi(
            LocalDate dita,
            String statusi
    );

    List<prezencapuntoreve> findByProjektiIdAndDita(
            Long projektiId,
            LocalDate dita
    );

    boolean existsByProjektiIdAndPuntoriIdAndDita(
            Long projektiId,
            Long puntoriId,
            LocalDate dita
    );
}