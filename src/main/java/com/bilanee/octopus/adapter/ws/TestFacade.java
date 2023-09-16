package com.bilanee.octopus.adapter.ws;

import com.bilanee.octopus.basic.ErrorEnums;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.common.Kit;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试相关
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class TestFacade {

    @GetMapping("/ws/push")
    public Result<Void> hello(String wsTopic, String body) {
        WsTopic topic = Kit.enumOf(WsTopic::name, wsTopic).orElse(null);
        BizEx.nullThrow(wsTopic, ErrorEnums.PARAM_IS_NULL.message(wsTopic +"不是有效的Topic"));
        WsHandler.cast(WsMessage.builder().wsTopic(topic).body(body).build());
        return Result.success();
    }
}
