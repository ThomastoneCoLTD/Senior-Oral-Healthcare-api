# DENTI-GLOBAL
## 📁 APP
### 📁 회원 관리
#### 사용자 회원 인증
**POST** `{{host}}/login/verify`
### Request Body
```json
{
    "userPhoneNumber" : "01012345678",
    "userName" : "김덴티"
}
```
### Response Example
```json
{
    "rt": 200,
    "rtMsg": "API Call successful",
    "response": {
        "patientId": null
    }
}
```

---
#### 아이디 중복 확인
**GET** `{{host}}/login/loginIdentifier-check?userLoginIdentifier=dentix1235`

---
#### 회원가입
**POST** `{{host}}/login/signUp`
### Request Headers
| Key | Value |
|-----|--------|
| appVersion | 1.1.1 |
| deviceType | iOS |
### Request Body
```json
{
    "userLoginIdentifier": "user200",
    "userPassword": "test1234!",
    "userName": "홍길동",
    "userGender": "M",
    "userPhoneNumber": "01020020020",
    "findPwdQuestionId": 1,
    "findPwdAnswer": "테스트답변",
    "organizationId": 2,
    "appServiceIds": [1, 2],
    "userServiceAgreementRequest": [1,2,3,4,5],
    "birth": "1990-08-21"
}
```

---
#### 기관확인
**GET** `localhost:8080/organizations/check-duplicate`
### Request Headers
| Key | Value |
|-----|--------|
| appVersion | 1.1.1 |
| deviceType | iOS |
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1MSIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzYxMDM2MTI5LCJleHAiOjE3NjEwNDMzMjl9.W4w_09DU0fS3n00l_eT_FpgHgYUiwRNNmHZ4OfNjV0c |

---
#### 로그인
**POST** `https://denti-api.thomabio.com/login`
### Request Headers
| Key | Value |
|-----|--------|
| appVersion | 1.1.1 |
| deviceType | iOS |
### Request Body
```json
{
    "userType": "user",
    "loginId" : "hana0142",
    "password" : "test1234!"
}
```

---
#### 자동 로그인
**PUT** `{{host}}/user/auto-login`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMCIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNjkwODcxMDg2LCJleHAiOjE2OTA4NzgyODZ9.Gy2JDDrNz48jM6-6RKurHwivQRSgwKl6_QrF5tgD_0w |
| appVersion | 1.1.1 |
| deviceType | iOS |
### Request Body
```json
{
    "userDeviceModel" : "iPhone 12",
    "userDeviceManufacturer" : "APPLE",
    "userOsVersion" : "1.1",
    "userDeviceToken" : "deviceToken"
}
```

---
#### Access Token 재발급
**PUT** `{{host}}/login/access-token`
### Request Headers
| Key | Value |
|-----|--------|
| RefreshToken | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1MSIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzYyMjA5NDcyLCJleHAiOjE3NjM0MTkwNzJ9.Djt6l9CzFYyT4f26ky-PWRUSihBfEzHAZtTAhmxQjd8 |
| appVersion | 1.1.1 |
| deviceType | iOS |

---
#### 비밀번호 찾기
**POST** `{{host}}/login/find-password`
### Request Body
```json
{
    "userLoginIdentifier" : "test1111",
    "findPwdQuestionId" : 1,
    "findPwdAnswer" : "red"
}
```

---
#### 비밀번호 확인
**POST** `{{host}}/user/password-verify`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1MSIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzYyMjE4MDg2LCJleHAiOjE3NjIyMjUyODZ9.yaqAifj71q2pMsRNzvGhaJCsDvvoV-nDd_ASo_7aafo |
### Request Body
```json
{
    "userPassword" : "qwer1234!"
}
```

---
#### 비밀번호 재설정
**PUT** `{{host}}/login/password`
### Request Body
```json
{
    "userId" : 25,
    "userPassword" : "test1234!"
}
```

---
#### 보안정보 수정 : 비밀번호 변경
**PUT** `{{host}}/user/password`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMCIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNjkwODcxNDQ1LCJleHAiOjE2OTA4Nzg2NDV9.zcr4-MnLjP48EDRvJzQ9aivg5GggFWmHKV3r3JZN_-Q |
### Request Body
```json
{
    "userPassword" : "qwer1234!"
}
```

---
#### 보안정보 수정 : 질문과 답변 수정
**PUT** `{{host}}/user/qna`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMCIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNjkwODcxNDQ1LCJleHAiOjE2OTA4Nzg2NDV9.zcr4-MnLjP48EDRvJzQ9aivg5GggFWmHKV3r3JZN_-Q |
### Request Body
```json
{
    "findPwdQuestionId" : 1,
    "findPwdAnswer" : "초록색"
}
```

---
#### 회원 정보 수정
**PUT** `{{host}}/user`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMCIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNjkwODcxNDQ1LCJleHAiOjE2OTA4Nzg2NDV9.zcr4-MnLjP48EDRvJzQ9aivg5GggFWmHKV3r3JZN_-Q |
### Request Body
```json
{
    "userName" : "김덴티다",
    "userGender" : "W"
}
```

---
#### 회원 정보 조회
**GET** `{{host}}/user`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1MSIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzYyMjE4MDg2LCJleHAiOjE3NjIyMjUyODZ9.yaqAifj71q2pMsRNzvGhaJCsDvvoV-nDd_ASo_7aafo |

---
#### 서비스 동의 여부 리스트 조회
**GET** `{{host}}/user/serviceAgreement`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1MSIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzYxMTc3NzY1LCJleHAiOjE3NjExODQ5NjV9.eXAvOGYBn1bVhbZGZUR3GH0qD8YuROO9bRjP3IM6zWU |

---
#### 서비스 이용동의 여부 수정
**PUT** `{{host}}/user/service-agreement`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1MSIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzYyMjE4MDg2LCJleHAiOjE3NjIyMjUyODZ9.yaqAifj71q2pMsRNzvGhaJCsDvvoV-nDd_ASo_7aafo |
### Request Body
```json
{
    "serviceAgreeId": 3,
    "isUserServiceAgree" : "Y"
}
```

---
#### 구독 서비스 수정
**PUT** `{{host}}/user/service/change`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1MSIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzYyMjE4MDg2LCJleHAiOjE3NjIyMjUyODZ9.yaqAifj71q2pMsRNzvGhaJCsDvvoV-nDd_ASo_7aafo |
### Request Body
```json
{
    "serviceId": 1
}
```

---
#### 로그아웃
**PUT** `{{host}}/user/logout`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMCIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNjkwODcxNDQ1LCJleHAiOjE2OTA4Nzg2NDV9.zcr4-MnLjP48EDRvJzQ9aivg5GggFWmHKV3r3JZN_-Q |

---
#### 회원 탈퇴
**DELETE** `{{host}}/user`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMCIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNjkwODcxNDQ1LCJleHAiOjE2OTA4Nzg2NDV9.zcr4-MnLjP48EDRvJzQ9aivg5GggFWmHKV3r3JZN_-Q |

---
#### 비밀번호 찾기 질문 리스트
**GET** `{{host}}/password/questions`

---
### 📁 메인
#### 대시보드
**GET** `{{host}}/oralCheck/dashboard`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMCIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNjkwODczMzgxLCJleHAiOjE2OTA4ODA1ODF9.xTLM_fIxi6jDEXzKvq3sM7TjLukVE58Soxh1dmB7kP4 |

