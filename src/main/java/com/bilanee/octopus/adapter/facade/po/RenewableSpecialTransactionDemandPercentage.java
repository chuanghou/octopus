package com.bilanee.octopus.adapter.facade.po;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RenewableSpecialTransactionDemandPercentage {

    /**
     * 送电省风
     */
    Double transferWind;

    /**
     * 送电省光
     */
    Double transferSolar;

    /**
     * 受电省风
     */
    Double receiverWind;

    /**
     * 受电省光
     */
    Double receiverSolar;

    public String storeValue() {
        return String.format("%s:%s:%s:%s", transferWind, transferSolar, receiverWind, receiverSolar);
    }

    static public RenewableSpecialTransactionDemandPercentage resolve(String value) {
        String[] split = StringUtils.split(value, ":");
        RenewableSpecialTransactionDemandPercentage resolved = new RenewableSpecialTransactionDemandPercentage();
        resolved.setTransferWind(Double.parseDouble(split[0]));
        resolved.setTransferSolar(Double.parseDouble(split[1]));
        resolved.setReceiverWind(Double.parseDouble(split[2]));
        resolved.setReceiverSolar(Double.parseDouble(split[3]));
        return resolved;
    }

}
