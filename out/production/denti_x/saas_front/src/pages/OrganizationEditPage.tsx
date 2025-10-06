import {
  Box,
  Flex,
  VStack,
  FormControl,
  FormLabel,
  Input,
  Select,
  Button,
  Heading,
  Text,
} from "@chakra-ui/react";

export default function OrganizationEditPage() {
  return (
    <Flex p={6} gap={8} align="flex-start">
      {/* 왼쪽: 기관 정보 수정 폼 */}
      <Box flex="2" bg="white" p={8} rounded="md" shadow="sm" maxW="600px">
        <VStack spacing={6} align="stretch">
          <FormControl isRequired>
            <FormLabel>기관명</FormLabel>
            <Input defaultValue="서울치과" />
          </FormControl>

          <FormControl>
            <FormLabel>기관명(영문)</FormLabel>
            <Input defaultValue="Seoul Dental Clinic" />
          </FormControl>

          <FormControl>
            <FormLabel>전화번호</FormLabel>
            <Input placeholder="02-123-4567" />
          </FormControl>

          <FormControl>
            <FormLabel>주소</FormLabel>
            <Input placeholder="서울특별시 강남구 ..." />
          </FormControl>

          <FormControl isRequired>
            <FormLabel>구독 상품</FormLabel>
            <Select defaultValue="medium">
              <option value="small">Small</option>
              <option value="medium">Medium</option>
              <option value="flexible">Flexible</option>
            </Select>
          </FormControl>

          <Flex justify="flex-end" mt={4} gap={4}>
            <Button variant="outline" colorScheme="gray">
              취소
            </Button>
            <Button colorScheme="blue">저장</Button>
          </Flex>
        </VStack>
      </Box>

      {/* 오른쪽: 구독 상품 안내 */}
      <Box
        flex="1"
        bg="gray.50"
        p={8}
        borderLeft="1px"
        borderColor="gray.200"
        rounded="md"
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
              소규모 기관을 위한 기본 기능 제공. <br />
              사용자 수: 최대 10명 <br />월 요금: ₩50,000
            </Text>
          </Box>

          <Box>
            <Text fontWeight="bold" color="green.600">
              Medium
            </Text>
            <Text fontSize="sm" color="gray.600">
              성장 단계 기관을 위한 확장 기능 제공. <br />
              사용자 수: 최대 50명 <br />월 요금: ₩200,000
            </Text>
          </Box>

          <Box>
            <Text fontWeight="bold" color="purple.600">
              Flexible
            </Text>
            <Text fontSize="sm" color="gray.600">
              대규모 기관 및 맞춤형 기능 제공. <br />
              사용자 수: 무제한 <br />월 요금: 협의
            </Text>
          </Box>
        </VStack>
      </Box>
    </Flex>
  );
}
