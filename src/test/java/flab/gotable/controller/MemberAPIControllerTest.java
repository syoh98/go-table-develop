package flab.gotable.controller;
import flab.gotable.domain.entity.Member;
import flab.gotable.dto.ApiResponse;
import flab.gotable.dto.request.MemberLoginRequestDto;
import flab.gotable.dto.request.MemberSignUpRequestDto;
import flab.gotable.exception.DuplicatedIdException;
import flab.gotable.exception.MemberNotFoundException;
import flab.gotable.mapper.MemberMapper;
import flab.gotable.service.LoginService;
import flab.gotable.service.MemberService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

class MemberAPIControllerTest {
    private MemberAPIController memberAPIController;

    private MockHttpSession session;

    @Autowired
    private LoginService loginService;

    @BeforeEach
    void setup() {
       session = new MockHttpSession();

       memberAPIController = new MemberAPIController(
                new MemberService(
                        new MemberMapper() {
                            final Map<String, Member> memberMap = new HashMap<>();

                            @Override
                            public void saveMember(Member member) {
                                memberMap.put(member.getId(), member);
                            }

                            @Override
                            public boolean existId(String id) {
                                return memberMap.containsKey(id);
                            }

                            @Override
                            public Member findMemberById(String id) {
                                return memberMap.get(id);
                            }

                            @Override
                            public boolean isMemberExistSeq(long seq) {
                                return memberMap.containsKey(seq);
                            }
                        }, new PasswordEncoder() {
                            @Override
                            public String encode(CharSequence rawPassword) {
                                return rawPassword.toString();
                            }

                            @Override
                            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                                if(!rawPassword.equals(encodedPassword)) {
                                    return false;
                                }
                                return true;
                            }
                        }
                ), loginService = new LoginService()
        );
    }

    @Test
    @DisplayName("중복된 아이디가 존재하지 않을 경우 200 OK를 반환한다.")
    void duplicatedIdNotExist() {
        // given
        memberAPIController.signup(new MemberSignUpRequestDto("제로영", "zero0", "q1w2e3r4", "010-1111-2222"));

        // when
        ResponseEntity<HttpStatus> testId = memberAPIController.isDuplicatedId("zero010101");

        // then
        Assertions.assertEquals(HttpStatus.OK, testId.getStatusCode());
    }

    @Test
    @DisplayName("중복된 아이디가 존재할 경우 DuplicatedIdException 예외를 발생시킨다.")
    void duplicatedIdExist() {
        // given
        memberAPIController.signup(new MemberSignUpRequestDto("오소영", "sozero", "q1w2e3r4", "010-1111-2222"));

        // then
        Assertions.assertThrows(DuplicatedIdException.class, () -> memberAPIController.signup(new MemberSignUpRequestDto("오소영", "sozero", "q1w2e3r4", "010-1111-2222")));
    }

    @Test
    @DisplayName("회원가입에 성공할 경우 201 Created를 반환한다.")
    void signupSuccess() {
        // given
        ResponseEntity<ApiResponse> result = memberAPIController.signup(new MemberSignUpRequestDto("제로영", "zero0", "q1w2e3r4", "010-1111-2222"));

        // then
        Assertions.assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    @DisplayName("로그인에 성공할 경우 200 OK를 반환한다.")
    void loginSuccess() {
        // given
        memberAPIController.signup(new MemberSignUpRequestDto("오소영", "sozero", "q1w2e3r4", "010-1111-2222"));

        // when
        ResponseEntity<ApiResponse> result = memberAPIController.login(new MemberLoginRequestDto("sozero", "q1w2e3r4"), session);

        // then
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    @DisplayName("가입한 회원 정보를 찾지 못해 로그인에 실패할 경우 MemberNotFoundException 예외를 발생시킨다.")
    void loginFailNotExistId() {
        // given
        memberAPIController.signup(new MemberSignUpRequestDto("오소영", "sozero", "q1w2e3r4", "010-1111-2222"));

        // then
        Assertions.assertThrows(MemberNotFoundException.class, () -> memberAPIController.login(new MemberLoginRequestDto("testId", "r5t6y7u8"), session));
    }

    @Test
    @DisplayName("가입한 회원의 패스워드가 일치하지 않아 로그인에 실패할 경우 MemberNotFoundException 예외를 발생시킨다.")
    void loginFailNotExistMember() {
        // given
        memberAPIController.signup(new MemberSignUpRequestDto("오소영", "sozero", "q1w2e3r4", "010-1111-2222"));

        // then
        Assertions.assertThrows(MemberNotFoundException.class, () -> memberAPIController.login(new MemberLoginRequestDto("sozero", "r5t6y7u8"), session));
    }
}