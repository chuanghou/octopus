package com.bilanee.octopus.infrastructure.entity;

import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.fasterxml.jackson.core.type.TypeReference;
import com.stellariver.milky.common.tool.util.Json;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class RollBiddenHandler extends BaseTypeHandler<Map<Integer, Boolean>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,Map<Integer, Boolean> parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, Json.toJson(parameter));
    }

    @Override
    public Map<Integer, Boolean> getNullableResult(ResultSet rs, String columnName)
            throws SQLException {
        String result = rs.getString(columnName);
        return rs.wasNull() ? null : Json.parse(result, new TypeReference<Map<Integer, Boolean>>() {
        });
    }

    @Override
    public Map<Integer, Boolean> getNullableResult(ResultSet rs, int columnIndex)
            throws SQLException {
        String result = rs.getString(columnIndex);
        return rs.wasNull() ? null : Json.parse(result, new TypeReference<Map<Integer, Boolean>>() {
        });
    }

    @Override
    public Map<Integer, Boolean> getNullableResult(CallableStatement cs, int columnIndex)
            throws SQLException {
        String result = cs.getString(columnIndex);
        return cs.wasNull() ? null : Json.parse(result, new TypeReference<Map<Integer, Boolean>>() {
        });
    }

}
