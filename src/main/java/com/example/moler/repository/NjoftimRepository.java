package com.example.moler.repository;

import com.example.moler.model.Njoftim;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NjoftimRepository extends JpaRepository<Njoftim, Long> {

    // ---------- worker ----------

    Page<Njoftim> findByPuntori_Id(Long idPuntorit, Pageable pageable);

    long countByPuntori_IdAndEshteLexuarFalse(Long idPuntorit);

    Optional<Njoftim> findByIdAndPuntori_Id(Long id, Long idPuntorit);

    @Modifying
    @Transactional
    @Query("""
        UPDATE Njoftim n
        SET n.eshteLexuar = true, n.dataLeximit = :tani
        WHERE n.puntori.id = :idPuntorit
          AND n.eshteLexuar = false
    """)
    int shenoTeGjithaSiTeLexuaraPuntori(
            @Param("idPuntorit") Long idPuntorit,
            @Param("tani") LocalDateTime tani
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM Njoftim n WHERE n.puntori.id = :idPuntorit")
    int fshiTeGjithaPuntori(@Param("idPuntorit") Long idPuntorit);

    // ---------- manager ----------

    Page<Njoftim> findByMenaxheri_Id(Long idMenaxherit, Pageable pageable);

    long countByMenaxheri_IdAndEshteLexuarFalse(Long idMenaxherit);

    Optional<Njoftim> findByIdAndMenaxheri_Id(Long id, Long idMenaxherit);

    @Modifying
    @Transactional
    @Query("""
        UPDATE Njoftim n
        SET n.eshteLexuar = true, n.dataLeximit = :tani
        WHERE n.menaxheri.id = :idMenaxherit
          AND n.eshteLexuar = false
    """)
    int shenoTeGjithaSiTeLexuaraMenaxheri(
            @Param("idMenaxherit") Long idMenaxherit,
            @Param("tani") LocalDateTime tani
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM Njoftim n WHERE n.menaxheri.id = :idMenaxherit")
    int fshiTeGjithaMenaxheri(@Param("idMenaxherit") Long idMenaxherit);
}
