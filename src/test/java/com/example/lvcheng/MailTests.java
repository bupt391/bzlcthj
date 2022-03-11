package com.example.lvcheng;

import com.example.lvcheng.util.MailClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = LvchengApplication.class)
public class MailTests {
    @Autowired
    private MailClient mailClient;

    @Test
    public void testTextMail(){
        mailClient.sendMail("nuaa36103@163.com", "TEST", "Welcome!");
    }
}
