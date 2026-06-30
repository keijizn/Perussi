package com.perussi.repository;

import com.perussi.model.PedidoItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PedidoItemRepository extends JpaRepository<PedidoItem, UUID> {
}