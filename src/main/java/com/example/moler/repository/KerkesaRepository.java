package com.example.moler.repository;

import com.example.moler.model.Kerkesa;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KerkesaRepository extends JpaRepository<Kerkesa, Long> {

    // Worker and manager are loaded in the same query for the response

    // ---------- worker: requests they created ----------

    @EntityGraph(attributePaths = {"puntori", "menaxheri"})
    List<Kerkesa> findByPuntori_Id(Long idPuntorit, Sort sort);

    @EntityGraph(attributePaths = {"puntori", "menaxheri"})
    List<Kerkesa> findByPuntori_IdAndStatusi(Long idPuntorit, String statusi, Sort sort);

    @EntityGraph(attributePaths = {"puntori", "menaxheri"})
    Optional<Kerkesa> findByIdAndPuntori_Id(Long id, Long idPuntorit);

    // Worker dashboard: number of own requests with the given status
    long countByPuntori_IdAndStatusi(Long idPuntorit, String statusi);

    // ---------- manager: requests sent to them ----------

    @EntityGraph(attributePaths = {"puntori", "menaxheri"})
    List<Kerkesa> findByMenaxheri_Id(Long idMenaxherit, Sort sort);

    @EntityGraph(attributePaths = {"puntori", "menaxheri"})
    List<Kerkesa> findByMenaxheri_IdAndStatusi(Long idMenaxherit, String statusi, Sort sort);

    @EntityGraph(attributePaths = {"puntori", "menaxheri"})
    Optional<Kerkesa> findByIdAndMenaxheri_Id(Long id, Long idMenaxherit);
}
