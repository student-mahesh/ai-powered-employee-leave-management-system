package com.mahesh.empleave.ai;

import com.mahesh.empleave.user.AppUser;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "policy_documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String fileName;
    private String version;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser uploadedBy;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();

    public PolicyDocument(String fileName, String version, AppUser uploadedBy) {
        this.fileName = fileName;
        this.version = version;
        this.uploadedBy = uploadedBy;
    }
}
