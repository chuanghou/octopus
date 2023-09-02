package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.*;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class ClearUtil {

    @Nullable
    static public Point<Double> resolveInterPoint(List<Bid> buyBids, List<Bid> sellBids) {

        Point<Double> interPoint = null;

        buyBids = buyBids.stream().sorted(Comparator.comparing(Bid::getPrice).reversed()).collect(Collectors.toList());
        sellBids = sellBids.stream().sorted(Comparator.comparing(Bid::getPrice)).collect(Collectors.toList());

        RangeMap<Double, Range<Double>> buyBrokenLine = buildRangeMap(buyBids, Double.MAX_VALUE, 0D);
        RangeMap<Double, Range<Double>> sellBrokenLine = buildRangeMap(sellBids, 0D, Double.MAX_VALUE);

        List<Double> xes = Stream.of(buyBrokenLine.asMapOfRanges().keySet(), sellBrokenLine.asMapOfRanges().keySet())
                .flatMap(Collection::stream)
                .flatMap(r -> Stream.of(r.lowerEndpoint(), r.upperEndpoint()))
                .distinct()
                .sorted(Double::compareTo)
                .collect(Collectors.toList());
        for (Double x : xes) {
            Range<Double> buyR = buyBrokenLine.get(x);
            Range<Double> sellR = sellBrokenLine.get(x);
            if (buyR == null || sellR == null) {
                break;
            }
            /*
                the range intersection in guava, need to check is connected before, I know that
                guava think null should be a meaning object, so they don't return null, they would rather
                let you get an exception but not null.
             */
            if (buyR.isConnected(sellR)) {
                Range<Double> interRange = buyR.intersection(sellR);
                interPoint = new Point<>(x, (interRange.lowerEndpoint() + interRange.upperEndpoint()) / 2);
            }
        }

        return interPoint;
    }

    private static RangeMap<Double, Range<Double>> buildRangeMap(List<Bid> sortedBids, Double startY, Double endY) {

        RangeMap<Double, Range<Double>> rangeMap = TreeRangeMap.create();

        if (sortedBids.isEmpty()) {
            return rangeMap;
        }

        Double x = 0D;
        Range<Double> xRange = Range.singleton(x);
        Range<Double> yRange = sorted(startY, sortedBids.get(0).getPrice());
        rangeMap.put(xRange, yRange);
        for (int i = 0; i < sortedBids.size(); i++) {
            Bid bid = sortedBids.get(i);
            xRange = Range.open(x, bid.getQuantity() + x);
            yRange = Range.singleton(bid.getPrice());
            rangeMap.put(xRange, yRange);
            xRange = Range.singleton(bid.getQuantity() + x);
            if (i == sortedBids.size() - 1) {
                yRange = sorted(bid.getPrice(), endY);
                rangeMap.put(xRange, yRange);
            } else {
                yRange = sorted(bid.getPrice(), sortedBids.get(i + 1).getPrice());
                rangeMap.put(xRange, yRange);
            }
            x = x + bid.getQuantity();
        }
        return rangeMap;
    }

    static private Range<Double> sorted(Double a, Double b) {
        Double min = Math.min(a, b);
        Double max = Math.max(a, b);
        return Range.closed(min, max);
    }




}
