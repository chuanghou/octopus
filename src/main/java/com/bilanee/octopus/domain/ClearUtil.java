package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.*;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.stellariver.milky.common.base.BeanUtil;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class ClearUtil {

    @Nullable
    public static Point<Double> analyzeInterPoint(List<Bid> sortedBuyBids, List<Bid> sortedSellBids) {

        Point<Double> interPoint = null;

        RangeMap<Double, Range<Double>> buyBrokenLine = buildRangeMap(sortedBuyBids, Double.MAX_VALUE, 0D);
        RangeMap<Double, Range<Double>> sellBrokenLine = buildRangeMap(sortedSellBids, 0D, Double.MAX_VALUE);

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
        Range<Double> yRange = closeRange(startY, sortedBids.get(0).getPrice());
        rangeMap.put(xRange, yRange);
        for (int i = 0; i < sortedBids.size(); i++) {
            Bid bid = sortedBids.get(i);
            xRange = Range.open(x, bid.getQuantity() + x);
            yRange = Range.singleton(bid.getPrice());
            rangeMap.put(xRange, yRange);
            xRange = Range.singleton(bid.getQuantity() + x);
            if (i == sortedBids.size() - 1) {
                yRange = closeRange(bid.getPrice(), endY);
                rangeMap.put(xRange, yRange);
            } else {
                yRange = closeRange(bid.getPrice(), sortedBids.get(i + 1).getPrice());
                rangeMap.put(xRange, yRange);
            }
            x = x + bid.getQuantity();
        }
        return rangeMap;
    }

    private static Range<Double> closeRange(Double a, Double b) {
        Double min = Math.min(a, b);
        Double max = Math.max(a, b);
        return Range.closed(min, max);
    }

    public static void deal(List<Bid> bids, Point<Double> interPoint) {
        Double dealPrice = interPoint.y;
        Double totalDealQuantity = interPoint.x;

        Double accumulateQuantity = 0D;
        int endIndex = 0;
        for (int i = 0; i < bids.size(); i++) {
            accumulateQuantity += bids.get(i).getQuantity();
            if (accumulateQuantity >= totalDealQuantity) {
                endIndex = i;
                break;
            }
        }

        Double price = bids.get(endIndex).getPrice();
        Integer startIndex = null;
        for (int i = 0; i < endIndex + 1; i++) {
            if (bids.get(i).getPrice().equals(price)) {
                startIndex = i;
                break;
            }
        }
        if (startIndex == null) {
            throw new RuntimeException();
        }

        List<Bid> averageBids = bids.subList(startIndex, endIndex + 1);
        List<Bid> notAverageBids = bids.subList(0, startIndex);
        Double notAverageQuantity = notAverageBids.stream().map(Bid::getQuantity).reduce(0D, Double::sum);
        double averageBidsQuantity = totalDealQuantity - notAverageQuantity;
        Double originalAverageBidsQuantity = averageBids.stream().map(Bid::getQuantity).reduce(0D, Double::sum);
//        UniqueIdGetter uniqueIdGetter = BeanUtil.getBean(UniqueIdGetter.class);
        averageBids.forEach(b -> {
            double averageQuantity = (averageBidsQuantity / originalAverageBidsQuantity) * b.getQuantity();
            Deal deal = Deal.builder()
//                    .dealId(uniqueIdGetter.get())
                    .quantity(averageQuantity).price(dealPrice).timeStamp(Clock.currentTimeMillis()).build();
            b.getDeals().add(deal);
        });
        notAverageBids.forEach(b -> {
            Deal deal = Deal.builder()
//                    .dealId(uniqueIdGetter.get())
                    .quantity(b.getQuantity()).price(dealPrice).timeStamp(Clock.currentTimeMillis()).build();
            b.getDeals().add(deal);
        });
    }




}
