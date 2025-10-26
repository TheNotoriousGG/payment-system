package org.example.personservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.time.LocalDateTime;
import java.util.UUID;

@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@AuditTable(schema = "person_aud", value = "individuals_aud")
@Setter
@Getter
@Entity
@Table(name = "individuals", schema = "person")
public class Individual extends BaseEntity {

    @Size(max = 64)
    @Column(name = "passport_number", nullable = false, unique = true, length = 64)
    private String passportNumber;

    @Size(max = 64)
    @Column(name = "phone_number", nullable = false, unique = true, length = 64)
    private String phoneNumber;

    @OneToOne(optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
