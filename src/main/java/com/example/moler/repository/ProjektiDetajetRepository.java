package com.example.moler.repository;

import com.example.moler.model.projektidetajet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface ProjektiDetajetRepository
        extends JpaRepository<projektidetajet, Long> {

    Optional<projektidetajet> findByProjektiId(Long projektiId);

    boolean existsByProjektiId(Long projektiId);
}