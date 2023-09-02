package com.bilanee.octopus.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bilanee.octopus.basic.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.util.Json;
import com.stellariver.milky.infrastructure.base.ErrorEnums;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import javax.annotation.Nullable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("octopus_meta_unit_do")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MetaUnitDO {

    Integer metaUnitId;
    Integer nodeId;
    String name;
    Province province;
    UnitType unitType;
    @Nullable
    GeneratorType generatorType;
    Integer sourceId;
    @TableField(typeHandler = Handler.class)
    Map<TimeFrame, Map<Direction, Double>> capacity;

    static public class Handler extends BaseTypeHandler<Map<TimeFrame, Map<Direction, Double>>> {

        @Override
        public void setNonNullParameter(PreparedStatement ps, int i, Map<TimeFrame, Map<Direction, Double>> parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setString(i, Json.toJson(parameter));
        }

        @Override
        public Map<TimeFrame, Map<Direction, Double>> getNullableResult(ResultSet rs, String columnName)
                throws SQLException {
            String result = rs.getString(columnName);
            return rs.wasNull() ? null : Json.parse(result, new TypeReference<Map<TimeFrame, Map<Direction, Double>>>() {
            });
        }

        @Override
        public Map<TimeFrame, Map<Direction, Double>> getNullableResult(ResultSet rs, int columnIndex)
                throws SQLException {
            String result = rs.getString(columnIndex);
            return rs.wasNull() ? null : Json.parse(result, new TypeReference<Map<TimeFrame, Map<Direction, Double>>>() {
            });
        }

        @Override
        public Map<TimeFrame, Map<Direction, Double>> getNullableResult(CallableStatement cs, int columnIndex)
                throws SQLException {
            String result = cs.getString(columnIndex);
            return cs.wasNull() ? null : Json.parse(result, new TypeReference<Map<TimeFrame, Map<Direction, Double>>>() {
            });
        }

    }

}
