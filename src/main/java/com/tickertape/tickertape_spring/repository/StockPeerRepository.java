package com.tickertape.tickertape_spring.repository;

import com.tickertape.tickertape_spring.model.StockPeer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface StockPeerRepository extends MongoRepository<StockPeer, String> {

    Optional<StockPeer> findBySid(String sid);
}