---
#### 콘텐츠 조회
**GET** `{{host}}/contents`

---
#### 콘텐츠 카드뉴스
**GET** `{{host}}/contents/card?contentsId=1`

---
### 📁 구강 관리
#### 구강검진 사진 촬영
**POST** `{{host}}/oralCheck/photo`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1MSIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzYyMTI0MTk0LCJleHAiOjE3NjIxMzEzOTR9.C3p89puRPTTP4cqiMGUmRPFHoL82bNBcpkVbL7MJE9w |
### Request Form Data
| Key | Type | Value |
|-----|------|--------|
| file | file |  |

---
#### 구강검진 사진파일 업로드
**POST** `{{host}}/oralCheck/upload`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1MSIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzYyMTI0MTk0LCJleHAiOjE3NjIxMzEzOTR9.C3p89puRPTTP4cqiMGUmRPFHoL82bNBcpkVbL7MJE9w |
### Request Form Data
| Key | Type | Value |
|-----|------|--------|
| file | file |  |

---
#### 구강검진 결과
**GET** `{{host}}/oralCheck/result?oralCheckId=25672`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1MSIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzYyMTM0OTI4LCJleHAiOjE3NjIxNDIxMjh9.Jv03VESdg8EuoYTVJjtqwn8t3NUgos4tHtjQzou5M0w |

---
#### 구강 상태 조회
**GET** `{{host}}/oralCheck`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1MSIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzYwNTAxNDg0LCJleHAiOjE3NjA1MDg2ODR9.jZEeWDo969kNh5Oq8T-AWZ-AfjvVKSB2fnmg5xInrFo |

---
#### 사용X_양치질 기록
**POST** `{{host}}/toothBrushing`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMCIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNjkwODczMzgxLCJleHAiOjE2OTA4ODA1ODF9.xTLM_fIxi6jDEXzKvq3sM7TjLukVE58Soxh1dmB7kP4 |

---
#### (구) 플라그 검출 AI
**POST** `https://dentix-ai.kai-i.com/api/plaque`
### Request Form Data
| Key | Type | Value |
|-----|------|--------|
| picture | file |  |

---
#### 플라그 검출 AI
**POST** `https://ai-backend-gpu.kai-i.com/denti_x/api/plaque`
### Request Form Data
| Key | Type | Value |
|-----|------|--------|
| picture | file |  |

---
### 📁 문진표
#### 문진표 양식 조회
**GET** `{{host}}/questionnaire/template`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1MSIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzYxMzQ4MjM5LCJleHAiOjE3NjEzNTU0Mzl9.znrID9ZxbbOmdGvO-fwRV473gqNUjXU2s6EXpPjI3w0 |

---
#### 문진표 제출
**POST** `{{host}}/questionnaire/submit`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5OSIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNjk4NjM4ODM4LCJleHAiOjE2OTg2NDYwMzh9.a7waOmGkVbjuCCZsbbNPSJEFbBu5hHFenrgq3eqi69s |
### Request Body
```json
{
    "form" : [
        {"key": "q_1", "value": [1]},
        {"key": "q_2", "value": [2]},
        {"key": "q_3", "value": [1, 2, 3, 4, 5, 6, 7]},
        {"key": "q_4", "value": [1]},
        {"key": "q_5", "value": [1]},
        {"key": "q_6", "value": [1]},
        {"key": "q_7", "value": [1]},
        {"key": "q_8", "value": [9]},
        {"key": "q_9", "value": [6]},
        {"key": "q_10", "value": [1]}
    ]
}
```

---
#### 문진표 결과 조회
**GET** `{{host}}/questionnaire/result?questionnaireId=10`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMCIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaWF0IjoxNjkwODczMzgxLCJleHAiOjE2OTA4ODA1ODF9.xTLM_fIxi6jDEXzKvq3sM7TjLukVE58Soxh1dmB7kP4 |

---
#### (구) 문진표 AI
**POST** `https://dentix-ai.kai-i.com/api/prsc`
### Request Form Data
| Key | Type | Value |
|-----|------|--------|
| survey | text | {
  "form" : {
    "q_1" :3,
    "q_2" : 2,
    "q_3" : [1,3],
    "q_4" : 4,
    "q_5" : [2],
    "q_6" : [3],
    "q_7" : 3,
    "q_8" : [4],
    "q_9" : 2,
    "q_10" :1 
  }
} |

---
#### 문진표 AI
**POST** `https://ai-backend-gpu.kai-i.com/denti_x/api/prsc`
### Request Form Data
| Key | Type | Value |
|-----|------|--------|
| survey | text | {
  "form" : {
    "q_1" :3,
    "q_2" : 2,
    "q_3" : [1,3],
    "q_4" : 4,
    "q_5" : [2],
    "q_6" : [3],
    "q_7" : 3,
    "q_8" : [4],
    "q_9" : 2,
    "q_10" :1 
  }
} |

---
### 📁 기타
#### 약관 조회
**GET** `{{host}}/service-agreement`

---
## 📁 WEB
### 📁 관리자
#### 관리자 등록
**POST** `{{host}}localhost:8080/admin/account`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NDU5MTE2NywiZXhwIjoxNjk0NTk4MzY3fQ.KQoUYzjQQ6XIMOoXHvJOoABZYmV435DHajWuEi8YqW4 |
### Request Body
```json
{
    "adminName" : "denti-cash",
    "adminLoginIdentifier" : "denti-cash",
    "adminPhoneNumber" : "01010041004"
}
```

---
#### 로그인
**POST** `{{host}}/login`
### Request Body
```json
// {
//     "userType":"admin",
//     "loginId" : "admin",
//     "password" : "test1234!"
// }

{
    "userType":"admin",
    "loginId" : "test1",
    "password" : "dentix2023!"
}
```

---
#### 자동 로그인
**PUT** `{{host}}/admin/account/auto-login`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NDU5MTE2NywiZXhwIjoxNjk0NTk4MzY3fQ.KQoUYzjQQ6XIMOoXHvJOoABZYmV435DHajWuEi8YqW4 |

---
#### 비밀번호 변경
**PUT** `{{host}}/admin/account/password`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NDU5MTE2NywiZXhwIjoxNjk0NTk4MzY3fQ.KQoUYzjQQ6XIMOoXHvJOoABZYmV435DHajWuEi8YqW4 |
### Request Body
```json
{
    "adminPassword" : "dentixadmin123!"
}
```

---
#### 관리자 목록 조회
**GET** `localhost:8080/admin/account/list?page=1&size=10`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | package com.kaii.dentix.domain.admin.application;

