package org.example.personservice.mapper;

import lombok.Setter;
import org.example.personservice.entity.Address;
import org.example.personservice.repository.CountryRepository;
import org.example.personservice.util.DateTimeUtil;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
@Setter(onMethod_ = @Autowired)
public class AddressMapper {

    protected CountryRepository countryRepository;
    protected DateTimeUtil dateTimeUtil;

    //public abstract Address to(IndiVidualWriteDto dto)
}
