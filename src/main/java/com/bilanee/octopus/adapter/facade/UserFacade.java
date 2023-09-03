package com.bilanee.octopus.adapter.facade;

import com.bilanee.octopus.adapter.facade.po.UserEditPO;
import com.bilanee.octopus.adapter.facade.vo.UserVO;
import com.bilanee.octopus.basic.ErrorEnums;
import com.bilanee.octopus.basic.TokenUtils;
import com.bilanee.octopus.infrastructure.entity.UserDO;
import com.bilanee.octopus.infrastructure.mapper.UserDOMapper;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.common.Kit;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequestMapping("/api/user")
public class UserFacade {

    final UserDOMapper userDOMapper;

    @GetMapping("login")
    public Result<String> login(String userId, String password) {
        UserDO userDO = userDOMapper.selectById(userId);
        if (userDO == null) {
            throw new BizEx(ErrorEnums.ACCOUNT_NOT_EXISTED);
        }
        if (Kit.notEq(userDO.getPassword(), password)) {
            throw new BizEx(ErrorEnums.PASSWORD_ERROR);
        }
        return Result.success(TokenUtils.sign(userId));
    }

    @PostMapping("edit")
    public Result<String> edit(@RequestBody UserEditPO userEditPO, @RequestHeader String token) {
        String userId = TokenUtils.getUserId(token);
        UserDO userDO = userDOMapper.selectById(userId);
        userDO.setPassword(userEditPO.getPassword());
        userDO.setUserName(userEditPO.getUserName());
        return Result.success(TokenUtils.sign(userId));
    }

    @GetMapping("getUser")
    public Result<UserVO> getUser(@RequestHeader String token) {
        String userId = TokenUtils.getUserId(token);
        UserDO userDO = userDOMapper.selectById(userId);
        UserVO userVO = new UserVO(userDO.getUserId(), userDO.getUserName(), userDO.getPortrait());
        return Result.success(userVO);
    }

}