import com.kaii.dentix.domain.admin.dao.AdminRepository;
import com.kaii.dentix.domain.admin.dao.user.AdminUserCustomRepository;
import com.kaii.dentix.domain.admin.domain.Admin;
import com.kaii.dentix.domain.admin.dto.*;
import com.kaii.dentix.domain.admin.dto.request.AdminStatisticRequest;
import com.kaii.dentix.domain.admin.dto.statistic.*;
import com.kaii.dentix.domain.jwt.JwtTokenUtil;
import com.kaii.dentix.domain.jwt.TokenType;
import com.kaii.dentix.domain.oralCheck.application.OralCheckService;
import com.kaii.dentix.domain.oralCheck.dao.OralCheckCustomRepository;
import com.kaii.dentix.domain.admin.dto.statistic.OralCheckResultTypeCount;
import com.kaii.dentix.domain.questionnaire.dao.QuestionnaireCustomRepository;
import com.kaii.dentix.domain.type.oral.OralCheckResultType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStatisticService {

    private final AdminUserCustomRepository adminUserCustomRepository;

    private final OralCheckCustomRepository oralCheckCustomRepository;

    private final OralCheckService oralCheckService;

    private final QuestionnaireCustomRepository questionnaireCustomRepository;

    private final JwtTokenUtil jwtTokenUtil;

    private final AdminRepository adminRepository;

//    private final Admin admin;
    /**
     *  사용자 통계
     */
    @Transactional(readOnly = true)
    public AdminUserStatisticResponse userStatistic(AdminStatisticRequest request){

        // 통계 1. 전체 남녀 가입률
        AdminUserSignUpCountDto userSignUpCount = adminUserCustomRepository.userSignUpCount(request);

        // 통계 2. 평균 구강검진
        OralCheckResultTypeCount userOralCheckList = oralCheckCustomRepository.userOralCheckList(request); // 구강검진 결과 타입별 횟수

        int allUserOralCheckCount = oralCheckCustomRepository.allUserOralCheckCount(request);  // 구강검진을 한 총 사용자 수

        int allOralCheckCount = userOralCheckList.getCountHealthy() + userOralCheckList.getCountGood() + userOralCheckList.getCountAttention() + userOralCheckList.getCountDanger(); // 전체 구강검진 횟수
        int oralCheckAverage = 0; // 사용자 당 평균 구강검진 횟수

        if (allUserOralCheckCount > 0) {
            oralCheckAverage = Math.round((float) allOralCheckCount / allUserOralCheckCount);
        }

        OralCheckResultType averageState = oralCheckService.getState(userOralCheckList); // 전체 평균 구강 상태

        // 통계 3. 평균 문진표 유형
        List<QuestionnaireStatisticDto> questionnaireList = questionnaireCustomRepository.questionnaireList(request); // 모든 문진표 리스트

        int allQuestionnaireCount = questionnaireCustomRepository.allQuestionnaireCount(request); // 전체 문진표 작성 횟수

        AllQuestionnaireResultTypeCount allQuestionnaireResultTypeCount = new AllQuestionnaireResultTypeCount(); // 모든 문진표 결과 유형

        if (allQuestionnaireCount > 0){
            int countA = 0;
            int countB = 0;
            int countC = 0;
            int countD = 0;
            int countE = 0;
            int countF = 0;
            int countG = 0;
            int countH = 0;
            int countI = 0;
            int countJ = 0;
            int countK = 0;

            for (QuestionnaireStatisticDto questionnaireStatisticDto : questionnaireList){
                switch (questionnaireStatisticDto.getQuestionnaireType()) { // 문진표 결과 타입별 횟수 count
                    case "A" -> countA ++;
                    case "B" -> countB ++;
                    case "C" -> countC ++;
                    case "D" -> countD ++;
                    case "E" -> countE ++;
                    case "F" -> countF ++;
                    case "G" -> countG ++;
                    case "H" -> countH ++;
                    case "I" -> countI ++;
                    case "J" -> countJ ++;
                    case "K" -> countK ++;
                }
            }

            allQuestionnaireResultTypeCount = AllQuestionnaireResultTypeCount.builder()
                    .countA(countA)
                    .countB(countB)
                    .countC(countC)
                    .countD(countD)
                    .countE(countE)
                    .countF(countF)
                    .countG(countG)
                    .countH(countH)
                    .countI(countI)
                    .countJ(countJ)
                    .countK(countK)
                    .build();
        }

        return AdminUserStatisticResponse.builder()
                .userSignUpCount(userSignUpCount)
                .averageState(averageState)
                .oralCheckCount(allOralCheckCount)
                .oralCheckAverage(oralCheckAverage)
                .oralCheckResultTypeCount(userOralCheckList)
                .questionnaireAllCount(allQuestionnaireCount)
                .allQuestionnaireResultTypeCount(allQuestionnaireResultTypeCount)
                .build();
    }

    /**
     *  관리자 통계 조회
     */
    @Transactional(readOnly = true)
    public AdminUserStatisticResponse getOrgStatistics(AdminStatisticRequest request, HttpServletRequest httpRequest) {

        // ✅ 1️⃣ 현재 로그인 관리자 식별
        Long adminId = jwtTokenUtil.getUserId(
                jwtTokenUtil.getAccessToken(httpRequest),
                TokenType.AccessToken
        );

        // ✅ 2️⃣ 관리자 → 소속 기관 ID 가져오기
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        if (admin.getOrganization() == null) {
            throw new IllegalStateException("현재 관리자는 아직 기관에 소속되어 있지 않습니다.");
        }

        Long organizationId = admin.getOrganization().getOrganizationId();

        // ✅ 3️⃣ request 에 기관ID 자동 주입
        request.setOrganizationId(organizationId);

//        log.info("📊 통계 요청 관리자={}, 기관ID={}", admin.getAdminName(), organizationId);

        // ✅ 4️⃣ 통계 1. 전체 남녀 가입률
        AdminUserSignUpCountDto userSignUpCount = adminUserCustomRepository.userSignUpCount(request);

        // ✅ 5️⃣ 통계 2. 구강검진 통계
        OralCheckResultTypeCount userOralCheckList = oralCheckCustomRepository.userOralCheckList(request);
        int allUserOralCheckCount = oralCheckCustomRepository.allUserOralCheckCount(request);
        int allOralCheckCount = userOralCheckList.getCountHealthy()
                + userOralCheckList.getCountGood()
                + userOralCheckList.getCountAttention()
                + userOralCheckList.getCountDanger();

        int oralCheckAverage = allUserOralCheckCount > 0
                ? Math.round((float) allOralCheckCount / allUserOralCheckCount)
                : 0;

        OralCheckResultType averageState = oralCheckService.getState(userOralCheckList);

        // ✅ 6️⃣ 통계 3. 문진표 통계
        List<QuestionnaireStatisticDto> questionnaireList = questionnaireCustomRepository.questionnaireList(request);
        int allQuestionnaireCount = questionnaireCustomRepository.allQuestionnaireCount(request);

        AllQuestionnaireResultTypeCount allQuestionnaireResultTypeCount = calcQuestionnaireCount(questionnaireList, allQuestionnaireCount);

        // ✅ 7️⃣ 응답 반환
        return AdminUserStatisticResponse.builder()
                .userSignUpCount(userSignUpCount)
                .averageState(averageState)
                .oralCheckCount(allOralCheckCount)
                .oralCheckAverage(oralCheckAverage)
                .oralCheckResultTypeCount(userOralCheckList)
                .questionnaireAllCount(allQuestionnaireCount)
                .allQuestionnaireResultTypeCount(allQuestionnaireResultTypeCount)
                .build();
    }

    /**
     * 문진표 유형별 count 계산
     */
    private AllQuestionnaireResultTypeCount calcQuestionnaireCount(List<QuestionnaireStatisticDto> list, int totalCount) {
        if (totalCount == 0) return new AllQuestionnaireResultTypeCount();

        int[] counts = new int[11]; // A~K (11개)
        for (QuestionnaireStatisticDto dto : list) {
            switch (dto.getQuestionnaireType()) {
                case "A" -> counts[0]++;
                case "B" -> counts[1]++;
                case "C" -> counts[2]++;
                case "D" -> counts[3]++;
                case "E" -> counts[4]++;
                case "F" -> counts[5]++;
                case "G" -> counts[6]++;
                case "H" -> counts[7]++;
                case "I" -> counts[8]++;
                case "J" -> counts[9]++;
                case "K" -> counts[10]++;
            }
        }

        return AllQuestionnaireResultTypeCount.builder()
                .countA(counts[0]).countB(counts[1]).countC(counts[2]).countD(counts[3])
                .countE(counts[4]).countF(counts[5]).countG(counts[6]).countH(counts[7])
                .countI(counts[8]).countJ(counts[9]).countK(counts[10])
                .build();
    }

}
 |

