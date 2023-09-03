package com.bilanee.octopus.adapter.tunnel;

import com.stellariver.milky.common.tool.util.Collect;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class AssignUtils {

    static Map<Integer, Pair<Integer, Integer>> roundOneMap = Collect.asMap(
            1, Pair.of(1, 6),
            2, Pair.of(2, 4),
            3, Pair.of(3, 5)
    );

    static Map<Integer, Pair<Integer, Integer>> roundTwoMap = Collect.asMap(
            1, Pair.of(2, 4),
            2, Pair.of(3, 5),
            3, Pair.of(1, 6)
    );

    static Map<Integer, Pair<Integer, Integer>> roundThreeMap = Collect.asMap(
            1, Pair.of(3, 5),
            2, Pair.of(1, 6),
            3, Pair.of(2, 4)
    );

    static Map<Integer, Map<Integer, Pair<Integer, Integer>>> alloacteMap = Collect.asMap(
            1, roundOneMap,
            2, roundTwoMap,
            3, roundThreeMap
    );

    private static Pair<Integer, Integer> allocate(Integer roundId, Integer userId, Integer userCount, Integer unitCount) {
        int groupMemberCount = (userCount / 3) + (((userCount % 3) == 0) ? 0 : 1);
        Map<Integer, Pair<Integer, Integer>> integerPairMap = alloacteMap.get(roundId);
        int groupNumber = userId / groupMemberCount + (((userId % groupMemberCount) == 0) ? 0 : 1);
        Pair<Integer, Integer> pair = integerPairMap.get(groupNumber);
        int i = ((userId - 1) % groupMemberCount) + 1;
        int k = unitCount / 6;
        return Pair.of((pair.getLeft() - 1) * k + i, (pair.getRight() - 1) * k + i);
    }

    // 方圣哲给的原始分配方案，相对冗杂，且id不是从0开始，这个函数计算了分配方案，并将被分配的MetaUnit的sourceId计算出来了
    static List<Map<Integer, List<Integer>>> assignSourceId(Integer roundTotal, Integer userIdTotal, Integer sourceIdTotal) {
        List<Map<Integer, List<Integer>>> result = new ArrayList<>();
        IntStream.range(1, roundTotal + 1).forEach(roundId -> {
            Map<Integer, List<Integer>> allocate = new HashMap<>();
            IntStream.range(1, userIdTotal + 1).forEach(userId -> {
                Pair<Integer, Integer> allocateIds = allocate(roundId, userId, userIdTotal, sourceIdTotal);
                List<Integer> list = Collect.asList(allocateIds.getLeft(), allocateIds.getRight());
                allocate.put(userId - 1, list);
            });
            result.add(allocate);
        });
        return result;
    }

    static public List<Integer> assignSourceId(Integer roundId,
                                               Integer userTotal,
                                               Integer sourceIdTotal,
                                               Integer positionId) {
        Pair<Integer, Integer> allocateIds = allocate(roundId + 1, positionId + 1, userTotal, sourceIdTotal);
        return  Collect.asList(allocateIds.getLeft(), allocateIds.getRight());

    }


}
