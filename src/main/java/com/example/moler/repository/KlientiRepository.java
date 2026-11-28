package com.example.moler.repository;

import com.example.moler.model.klienti;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KlientiRepository extends JpaRepository<klienti, Long> {
}