---
#### 비밀번호 초기화
**PUT** `{{host}}/admin/account/reset-password?adminId=25`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NDU5MTE2NywiZXhwIjoxNjk0NTk4MzY3fQ.KQoUYzjQQ6XIMOoXHvJOoABZYmV435DHajWuEi8YqW4 |

---
#### 관리자 삭제
**DELETE** `{{host}}/admin/account?adminId=25`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NDU5MTE2NywiZXhwIjoxNjk0NTk4MzY3fQ.KQoUYzjQQ6XIMOoXHvJOoABZYmV435DHajWuEi8YqW4 |

---
#### Access Token 재발급
**PUT** `{{host}}/login/access-token`
### Request Headers
| Key | Value |
|-----|--------|
| RefreshToken | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDAiLCJyb2xlcyI6IlJPTEVfVVNFUiIsImlhdCI6MTY5MjE0MDY2OCwiZXhwIjoxNjkzMzUwMjY4fQ.StFb0ZvSMoX3U2wX_OR2jXKIN8fsiPOzZt4-hN4orcA |
| appVersion | 1.1.1 |
| deviceType | iOS |

---
#### 지갑생성
**GET** ``

---
#### 기관생성
**POST** `{{host}}/organizations`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImFkbWluSXNTdXBlciI6Ik4iLCJvcmdhbml6YXRpb25JZCI6NSwiaWF0IjoxNzYyMTE4ODIzLCJleHAiOjE3NjIxMjYwMjN9.aIrXLQYaWQ6r6QMbtOBms9Yg_H-BmkbSodpd2TsTxQg |
### Request Body
```json
{
    "organizationName" : "Test339",
    "organizationPhoneNumber" : "01023349998",
    "subscriptionPlanId" : 3
}
```

---
#### 기관생성 Copy
**GET** `localhost:8080/admin/subscriptions/all`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMyIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTc2MDkxMjAwNywiZXhwIjoxNzYwOTE5MjA3fQ.TmLgMj2aspTJYn8E6k35VfozuuWk7BFSwxAnTaPo7DA |

---
#### 기관중복확인
**POST** `{{host}}/organizations`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3Iiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzU5NjU1MjEyLCJleHAiOjE3NTk2NjI0MTJ9.j0t8lhdBPEdoSOwko4YIc4gT8-Hph_mhE9vyRn7AHQY |
### Request Body
```json
{
    "organizationName" : "Test33",
    "subscriptionPlanId" : 3
}
```

---
#### 파일일괄등록
**POST** `localhost:8080/admin/user/bulk-upload`
### Request Headers
| Key | Value |
|-----|--------|
| appVersion | 1.1.1 |
| deviceType | iOS |
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImFkbWluSXNTdXBlciI6Ik4iLCJvcmdhbml6YXRpb25JZCI6NSwiaWF0IjoxNzYxODg4NzYxLCJleHAiOjE3NjE4OTU5NjF9.y1N7GZ0axV6m4ayvqiWagj9hmpmQIJpgxkf2SrOPSpo |
### Request Form Data
| Key | Type | Value |
|-----|------|--------|
| file | file |  |

---
#### New Request
**GET** `{{host}}/admin/organization/organization`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImFkbWluSXNTdXBlciI6Ik4iLCJvcmdhbml6YXRpb25JZCI6NSwiaWF0IjoxNzYyMTU3MTEyLCJleHAiOjE3NjIxNjQzMTJ9.YzSBdwq8WVtsqUkalAXnSUFNo2IqK1XkAt-PJoG3v5o |

---
#### 기관정보조회_구독정보_super
**GET** `localhost:8080/admin/organization/organization`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3Iiwicm9sZXMiOiJST0xFX0FETUlOIiwiYWRtaW5Jc1N1cGVyIjoiWSIsIm9yZ2FuaXphdGlvbklkIjoxLCJpYXQiOjE3NjE2MjM3NjQsImV4cCI6MTc2MTYzMDk2NH0.bU_Jaszxpcxy9SeCYmfmsGJq8frrfYNWnJQoPtzW1vA |

---
#### 기관정보조회_구독정보_일반
**GET** `localhost:8080/admin/statistic/me`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImFkbWluSXNTdXBlciI6Ik4iLCJvcmdhbml6YXRpb25JZCI6NSwiaWF0IjoxNzYxNTMxNjEyLCJleHAiOjE3NjE1Mzg4MTJ9.4mCTZ18cBOwrCzRZnH1wOtJcpp9WZacUpTRwwkeL78s |

---
#### 기관정보조회_구독정보_super
**GET** `localhost:8080/admin/organization/my`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImFkbWluSXNTdXBlciI6Ik4iLCJvcmdhbml6YXRpb25JZCI6NSwiaWF0IjoxNzYxMzg4MTMwLCJleHAiOjE3NjEzOTUzMzB9.AFGwFO4aj6H_adWqcJNltaqW7quf_rsf28y_Uvzj7Bo |

---
#### 기관정보조회_사용량정보
**GET** `localhost:8080/admin/users/usage`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3Iiwicm9sZXMiOiJST0xFX0FETUlOIiwiYWRtaW5Jc1N1cGVyIjoiWSIsIm9yZ2FuaXphdGlvbklkIjoxLCJpYXQiOjE3NjE2MjM3NjQsImV4cCI6MTc2MTYzMDk2NH0.bU_Jaszxpcxy9SeCYmfmsGJq8frrfYNWnJQoPtzW1vA |

---
#### 기관별 사용자 통계_사용량정보
**GET** `localhost:8080/admin/statistic/me`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3Iiwicm9sZXMiOiJST0xFX0FETUlOIiwiYWRtaW5Jc1N1cGVyIjoiWSIsIm9yZ2FuaXphdGlvbklkIjoxLCJpYXQiOjE3NjE2MjM3NjQsImV4cCI6MTc2MTYzMDk2NH0.bU_Jaszxpcxy9SeCYmfmsGJq8frrfYNWnJQoPtzW1vA |

---
#### 기관별 사용자 통계_사용량정보 Copy
**GET** `localhost:8080/admin/statistic/me`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3Iiwicm9sZXMiOiJST0xFX0FETUlOIiwiYWRtaW5Jc1N1cGVyIjoiWSIsIm9yZ2FuaXphdGlvbklkIjoxLCJpYXQiOjE3NjE2MjM3NjQsImV4cCI6MTc2MTYzMDk2NH0.bU_Jaszxpcxy9SeCYmfmsGJq8frrfYNWnJQoPtzW1vA |

