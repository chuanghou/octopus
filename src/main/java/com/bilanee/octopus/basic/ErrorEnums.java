package com.bilanee.octopus.basic;

import com.stellariver.milky.common.base.ErrorEnum;
import com.stellariver.milky.common.base.ErrorEnumsBase;
import com.stellariver.milky.common.base.Message;
import com.stellariver.milky.common.tool.common.Kit;

import java.lang.reflect.Field;

/**
 * @author houchuang
 */
public class ErrorEnums extends ErrorEnumsBase {


    @Message("未登录")
    public static ErrorEnum NOT_LOGIN;

    @Message("密码错误")
    public static ErrorEnum PASSWORD_ERROR;

    @Message("用户不存在")
    public static ErrorEnum ACCOUNT_NOT_EXISTED;

    static {
        for (Field field : ErrorEnums.class.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object o = field.get(null);
                if (o != null) {
                    continue;
                }
            } catch (Throwable ignore) {}

            String code = field.getName();
            String message = Kit.op(field.getAnnotation(Message.class)).map(Message::value).filter(s -> !s.isEmpty()).orElse("系统累趴下啦！请稍后再试");
            String prefix = Kit.op(field.getAnnotation(Message.class)).map(Message::prefix).orElse("");
            String p = prefix.isEmpty() ? prefix : prefix + ": ";
            ErrorEnum errorEnum = new ErrorEnum(code, p + message);
            try {
                field.set(null, errorEnum);
            } catch (Throwable ignore) {}
        }
    }

}
