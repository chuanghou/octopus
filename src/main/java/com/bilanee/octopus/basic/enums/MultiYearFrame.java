package com.bilanee.octopus.basic.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MultiYearFrame {

    TRANSFER_WIND(Province.TRANSFER, RenewableType.WIND),
    RECEIVER_WIND(Province.RECEIVER, RenewableType.WIND),
    TRANSFER_SOLAR(Province.TRANSFER, RenewableType.SOLAR),
    RECEIVER_SOLAR(Province.RECEIVER, RenewableType.SOLAR);

    final private Province province;
    final private RenewableType renewableType;


}