---
#### 기관별 사용자 통계_두번째페이지
**GET** `localhost:8080/admin/statistic/org/users`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImFkbWluSXNTdXBlciI6Ik4iLCJvcmdhbml6YXRpb25JZCI6NSwiaWF0IjoxNzYxMzg4MTMwLCJleHAiOjE3NjEzOTUzMzB9.AFGwFO4aj6H_adWqcJNltaqW7quf_rsf28y_Uvzj7Bo |

---
#### 사용자 리스트
**GET** `localhost:8080/admin/user/users?page=1&size=30`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImFkbWluSXNTdXBlciI6Ik4iLCJvcmdhbml6YXRpb25JZCI6NSwiaWF0IjoxNzYxMzg4MTMwLCJleHAiOjE3NjEzOTUzMzB9.AFGwFO4aj6H_adWqcJNltaqW7quf_rsf28y_Uvzj7Bo |

---
#### 사용자 리스트 Copy
**GET** `localhost:8080/admin/user/users?page=1&size=30`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImFkbWluSXNTdXBlciI6Ik4iLCJvcmdhbml6YXRpb25JZCI6NSwiaWF0IjoxNzYxMzg4MTMwLCJleHAiOjE3NjEzOTUzMzB9.AFGwFO4aj6H_adWqcJNltaqW7quf_rsf28y_Uvzj7Bo |

---
### 📁 통계
#### 사용자 통계
**GET** `localhost:8080/admin/statistic/org/users`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImFkbWluSXNTdXBlciI6Ik4iLCJvcmdhbml6YXRpb25JZCI6NSwiaWF0IjoxNzYxMDMwNTYzLCJleHAiOjE3NjEwMzc3NjN9.4aBEF4Dwhbe9VksdMjw0mlZAWN1w_FsgZddtV34w98c |

---
#### New Request
**GET** ``

---
### 📁 사용자 관리
#### 사용자 인증
**PUT** `{{host}}/admin/user/verify?userId=22`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzU5ODgzODE4LCJleHAiOjE3NTk4OTEwMTh9.-dQ2LqGkbPuzGmhLtzVmzWjQaSL0-hPyNuCymBX98Y8 |

---
#### 사용자 정보 조회
**GET** `{{host}}/admin/user/info?userId=140`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NTcwNTg4NiwiZXhwIjoxNjk1NzEzMDg2fQ.UMtooybvo02JZNgMhl5wNej77vm3yKGGAQ1DfENiWIs |

---
#### 사용자 정보 수정
**PUT** `{{host}}/admin/user`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NTcwNTg4NiwiZXhwIjoxNjk1NzEzMDg2fQ.UMtooybvo02JZNgMhl5wNej77vm3yKGGAQ1DfENiWIs |
### Request Body
```json
{
    "userId": 140,
    "userLoginIdentifier": "asdasd123",
    "userName": "김덴",
    "userGender": "M"
}
```

---
#### 사용자 목록 조회
**GET** `localhost:8080/admin/user?page=1&size=10`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3Iiwicm9sZXMiOiJST0xFX0FETUlOIiwiYWRtaW5Jc1N1cGVyIjoiWSIsIm9yZ2FuaXphdGlvbklkIjoxLCJpYXQiOjE3NjE1MjQ4NjYsImV4cCI6MTc2MTUzMjA2Nn0.-psZGgiXoHMbSX7uxHtKTlNNWwjgB7Z1uIxb2sqRIS8 |

---
#### 사용자 목록 조회_검색
**GET** `localhost:8080/admin/user?page=1&size=50&userIdentifierOrName=guseob&userGender=M&isVerify=Y`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3Iiwicm9sZXMiOiJST0xFX0FETUlOIiwiYWRtaW5Jc1N1cGVyIjoiWSIsIm9yZ2FuaXphdGlvbklkIjoxLCJpYXQiOjE3NjE1MjQ4NjYsImV4cCI6MTc2MTUzMjA2Nn0.-psZGgiXoHMbSX7uxHtKTlNNWwjgB7Z1uIxb2sqRIS8 |
| ss |  |

---
#### 사용자 목록 조회 Copy
**GET** `{{host}}/admin/user/25/wallet`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzU5OTE3Nzk4LCJleHAiOjE3NTk5MjQ5OTh9.1CF9f90Lfud8iXkppHv38qvEKM9QdmHY5aXQM5zlQuo |

---
#### 사용자 삭제
**DELETE** `{{host}}/admin/user?userId=20`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NDU5MTg0MywiZXhwIjoxNjk0NTk5MDQzfQ.XR1A8AZeY88kgp3336Gmwmw25jbAcNjNDa-KXKpHYP0 |

---
### 📁 환자 관리
#### 환자 등록
**POST** `{{host}}/admin/patient`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization |  |
### Request Body
```json
{
    "patientName" : "관리자환자님입니다",
    "patientPhoneNumber" : "01056781232"
}
```

---
#### 환자 목록 조회
**GET** `{{host}}/admin/patient?page=1&size=10`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization |  |

---
#### 환자 삭제
**DELETE** `{{host}}/admin/patient?patientId=1`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization |  |

---
### 📁 블록체인_토큰
#### 사용자  검색_및_목록
**GET** `{{host}}/admin/user?page=1&size=10`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMjQwMDMyLCJleHAiOjE3NjAyNDcyMzJ9.Arkfd4ngExrp3gKqtnyESHmB_lYfALvFL0V4hNn_KCE |

---
#### 토큰 미션 검색
**GET** `43.201.106.70:8080/admin/missions/search?active=true&keyword=리워&page=1&size=10`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMzEzMDA5LCJleHAiOjE3NjAzMjAyMDl9.gn5WDM8EiNfY5e6V5LCFvUnQjQfjR6FAICuwmPQBR2E |

---
#### 관리자 거래주소 조회
**GET** `localhost:8080/admin/wallet/contracts`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMzM0MzE3LCJleHAiOjE3NjAzNDE1MTd9.9PINDYSB3FN2bofFFCaNPJaGTNAZhKMJDrFpg81qfdo |

---
#### 사용자 지갑주소
**GET** `localhost:8080/admin/user/25/wallet`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMTcwMTE4LCJleHAiOjE3NjAxNzczMTh9.gcY9P4EXC0XijQke0mi921xMjmdXrKbYm9AtXY91yXc |

---
#### 사용자  거래내역 전체
**GET** `localhost:8080/admin/wallet/users`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMzIwNDY5LCJleHAiOjE3NjAzMjc2Njl9.6NBN2hmB7htz6uezQkRD34MUWqsAen1kLhPN_99ZOxs |

---
#### 관리자 발행&리워드
**GET** `localhost:8080/admin/wallet/issue-reward`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMzIwNDY5LCJleHAiOjE3NjAzMjc2Njl9.6NBN2hmB7htz6uezQkRD34MUWqsAen1kLhPN_99ZOxs |

---
#### 관리자 충전&회수
**GET** `localhost:8080/admin/wallet/charge-reclaim?category=CHARGE_RECLAIM`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMzMwMzcwLCJleHAiOjE3NjAzMzc1NzB9.rlGUFw7U_4MrcLiD79Rc0wEmUgoZ7HA4fNEcmI2HDrY |

