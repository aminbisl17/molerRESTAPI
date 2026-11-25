package com.example.moler.repository;

import com.example.moler.model.prezencapuntoreve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
        SELECT MAX(p.dita)
        FROM prezencapuntoreve p
        WHERE p.puntori.id = :idPuntorit
        AND p.projekti.statusi <> 'c'
    """)
   
LocalDate findLatestDitaByPuntoriId(
        @Param("idPuntorit") Long idPuntorit
);

@Query("""
    SELECT p
    FROM prezencapuntoreve p
    WHERE p.puntori.id = :idPuntorit
      AND p.dita = :dita
      AND p.statusi = 'v'
      AND p.projekti.statusi <> 'c'
""")

boolean existsByPuntori_IdAndDita(Long idPuntorit, LocalDate dita);

List<prezencapuntoreve> findLatestAttendanceByPuntoriId(
        @Param("idPuntorit") Long idPuntorit,
        @Param("dita") LocalDate dita
);

    List<prezencapuntoreve> findAllByPuntori_IdAndDita(
            Long idPuntorit,
            LocalDate dita
    );
}