package com.tickertape.tickertape_spring.repository;

import com.tickertape.tickertape_spring.model.EquityHolding;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EquityHoldingRepository extends MongoRepository<EquityHolding, String> {

    List<EquityHolding> findAllByOrderByLineAsc();
}
