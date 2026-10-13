package com.marudham.web.repository;
import com.marudham.web.model.CollectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CollectionRequestRepository extends JpaRepository<CollectionRequest,Long> { List<CollectionRequest> findAllByOrderByCreatedAtDesc(); }
