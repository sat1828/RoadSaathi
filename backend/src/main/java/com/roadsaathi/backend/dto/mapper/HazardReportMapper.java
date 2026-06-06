package com.roadsaathi.backend.dto.mapper;

import com.roadsaathi.backend.dto.HazardReportResponse;
import com.roadsaathi.backend.model.HazardReport;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.ERROR)
public interface HazardReportMapper {

    @Mapping(target = "latitude", source = "location", qualifiedByName = "latitude")
    @Mapping(target = "longitude", source = "location", qualifiedByName = "longitude")
    @Mapping(target = "classificationLabel", ignore = true)
    @Mapping(target = "confidenceScore", source = "confidence")
    HazardReportResponse toResponse(HazardReport report);

    @Named("latitude")
    default Double mapLatitude(Point point) {
        if (point == null) return null;
        return point.getY();
    }

    @Named("longitude")
    default Double mapLongitude(Point point) {
        if (point == null) return null;
        return point.getX();
    }
}
