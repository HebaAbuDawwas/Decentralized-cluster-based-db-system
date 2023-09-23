package com.atypon.userinteractiveservice.utils.usersmysql;

import com.atypon.userinteractiveservice.utils.usersmysql.repositories.UserRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class UsersMySQLUtils implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }
    public static Boolean isExistsUser(String username){
        UserRepository userRepository = context.getBean(UserRepository.class);
        return userRepository.existsByUsername(username);
    }

}
