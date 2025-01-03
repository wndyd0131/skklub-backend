package com.skklub.admin.service;

import com.skklub.admin.domain.User;
import com.skklub.admin.domain.enums.Role;
import com.skklub.admin.exception.deprecated.AuthException;
import com.skklub.admin.exception.deprecated.ErrorCode;
import com.skklub.admin.repository.UserRepository;
import com.skklub.admin.security.jwt.TokenProvider;
import com.skklub.admin.security.jwt.dto.JwtDTO;
import com.skklub.admin.security.redis.RedisUtil;
import com.skklub.admin.service.dto.UserLoginDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final RedisUtil redisUtil;


    public void createUser(User user) {
        validateUsernameDuplication(user.getUsername());
        String password = user.getPassword();
        String encodedPw = bCryptPasswordEncoder.encode(password);
        user.encodePw(encodedPw);
        userRepository.save(user);
    }

    //User Login
    public UserLoginDTO loginUser(String username, String password){
        //기본 username, password 인증
        //1. 일치하는 username 없음
        User user = Optional.ofNullable(userRepository.findByUsername(username))
                .orElseThrow(()->
                    new AuthException(ErrorCode.USER_NOT_FOUND, "invalid user account"));
        //2. password 불일치
        if(!bCryptPasswordEncoder.matches(password,user.getPassword())){
            throw new AuthException(ErrorCode.USER_NOT_FOUND, "invalid user account");
        }
        //토큰 발급
        JwtDTO tokens = TokenProvider.createTokens(user.getId(),username,user.getRole());
        String refreshToken = tokens.getRefreshToken();
        String key = "RT:" + username;
        //Redis 저장된 refreshToken 확인 -> 업데이트
        if(redisUtil.hasKeyRefreshToken(key)) {
            redisUtil.deleteRefreshToken(key);
        }redisUtil.setRefreshToken(key, refreshToken, TokenProvider.getExpiration(refreshToken), TimeUnit.MILLISECONDS);
        //정상 로그인 완료 -> JWT 반환
        return new UserLoginDTO(user.getId(), user.getUsername(), user.getRole(), tokens.getAccessToken(), tokens.getRefreshToken());
    }

    //User Update
    public Optional<User> updateUser(Long userId, String password, Role role, String name, String contact, String accessToken){
        //update 진행
        String username = userRepository.findById(userId).get().getUsername();
        String encPwd = bCryptPasswordEncoder.encode(password);

        return userRepository.findById(userId)
                .map(baseUser -> {
                    baseUser.update(new User(username, encPwd, role, name, contact));
                    invalidateUserAccessToken(username, accessToken);
                    log.info("user updated -> userId : {}, username : {}", userId, username);
                    return baseUser;
                });
    }

    //User Logout
    public String logoutUser(String username,String accessToken) {
        invalidateUserAccessToken(username, accessToken);
        return username;
    }

    private void invalidateUserAccessToken(String username, String accessToken) {
        //Redis 에 저장된 refresh token 있을 경우 삭제
        if (redisUtil.hasKeyRefreshToken("RT:" + username)) {
            redisUtil.deleteRefreshToken("RT:" + username);
        }
        //해당 AccessToken 유효시간 반영해서 BlackList 저장
        String bannedToken = TokenProvider.resolveToken(accessToken);
        redisUtil.setBlackList(bannedToken, "AT:" + username, TokenProvider.getExpiration(bannedToken) + (60 * 10 * 1000), TimeUnit.MILLISECONDS);
    }

    public void validateUsernameDuplication(String username) throws AuthException{
        Optional.ofNullable(userRepository.findByUsername(username))
                .ifPresent(user->{
                    throw new AuthException(ErrorCode.USERNAME_DUPLICATED, username+" is already exists");
                });
    }

}
