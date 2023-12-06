package com.bilanee.octopus.adapter.ws;

import com.bilanee.octopus.basic.ErrorEnums;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.common.Kit;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

/**
 * 测试WebSocket接口
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class TestFacade {

    /**
     * 推送测试接口
     * @param wsTopic 推送topic, 可选值STAGE_ID, AN_INTRA_BID, MO_INTRA_BID
     * @param body 推送内容
     */
    @GetMapping("/ws/push")
    public Result<Void> testWebSocket(@NotBlank String wsTopic, String body) {
        WsTopic topic = Kit.enumOf(WsTopic::name, wsTopic).orElse(null);
        BizEx.nullThrow(wsTopic, ErrorEnums.PARAM_IS_NULL.message(wsTopic +"不是有效的Topic"));
        WebSocket.cast(WsMessage.builder().wsTopic(topic).body(body).build());
        return Result.success();
    }
}
