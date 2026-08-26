package com.kaii.dentix.domain.jwt;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.user.dao.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtTokenUtilTest {

    private final JwtTokenUtil jwtTokenUtil = new JwtTokenUtil(
            mock(UserRepository.class),
            mock(AdminRepository.class)
    );

    @Test
    void extractsOnlyStandardBearerAccessToken() {
        MockHttpServletRequest bearerRequest = new MockHttpServletRequest();
        bearerRequest.addHeader("Authorization", "Bearer signed.jwt.value");

        MockHttpServletRequest rawRequest = new MockHttpServletRequest();
        rawRequest.addHeader("Authorization", "signed.jwt.value");

        assertThat(jwtTokenUtil.getAccessToken(bearerRequest)).isEqualTo("signed.jwt.value");
        assertThat(jwtTokenUtil.getAccessToken(rawRequest)).isNull();
    }
}
