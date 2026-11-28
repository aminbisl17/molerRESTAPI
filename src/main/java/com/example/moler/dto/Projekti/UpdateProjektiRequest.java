
package com.example.moler.dto.Projekti;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateProjektiRequest(

        String emri,

        String lokacioni,

        String kerkesa,

        String statusi,

        String prioriteti,

        String ambienti,

        LocalDate dataFillimitTePlanifikuar,

        LocalDate dataPerfundimitTePlanifikuar,

        // Cancellation description
        String pershkrimiAnulimit,

        // Assigns or changes the client (null = unchanged)
        Long idKlientit,

        // true removes the client from the project
        Boolean hiqKlientin,

        List<Long> menaxheret,

        List<Long> puntoret,

        List<MaterialRequest> materialet,

        DetajetRequest detajet

) {

    public record MaterialRequest(
            Long idMaterialit,
            BigDecimal sasia
    ) {}

    public record DetajetRequest(
            BigDecimal m2,
            BigDecimal pagesa,
            String pershkrimi
    ) {}
}
