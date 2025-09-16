package org.example.personservice.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Audited
@Table(name = "individuals", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Individual {
    
    @Id
    @GeneratedValue(generator = "uuid-ossp")
    @Column(columnDefinition = "UUID")
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;
    
    @Column(name = "passport_number", length = 32)
    private String passportNumber;
    
    @Column(name = "phone_number", length = 32)
    private String phoneNumber;
    
    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;
    
    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt;
    
    @Column(name = "status", length = 32)
    private String status;
}
