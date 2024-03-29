package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.Bid;
import com.bilanee.octopus.basic.Deal;
import com.bilanee.octopus.basic.Point;
import com.bilanee.octopus.basic.enums.BidStatus;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.stellariver.milky.common.tool.Doubles;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class ClearUtil {

    @Nullable
    public static Point<Double> analyzeInterPoint(RangeMap<Double, Range<Double>> buyBrokenLine, RangeMap<Double, Range<Double>> sellBrokenLine) {

        Point<Double> interPoint = null;

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

    public static RangeMap<Double, Range<Double>> buildRangeMap(List<Bid> sortedBids, Double startY, Double endY) {

        RangeMap<Double, Range<Double>> rangeMap = TreeRangeMap.create();

        if (sortedBids.isEmpty()) {
            return rangeMap;
        }

        Double x = 0D;
        Range<Double> xRange = Range.singleton(x);
        Range<Double> yRange = closeRange(startY, sortedBids.get(0).getPriceAfterTariff());
        rangeMap.put(xRange, yRange);
        for (int i = 0; i < sortedBids.size(); i++) {
            Bid bid = sortedBids.get(i);
            xRange = Range.open(x, bid.getQuantity() + x);
            yRange = Range.singleton(bid.getPriceAfterTariff());
            rangeMap.put(xRange, yRange);
            xRange = Range.singleton(bid.getQuantity() + x);
            if (i == sortedBids.size() - 1) {
                yRange = closeRange(bid.getPriceAfterTariff(), endY);
                rangeMap.put(xRange, yRange);
            } else {
                yRange = closeRange(bid.getPriceAfterTariff(), sortedBids.get(i + 1).getPriceAfterTariff());
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

    public static void deal(List<Bid> bids, Point<Double> interPoint, UniqueIdGetter uniqueIdGetter) {
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

        Double price = bids.get(endIndex).getPriceAfterTariff();

        for (int i = endIndex + 1; i < bids.size(); i++) {
            if (!bids.get(i).getPriceAfterTariff().equals(bids.get(endIndex).getPriceAfterTariff())) {
                break;
            }
            endIndex = i;
        }

        Integer startIndex = null;
        for (int i = 0; i < endIndex + 1; i++) {
            if (bids.get(i).getPriceAfterTariff().equals(price)) {
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
        averageBids.forEach(b -> {
            double averageQuantity = (averageBidsQuantity / originalAverageBidsQuantity) * b.getQuantity();
            Deal deal = Deal.builder().quantity(averageQuantity).price(Doubles.add(dealPrice, b.getTariff())).timeStamp(Clock.currentTimeMillis()).build();
            b.getDeals().add(deal);
        });
        notAverageBids.forEach(b -> {
            Deal deal = Deal.builder().quantity(b.getQuantity()).price(Doubles.add(dealPrice, b.getTariff())).timeStamp(Clock.currentTimeMillis()).build();
            b.getDeals().add(deal);
        });

        bids.forEach(bid -> {
            Double dealQuantity = bid.getDeals().stream().map(Deal::getQuantity).reduce(0D, Double::sum);
            if (dealQuantity > 0) {
                bid.setBidStatus(dealQuantity < bid.getQuantity() ? BidStatus.PART_DEAL : BidStatus.COMPLETE_DEAL);
            }
        });
    }




}
