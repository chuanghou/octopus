package com.bilanee.octopus.adapter.facade;

import com.stellariver.milky.common.base.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class TestFacade {

    @GetMapping("hello")
    public Result<String> hello() {
        return Result.success("Hello");
    }
}
