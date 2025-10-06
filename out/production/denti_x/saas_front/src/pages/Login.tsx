import { useNavigate } from "react-router-dom";
import { useState } from "react";
import {
  Box,
  Button,
  Flex,
  FormControl,
  FormLabel,
  Image,
  Input,
  Select,
  Tab,
  TabList,
  Tabs,
  Text,
  useToast,
} from "@chakra-ui/react";
import { Link as RouterLink } from "react-router-dom";
import api from "../api"; // ✅ default export인 axios 인스턴스
import type { AxiosError } from "axios";

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  isFirstLogin: string;
  adminId?: number;
  adminName?: string;
  adminIsSuper?: string;
  userId?: number;
  userName?: string;
}

interface ApiResponse {
  rt?: number;
  rtMsg?: string;
  response?: LoginResponse;
}

export default function Login() {
  const [language, setLanguage] = useState("ko");
  const [focusedField, setFocusedField] = useState<string | null>(null);
  const [selectedTab, setSelectedTab] = useState<"user" | "admin">("user");
  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const toast = useToast();
  const navigate = useNavigate();

  const handleFocus = (field: string) => setFocusedField(field);
  const handleBlur = () => setFocusedField(null);

  const handleLogin = async () => {
    try {
      const body = { userType: selectedTab, loginId, password };
      const res = await api.post<ApiResponse>("/login", body, {
        headers: { "Content-Type": "application/json" },
      });

      console.log("✅ 로그인 응답:", res.data);

      const data: LoginResponse =
        res.data.response ?? (res.data as LoginResponse);

      if (!data.accessToken) {
        throw new Error("accessToken이 없습니다.");
      }

      localStorage.setItem("accessToken", data.accessToken);
      localStorage.setItem("refreshToken", data.refreshToken);
      localStorage.setItem("userType", selectedTab);

      if (selectedTab === "admin") {
        localStorage.setItem("adminName", data.adminName || "");
        localStorage.setItem("adminId", data.adminId?.toString() || "");
      } else {
        localStorage.setItem("userName", data.userName || "");
        localStorage.setItem("userId", data.userId?.toString() || "");
      }

      toast({
        title: "로그인 성공",
        description:
          selectedTab === "admin"
            ? `${data.adminName || "관리자"}님 환영합니다!`
            : `${data.userName || "회원"}님 환영합니다!`,
        status: "success",
        duration: 2000,
        isClosable: true,
      });

      navigate("/admin/users");
    } catch (error) {
      const axiosError = error as AxiosError<ApiResponse>;
      console.error("❌ 로그인 에러:", axiosError);

      const message =
        axiosError.response?.data?.rtMsg ??
        axiosError.response?.data?.response ??
        axiosError.message ??
        "아이디 또는 비밀번호를 확인하세요.";

      setError(typeof message === "string" ? message : "로그인 실패");

      toast({
        title: "로그인 실패",
        description: typeof message === "string" ? message : "로그인 실패",
        status: "error",
        duration: 2500,
        isClosable: true,
      });
    }
  };

  return (
    <Flex minH="100vh" align="center" justify="center" bg="white" px={6}>
      <Box w="100%" maxW="400px">
        <Flex justify="space-between" align="center" mb={6}>
          <Button
            variant="ghost"
            p={0}
            minW="auto"
            onClick={() => navigate(-1)}
          >
            <Image src="/images/back.png" alt="뒤로가기" w="14px" h="14px" />
          </Button>

          <Select
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            w="120px"
            borderRadius="md"
            border="1px solid"
            borderColor="gray.300"
            fontSize="sm"
            textAlign="center"
            iconSize="16px"
          >
            <option value="ko">한국어</option>
            <option value="en">English</option>
            <option value="vi">베트남어</option>
          </Select>
        </Flex>

        <Image
          src="/images/denti_x.png"
          alt="Dentix Logo"
          mx="auto"
          mb={8}
          h="40px"
        />

        <Tabs
          variant="unstyled"
          align="center"
          mb={6}
          onChange={(index) => setSelectedTab(index === 0 ? "user" : "admin")}
        >
          <TabList borderBottom="1px solid" borderColor="gray.200">
            <Tab
              _selected={{
                color: "blue.600",
                borderBottom: "2px solid",
                borderColor: "blue.600",
              }}
              flex="1"
            >
              개인 회원
            </Tab>
            <Tab
              _selected={{
                color: "blue.600",
                borderBottom: "2px solid",
                borderColor: "blue.600",
              }}
              flex="1"
            >
              관리자 회원
            </Tab>
          </TabList>
        </Tabs>

        <Box mb={4}>
          <FormControl>
            <FormLabel>아이디</FormLabel>
            <Input
              type="text"
              value={loginId}
              onChange={(e) => setLoginId(e.target.value)}
              onFocus={() => handleFocus("id")}
              onBlur={handleBlur}
              borderColor={focusedField === "id" ? "red.400" : "gray.300"}
              focusBorderColor="red.400"
              _hover={{
                borderColor: focusedField === "id" ? "red.400" : "gray.400",
              }}
            />
          </FormControl>
        </Box>

        <Box mb={6}>
          <FormControl>
            <FormLabel>비밀번호</FormLabel>
            <Input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              onFocus={() => handleFocus("pw")}
              onBlur={handleBlur}
              borderColor={focusedField === "pw" ? "red.400" : "gray.300"}
              focusBorderColor="red.400"
              _hover={{
                borderColor: focusedField === "pw" ? "red.400" : "gray.400",
              }}
            />
          </FormControl>
        </Box>

        <Button
          colorScheme="blue"
          w="100%"
          h="50px"
          mb={4}
          onClick={handleLogin}
        >
          로그인
        </Button>

        {error && (
          <Text color="red.500" textAlign="center" mb={4}>
            {error}
          </Text>
        )}

        <Flex justify="center" gap={4} mt={4} fontSize="sm" color="gray.500">
          <RouterLink to="/register/verify">
            <Text as="span" color="blue.500" cursor="pointer">
              회원가입
            </Text>
          </RouterLink>
          <Text>|</Text>
          <RouterLink to="/find-password">
            <Text as="span" color="blue.500" cursor="pointer">
              비밀번호 찾기
            </Text>
          </RouterLink>
          <Text>|</Text>
          <RouterLink to="/contact" state={{ from: "login" }}>
            <Text as="span" color="blue.500" cursor="pointer">
              문의하기
            </Text>
          </RouterLink>
        </Flex>
      </Box>
    </Flex>
  );
}
