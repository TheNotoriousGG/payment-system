package org.example.personservice.mapper;


import lombok.Setter;

import org.example.person.dto.IndividualWriteDto;
import org.example.personservice.entity.Individual;
import org.example.personservice.entity.User;
import org.example.personservice.util.DateTimeUtil;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import org.springframework.beans.factory.annotation.Autowired;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        injectionStrategy = CONSTRUCTOR,
        uses = {
                AddressMapper.class
        }
)
@Setter(onMethod_ = @Autowired)
public abstract class UserMapper {

    protected DateTimeUtil dateTimeUtil;

    @Named("toUser")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "created", expression = "java(dateTimeUtil.now())")
    @Mapping(target = "updated", expression = "java(dateTimeUtil.now())")
    @Mapping(target = "address", source = "dto", qualifiedByName = "toAddress")
    public abstract User to(IndividualWriteDto dto);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "updated", expression = "java(dateTimeUtil.now())")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "address", expression = "java(addressMapper.update(user, dto))")
    public abstract User update(
            @MappingTarget
            User user,
            IndividualWriteDto dto
    );

    public User update(Individual individual, IndividualWriteDto dto) {
        return update(individual.getUser(), dto);
    }
}