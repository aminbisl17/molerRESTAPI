package com.example.moler.service;

import com.example.moler.dto.Dashboard.MenaxheriDashboardResponse;
import com.example.moler.dto.Dashboard.PuntoriDashboardResponse;
import com.example.moler.exception.ResourceNotFoundException;
import com.example.moler.model.prezencapuntoreve;
import com.example.moler.model.puntori;
import com.example.moler.repository.PrezencaPuntoreveRepository;
import com.example.moler.repository.ProjektiDetajetRepository;
import com.example.moler.repository.ProjektiRepository;
import com.example.moler.repository.PuntoriRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
public class DashboardService {

    private final PuntoriRepository puntoriRepository;
    private final ProjektiRepository projektiRepository;
    private final PrezencaPuntoreveRepository prezencaRepository;
    private final ProjektiDetajetRepository projektiDetajetRepository;

    public DashboardService(
            PuntoriRepository puntoriRepository,
            ProjektiRepository projektiRepository,
            PrezencaPuntoreveRepository prezencaRepository,
            ProjektiDetajetRepository projektiDetajetRepository
    ) {
        this.puntoriRepository = puntoriRepository;
        this.projektiRepository = projektiRepository;
        this.prezencaRepository = prezencaRepository;
        this.projektiDetajetRepository = projektiDetajetRepository;
    }

    @Transactional(readOnly = true)
    public MenaxheriDashboardResponse getMenaxheriDashboard(Long idMenaxherit) {

        if (idMenaxherit == null) {
            throw new IllegalArgumentException("Menaxheri eshte i detyrueshem");
        }

        long puntoretAktiv = puntoriRepository.countByStatusi("a");

        long paVerifikuara = prezencaRepository.countPaVerifikuaraByMenaxheriId(idMenaxherit);

        // "e" + "w" together, e.g. 2 + 3 = 5
        long projektetAktive = projektiRepository.countByStatusiIn(List.of("e", "w"));

        // Total pagesa of completed ("cm") projects
        BigDecimal pasqyra = projektiDetajetRepository
                .sumPagesaByProjektiStatusi("cm")
                .setScale(2, RoundingMode.HALF_UP);

        return new MenaxheriDashboardResponse(puntoretAktiv, paVerifikuara, projektetAktive, pasqyra);
    }

    @Transactional(readOnly = true)
    public PuntoriDashboardResponse getPuntoriDashboard(Long idPuntorit) {

        puntori puntori = puntoriRepository.findById(idPuntorit)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Puntori nuk ekziston"));

        YearMonth muaji = YearMonth.now(ZoneId.of("Europe/Skopje"));
        LocalDate fillimi = muaji.atDay(1);
        LocalDate fundi = muaji.atEndOfMonth();

        List<prezencapuntoreve> teVerifikuara = prezencaRepository
                .findByPuntori_IdAndStatusiAndDitaBetween(idPuntorit, "v", fillimi, fundi);

        BigDecimal rrogaDitore = puntori.getRrogaDitore() == null
                ? BigDecimal.ZERO
                : puntori.getRrogaDitore();

        BigDecimal gjysmeDite = rrogaDitore.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        // Full day ("f") earns the daily wage, half day ("h") earns half of it
        BigDecimal rroga = teVerifikuara.stream()
                .map(p -> "h".equals(p.getLlojiOrarit()) ? gjysmeDite : rrogaDitore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new PuntoriDashboardResponse(teVerifikuara.size(), rroga);
    }
}
