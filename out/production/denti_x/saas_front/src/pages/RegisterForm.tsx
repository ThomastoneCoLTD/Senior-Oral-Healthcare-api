// src/pages/RegisterForm.tsx
import {
  Box,
  Button,
  Flex,
  Heading,
  VStack,
  FormControl,
  FormLabel,
  Input,
  Select,
  Text,
  HStack,
  Image,
} from "@chakra-ui/react";

export default function RegisterForm() {
  return (
    <Flex direction="column" minH="100vh" bg="gray.50">
      {/* 🔹 이 페이지 전용 헤더 */}
      <Flex
        as="header"
        bg="white"
        borderBottom="1px"
        borderColor="gray.200"
        px={10}
        py={4}
        align="center"
        justify="space-between"
      >
        {/* 왼쪽: 로고 */}
        <HStack spacing={3}>
          <Image
            src="/public/images/DentiGlobal.png"
            alt="Logo"
            height="30px"
          />
          <Heading as="h1" size="md" color="blue.600">
            관리자 페이지
          </Heading>
        </HStack>

        {/* 오른쪽: 로그인 링크 */}
        <Text fontSize="sm" color="gray.600">
          이미 계정이 있으신가요?{" "}
          <Text as="span" color="blue.600" fontWeight="bold" cursor="pointer">
            로그인
          </Text>
        </Text>
      </Flex>

      {/* 🔹 본문 */}
      <Flex flex="1" align="center" justify="center" p={10}>
        <Flex
          bg="white"
          borderRadius="lg"
          shadow="md"
          w="100%"
          maxW="1000px"
          overflow="hidden"
        >
          {/* 왼쪽: 회원가입 폼 */}
          <Box flex="2" p={10}>
            <Heading as="h2" size="lg" mb={8} color="blue.600">
              관리자 회원가입
            </Heading>

            <VStack spacing={6} align="stretch">
              <FormControl isRequired>
                <FormLabel>아이디</FormLabel>
                <Input placeholder="아이디를 입력하세요" />
              </FormControl>

              <FormControl isRequired>
                <FormLabel>이름</FormLabel>
                <Input placeholder="이름을 입력하세요" />
              </FormControl>

              <FormControl isRequired>
                <FormLabel>핸드폰 번호</FormLabel>
                <Input placeholder="010-0000-0000" />
              </FormControl>

              <FormControl isRequired>
                <FormLabel>비밀번호</FormLabel>
                <Input type="password" placeholder="비밀번호를 입력하세요" />
              </FormControl>

              <FormControl isRequired>
                <FormLabel>기관명</FormLabel>
                <Input placeholder="기관명을 입력하세요" />
              </FormControl>

              <FormControl isRequired>
                <FormLabel>구독 상품</FormLabel>
                <Select placeholder="구독 상품을 선택하세요">
                  <option value="small">Small</option>
                  <option value="medium">Medium</option>
                  <option value="flexible">Flexible</option>
                </Select>
              </FormControl>

              <Button colorScheme="blue" size="lg" mt={4} alignSelf="flex-end">
                회원가입
              </Button>
            </VStack>
          </Box>

          {/* 오른쪽: 구독 상품 안내 */}
          <Box
            flex="1"
            bg="gray.50"
            p={8}
            borderLeft="1px"
            borderColor="gray.200"
          >
            <Heading as="h3" size="md" mb={6} color="gray.700">
              구독 상품 안내
            </Heading>

            <VStack align="stretch" spacing={6}>
              <Box>
                <Text fontWeight="bold" color="blue.600">
                  Small
                </Text>
                <Text fontSize="sm" color="gray.600">
                  소규모 기관을 위한 기본 기능 제공. 사용자 수: 최대 10명 월
                  요금: ₩50,000
                </Text>
              </Box>

              <Box>
                <Text fontWeight="bold" color="green.600">
                  Medium
                </Text>
                <Text fontSize="sm" color="gray.600">
                  성장 단계 기관을 위한 확장 기능 제공. 사용자 수: 최대 50명 월
                  요금: ₩200,000
                </Text>
              </Box>

              <Box>
                <Text fontWeight="bold" color="purple.600">
                  Flexible
                </Text>
                <Text fontSize="sm" color="gray.600">
                  대규모 기관 및 맞춤형 기능 제공. 사용자 수: 무제한 월 요금:
                  협의
                </Text>
              </Box>
            </VStack>
          </Box>
        </Flex>
      </Flex>
    </Flex>
  );
}
