package pe.utp.hermilio.citas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/** Tarea programada de recordatorios (RF-12). Cron configurable en app.notificaciones.recordatorio-cron. */
@Component
public class RecordatorioScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioScheduler.class);

    private final NotificacionService notificacionService;
    private final Clock clock;

    public RecordatorioScheduler(NotificacionService notificacionService, Clock clock) {
        this.notificacionService = notificacionService;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.notificaciones.recordatorio-cron:0 0 8 * * *}")
    public void enviarRecordatoriosDeManana() {
        int total = notificacionService.generarRecordatorios(LocalDate.now(clock).plusDays(1));
        log.info("Recordatorios generados para las citas de mañana: {}", total);
    }
}