---
#### 관리자 거래주소
**GET** `localhost:8080/admin/wallet/summary?contractAddress=0x70d4cD2A9203e516d1569BBeFAEcb760053efb74fca`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMzM0MzE3LCJleHAiOjE3NjAzNDE1MTd9.9PINDYSB3FN2bofFFCaNPJaGTNAZhKMJDrFpg81qfdo |

---
#### 사용자  검색_및_목록 Copy 2
**GET** `localhost:8080/admin/wallet/by-contract?contractAddress=0x182827C979cd00DAB4C02C812fCdF2D3E84AeD5afca&page=1&size=10`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMTcwMTE4LCJleHAiOjE3NjAxNzczMTh9.gcY9P4EXC0XijQke0mi921xMjmdXrKbYm9AtXY91yXc |

---
#### 토큰전송
**POST** `http://localhost:8080/admin/wallet/transfer`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMTcwMTE4LCJleHAiOjE3NjAxNzczMTh9.gcY9P4EXC0XijQke0mi921xMjmdXrKbYm9AtXY91yXc |
### Request Body
```json
{
  "userLoginIdentifier": "cash0000",
  "amount": 10,
  "reason": "테스트 보상 지급"
}
```

---
#### 지갑 동기화_거래내역별
**POST** `http://localhost:8080/admin/wallet/sync-all`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMDk5NDA5LCJleHAiOjE3NjAxMDY2MDl9.Z6OWzwEf9OX5QSO6XDKZPSct6NQFJxMhnZJeLmWFBWI |
### Request Body
```json
{
    "contract_address":"0x182827C979cd00DAB4C02C812fCdF2D3E84AeD5afca"
}
```

---
#### 지갑 동기화_전체
**POST** `http://localhost:8080/admin/wallet/sync-balances`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMjYzMjIwLCJleHAiOjE3NjAyNzA0MjB9.Abo-ABsz1H6qi0jDa3Cj4KV6WcAbW_sI9sNjPFng4o8 |

---
#### 토큰충전
**POST** `http://localhost:8080/admin/wallet/create`
### Request Headers
| Key | Value |
|-----|--------|
| appVersion | 1.1.1 |
| deviceType | iOS |
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMTQzMDg2LCJleHAiOjE3NjAxNTAyODZ9.meYGuLRsqB2IBSPceo4dgpIfsuF3EUtt_THE8XS_7WA |
### Request Body
```json
{
  "token_name": "Dentix",
  "token_symbol": "DTX",
  "supply": 100
}
```

---
#### 관리자 전체거래 내역
**GET** `{{host}}/admin/wallet/ledger`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMzIyNzAxLCJleHAiOjE3NjAzMjk5MDF9.HqTAl7rxmXGx3NtuewUok-eYLyD6ce5VauV4JrDP6kQ |

---
#### 관리자 거래주소 토큰조회
**GET** `localhost:8080/admin/wallet/issue-manage`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMzIyNzAxLCJleHAiOjE3NjAzMjk5MDF9.HqTAl7rxmXGx3NtuewUok-eYLyD6ce5VauV4JrDP6kQ |

---
#### 관리자 거래주소 내역조회 Copy
**GET** `localhost:8080/admin/wallet/summary`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMTQ0ODg5LCJleHAiOjE3NjAxNTIwODl9.xsu0a_z6sJM3p0FlhZ1p6lssKnfi_jVAANh2xNLpeVM |

---
#### 관리자 상세거래주소 내역조회
**GET** `localhost:8080/admin/wallet/summary?contractAddress=0x70d4cD2A9203e516d1569BBeFAEcb760053efb74fca`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMjQwMDMyLCJleHAiOjE3NjAyNDcyMzJ9.Arkfd4ngExrp3gKqtnyESHmB_lYfALvFL0V4hNn_KCE |

---
#### 관리자 상세거래 내역조회
**GET** `localhost:8080/admin/wallet/ledger?contractAddress=0x182827C979cd00DAB4C02C812fCdF2D3E84AeD5afca`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMTQ0ODg5LCJleHAiOjE3NjAxNTIwODl9.xsu0a_z6sJM3p0FlhZ1p6lssKnfi_jVAANh2xNLpeVM |

---
#### 관리자 토큰_사용자 상세내역
**GET** `localhost:8080/admin/wallet/balance`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMTQ0ODg5LCJleHAiOjE3NjAxNTIwODl9.xsu0a_z6sJM3p0FlhZ1p6lssKnfi_jVAANh2xNLpeVM |
### Request Body
```json
{
  "page": 1,
  "size": 10,
  "sort": "DESC"
}
```

---
#### 관리자 토큰_사용자 상세내역 Copy
**POST** `localhost:8080/admin/wallet/ledger/list`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMTQ0ODg5LCJleHAiOjE3NjAxNTIwODl9.xsu0a_z6sJM3p0FlhZ1p6lssKnfi_jVAANh2xNLpeVM |
### Request Body
```json
{
  "page": 1,
  "size": 10,
  "sort": "DESC"
}
```

---
#### 관리자 토큰_사용자로부터회수
**POST** `localhost:8080/admin/wallet/retrieve/25`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMjQwMDMyLCJleHAiOjE3NjAyNDcyMzJ9.Arkfd4ngExrp3gKqtnyESHmB_lYfALvFL0V4hNn_KCE |
### Request Body
```json
{
  "reason": "중복 지급 회수"
}
```

---
### New Request
**GET** `localhost:8080/admin/user`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMDc1NzgwLCJleHAiOjE3NjAwODI5ODB9._7cLcVpIkfT9V61qRFe-EQcv_bmUj9GjRUtG9hVkj0E |

---
### 구독상품변경
**PUT** `localhost:8080/admin/subscriptions/organization/1/3`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImFkbWluSXNTdXBlciI6Ik4iLCJvcmdhbml6YXRpb25JZCI6NSwiaWF0IjoxNzYxNDM3MTQ3LCJleHAiOjE3NjE0NDQzNDd9.gSjesYj_YGZSMNhOrNEaQLOwTIHw2Ky95WyDP_t-0nI |

---
## 📁 SUPER_ADMIN
### 📁 관리자
#### 관리자 등록
**POST** `{{host}}localhost:8080/admin/account`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NDU5MTE2NywiZXhwIjoxNjk0NTk4MzY3fQ.KQoUYzjQQ6XIMOoXHvJOoABZYmV435DHajWuEi8YqW4 |
### Request Body
```json
{
    "adminName" : "denti-cash",
    "adminLoginIdentifier" : "denti-cash",
    "adminPhoneNumber" : "01010041004"
}
```

---
#### 로그인
**POST** `localhost:8080/login`
### Request Body
```json
{"userType":"admin",
    "loginId" : "admin",
    "password" : "test1234!"
}
```

---
#### 자동 로그인
**PUT** `{{host}}/admin/account/auto-login`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NDU5MTE2NywiZXhwIjoxNjk0NTk4MzY3fQ.KQoUYzjQQ6XIMOoXHvJOoABZYmV435DHajWuEi8YqW4 |

---
#### 비밀번호 변경
**PUT** `{{host}}/admin/account/password`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NDU5MTE2NywiZXhwIjoxNjk0NTk4MzY3fQ.KQoUYzjQQ6XIMOoXHvJOoABZYmV435DHajWuEi8YqW4 |
### Request Body
```json
{
    "adminPassword" : "dentixadmin123!"
}
```

