package com.example.moler.repository;

import com.example.moler.model.prezencapuntoreve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrezencaPuntoreveRepository
        extends JpaRepository<prezencapuntoreve, Long> {

    List<prezencapuntoreve> findByDita(LocalDate dita);

    List<prezencapuntoreve> findByPuntoriId(Long puntoriId);

    List<prezencapuntoreve> findByStatusi(String statusi);

    List<prezencapuntoreve> findByDitaAndStatusi(
            LocalDate dita,
            String statusi
    );

    boolean existsByPuntori_IdAndDita(
            Long idPuntorit,
            LocalDate dita
    );

    // One attendance per worker per day
    Optional<prezencapuntoreve> findByPuntori_IdAndDita(
            Long idPuntorit,
            LocalDate dita
    );

    // Latest attendance day that includes at least one non-completed project
    @Query("""
        SELECT MAX(p.dita)
        FROM prezencapuntoreve p
        JOIN p.detajet d
        WHERE p.puntori.id = :idPuntorit
          AND d.projekti.statusi <> 'c'
    """)
    LocalDate findLatestDitaByPuntoriId(
            @Param("idPuntorit") Long idPuntorit
    );

    // Project ids of the worker's VERIFIED attendance on the given day, excluding completed projects
    @Query("""
        SELECT DISTINCT d.projekti.id
        FROM PrezencaPuntoreveDetajet d
        WHERE d.prezenca.puntori.id = :idPuntorit
          AND d.prezenca.dita = :dita
          AND d.prezenca.statusi = 'v'
          AND d.projekti.statusi <> 'c'
    """)
    List<Long> findLatestProjektetByPuntoriId(
            @Param("idPuntorit") Long idPuntorit,
            @Param("dita") LocalDate dita
    );

    // Unverified attendance that includes at least one project this manager is responsible for
    @Query("""
        SELECT DISTINCT p
        FROM prezencapuntoreve p
        JOIN FETCH p.puntori pu
        JOIN FETCH p.detajet d
        JOIN FETCH d.projekti
        WHERE p.statusi = 'nv'
          AND EXISTS (
              SELECT 1
              FROM PrezencaPuntoreveDetajet d2, ProjektiPrezencaMenaxheret m
              WHERE d2.prezenca = p
                AND m.projekti = d2.projekti
                AND m.menaxheri.id = :idMenaxherit
          )
        ORDER BY p.dita DESC, pu.id
    """)
    List<prezencapuntoreve> findPaVerifikuaraByMenaxheriId(
            @Param("idMenaxherit") Long idMenaxherit
    );

    // All attendance (any status) that includes at least one of this manager's projects
    @Query("""
        SELECT DISTINCT p
        FROM prezencapuntoreve p
        JOIN FETCH p.puntori pu
        JOIN FETCH p.detajet d
        JOIN FETCH d.projekti
        WHERE EXISTS (
              SELECT 1
              FROM PrezencaPuntoreveDetajet d2, ProjektiPrezencaMenaxheret m
              WHERE d2.prezenca = p
                AND m.projekti = d2.projekti
                AND m.menaxheri.id = :idMenaxherit
          )
        ORDER BY p.dita DESC, pu.id
    """)
    List<prezencapuntoreve> findAllByMenaxheriId(
            @Param("idMenaxherit") Long idMenaxherit
    );

    // One worker's attendance in the given statuses, limited to this manager's projects
    @Query("""
        SELECT DISTINCT p
        FROM prezencapuntoreve p
        JOIN FETCH p.puntori pu
        JOIN FETCH p.detajet d
        JOIN FETCH d.projekti
        WHERE pu.id = :idPuntorit
          AND p.statusi IN :statuset
          AND EXISTS (
              SELECT 1
              FROM PrezencaPuntoreveDetajet d2, ProjektiPrezencaMenaxheret m
              WHERE d2.prezenca = p
                AND m.projekti = d2.projekti
                AND m.menaxheri.id = :idMenaxherit
          )
        ORDER BY p.dita DESC
    """)
    List<prezencapuntoreve> findByPuntoriIdAndMenaxheriIdAndStatusiIn(
            @Param("idPuntorit") Long idPuntorit,
            @Param("idMenaxherit") Long idMenaxherit,
            @Param("statuset") List<String> statuset
    );

    // One worker's attendance in the given statuses, across all projects
    @Query("""
        SELECT DISTINCT p
        FROM prezencapuntoreve p
        JOIN FETCH p.puntori pu
        JOIN FETCH p.detajet d
        JOIN FETCH d.projekti
        WHERE pu.id = :idPuntorit
          AND p.statusi IN :statuset
        ORDER BY p.dita DESC
    """)
    List<prezencapuntoreve> findByPuntoriIdAndStatusiIn(
            @Param("idPuntorit") Long idPuntorit,
            @Param("statuset") List<String> statuset
    );

    // Of the given ids, the attendance that includes at least one of this manager's projects
    @Query("""
        SELECT p
        FROM prezencapuntoreve p
        WHERE p.id IN :ids
          AND EXISTS (
              SELECT 1
              FROM PrezencaPuntoreveDetajet d, ProjektiPrezencaMenaxheret m
              WHERE d.prezenca = p
                AND m.projekti = d.projekti
                AND m.menaxheri.id = :idMenaxherit
          )
    """)
    List<prezencapuntoreve> findByIdInAndMenaxheriId(
            @Param("ids") List<Long> ids,
            @Param("idMenaxherit") Long idMenaxherit
    );
}
