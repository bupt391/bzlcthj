package com.example.lvcheng.aspect;


import org.aspectj.lang.annotation.*;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
//
//@Component
//@Aspect
public class AlphaAspect {

    @Pointcut("execution(* com.example.lvcheng..servcie.*.*(..))")
    public void pointcut(){

    }


    @Before("pointcut()")
    public void before(){
        System.out.println("before");
    }


    @After("pointcut()")
    public void after(){
        System.out.println("after");
    }


    @AfterReturning("pointcut()")
    public void afterReturning(){
        System.out.println("afterReturning");
    }


    @AfterThrowing("pointcut()")
    public void afterThrowing(){
        System.out.println("afterThrowing");
    }





}