---
#### 관리자 목록 조회
**GET** `{{host}}/admin/account/list?page=1&size=10`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NDU5MTE2NywiZXhwIjoxNjk0NTk4MzY3fQ.KQoUYzjQQ6XIMOoXHvJOoABZYmV435DHajWuEi8YqW4 |

---
#### 비밀번호 초기화
**PUT** `{{host}}/admin/account/reset-password?adminId=25`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NDU5MTE2NywiZXhwIjoxNjk0NTk4MzY3fQ.KQoUYzjQQ6XIMOoXHvJOoABZYmV435DHajWuEi8YqW4 |

---
#### 관리자 삭제
**DELETE** `{{host}}/admin/account?adminId=25`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NDU5MTE2NywiZXhwIjoxNjk0NTk4MzY3fQ.KQoUYzjQQ6XIMOoXHvJOoABZYmV435DHajWuEi8YqW4 |

---
#### Access Token 재발급
**PUT** `{{host}}/login/access-token`
### Request Headers
| Key | Value |
|-----|--------|
| RefreshToken | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDAiLCJyb2xlcyI6IlJPTEVfVVNFUiIsImlhdCI6MTY5MjE0MDY2OCwiZXhwIjoxNjkzMzUwMjY4fQ.StFb0ZvSMoX3U2wX_OR2jXKIN8fsiPOzZt4-hN4orcA |
| appVersion | 1.1.1 |
| deviceType | iOS |

---
#### 지갑생성
**GET** ``

---
#### 기관생성
**POST** `localhost:8080/organizations`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMyIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTc2MDg3ODE1MiwiZXhwIjoxNzYwODg1MzUyfQ.P1XLQWWtBsZwBNei5j1EK7Px1rCbg8R2JLaRGgdCcB0 |
### Request Body
```json
{
    "organizationName" : "Test33331",
    "organizationPhoneNumber" : "01023343678",
    "subscriptionPlanId" : 3
}
```

---
#### 기관생성 Copy
**GET** `localhost:8080/admin/subscriptions/all`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMyIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTc2MDkxMjAwNywiZXhwIjoxNzYwOTE5MjA3fQ.TmLgMj2aspTJYn8E6k35VfozuuWk7BFSwxAnTaPo7DA |

---
#### 기관중복확인
**POST** `{{host}}/organizations`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3Iiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzU5NjU1MjEyLCJleHAiOjE3NTk2NjI0MTJ9.j0t8lhdBPEdoSOwko4YIc4gT8-Hph_mhE9vyRn7AHQY |
### Request Body
```json
{
    "organizationName" : "Test33",
    "subscriptionPlanId" : 3
}
```

---
#### New Request
**GET** ``

---
#### New Request Copy
**GET** ``

---
### 📁 통계
#### 사용자 통계
**GET** `{{host}}/admin/statistic`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NDU5MTE2NywiZXhwIjoxNjk0NTk4MzY3fQ.KQoUYzjQQ6XIMOoXHvJOoABZYmV435DHajWuEi8YqW4 |

---
#### New Request
**GET** ``

---
### 📁 사용자 관리
#### 사용자 인증
**PUT** `{{host}}/admin/user/verify?userId=22`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzU5ODgzODE4LCJleHAiOjE3NTk4OTEwMTh9.-dQ2LqGkbPuzGmhLtzVmzWjQaSL0-hPyNuCymBX98Y8 |

---
#### 사용자 정보 조회
**GET** `{{host}}/admin/user/info?userId=140`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NTcwNTg4NiwiZXhwIjoxNjk1NzEzMDg2fQ.UMtooybvo02JZNgMhl5wNej77vm3yKGGAQ1DfENiWIs |

---
#### 사용자 정보 수정
**PUT** `{{host}}/admin/user`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NTcwNTg4NiwiZXhwIjoxNjk1NzEzMDg2fQ.UMtooybvo02JZNgMhl5wNej77vm3yKGGAQ1DfENiWIs |
### Request Body
```json
{
    "userId": 140,
    "userLoginIdentifier": "asdasd123",
    "userName": "김덴",
    "userGender": "M"
}
```

---
#### 사용자 목록 조회
**GET** `localhost:8080/admin/user?page=1&size=10`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMyIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTc2MDk2MDk4MCwiZXhwIjoxNzYwOTY4MTgwfQ.6uojFEp4-OVyu9FiyHA42pO6AHL-d6b1gZRY_C-0akY |

---
#### 사용자 목록 조회_검색
**GET** `{{host}}/admin/user?page=1&size=10&isVerify=Y&hasWallet=true&allDatePeriod=MONTH3&startDate=2025-07-01&endDate=2025-10-08`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzU5OTE3Nzk4LCJleHAiOjE3NTk5MjQ5OTh9.1CF9f90Lfud8iXkppHv38qvEKM9QdmHY5aXQM5zlQuo |

---
#### 사용자 목록 조회 Copy
**GET** `{{host}}/admin/user/25/wallet`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzU5OTE3Nzk4LCJleHAiOjE3NTk5MjQ5OTh9.1CF9f90Lfud8iXkppHv38qvEKM9QdmHY5aXQM5zlQuo |

---
#### 사용자 삭제
**DELETE** `{{host}}/admin/user?userId=20`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImlhdCI6MTY5NDU5MTg0MywiZXhwIjoxNjk0NTk5MDQzfQ.XR1A8AZeY88kgp3336Gmwmw25jbAcNjNDa-KXKpHYP0 |

---
### 📁 환자 관리
#### 환자 등록
**POST** `{{host}}/admin/patient`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization |  |
### Request Body
```json
{
    "patientName" : "관리자환자님입니다",
    "patientPhoneNumber" : "01056781232"
}
```

---
#### 환자 목록 조회
**GET** `{{host}}/admin/patient?page=1&size=10`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization |  |

---
#### 환자 삭제
**DELETE** `{{host}}/admin/patient?patientId=1`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization |  |

---
### 📁 블록체인_토큰
#### 사용자  검색_및_목록
**GET** `{{host}}/admin/user?page=1&size=10`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMjQwMDMyLCJleHAiOjE3NjAyNDcyMzJ9.Arkfd4ngExrp3gKqtnyESHmB_lYfALvFL0V4hNn_KCE |

---
#### 토큰 미션 검색
**GET** `43.201.106.70:8080/admin/missions/search?active=true&keyword=리워&page=1&size=10`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMzEzMDA5LCJleHAiOjE3NjAzMjAyMDl9.gn5WDM8EiNfY5e6V5LCFvUnQjQfjR6FAICuwmPQBR2E |

---
#### 관리자 거래주소 조회
**GET** `localhost:8080/admin/wallet/contracts`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMzM0MzE3LCJleHAiOjE3NjAzNDE1MTd9.9PINDYSB3FN2bofFFCaNPJaGTNAZhKMJDrFpg81qfdo |

---
#### 사용자 지갑주소
**GET** `localhost:8080/admin/user/25/wallet`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMTcwMTE4LCJleHAiOjE3NjAxNzczMTh9.gcY9P4EXC0XijQke0mi921xMjmdXrKbYm9AtXY91yXc |

---
#### 사용자  거래내역 전체
**GET** `localhost:8080/admin/wallet/users`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMzIwNDY5LCJleHAiOjE3NjAzMjc2Njl9.6NBN2hmB7htz6uezQkRD34MUWqsAen1kLhPN_99ZOxs |

