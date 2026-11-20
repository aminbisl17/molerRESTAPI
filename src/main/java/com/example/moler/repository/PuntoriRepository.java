package com.example.moler.repository;

import com.example.moler.model.puntori;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PuntoriRepository extends JpaRepository<puntori, Long> {

    Optional<puntori> findByUsername(String username);

    boolean existsByNumriTelefonitAndIdNot(String numriTelefonit, Long id);
    
    boolean existsByUsername(String username);

    Optional<puntori> findByNumriTelefonit(String numriTelefonit);

    boolean existsByNumriTelefonit(String numriTelefonit);

    boolean existsByUsernameAndIdNot(String username, Long id);

boolean existsByEmailAndIdNot(String email, Long id);

    Optional<puntori> findByEmail(String email);

    List<puntori> findByStatusi(String statusi);

    List<puntori> findByPozita(String pozita);

    List<puntori> findByStatusiAndPozita(String statusi, String pozita);
}