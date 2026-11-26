package com.example.moler.repository;

import com.example.moler.model.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByExpoToken(String expoToken);

    // For sending: all devices of one worker / one manager
    List<PushToken> findByPuntori_Id(Long idPuntorit);

    List<PushToken> findByMenaxheri_Id(Long idMenaxherit);

    // For sending to many people at once
    List<PushToken> findByPuntori_IdIn(Collection<Long> idPuntoreve);

    List<PushToken> findByMenaxheri_IdIn(Collection<Long> idMenaxhereve);

    // Removes tokens Expo reported as no longer registered
    @Transactional
    long deleteByExpoTokenIn(Collection<String> expoTokens);
}
