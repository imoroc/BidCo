package com.bidco.api_rest.controller;

import com.bidco.api_rest.service.contract.SubastaEventosService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/subastas")
public class SubastaEventosController {

    private final SubastaEventosService eventos;

    public SubastaEventosController(SubastaEventosService eventos) {
        this.eventos = eventos;
    }

    /** Flujo público de solo lectura: precio y cierre de una subasta normal, igual que el que ya muestra su ficha. */
    @GetMapping(value = "/{id}/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter eventos(@PathVariable Long id) {
        return eventos.suscribir(id);
    }
}
