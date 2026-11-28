package com.example.moler.repository;

import com.example.moler.model.klienti;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KlientiRepository extends JpaRepository<klienti, Long> {

    // Phone number uniqueness
    boolean existsByNumriITelefonit(String numriITelefonit);

    boolean existsByNumriITelefonitAndIdNot(String numriITelefonit, Long id);

    // UQ_klienti_unique_details (emri, mbiemri, lokacioni, numri_i_telefonit).
    // SQL Server treats NULLs as equal in unique constraints, so two clients
    // with the same details and no phone number collide.
    boolean existsByEmriAndMbiemriAndLokacioniAndNumriITelefonitIsNull(
            String emri,
            String mbiemri,
            String lokacioni
    );

    boolean existsByEmriAndMbiemriAndLokacioniAndNumriITelefonitIsNullAndIdNot(
            String emri,
            String mbiemri,
            String lokacioni,
            Long id
    );
}
