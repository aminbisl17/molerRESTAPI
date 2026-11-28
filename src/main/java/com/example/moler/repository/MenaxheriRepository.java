package com.example.moler.repository;

import com.example.moler.model.menaxheri;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenaxheriRepository extends JpaRepository<menaxheri, Long> {

    Optional<menaxheri> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<menaxheri> findByNumriTelefonit(String numriTelefonit);

    boolean existsByNumriTelefonit(String numriTelefonit);

    Optional<menaxheri> findByEmail(String email);

    // Only the IDs, e.g. for notifying all managers
    @Query("SELECT m.id FROM menaxheri m")
    List<Long> findAllIds();
}