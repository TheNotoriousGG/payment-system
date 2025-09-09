package org.example.individualsapi.mapper;

import org.example.individualsapi.model.KeycloakUserResponse;
import org.example.individualsapi.model.dto.UserInfoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", ignore = true)
    @Mapping(source = "createdTimestamp", target = "createdAt", qualifiedByName = "timestampToOffsetDateTime")
    UserInfoResponse toUserInfoResponse(KeycloakUserResponse keycloakUserResponse);

    @Named("timestampToOffsetDateTime")
    default OffsetDateTime timestampToOffsetDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC);
    }
}