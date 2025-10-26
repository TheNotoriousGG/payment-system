package org.example.personservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@AuditTable(schema = "person_aud", value = "addresses_aud")
@Setter
@Getter
@Entity
@Table(name = "addresses", schema = "person")
public class Address extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @Size(max = 128)
    @Column(name = "address", nullable = false, length = 128)
    private String address;

    @Size(max = 32)
    @Column(name = "zip_code", nullable = false, length = 32)
    private String zipCode;

    @Size(max = 128)
    @Column(name = "city", nullable = false, length = 128)
    private String city;
}
