package com.bilanee.octopus.adapter.facade.vo;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RetailPackageVO {

    String headMessage;

    List<UnitPackage> unitPackages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class UnitPackage {

        Long unitId;
        String unitName;
        List<PackageDetail> packageDetails;

    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class PackageDetail {

        Integer id;
        String description;
        Boolean checked;

    }
}

