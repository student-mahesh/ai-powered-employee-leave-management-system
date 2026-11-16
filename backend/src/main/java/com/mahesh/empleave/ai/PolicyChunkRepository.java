package com.mahesh.empleave.ai;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyChunkRepository extends JpaRepository<PolicyChunk, Long> {
    List<PolicyChunk> findByDocumentOrderByChunkIndex(PolicyDocument document);
    List<PolicyChunk> findByVectorIdIn(List<String> vectorIds);
}
