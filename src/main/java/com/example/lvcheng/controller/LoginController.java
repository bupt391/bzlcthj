package com.example.lvcheng.controller;

import com.example.lvcheng.entity.User;
import com.example.lvcheng.service.UserService;
import com.example.lvcheng.util.LvchengConstant;
import com.example.lvcheng.util.LvchengUtil;
import com.example.lvcheng.util.RedisKeyUtil;
import com.google.code.kaptcha.Producer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Controller
public class LoginController implements LvchengConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage(){
        return "site/register";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage(){
        return "site/login";
    }

    /**
     * 注册用户
     * @param model
     * @param user
     * @return
     */
    @PostMapping("/register")
    public String register(Model model, User user) {
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "注册成功, 我们已经向您的邮箱发送了一封激活邮件，请尽快激活!");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }

    @RequestMapping(path = "/activition/{UserId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code){

        System.out.println(code);
        int result = userService.activition(userId,code);
        System.out.println(result);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功, 您的账号已经可以正常使用!");
            model.addAttribute("target", "/login");
        }
        else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效的操作, 您的账号已被激活过!");
            model.addAttribute("target", "/index");
        }
        else {
            model.addAttribute("msg", "激活失败, 您提供的激活码不正确!");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    /*
    * 生成验证码，并存入Redis
    * @param response
    * */
    @RequestMapping(path = "/kaptcha", method=RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session) {
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);
        //將驗證碼存入Session
//        session.setAttribute("kaptcha", text);

        // 验证码的归属者
        String kaptchaOwner = LvchengUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        // 将验证码存入 redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);

        //將圖片輸出給瀏覽器
        response.setContentType("image/png'");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败", e.getMessage());
        }
    }

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @param code 验证码
     * @param rememberMe 是否记住我（点击记住我后，凭证的有效期延长）
     * @param model
     * @param response
     * @return
     */
    @RequestMapping(path= "/login", method = RequestMethod.POST)
    public String login(String username,
                        String password,
                        String code,
                        boolean rememberMe,
                        Model model,
                        HttpServletResponse response,
                        @CookieValue("kaptchaOwner") String kaptchaOwner) {
        // 检查验证码
        String kaptcha = null;
        if (StringUtils.isNotBlank(kaptchaOwner)) {
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码错误");
            return "/site/login";
        }

        // 凭证过期时间（是否记住我）
        int expiredSeconds = rememberMe ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        // 验证用户名和密码
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) {
            // 账号和密码均正确，则服务端会生成 ticket，浏览器通过 cookie 存储 ticket
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath); // cookie 有效范围
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            //重定向首页
            return "redirect:/index";
        }
        else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }

    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        return "redirect:/login";
    }







}
