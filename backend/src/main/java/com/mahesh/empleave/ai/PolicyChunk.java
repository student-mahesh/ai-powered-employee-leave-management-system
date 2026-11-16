package com.mahesh.empleave.ai;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "policy_chunks", uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "chunk_index"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyChunk {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private PolicyDocument document;
    @Column(name = "chunk_index", nullable = false) private int chunkIndex;
    private Integer sourcePage;
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT") private String content;
    private String vectorId;

    public PolicyChunk(PolicyDocument document, int chunkIndex, Integer sourcePage, String content, String vectorId) {
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.sourcePage = sourcePage;
        this.content = content;
        this.vectorId = vectorId;
    }

    public void assignVectorId(String vectorId) {
        this.vectorId = vectorId;
    }
}
