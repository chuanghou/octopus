package com.bilanee.octopus.adapter.facade;

import com.bilanee.octopus.adapter.facade.po.LoginPO;
import com.bilanee.octopus.adapter.facade.po.UserEditPO;
import com.bilanee.octopus.adapter.facade.vo.UserVO;
import com.bilanee.octopus.basic.ErrorEnums;
import com.bilanee.octopus.basic.TokenUtils;
import com.bilanee.octopus.basic.enums.UserType;
import com.bilanee.octopus.infrastructure.entity.AdminDO;
import com.bilanee.octopus.infrastructure.entity.UserDO;
import com.bilanee.octopus.infrastructure.mapper.AdminDOMapper;
import com.bilanee.octopus.infrastructure.mapper.UserDOMapper;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.common.Kit;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 用户身份
 */

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequestMapping("/api/user")
public class UserFacade {

    final UserDOMapper userDOMapper;
    final AdminDOMapper adminDOMapper;

    /**
     * 普通用户登录接口，返回错误码为 NOT_LOGIN 时，路由到登录
     * @see ErrorEnums
     * @param loginPO 登录账号和密码
     * @return 用户token
     */
    @PostMapping("login")
    public Result<String> login(@RequestBody LoginPO loginPO) {
        UserDO userDO = userDOMapper.selectById(loginPO.getUserId());
        if (userDO == null) {
            throw new BizEx(ErrorEnums.ACCOUNT_NOT_EXISTED);
        }
        if (Kit.notEq(userDO.getPassword(), loginPO.getPassword())) {
            throw new BizEx(ErrorEnums.PASSWORD_ERROR);
        }
        if (Kit.eq(userDO.getUserType(), UserType.ROBOT)) {
            throw new BizEx(ErrorEnums.PARAM_FORMAT_WRONG.message("机器人不允许登陆"));
        }
        return Result.success(TokenUtils.sign(loginPO.getUserId()));
    }


    /**
     * 管理员用户登录接口，返回错误码为 NOT_LOGIN 时，路由到登录
     * @see ErrorEnums
     * @param loginPO 登录账号和密码
     * @return 用户token
     */
    @PostMapping("adminLogin")
    public Result<String> adminLogin(@RequestBody LoginPO loginPO) {
        AdminDO adminDO = adminDOMapper.selectById(loginPO.getUserId());
        if (adminDO == null) {
            throw new BizEx(ErrorEnums.ACCOUNT_NOT_EXISTED);
        }
        if (Kit.notEq(adminDO.getPassword(), loginPO.getPassword())) {
            throw new BizEx(ErrorEnums.PASSWORD_ERROR);
        }
        return Result.success(TokenUtils.sign(loginPO.getUserId()));
    }


    /**
     * 用户信息编辑接口
     * @param userEditPO 用户编辑信息
     */
    @PostMapping("edit")
    public Result<Void> edit(@RequestBody UserEditPO userEditPO, @RequestHeader String token) {
        String userId = TokenUtils.getUserId(token);
        UserDO userDO = userDOMapper.selectById(userId);
        if (StringUtils.isNotBlank(userEditPO.getUserName())) {
            userDO.setUserName(userEditPO.getUserName());
        }
        if (StringUtils.isNotBlank(userEditPO.getPassword())) {
            userDO.setPassword(userEditPO.getPassword());
        }
        userDOMapper.updateById(userDO);
        return Result.success();
    }

    /**
     * 获取用户信息
     */
    @GetMapping("getUser")
    public Result<UserVO> getUser(@RequestHeader String token) {
        String userId = TokenUtils.getUserId(token);
        UserDO userDO = userDOMapper.selectById(userId);
        UserVO userVO = new UserVO(userDO.getUserId(), userDO.getUserName(), userDO.getPortrait(), userDO.getPassword());
        return Result.success(userVO);
    }

}
