package com.mahesh.empleave.ai;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, Long> {
    @EntityGraph(attributePaths = "uploadedBy")
    List<PolicyDocument> findAllByOrderByCreatedAtDesc();
}
