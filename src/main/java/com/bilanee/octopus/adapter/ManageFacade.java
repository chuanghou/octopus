package com.bilanee.octopus.adapter;

import com.stellariver.milky.common.base.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("manage")
public class ManageFacade {

    @PostMapping("createComp")
    public Result<Void> createComp() {

        return Result.success();
    }



}
