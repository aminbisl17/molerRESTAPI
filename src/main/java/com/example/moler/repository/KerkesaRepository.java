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

    // Owner (worker / manager) is loaded in the same query for the response

    // ---------- worker: own requests ----------

    @EntityGraph(attributePaths = {"puntori", "menaxheri"})
    List<Kerkesa> findByPuntori_Id(Long idPuntorit, Sort sort);

    @EntityGraph(attributePaths = {"puntori", "menaxheri"})
    List<Kerkesa> findByPuntori_IdAndStatusi(Long idPuntorit, String statusi, Sort sort);

    @EntityGraph(attributePaths = {"puntori", "menaxheri"})
    Optional<Kerkesa> findByIdAndPuntori_Id(Long id, Long idPuntorit);

    // ---------- manager: all requests of the tenant ----------

    @Override
    @EntityGraph(attributePaths = {"puntori", "menaxheri"})
    List<Kerkesa> findAll(Sort sort);

    @EntityGraph(attributePaths = {"puntori", "menaxheri"})
    List<Kerkesa> findByStatusi(String statusi, Sort sort);

    @Override
    @EntityGraph(attributePaths = {"puntori", "menaxheri"})
    Optional<Kerkesa> findById(Long id);
}
