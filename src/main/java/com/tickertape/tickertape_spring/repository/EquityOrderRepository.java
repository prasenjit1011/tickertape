package com.tickertape.tickertape_spring.repository;

import com.tickertape.tickertape_spring.model.EquityOrder;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EquityOrderRepository extends MongoRepository<EquityOrder, String> {

    List<EquityOrder> findAllByOrderByTradeDateDescLineAsc();
}
