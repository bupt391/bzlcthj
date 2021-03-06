package com.example.lvcheng.controller;


import com.example.lvcheng.annotation.LoginRequired;
import com.example.lvcheng.entity.User;
import com.example.lvcheng.service.FollowService;
import com.example.lvcheng.service.LikeService;
import com.example.lvcheng.service.UserService;
import com.example.lvcheng.util.HostHolder;
import com.example.lvcheng.util.LvchengConstant;
import com.example.lvcheng.util.LvchengUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.coyote.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping("/user")
public class UserController implements LvchengConstant{
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${lvcheng.path.upload}")
    private String uploadPath;

    @Value("${lvcheng.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(){
        return "/site/setting";
    }

    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model){
        if(headerImage == null){
            model.addAttribute("error" , "您还没有选择图片！");
            return "/site/setting";
        }
        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error", "文件格式不正确！");
            return "/site/setting";
        }

        //生成随机文件名
        fileName = LvchengUtil.generateUUID() + suffix;
        //确定文件存放路径
        File dest = new File(uploadPath + "/" + fileName);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败:" + e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常！", e);
        }

        //更新当前用户头像的路径(web访问路径)
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";

    }

    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response){
        //服务器存放路径
        fileName = uploadPath + "/" + fileName;
        //文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        //响应图片
        response.setContentType("image/" + suffix);
        try (
                FileInputStream fis = new FileInputStream(fileName);
                OutputStream os = response.getOutputStream();
        ){
            byte[] buffer = new byte[1024];
            int b = 0;
            while((b = fis.read(buffer)) != -1){
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败:" + e.getMessage());
        }
    }


    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model){
        User user = userService.findUserById(userId);
        if(user == null){
            throw new RuntimeException("该用户不存在1");
        }
            model.addAttribute("user", user);
            int likeCount = likeService.findUserLikeCount(userId);
            model.addAttribute("likeCount", likeCount);

            long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
            model.addAttribute("followeeCount", followeeCount);

            long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
            model.addAttribute("followerCount" , followerCount);

            // 当前登录用户是否已关注该用户
            boolean hasFollowed = false;
            if (hostHolder.getUser() != null) {
                hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
            }
            model.addAttribute("hasFollowed", hasFollowed);
            model.addAttribute("tab", "profile"); // 该字段用于指示标签栏高亮

            return "/site/profile";


    }


}