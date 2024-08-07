package com.bilanee.octopus.config;

import com.alibaba.druid.filter.FilterChain;
import com.alibaba.druid.filter.FilterEventAdapter;
import com.alibaba.druid.proxy.jdbc.PreparedStatementProxy;
import com.alibaba.druid.proxy.jdbc.ResultSetProxy;
import com.alibaba.druid.proxy.jdbc.StatementProxy;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.function.Supplier;

@CustomLog
@NoArgsConstructor
public class MilkyLogFilter extends FilterEventAdapter {

    private Long sqlCost = 3000L;
    private Long sqlCount = 1000L;

    public MilkyLogFilter(long sqlCost, long sqlCount) {
        this.sqlCost = sqlCost;
        this.sqlCount = sqlCount;
    }

    private final ThreadLocal<Pair<ResultSetProxy, Long>> recordCounter = ThreadLocal.withInitial(() -> Pair.of(null, 0L));

    @Override
    protected void statementExecuteAfter(StatementProxy statement, String sql, boolean result) {
        if (enable.get()) {
            return;
        }
        print(statement, sql);
    }

    @Override
    protected void statementExecuteBatchAfter(StatementProxy statement, int[] result) {
        if (enable.get()) {
            return;
        }
        String sql;
        if (statement instanceof PreparedStatementProxy) {
            sql = ((PreparedStatementProxy) statement).getSql();
        } else {
            sql = statement.getBatchSql();
        }
        print(statement, sql);
    }

    @Override
    protected void statementExecuteQueryAfter(StatementProxy statement, String sql, ResultSetProxy resultSet)  {
        if (enable.get()) {
            return;
        }
        print(statement, sql);
    }

    @Override
    protected void statementExecuteUpdateAfter(StatementProxy statement, String sql, int updateCount) {
        if (enable.get()) {
            return;
        }
        print(statement, sql);
    }

    @SneakyThrows
    public void print(StatementProxy statement, String sql) {

        sql = DruidUtils.resolveSql(statement, sql);

        statement.setLastExecuteTimeNano();
        double nanos = statement.getLastExecuteTimeNano();
        int updateCount = Math.max(0, statement.getUpdateCount());
        sql = sql + " ==>> " + "[affected: " + updateCount + "]";
        double cost = nanos / 1000_000L;
        if (cost > sqlCost) {
            log.cost(cost).error(sql);
        } else {
            log.cost(cost).info(sql);
        }

    }

    private static final ThreadLocal<Boolean> enable = ThreadLocal.withInitial(() -> false);

    public static <T> T byPass(Supplier<T> supplier) {
        enable.set(true);
        try {
            return supplier.get();
        } finally {
            enable.set(false);
        }
    }

    @Override
    public boolean resultSet_next(FilterChain chain, ResultSetProxy resultSet) throws SQLException {

        boolean moreRows = super.resultSet_next(chain, resultSet);
        if (moreRows && (!enable.get())) {

            Pair<ResultSetProxy, Long> proxyCounter = recordCounter.get();
            if (proxyCounter.getKey() == null || proxyCounter.getKey() != resultSet) {
                recordCounter.set(Pair.of(resultSet, 1L));
            } else {
                recordCounter.set(Pair.of(proxyCounter.getLeft(), proxyCounter.getRight() + 1));
            }

            StringBuilder builder = new StringBuilder();
            builder.append("record: [");
            ResultSetMetaData meta = resultSet.getMetaData();
            for (int i = 0, size = meta.getColumnCount(); i < size; ++i) {
                if (i != 0) {
                    builder.append(", ");
                }
                int columnIndex = i + 1;
                int type = meta.getColumnType(columnIndex);

                Object value;
                if (type == Types.TIMESTAMP) {
                    value = resultSet.getTimestamp(columnIndex);
                } else if (type == Types.BLOB) {
                    value = "<BLOB>";
                } else if (type == Types.CLOB) {
                    value = "<CLOB>";
                } else if (type == Types.NCLOB) {
                    value = "<NCLOB>";
                } else if (type == Types.BINARY) {
                    value = "<BINARY>";
                } else {
                    value = resultSet.getObject(columnIndex);
                }
                builder.append(String.format("%s:%s", meta.getColumnName(columnIndex).toLowerCase(), value));
            }
            builder.append("]");
            Long countResult = recordCounter.get().getRight();
            if (recordCounter.get().getRight() > sqlCount) {
                log.arg0(countResult).error(builder.toString());
            } else {
                log.arg0(countResult).info(builder.toString());
            }
        }

        return moreRows;
    }

}
