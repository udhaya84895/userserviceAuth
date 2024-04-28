package com.auth1.auth.learning.service;

import com.auth1.auth.learning.dtos.SendEmailMessageDto;
import com.auth1.auth.learning.model.Role;
import com.auth1.auth.learning.model.Token;
import com.auth1.auth.learning.model.User;
import com.auth1.auth.learning.repository.TokenRepository;
import com.auth1.auth.learning.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;

    @Autowired
    ObjectMapper objectMapper;


    public User signUp(String email, String password, String name){

        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        user.setRoles(new ArrayList<>());
        user.setEmailVerified(true);

        User savedUser = userRepository.save(user);

        SendEmailMessageDto messageDto = new SendEmailMessageDto();
        messageDto.setFrom("shikherkak@gmail.com");
        messageDto.setTo(email);
        messageDto.setSubject("This is a message from kash");
        messageDto.setBody("Hiya!! Whatcha doin??");

//        try {
//            kafkaTemplate.send(
//                    "sendEmail",
//                    objectMapper.writeValueAsString(messageDto)
//                    );
//        }
//        catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }

        return savedUser;


    }

    public Token login(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            throw new RuntimeException("Invalid userOptional or password");
        }

        User user = userOptional.get();

        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid userOptional or password");
        }

        Token token = new Token();
        token.setUser(user);
        token.setValue(UUID.randomUUID().toString());

        Date expiredDate = getExpiredDate();

        token.setExpireAt(expiredDate);

        return tokenRepository.save(token);
    }

    // expiration date will be 30 days after today.
    private Date getExpiredDate() {

        Calendar calendarDate = Calendar.getInstance();
        calendarDate.setTime(new Date());


        //add(Calendar.DAY_OF_MONTH, -5).
        calendarDate.add(Calendar.DAY_OF_MONTH, 30);

        Date expiredDate = calendarDate.getTime();
        return expiredDate;

    }

    public void logout(String token) {
        // '1', '2024-04-25 21:58:17.639000', '65c338ac-69f6-43a4-b460-dd4dd3e39534', '1'

        Optional<Token> tokenOptional = tokenRepository.findByValueAndDeletedEquals(token, false);

        if (tokenOptional.isEmpty()) {
            throw new RuntimeException("Token is invalid ");
        }

        Token tokenObject = tokenOptional.get();
        tokenObject.setDeleted(true);

        tokenRepository.save(tokenObject);
    }

    public boolean validateToken(String token) {

        /*
          To validate token
          1. Check if token value is present
          2. Check if token is not deleted
          3. Check if token is not expired
         */

        Optional<Token> tokenOptional = tokenRepository.findByValueAndDeletedEqualsAndExpireAtGreaterThan(
                token, false, new Date()
        );

        if (tokenOptional.isEmpty()) {
            return false;
        }
        return true;
    }

    public String getUserEmail(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if(userOptional.isEmpty())
        {
            return "Not Found";
        }

        User user = userOptional.get();
        return user.getName();
    }
}