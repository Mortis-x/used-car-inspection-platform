package com.usedcar.platform.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.usedcar.platform.vehicle.Vehicle;
import com.usedcar.platform.vehicle.VehicleMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 首次启动时初始化演示账号与两台示例车辆，密码统一为 123456。
 * 生产环境应移除演示数据，改为后台账号管理。
 */
@Component
public class DemoDataInitializer implements ApplicationRunner {

    private static final String DEFAULT_PASSWORD = "123456";

    private final UserMapper userMapper;
    private final VehicleMapper vehicleMapper;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(UserMapper userMapper, VehicleMapper vehicleMapper,
                               PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.vehicleMapper = vehicleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userMapper.selectCount(null) == 0) {
            create("admin", "系统管理员", Role.ADMIN, "平台运维");
            create("operator", "王运营", Role.OPERATOR, "平台运营中心");
            create("inspector", "张工", Role.INSPECTOR, "星锐检测站");
            create("inspector2", "李工", Role.INSPECTOR, "星锐检测站");
            create("dealer", "宏达车行", Role.DEALER, "宏达二手车");
            create("buyer", "陈先生", Role.BUYER, null);
            create("arbitrator", "赵仲裁", Role.ARBITRATOR, "平台纠纷仲裁组");
        }

        User dealer = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, "dealer"));
        if (dealer != null) {
            seedVehicles(dealer.getId());
        }
    }

    private User create(String username, String realName, Role role, String orgName) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setRealName(realName);
        user.setRole(role.name());
        user.setOrgName(orgName);
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    private void seedVehicles(Long dealerId) {
        if (vehicleMapper.selectCount(null) > 0) {
            return;
        }
        Vehicle passat = new Vehicle();
        passat.setVin("LSGKB54E0BA123456");
        passat.setPlateNo("沪A12345");
        passat.setBrand("大众");
        passat.setModel("帕萨特 2020款 330TSI");
        passat.setColor("黑色");
        passat.setFirstRegDate(LocalDate.of(2020, 6, 15));
        passat.setMileage(68000);
        passat.setTransferCount(1);
        passat.setDealerId(dealerId);
        vehicleMapper.insert(passat);

        Vehicle accord = new Vehicle();
        accord.setVin("LHGCM82633A654321");
        accord.setPlateNo("沪B67890");
        accord.setBrand("本田");
        accord.setModel("雅阁 2019款 260TURBO");
        accord.setColor("白色");
        accord.setFirstRegDate(LocalDate.of(2019, 9, 20));
        accord.setMileage(92000);
        accord.setTransferCount(2);
        accord.setDealerId(dealerId);
        vehicleMapper.insert(accord);
    }
}
