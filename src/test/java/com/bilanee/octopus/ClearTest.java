package com.bilanee.octopus;

import com.bilanee.octopus.basic.Bid;
import com.bilanee.octopus.basic.Deal;
import com.bilanee.octopus.basic.Direction;
import com.bilanee.octopus.basic.Point;
import com.bilanee.octopus.domain.ClearUtil;
import com.bilanee.octopus.domain.Comp;
import com.stellariver.milky.common.tool.util.Collect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

public class ClearTest {

    @Test
    public void interPointTest() {

        Point<Double> point = ClearUtil.analyzeInterPoint(new ArrayList<>(), new ArrayList<>());
        Assertions.assertNull(point);

        Bid buyBid = Bid.builder().direction(Direction.BUY).quantity(100D).price(100D).build();
        point = ClearUtil.analyzeInterPoint(Collect.asList(buyBid), new ArrayList<>());
        Assertions.assertNull(point);

        Bid sellBid = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();
        point = ClearUtil.analyzeInterPoint(Collect.asList(buyBid), Collect.asList(sellBid));
        Assertions.assertNotNull(point);
        Assertions.assertEquals(point, new Point<>(0D, 150D));

        sellBid = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();
        point = ClearUtil.analyzeInterPoint(Collect.asList(buyBid), Collect.asList(sellBid));
        Assertions.assertNotNull(point);
        Assertions.assertEquals(point, new Point<>(100D, 100D));

        Bid buyBid0 = Bid.builder().direction(Direction.BUY).quantity(100D).price(400D).build();
        Bid buyBid1 = Bid.builder().direction(Direction.BUY).quantity(100D).price(300D).build();
        Bid buyBid2 = Bid.builder().direction(Direction.BUY).quantity(100D).price(200D).build();

        Bid sellBid0 = Bid.builder().direction(Direction.SELL).quantity(100D).price(400D).build();
        Bid sellBid1 = Bid.builder().direction(Direction.SELL).quantity(100D).price(300D).build();
        Bid sellBid2 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();
        Bid sellBid3 = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();

        point = ClearUtil.analyzeInterPoint(
                Collect.asList(buyBid0, buyBid1, buyBid2).stream().sorted(Comparator.comparing(Bid::getPrice).reversed()).collect(Collectors.toList()),
                Collect.asList(sellBid0, sellBid1, sellBid2, sellBid3).stream().sorted(Comparator.comparing(Bid::getPrice)).collect(Collectors.toList()));
        Assertions.assertNotNull(point);
        Assertions.assertEquals(point, new Point<>(200D, 250D));

        buyBid0 = Bid.builder().direction(Direction.BUY).quantity(100D).price(300D).build();
        buyBid1 = Bid.builder().direction(Direction.BUY).quantity(100D).price(150D).build();

        sellBid0 = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();
        sellBid1 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();

        point = ClearUtil.analyzeInterPoint(
                Collect.asList(buyBid0, buyBid1).stream().sorted(Comparator.comparing(Bid::getPrice).reversed()).collect(Collectors.toList()),
                Collect.asList(sellBid0, sellBid1).stream().sorted(Comparator.comparing(Bid::getPrice)).collect(Collectors.toList()));
        Assertions.assertNotNull(point);
        Assertions.assertEquals(point, new Point<>(100D, 175D));


        buyBid0 = Bid.builder().direction(Direction.BUY).quantity(150D).price(300D).build();
        buyBid1 = Bid.builder().direction(Direction.BUY).quantity(150D).price(100D).build();

        sellBid0 = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();
        sellBid1 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();

        point = ClearUtil.analyzeInterPoint(
                Collect.asList(buyBid0, buyBid1).stream().sorted(Comparator.comparing(Bid::getPrice).reversed()).collect(Collectors.toList()),
                Collect.asList(sellBid0, sellBid1).stream().sorted(Comparator.comparing(Bid::getPrice)).collect(Collectors.toList()));
        Assertions.assertNotNull(point);
        Assertions.assertEquals(point, new Point<>(150D, 200D));

        buyBid0 = Bid.builder().direction(Direction.BUY).quantity(150D).price(300D).build();
        buyBid1 = Bid.builder().direction(Direction.BUY).quantity(150D).price(100D).build();

        sellBid0 = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();
        sellBid1 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();

        point = ClearUtil.analyzeInterPoint(
                Collect.asList(buyBid0, buyBid1).stream().sorted(Comparator.comparing(Bid::getPrice).reversed()).collect(Collectors.toList()),
                Collect.asList(sellBid0, sellBid1).stream().sorted(Comparator.comparing(Bid::getPrice)).collect(Collectors.toList()));
        Assertions.assertNotNull(point);
        Assertions.assertEquals(point, new Point<>(150D, 200D));


        buyBid0 = Bid.builder().direction(Direction.BUY).quantity(150D).price(300D).build();
        buyBid1 = Bid.builder().direction(Direction.BUY).quantity(150D).price(200D).build();

        sellBid0 = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();
        sellBid1 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();

        point = ClearUtil.analyzeInterPoint(
                Collect.asList(buyBid0, buyBid1).stream().sorted(Comparator.comparing(Bid::getPrice).reversed()).collect(Collectors.toList()),
                Collect.asList(sellBid0, sellBid1).stream().sorted(Comparator.comparing(Bid::getPrice)).collect(Collectors.toList()));
        Assertions.assertNotNull(point);
        Assertions.assertEquals(point, new Point<>(200D, 200D));

        buyBid0 = Bid.builder().direction(Direction.BUY).quantity(150D).price(300D).build();
        buyBid1 = Bid.builder().direction(Direction.BUY).quantity(150D).price(200D).build();

        sellBid0 = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();
        sellBid1 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();
        sellBid2 = Bid.builder().direction(Direction.SELL).quantity(100D).price(300D).build();

        point = ClearUtil.analyzeInterPoint(
                Collect.asList(buyBid0, buyBid1).stream().sorted(Comparator.comparing(Bid::getPrice).reversed()).collect(Collectors.toList()),
                Collect.asList(sellBid0, sellBid1, sellBid2).stream().sorted(Comparator.comparing(Bid::getPrice)).collect(Collectors.toList()));
        Assertions.assertNotNull(point);
        Assertions.assertEquals(point, new Point<>(200D, 200D));
    }


