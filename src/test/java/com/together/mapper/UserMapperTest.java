package com.together.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.together.dto.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

/**
 * 로컬 Oracle 컨테이너(FREEPDB1)에 실제로 질의가 나가는지 확인한다.
 *
 * <p>{@code @MybatisTest} 는 기본이 트랜잭션이라 각 테스트 후 롤백된다. 컨테이너가 떠 있지 않으면
 * 이 테스트는 실패한다 — 배선이 아니라 환경 문제이므로 그때는 {@code docker start oracle-free} 로 해결한다.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    @DisplayName("insert 하면 IDENTITY 로 채번된 ID 가 되돌아온다")
    void insertAssignsGeneratedId() {
        User user = new User();
        user.setKakaoId("kakao-test-0001");
        user.setEmail("tester@example.com");

        int affected = userMapper.insert(user);

        assertThat(affected).isEqualTo(1);
        assertThat(user.getId()).isNotNull();
    }

    @Test
    @DisplayName("kakaoId 로 방금 넣은 행을 다시 읽어온다")
    void findByKakaoIdReturnsInsertedRow() {
        User user = new User();
        user.setKakaoId("kakao-test-0002");
        user.setEmail("tester2@example.com");
        userMapper.insert(user);

        User found = userMapper.findByKakaoId("kakao-test-0002");

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(user.getId());
        assertThat(found.getEmail()).isEqualTo("tester2@example.com");
        // DEFAULT SYSTIMESTAMP 가 걸려 있어 DB 가 채워준다.
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("없는 kakaoId 는 null 을 돌려준다")
    void findByKakaoIdReturnsNullWhenAbsent() {
        assertThat(userMapper.findByKakaoId("kakao-does-not-exist")).isNull();
    }
}