---
#### 관리자 발행&리워드
**GET** `localhost:8080/admin/wallet/issue-reward`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMzIwNDY5LCJleHAiOjE3NjAzMjc2Njl9.6NBN2hmB7htz6uezQkRD34MUWqsAen1kLhPN_99ZOxs |

---
#### 관리자 충전&회수
**GET** `localhost:8080/admin/wallet/charge-reclaim?category=CHARGE_RECLAIM`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMzMwMzcwLCJleHAiOjE3NjAzMzc1NzB9.rlGUFw7U_4MrcLiD79Rc0wEmUgoZ7HA4fNEcmI2HDrY |

---
#### 관리자 거래주소
**GET** `localhost:8080/admin/wallet/summary?contractAddress=0x70d4cD2A9203e516d1569BBeFAEcb760053efb74fca`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMzM0MzE3LCJleHAiOjE3NjAzNDE1MTd9.9PINDYSB3FN2bofFFCaNPJaGTNAZhKMJDrFpg81qfdo |

---
#### 사용자  검색_및_목록 Copy 2
**GET** `localhost:8080/admin/wallet/by-contract?contractAddress=0x182827C979cd00DAB4C02C812fCdF2D3E84AeD5afca&page=1&size=10`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMTcwMTE4LCJleHAiOjE3NjAxNzczMTh9.gcY9P4EXC0XijQke0mi921xMjmdXrKbYm9AtXY91yXc |

---
#### 토큰전송
**POST** `http://localhost:8080/admin/wallet/transfer`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMTcwMTE4LCJleHAiOjE3NjAxNzczMTh9.gcY9P4EXC0XijQke0mi921xMjmdXrKbYm9AtXY91yXc |
### Request Body
```json
{
  "userLoginIdentifier": "cash0000",
  "amount": 10,
  "reason": "테스트 보상 지급"
}
```

---
#### 지갑 동기화_거래내역별
**POST** `http://localhost:8080/admin/wallet/sync-all`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMDk5NDA5LCJleHAiOjE3NjAxMDY2MDl9.Z6OWzwEf9OX5QSO6XDKZPSct6NQFJxMhnZJeLmWFBWI |
### Request Body
```json
{
    "contract_address":"0x182827C979cd00DAB4C02C812fCdF2D3E84AeD5afca"
}
```

---
#### 지갑 동기화_전체
**POST** `http://localhost:8080/admin/wallet/sync-balances`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMjYzMjIwLCJleHAiOjE3NjAyNzA0MjB9.Abo-ABsz1H6qi0jDa3Cj4KV6WcAbW_sI9sNjPFng4o8 |

---
#### 토큰충전
**POST** `http://localhost:8080/admin/wallet/create`
### Request Headers
| Key | Value |
|-----|--------|
| appVersion | 1.1.1 |
| deviceType | iOS |
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMTQzMDg2LCJleHAiOjE3NjAxNTAyODZ9.meYGuLRsqB2IBSPceo4dgpIfsuF3EUtt_THE8XS_7WA |
### Request Body
```json
{
  "token_name": "Dentix",
  "token_symbol": "DTX",
  "supply": 100
}
```

---
#### 관리자 전체거래 내역
**GET** `{{host}}/admin/wallet/ledger`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMzIyNzAxLCJleHAiOjE3NjAzMjk5MDF9.HqTAl7rxmXGx3NtuewUok-eYLyD6ce5VauV4JrDP6kQ |

---
#### 관리자 거래주소 토큰조회
**GET** `localhost:8080/admin/wallet/issue-manage`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMzIyNzAxLCJleHAiOjE3NjAzMjk5MDF9.HqTAl7rxmXGx3NtuewUok-eYLyD6ce5VauV4JrDP6kQ |

---
#### 관리자 거래주소 내역조회 Copy
**GET** `localhost:8080/admin/wallet/summary`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMTQ0ODg5LCJleHAiOjE3NjAxNTIwODl9.xsu0a_z6sJM3p0FlhZ1p6lssKnfi_jVAANh2xNLpeVM |

---
#### 관리자 상세거래주소 내역조회
**GET** `localhost:8080/admin/wallet/summary?contractAddress=0x70d4cD2A9203e516d1569BBeFAEcb760053efb74fca`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMjQwMDMyLCJleHAiOjE3NjAyNDcyMzJ9.Arkfd4ngExrp3gKqtnyESHmB_lYfALvFL0V4hNn_KCE |

---
#### 관리자 상세거래 내역조회
**GET** `localhost:8080/admin/wallet/ledger?contractAddress=0x182827C979cd00DAB4C02C812fCdF2D3E84AeD5afca`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMTQ0ODg5LCJleHAiOjE3NjAxNTIwODl9.xsu0a_z6sJM3p0FlhZ1p6lssKnfi_jVAANh2xNLpeVM |

---
#### 관리자 토큰_사용자 상세내역
**GET** `localhost:8080/admin/wallet/balance`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMTQ0ODg5LCJleHAiOjE3NjAxNTIwODl9.xsu0a_z6sJM3p0FlhZ1p6lssKnfi_jVAANh2xNLpeVM |
### Request Body
```json
{
  "page": 1,
  "size": 10,
  "sort": "DESC"
}
```

---
#### 관리자 토큰_사용자 상세내역 Copy
**POST** `localhost:8080/admin/wallet/ledger/list`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMTQ0ODg5LCJleHAiOjE3NjAxNTIwODl9.xsu0a_z6sJM3p0FlhZ1p6lssKnfi_jVAANh2xNLpeVM |
### Request Body
```json
{
  "page": 1,
  "size": 10,
  "sort": "DESC"
}
```

---
#### 관리자 토큰_사용자로부터회수
**POST** `localhost:8080/admin/wallet/retrieve/25`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOiJST0xFX0FETUlOIiwiaWF0IjoxNzYwMjQwMDMyLCJleHAiOjE3NjAyNDcyMzJ9.Arkfd4ngExrp3gKqtnyESHmB_lYfALvFL0V4hNn_KCE |
### Request Body
```json
{
  "reason": "중복 지급 회수"
}
```

---
### New Request
**GET** `http://localhost:8080/api/aws/metrics/summary`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3Iiwicm9sZXMiOiJST0xFX0FETUlOIiwiYWRtaW5Jc1N1cGVyIjoiWSIsIm9yZ2FuaXphdGlvbklkIjoxLCJpYXQiOjE3NjE5OTI0NjEsImV4cCI6MTc2MTk5OTY2MX0.1Qg1YlC1lT6Vv9lkXwaTl4rau_Soqp6vm-D11Bv5jDw |

---
### New Request Copy
**PUT** `localhost:8080/organizations/1/subscription/3`
### Request Headers
| Key | Value |
|-----|--------|
| Authorization | eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNSIsInJvbGVzIjoiUk9MRV9BRE1JTiIsImFkbWluSXNTdXBlciI6Ik4iLCJvcmdhbml6YXRpb25JZCI6NSwiaWF0IjoxNzYxNDM3MTQ3LCJleHAiOjE3NjE0NDQzNDd9.gSjesYj_YGZSMNhOrNEaQLOwTIHw2Ky95WyDP_t-0nI |

---
### New Request
**GET** ``

---