    @Test
    public void testAverageBids() {
        Bid sellBid0 = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();
        Bid sellBid1 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();
        Bid sellBid2 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();
        ClearUtil.deal(Collect.asList(sellBid0, sellBid1, sellBid2), new Point<>(250D, 200D));
        Deal deal0 = sellBid0.getDeals().get(0);
        Assertions.assertEquals(deal0.getQuantity(), sellBid0.getQuantity());
        Deal deal1 = sellBid1.getDeals().get(0);
        Assertions.assertEquals(deal1.getQuantity(), 75D);
        Deal deal2 = sellBid2.getDeals().get(0);
        Assertions.assertEquals(deal2.getQuantity(), 75D);

        sellBid0 = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();
        sellBid1 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();
        sellBid2 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();
        ClearUtil.deal(Collect.asList(sellBid0, sellBid1, sellBid2), new Point<>(50D, 100D));
        deal0 = sellBid0.getDeals().get(0);
        Assertions.assertEquals(deal0.getQuantity(),50);

        sellBid0 = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();
        sellBid1 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();
        sellBid2 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();
        ClearUtil.deal(Collect.asList(sellBid0, sellBid1, sellBid2), new Point<>(100D, 100D));
        deal0 = sellBid0.getDeals().get(0);
        Assertions.assertEquals(deal0.getQuantity(),100);

        sellBid0 = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();
        sellBid1 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();
        sellBid2 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();
        ClearUtil.deal(Collect.asList(sellBid0, sellBid1, sellBid2), new Point<>(200D, 200D));
        deal0 = sellBid0.getDeals().get(0);
        Assertions.assertEquals(deal0.getQuantity(),100);
        deal1 = sellBid1.getDeals().get(0);
        Assertions.assertEquals(deal1.getQuantity(),100);
    }


}
