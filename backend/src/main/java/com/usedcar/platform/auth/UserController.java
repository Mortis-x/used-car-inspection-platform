package com.usedcar.platform.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usedcar.platform.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 按角色查询可用人员，用于检测单指派检测师等下拉框。
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> byRole(@RequestParam String role) {
        List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .eq(User::getRole, role)
                .eq(User::getStatus, 1)
                .orderByAsc(User::getId));
        List<Map<String, Object>> result = users.stream().map(user -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", user.getId());
            item.put("username", user.getUsername());
            item.put("realName", user.getRealName());
            item.put("orgName", user.getOrgName());
            return item;
        }).toList();
        return ApiResponse.ok(result);
    }
}
