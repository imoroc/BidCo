package com.bidco.api_rest.service.contract;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.Instant;

/** Difunde en directo el precio y el cierre de las subastas normales a quien tenga su ficha abierta. */
public interface SubastaEventosService {

    /**
     * Abre un flujo de eventos para una subasta normal. Envía de inmediato su estado actual, para que el
     * cliente no dependa de cuándo cargó la ficha. Las subastas a ciegas no emiten: revelarían el precio.
     */
    SseEmitter suscribir(Long subastaId);

    /** Avisa de un nuevo precio y del cierre vigente. Si hay una transacción en curso, se envía al confirmarse. */
    void publicarPuja(Long subastaId, BigDecimal precioFinal, Instant fechaFinal);
}
