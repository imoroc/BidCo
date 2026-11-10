package com.bidco.api_rest.config;

import com.bidco.api_rest.repository.SorteoRepository;
import com.bidco.api_rest.repository.SubastaRepository;
import com.bidco.api_rest.service.contract.SorteoService;
import com.bidco.api_rest.service.contract.SubastaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Cierra las subastas y los sorteos vencidos sin depender de que un usuario abra su ficha.
 *
 * <p>Reutiliza la lógica idempotente de los servicios (con bloqueo de fila), de modo que
 * puede convivir con el cierre manual y con varias instancias de la API. Cada elemento se
 * cierra en su propia transacción: si uno falla, los demás se procesan igualmente.
 * Las subastas y sorteos sin pujas o participaciones no se tocan.
 */
@Component
public class CierreAutomaticoTask {

    private static final Logger log = LoggerFactory.getLogger(CierreAutomaticoTask.class);

    private final SubastaRepository subastaRepository;
    private final SorteoRepository sorteoRepository;
    private final SubastaService subastaService;
    private final SorteoService sorteoService;
    private final Clock clock;

    public CierreAutomaticoTask(SubastaRepository subastaRepository,
                                SorteoRepository sorteoRepository,
                                SubastaService subastaService,
                                SorteoService sorteoService,
                                Clock clock) {
        this.subastaRepository = subastaRepository;
        this.sorteoRepository = sorteoRepository;
        this.subastaService = subastaService;
        this.sorteoService = sorteoService;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString = "${app.cierre.intervalo-ms:60000}",
            initialDelayString = "${app.cierre.retraso-inicial-ms:30000}")
    public void cerrarVencidas() {
        int subastas = cerrarSubastasVencidas();
        int sorteos = cerrarSorteosVencidos();
        if (subastas > 0 || sorteos > 0) {
            log.info("Cierre automático: {} subasta(s) y {} sorteo(s) cerrados", subastas, sorteos);
        }
    }

    int cerrarSubastasVencidas() {
        List<Long> ids = subastaRepository.findIdsVencidasConPujasSinGanador(LocalDate.now(clock));
        int cerradas = 0;
        for (Long id : ids) {
            try {
                subastaService.asignarGanadorYActualizarPrecioFinal(id);
                cerradas++;
            } catch (RuntimeException e) {
                log.warn("No se pudo cerrar la subasta {}: {}", id, e.getMessage());
            }
        }
        return cerradas;
    }

    int cerrarSorteosVencidos() {
        List<Long> ids = sorteoRepository.findIdsVencidosConParticipacionesSinGanador(LocalDate.now(clock));
        int cerrados = 0;
        for (Long id : ids) {
            try {
                sorteoService.asignarGanadorYActualizarPrecioFinal(id);
                cerrados++;
            } catch (RuntimeException e) {
                log.warn("No se pudo cerrar el sorteo {}: {}", id, e.getMessage());
            }
        }
        return cerrados;
    }
}
