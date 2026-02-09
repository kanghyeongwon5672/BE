package com.tjoeun.boxmon.feature.user.service;

import com.tjoeun.boxmon.exception.DuplicateEmailException;
import com.tjoeun.boxmon.exception.InvalidPasswordException;
import com.tjoeun.boxmon.exception.InvalidTokenException;
import com.tjoeun.boxmon.exception.TokenTypeMismatchException;
import com.tjoeun.boxmon.exception.UserNotFoundException;
import com.tjoeun.boxmon.feature.user.domain.Driver;
import com.tjoeun.boxmon.feature.user.domain.Shipper;
import com.tjoeun.boxmon.feature.user.domain.User;
import com.tjoeun.boxmon.feature.user.domain.UserType;
import com.tjoeun.boxmon.feature.user.dto.LoginRequest;
import com.tjoeun.boxmon.feature.user.dto.LoginResponse;
import com.tjoeun.boxmon.feature.user.dto.SignupRequest;
import com.tjoeun.boxmon.feature.user.dto.TokenRefreshRequest;
import com.tjoeun.boxmon.feature.user.dto.TokenRefreshResponse;
import com.tjoeun.boxmon.feature.user.repository.DriverRepository;
import com.tjoeun.boxmon.feature.user.repository.ShipperRepository;
import com.tjoeun.boxmon.feature.user.repository.UserRepository;

import com.tjoeun.boxmon.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ShipperRepository shipperRepository;
    private final DriverRepository driverRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    //회원가입
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("이미 존재하는 이메일입니다");
        }
        String encodedPW = passwordEncoder.encode(request.getPassword());
        User user = new User(
                request.getEmail(),
                encodedPW,
                request.getName(),
                request.getPhone(),
                request.getBirth(),
                request.getUserType()
        );
        userRepository.save(user);

        userRepository.save(user); // 여기서 user_id 생성됨

        // 🔥 여기 추가
        if (request.getUserType() == UserType.SHIPPER) {
            Shipper shipper = new Shipper(user); // user_id FK
            shipperRepository.save(shipper);
        } else if (request.getUserType() == UserType.DRIVER) {
            Driver driver = new Driver(user);
            driverRepository.save(driver);
        }
    }


    //로그인
    public LoginResponse login(LoginRequest request){
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()-> new UserNotFoundException("사용자 없음"));

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new InvalidPasswordException("비밀번호 불일치");
        }

        // Access Token 생성 (15분 만료)
        String accessToken = jwtProvider.createAccessToken(user.getUserId());
        
        // Refresh Token 생성 (14일 만료)
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId());

        return new LoginResponse(accessToken, refreshToken);
    }

    // Access Token 갱신
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        String refreshTokenValue = request.getRefreshToken();

        // Refresh Token 유효성 검증 (서명, 만료 확인)
        if (!jwtProvider.validateToken(refreshTokenValue)) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token");
        }

        // Refresh Token 타입 확인
        String tokenType = jwtProvider.getTokenType(refreshTokenValue);
        if (!"REFRESH".equals(tokenType)) {
            throw new TokenTypeMismatchException("Refresh Token이 아닙니다");
        }

        // Refresh Token에서 userId 추출
        Long userId = jwtProvider.getUserIdFromToken(refreshTokenValue);

        // 새로운 Access Token 생성
        String newAccessToken = jwtProvider.createAccessToken(userId);

        // 새로운 Refresh Token 생성
        String newRefreshToken = jwtProvider.createRefreshToken(userId);

        return new TokenRefreshResponse(newAccessToken, newRefreshToken);
    }

}
