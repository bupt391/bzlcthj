package com.example.lvcheng.service;

import com.example.lvcheng.dao.LoginTicketMapper;
import com.example.lvcheng.dao.UserMapper;
import com.example.lvcheng.entity.LoginTicket;
import com.example.lvcheng.entity.User;
import com.example.lvcheng.util.LvchengConstant;
import com.example.lvcheng.util.LvchengUtil;
import com.example.lvcheng.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


@Service
public class UserService implements LvchengConstant {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    // 网站域名
    @Value("${community.path.domain}")
    private String domain;

    // 项目名(访问路径)
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private MailClient mailClient;

    public User findUserById(int id){
        return userMapper.selectById(id);
    }

    /**
     * 用户注册
     * @param user
     * @return Map<String, Object> 返回错误提示消息，如果返回的 map 为空，则说明注册成功
     */
    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();

        if (user == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空");
            return map;
        }

        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空");
            return map;
        }

        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空");
            return map;
        }

        // 验证账号是否已存在
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在");
            return map;
        }

        // 验证邮箱是否已存在
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册");
            return map;
        }

        // 注册用户
        user.setSalt(LvchengUtil.generateUUID().substring(0, 5)); // salt
        user.setPassword(LvchengUtil.md5(user.getPassword() + user.getSalt())); // 加盐加密
        user.setType(0); // 默认普通用户
        user.setStatus(0); // 默认未激活
        user.setActivationCode(LvchengUtil.generateUUID()); // 激活码
        // 随机头像（用户登录后可以自行修改）
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date()); // 注册时间
        userMapper.insertUser(user);

        // 给注册用户发送激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/echo/activation/用户id/激活码
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);//将内容传给模板
        mailClient.sendMail(user.getEmail(),"激活 Echo 账号", content);

        return map;
    }

    public int activition(int UserId, String code){
        User user = userMapper.selectById(UserId);
        if(user.getStatus() == 1){
            return ACTIVATION_REPEAT;
        }else if(user.getActivationCode().equals(code)){
            userMapper.updateStatus(UserId,1);
            return ACTIVATION_SUCCESS;
        }else{
            return ACTIVATION_FAILURE;
        }
    }

    /**
     * 用户登录（为用户创建凭证）
     * @param username
     * @param password
     * @param expiredSeconds 多少秒后凭证过期
     * @return Map<String, Object> 返回错误提示消息以及 ticket(凭证)
     */
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空");
            return map;
        }

        // 验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在");
            return map;
        }

        // 验证状态
        if (user.getStatus() == 0) {
            // 账号未激活
            map.put("usernameMsg", "该账号未激活");
            return map;
        }

        // 验证密码
        password = LvchengUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码错误");
            return map;
        }

        // 用户名和密码均正确，为该用户生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(LvchengUtil.generateUUID()); // 随机凭证
        loginTicket.setStatus(0); // 设置凭证状态为有效（当用户登出的时候，设置凭证状态为无效）
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000)); // 设置凭证到期时间

        loginTicketMapper.insertLoginTicket(loginTicket);
//        // 将登录凭证存入 redis
//        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
//        redisTemplate.opsForValue().set(redisKey, loginTicket);

        map.put("ticket", loginTicket.getTicket());

        return map;
    }

    public void logout(String ticket){
        loginTicketMapper.updateStatus(ticket,1);
    }

    /*
    * 通过ticket查找用户
    * */
    public LoginTicket findLoginTicket(String ticket){
        return loginTicketMapper.selectByTicket(ticket);
    }

    public int updateHeader(int userId, String headerUrl){
        return userMapper.updateHeader(userId,headerUrl);
    }

    /**
     * 根据 username 查询用户
     * @param username
     * @return
     */
    public User findUserByName(String username) {
        return userMapper.selectByName(username);
    }



}