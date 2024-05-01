package com.skklub.admin.service;

import com.skklub.admin.TestUserJoin;
import com.skklub.admin.domain.User;
import com.skklub.admin.domain.enums.Role;
import com.skklub.admin.exception.AuthException;
import com.skklub.admin.repository.UserRepository;
import com.skklub.admin.security.auth.PrincipalDetailsService;
import com.skklub.admin.security.jwt.TokenProvider;
import com.skklub.admin.security.redis.RedisUtil;
import com.skklub.admin.service.dto.UserLoginDTO;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@Transactional
public class UserServiceTest {
    private final UserService userService;
    private final TestUserJoin testUserJoin;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    private final RedisUtil redisUtil;

    /**
     * input :
     * expect result :
     */
    @Test
    @DisplayName("유저 생성 - 비밀번호 암호화 테스트")
    public void createUser_GivenFullData_SavedAsEncodedPw() throws Exception{
        //given
        String rawPassword = "1234";
        User user = new User("testUser", rawPassword, Role.ROLE_USER, "testUser", "testContact");

        //mocking

        //when
        userService.createUser(user);

        //then
        assertThat("비밀번호 암호화 및 암호화 일치 여부 확인",
                bCryptPasswordEncoder.matches(rawPassword, user.getPassword()), Matchers.is(true));

    }

    /**
     * input :
     * expect result :
     */
    @Test
    @DisplayName("유저 생성 - 중복 이름 실패")
    public void createUser_AlreadySavedName_AuthException() throws Exception{
        //given
        User preSavedUser = new User("testUser", "1234", Role.ROLE_USER, "testUser", "testContact");
        User postSavedUser = new User("testUser", "1234", Role.ROLE_USER, "testUser2", "testContact2");
        userRepository.save(preSavedUser);

        //mocking

        //when
        assertThrows(AuthException.class, () -> userService.createUser(postSavedUser));

        //then

    }

    @Autowired
    public UserServiceTest(UserService userService, TestUserJoin testUserJoin, UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder, PrincipalDetailsService principalDetailsService, RedisUtil redisUtil){
        this.userService = userService;
        this.testUserJoin = testUserJoin;
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.redisUtil = redisUtil;
    }

    @Test
    public void userJoin_Success(){
        //given
        String username = "user";
        String password = "1234";
        Role role = Role.ROLE_USER;
        String name = "명륜이";
        String contact = "010-1234-5678";

        //when
        testUserJoin.joinUser(username, password, role, name, contact);
        //then
        User user = userRepository.findByUsername(username);

        assertEquals(user.getUsername(),username);
        assertTrue(bCryptPasswordEncoder.matches(password,user.getPassword()));
        assertEquals(user.getRole(),role);
        assertEquals(user.getName(),name);
        assertEquals(user.getContact(), contact);
    }

    @Test
    public void userLogin_Success(){
        //given
        String username = "user";
        String password = "1234";
        Role role = Role.ROLE_USER;
        String name = "명륜이";
        String contact = "010-1234-5678";

        testUserJoin.joinUser(username, password, role, name, contact);

        //when
        UserLoginDTO userLoginDTO = userService.loginUser(username,password);

        //then
        assertTrue(TokenProvider.getUsername(userLoginDTO.getAccessToken()).equals(username));
        assertTrue(redisUtil.hasKeyRefreshToken("RT:" + username));
    }

    @Test
    public void userUpdate_Success(){
        //given
        String username = "tester";

        String password1 = "1234";
        Role role1 = Role.ROLE_USER;
        String name1 = "명륜이";
        String contact1 = "010-1234-5678";


        String password2 = "4321";
        Role role2 = Role.ROLE_USER;
        String name2 = "율전이";
        String contact2 = "010-8765-4321";

        testUserJoin.joinUser(username, password1, role1, name1, contact1);

        UserLoginDTO jwtDTO = userService.loginUser(username,password1);
        String accessToken = "Bearer "+jwtDTO.getAccessToken();

        //when
        User originalUser = userRepository.findByUsername(username);

        Long id = originalUser.getId();

        userService.updateUser(id, password2, role2, name2, contact2,accessToken);

        //then
        User updatedUser = userRepository.findById(id).get();

        assertEquals(updatedUser.getId(),id);
        assertEquals(updatedUser.getUsername(),username);
        assertTrue(bCryptPasswordEncoder.matches(password2,updatedUser.getPassword()));
        assertEquals(updatedUser.getRole(),role2);
        assertEquals(updatedUser.getName(),name2);
        assertEquals(updatedUser.getContact(), contact2);
    }

    @Test
    public void userLogout_Success(){
        String username = "user";
        String password = "1234";
        Role role = Role.ROLE_USER;
        String name = "명륜이";
        String contact = "010-1234-5678";

        testUserJoin.joinUser(username, password, role, name, contact);

        UserLoginDTO jwtDTO = userService.loginUser(username,password);
        String accessToken = jwtDTO.getAccessToken();

        //when
        String loggedOutUser = userService.logoutUser(username,"Bearer "+accessToken);

        //then
        assertEquals(loggedOutUser, username);
        assertTrue(redisUtil.hasKeyBlackList(accessToken));
        assertFalse(redisUtil.hasKeyRefreshToken("RT:" + username));
        }



}
