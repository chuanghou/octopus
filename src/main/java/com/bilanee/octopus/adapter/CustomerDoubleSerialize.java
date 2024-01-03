package com.bilanee.octopus.adapter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;


/**
 * 格式化
 *
 * double 精确到小数点后两位
 * Date:     2019/10/15 11:58
 * title:title
 * @author ht
 */
public class CustomerDoubleSerialize extends JsonSerializer<Double> {

    /**
     * 原本这里是  ##.00 ,带来的问题是如果数据库数据为0.00返回“ .00 “经评论指正，改为0.00
     */
    private final DecimalFormat df = new DecimalFormat("0.00");

    @Override
    public void serialize(Double arg0, JsonGenerator arg1, SerializerProvider arg2) throws IOException {
        if(arg0 != null) {
            BigDecimal bigDecimal = new BigDecimal(String.valueOf(arg0));
            df.setRoundingMode(RoundingMode.HALF_UP);
            //四舍五入。需要将数据转成bigDecimal, 否则会存在经度丢失问题
            String format = df.format(bigDecimal);
            double aDouble = Double.parseDouble(format);
            arg1.writeNumber(aDouble);//返回数字格式
        }
    }
}