package com.bidco.api_rest.service.impl;

import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.exception.TooManyRequestsException;
import com.bidco.api_rest.model.Subasta;
import com.bidco.api_rest.repository.SubastaRepository;
import com.bidco.api_rest.service.contract.SubastaEventosService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SubastaEventosServiceImpl implements SubastaEventosService {

    private static final Logger log = LoggerFactory.getLogger(SubastaEventosServiceImpl.class);

    /** El navegador reconecta solo al caducar; así no se acumulan conexiones de pestañas olvidadas. */
    public static final long CADUCIDAD_MS = Duration.ofMinutes(5).toMillis();
    public static final int MAX_POR_SUBASTA = 200;
    public static final int MAX_TOTAL = 2000;

    private final SubastaRepository subastaRepository;
    private final Map<Long, Set<SseEmitter>> suscriptores = new ConcurrentHashMap<>();
    private final AtomicInteger total = new AtomicInteger();

    public SubastaEventosServiceImpl(SubastaRepository subastaRepository) {
        this.subastaRepository = subastaRepository;
    }

    @Override
    public SseEmitter suscribir(Long subastaId) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new EntityNotFoundException("La subasta no existe"));
        if (!subasta.isSubastaNormal()) {
            throw new ForbiddenException("Las subastas a ciegas no emiten el precio en directo");
        }
        Set<SseEmitter> grupo = suscriptores.get(subastaId);
        if (total.get() >= MAX_TOTAL || (grupo != null && grupo.size() >= MAX_POR_SUBASTA)) {
            throw new TooManyRequestsException("Demasiadas conexiones abiertas. Recarga la página en unos segundos");
        }

        SseEmitter emitter = new SseEmitter(CADUCIDAD_MS);
        emitter.onCompletion(() -> retirar(subastaId, emitter));
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> retirar(subastaId, emitter));
        suscriptores.computeIfAbsent(subastaId, id -> ConcurrentHashMap.newKeySet()).add(emitter);
        total.incrementAndGet();

        enviar(subastaId, emitter, SseEmitter.event().name("estado").data(estado(subasta.getPrecioFinal(), subasta.getFechaFinal())));
        return emitter;
    }

    @Override
    public void publicarPuja(Long subastaId, BigDecimal precioFinal, Instant fechaFinal) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // Solo se avisa de lo que ya está confirmado: una puja revertida no debe mostrarse a nadie.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    difundir(subastaId, precioFinal, fechaFinal);
                }
            });
        } else {
            difundir(subastaId, precioFinal, fechaFinal);
        }
    }

    /** Un comentario SSE cada pocos segundos evita que proxies e intermediarios cierren la conexión por inactividad. */
    @Scheduled(fixedRate = 25_000)
    public void latido() {
        suscriptores.forEach((subastaId, grupo) -> {
            for (SseEmitter emitter : grupo) {
                enviar(subastaId, emitter, SseEmitter.event().comment("latido"));
            }
        });
    }

    public int suscriptoresActivos() {
        return total.get();
    }

    private void difundir(Long subastaId, BigDecimal precioFinal, Instant fechaFinal) {
        Set<SseEmitter> grupo = suscriptores.get(subastaId);
        if (grupo == null) {
            return;
        }
        SseEmitter.SseEventBuilder evento = SseEmitter.event().name("estado").data(estado(precioFinal, fechaFinal));
        for (SseEmitter emitter : grupo) {
            enviar(subastaId, emitter, evento);
        }
    }

    private void enviar(Long subastaId, SseEmitter emitter, SseEmitter.SseEventBuilder evento) {
        try {
            emitter.send(evento);
        } catch (IOException | RuntimeException e) {
            // Cliente desconectado o conexión ya cerrada: se retira sin más.
            log.debug("Conexión de eventos descartada en la subasta {}: {}", subastaId, e.getMessage());
            retirar(subastaId, emitter);
        }
    }

    private void retirar(Long subastaId, SseEmitter emitter) {
        Set<SseEmitter> grupo = suscriptores.get(subastaId);
        if (grupo != null && grupo.remove(emitter)) {
            total.decrementAndGet();
            if (grupo.isEmpty()) {
                suscriptores.remove(subastaId, grupo);
            }
        }
    }

    /** JSON escrito a mano: solo hay un número y un instante ISO, sin caracteres que haya que escapar. */
    static String estado(BigDecimal precioFinal, Instant fechaFinal) {
        String precio = precioFinal != null ? precioFinal.toPlainString() : "null";
        String fin = fechaFinal != null ? "\"" + fechaFinal + "\"" : "null";
        return "{\"precioFinal\":" + precio + ",\"fechaFinal\":" + fin + "}";
    }
}
