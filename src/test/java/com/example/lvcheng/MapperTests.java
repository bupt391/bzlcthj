package com.example.lvcheng;

import com.example.lvcheng.dao.DiscussPostMapper;
import com.example.lvcheng.dao.UserMapper;
import com.example.lvcheng.entity.DiscussPost;
import com.example.lvcheng.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@ContextConfiguration(classes = LvchengApplication.class)
public class MapperTests {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    public void testSelectUsers(){
        User user = userMapper.selectById(1);
        System.out.println(user);
    }

    @Test
    public void testSelectDiscusspost(){
        List<DiscussPost> list = discussPostMapper.selectDiscussPosts(0,0,10);
        for(DiscussPost discussPost: list){
            System.out.println(discussPost);
        }

        int rows = discussPostMapper.selectDiscussPostRows(0);
        System.out.println(rows);
    }


}
