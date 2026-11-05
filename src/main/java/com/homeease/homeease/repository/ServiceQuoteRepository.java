package com.homeease.homeease.repository;

import com.homeease.homeease.model.ServiceQuote;
import com.homeease.homeease.model.ServiceRequest;
import com.homeease.homeease.model.Worker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceQuoteRepository extends JpaRepository<ServiceQuote, Long> {

    List<ServiceQuote> findByServiceRequest(ServiceRequest request);

    Optional<ServiceQuote> findByServiceRequestAndWorker(ServiceRequest request, Worker worker);

    List<ServiceQuote> findByWorkerOrderByCreatedAtDesc(Worker worker);
}
