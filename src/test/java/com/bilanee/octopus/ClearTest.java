package com.bilanee.octopus;

import com.bilanee.octopus.basic.Bid;
import com.bilanee.octopus.basic.Direction;
import com.bilanee.octopus.basic.Point;
import com.bilanee.octopus.domain.ClearUtil;
import com.stellariver.milky.common.tool.util.Collect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

public class ClearTest {

    @Test
    public void interPointTest() {

        Point<Double> point = ClearUtil.resolveInterPoint(new ArrayList<>(), new ArrayList<>());
        Assertions.assertNull(point);

        Bid buyBid = Bid.builder().direction(Direction.BUY).quantity(100D).price(100D).build();
        point = ClearUtil.resolveInterPoint(Collect.asList(buyBid), new ArrayList<>());
        Assertions.assertNull(point);

        Bid sellBid = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();
        point = ClearUtil.resolveInterPoint(Collect.asList(buyBid), Collect.asList(sellBid));
        Assertions.assertNotNull(point);
        Assertions.assertEquals(point, new Point<>(0D, 150D));

        sellBid = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();
        point = ClearUtil.resolveInterPoint(Collect.asList(buyBid), Collect.asList(sellBid));
        Assertions.assertNotNull(point);
        Assertions.assertEquals(point, new Point<>(100D, 100D));

        Bid buyBid0 = Bid.builder().direction(Direction.BUY).quantity(100D).price(400D).build();
        Bid buyBid1 = Bid.builder().direction(Direction.BUY).quantity(100D).price(300D).build();
        Bid buyBid2 = Bid.builder().direction(Direction.BUY).quantity(100D).price(200D).build();

        Bid sellBid0 = Bid.builder().direction(Direction.SELL).quantity(100D).price(400D).build();
        Bid sellBid1 = Bid.builder().direction(Direction.SELL).quantity(100D).price(300D).build();
        Bid sellBid2 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();
        Bid sellBid3 = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();

        point = ClearUtil.resolveInterPoint(
                Collect.asList(buyBid0, buyBid1, buyBid2),
                Collect.asList(sellBid0, sellBid1, sellBid2, sellBid3));
        Assertions.assertNotNull(point);
        Assertions.assertEquals(point, new Point<>(200D, 250D));

        buyBid0 = Bid.builder().direction(Direction.BUY).quantity(100D).price(300D).build();
        buyBid1 = Bid.builder().direction(Direction.BUY).quantity(100D).price(150D).build();

        sellBid0 = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();
        sellBid1 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();

        point = ClearUtil.resolveInterPoint(
                Collect.asList(buyBid0, buyBid1),
                Collect.asList(sellBid0, sellBid1));
        Assertions.assertNotNull(point);
        Assertions.assertEquals(point, new Point<>(100D, 175D));


        buyBid0 = Bid.builder().direction(Direction.BUY).quantity(150D).price(300D).build();
        buyBid1 = Bid.builder().direction(Direction.BUY).quantity(150D).price(100D).build();

        sellBid0 = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();
        sellBid1 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();

        point = ClearUtil.resolveInterPoint(
                Collect.asList(buyBid0, buyBid1),
                Collect.asList(sellBid0, sellBid1));
        Assertions.assertNotNull(point);
        Assertions.assertEquals(point, new Point<>(150D, 200D));

        buyBid0 = Bid.builder().direction(Direction.BUY).quantity(150D).price(300D).build();
        buyBid1 = Bid.builder().direction(Direction.BUY).quantity(150D).price(100D).build();

        sellBid0 = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();
        sellBid1 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();

        point = ClearUtil.resolveInterPoint(
                Collect.asList(buyBid0, buyBid1),
                Collect.asList(sellBid0, sellBid1));
        Assertions.assertNotNull(point);
        Assertions.assertEquals(point, new Point<>(150D, 200D));


        buyBid0 = Bid.builder().direction(Direction.BUY).quantity(150D).price(300D).build();
        buyBid1 = Bid.builder().direction(Direction.BUY).quantity(150D).price(200D).build();

        sellBid0 = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();
        sellBid1 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();

        point = ClearUtil.resolveInterPoint(
                Collect.asList(buyBid0, buyBid1),
                Collect.asList(sellBid0, sellBid1));
        Assertions.assertNotNull(point);
        Assertions.assertEquals(point, new Point<>(200D, 200D));

        buyBid0 = Bid.builder().direction(Direction.BUY).quantity(150D).price(300D).build();
        buyBid1 = Bid.builder().direction(Direction.BUY).quantity(150D).price(200D).build();

        sellBid0 = Bid.builder().direction(Direction.SELL).quantity(100D).price(100D).build();
        sellBid1 = Bid.builder().direction(Direction.SELL).quantity(100D).price(200D).build();
        sellBid2 = Bid.builder().direction(Direction.SELL).quantity(100D).price(300D).build();

        point = ClearUtil.resolveInterPoint(
                Collect.asList(buyBid0, buyBid1),
                Collect.asList(sellBid0, sellBid1, sellBid2));
        Assertions.assertNotNull(point);
        Assertions.assertEquals(point, new Point<>(200D, 200D));
    }


}
