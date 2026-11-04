package com.perussi.service;

import com.perussi.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;

class PagamentoReconciliacaoSchedulerTest {
    @Test void failedOrderDoesNotBlockOthersAndIsRetriedNextCycle() {
        var repository = mock(PedidoRepository.class);
        var service = mock(PagamentoService.class);
        var a = UUID.randomUUID(); var b = UUID.randomUUID();
        when(repository.findPagamentosPendentes()).thenReturn(List.of(a, b));
        doThrow(new IllegalStateException("offline")).doNothing().when(service).reconciliarPendente(a);
        var scheduler = new PagamentoReconciliacaoScheduler(repository, service);
        scheduler.reconciliar(); scheduler.reconciliar();
        verify(service, times(2)).reconciliarPendente(a);
        verify(service, times(2)).reconciliarPendente(b);
    }
}
