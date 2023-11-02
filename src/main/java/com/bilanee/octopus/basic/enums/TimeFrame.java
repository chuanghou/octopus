package com.bilanee.octopus.basic.enums;

import com.bilanee.octopus.infrastructure.entity.TimeFrameDO;
import com.bilanee.octopus.infrastructure.mapper.TimeFrameDOMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.stellariver.milky.common.base.BeanUtil;
import com.stellariver.milky.common.tool.common.Kit;
import com.stellariver.milky.common.tool.util.Collect;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum TimeFrame {

    PEAK(1, "峰时段"),
    FLAT(2, "平时段"),
    VALLEY(3, " 谷时段");

    final Integer dbCode;
    final String desc;

    public static List<TimeFrame> list() {
        return Arrays.asList(TimeFrame.values());
    }

    static final Cache<TimeFrame, List<Integer>> cache =  CacheBuilder.newBuilder().maximumSize(3)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    @SneakyThrows
    public final List<Integer> getPrds() {
        TimeFrame timeFrame = this;
        return cache.get(this, () -> {
            TimeFrameDOMapper timeFrameDOMapper = BeanUtil.getBean(TimeFrameDOMapper.class);
            List<TimeFrameDO> timeFrameDOs = timeFrameDOMapper.selectList(null);
            Map<TimeFrame, Collection<TimeFrameDO>> collect = timeFrameDOs.stream()
                    .collect(Collect.listMultiMap(t -> Kit.enumOfMightEx(TimeFrame::getDbCode, t.getSendingPfvPrd()))).asMap();
            return collect.get(timeFrame).stream().map(TimeFrameDO::getPrd).collect(Collectors.toList());
        });
    }


}
