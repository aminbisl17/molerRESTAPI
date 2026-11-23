
package com.example.moler.repository;

import com.example.moler.model.materiali;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialiRepository extends JpaRepository<materiali, Long> {
}
