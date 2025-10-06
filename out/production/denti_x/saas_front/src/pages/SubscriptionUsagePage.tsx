import {
  Box,
  Heading,
  Text,
  Progress,
  Table,
  Thead,
  Tbody,
  Tr,
  Th,
  Td,
  useColorModeValue,
  Flex,
  SimpleGrid,
} from "@chakra-ui/react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { useEffect, useState } from "react";
import api from "../api/api";

// ✅ 사용자 정보 타입
interface UserItem {
  userId: number;
  userName: string;
  successCount: number;
}

// ✅ API 응답 데이터 타입
interface SubscriptionInfo {
  organizationName: string;
  planName: string;
  planCycle: string;
  price: number;
  maxSuccessResponses: number;
  totalSuccessCount: number;
  remainingCount: number;
  usageRate: number;
  users: UserItem[];
}

export default function SubscriptionUsagePage() {
  const [data, setData] = useState<SubscriptionInfo | null>(null);

  const blueShades = [
    "#E9F0FF",
    "#DAE6FF",
    "#B5CCFF",
    "#90B3FF",
    "#6B99FF",
    "#4680FF",
    "#2281DF",
    "#1C76DA",
    "#176CD6",
    "#0D59CF",
  ];

  const bgCard = useColorModeValue("white", "gray.800");
  const textGray = useColorModeValue("gray.600", "gray.300");

  useEffect(() => {
    api
      .get<{ response: SubscriptionInfo }>("/admin/subscription/usage")
      .then((res) => setData(res.data.response))
      .catch((err) => console.error("❌ 구독정보 불러오기 실패:", err));
  }, []);

  if (!data) {
    return (
      <Flex justify="center" align="center" minH="100vh">
        <Text color="gray.500" fontSize="lg">
          로딩 중...
        </Text>
      </Flex>
    );
  }

  // ✅ successCount 기준 내림차순 정렬 후 상위 10명만 표시
  const sortedUsers = [...data.users]
    .sort((a, b) => b.successCount - a.successCount)
    .slice(0, 10);

  // ✅ 사용량이 많을수록 더 진한 색
  const getColor = (index: number): string => {
    if (sortedUsers.length <= 1) return blueShades[blueShades.length - 1];
    const reversedIndex =
      blueShades.length - 1 - Math.floor((index / 9) * (blueShades.length - 1));
    return blueShades[reversedIndex];
  };

  interface BarShapeProps {
    x: number;
    y: number;
    width: number;
    height: number;
    index: number;
  }

  const CustomBarShape = (props: BarShapeProps) => {
    const { x, y, width, height, index } = props;
    return (
      <rect
        x={x}
        y={y}
        width={width}
        height={height}
        fill={getColor(index)}
        rx={4}
      />
    );
  };

  return (
    <Box p={{ base: 4, md: 8 }} bg="gray.50" minH="100vh">
      <Heading mb={6} size="lg">
        구독 정보
      </Heading>

      {/* 📦 구독 요약 카드 */}
      <Box
        bg={bgCard}
        p={6}
        rounded="xl"
        shadow="sm"
        mb={8}
        border="1px solid"
        borderColor="gray.100"
      >
        <Text fontSize="xl" fontWeight="bold" mb={2}>
          {data.organizationName}
        </Text>
        <Text color={textGray} mb={1}>
          요금제: <b>{data.planName}</b> ({data.planCycle})
        </Text>
        <Text color={textGray} mb={1}>
          월 요금: <b>{data.price.toLocaleString()}원</b>
        </Text>

        {/* ✅ 총 구독량 및 이용 중 정보 */}
        <SimpleGrid columns={{ base: 1, md: 2 }} spacing={4} mt={4}>
          <Box
            bg="blue.50"
            p={4}
            rounded="md"
            border="1px solid"
            borderColor="blue.100"
          >
            <Text fontWeight="bold" color="blue.700">
              총 구독량
            </Text>
            <Text fontSize="xl" fontWeight="bold" color="blue.600">
              {data.maxSuccessResponses.toLocaleString()} 회
            </Text>
          </Box>

          <Box
            bg="green.50"
            p={4}
            rounded="md"
            border="1px solid"
            borderColor="green.100"
          >
            <Text fontWeight="bold" color="green.700">
              현재 이용 구독량
            </Text>
            <Text fontSize="xl" fontWeight="bold" color="green.600">
              {data.totalSuccessCount.toLocaleString()} 회
            </Text>
          </Box>
        </SimpleGrid>

        {/* 🔹 사용량 게이지 */}
        <Box mt={6}>
          <Flex justify="space-between" mb={2}>
            <Text fontWeight="semibold">
              전체 응답 수: {data.totalSuccessCount}
            </Text>
            <Text color="gray.600">
              잔여 응답 수: {data.remainingCount} / {data.maxSuccessResponses}
            </Text>
          </Flex>
          <Progress
            colorScheme="blue"
            value={data.usageRate}
            borderRadius="full"
            height="12px"
            bg="blue.100"
          />
          <Text mt={2} fontSize="sm" color="gray.500">
            사용률: {data.usageRate.toFixed(1)}%
          </Text>
        </Box>
      </Box>

      {/* 📊 사용자별 사용량 바 차트 */}
      <Box
        bg={bgCard}
        p={6}
        rounded="xl"
        shadow="sm"
        border="1px solid"
        borderColor="gray.100"
        mb={8}
      >
        <Heading size="md" mb={4}>
          사용자별 사용량 (상위 10명)
        </Heading>

        <Box height={{ base: "300px", md: "400px" }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={sortedUsers}>
              <XAxis dataKey="userName" />
              <YAxis />
              <Tooltip />
              <Bar
                dataKey="successCount"
                shape={(props: unknown) => {
                  const barProps = props as BarShapeProps;
                  return <CustomBarShape {...barProps} />;
                }}
              />
            </BarChart>
          </ResponsiveContainer>
        </Box>
      </Box>

      {/* 📋 사용자별 사용량 테이블 */}
      <Box
        bg={bgCard}
        p={6}
        rounded="xl"
        shadow="sm"
        border="1px solid"
        borderColor="gray.100"
      >
        <Heading size="sm" mb={4}>
          사용자 상세 사용량 (상위 10명)
        </Heading>

        <Table size="sm">
          <Thead bg="gray.100">
            <Tr>
              <Th>순위</Th>
              <Th>이름</Th>
              <Th isNumeric>응답 성공 횟수</Th>
            </Tr>
          </Thead>
          <Tbody>
            {sortedUsers.map((user, i) => (
              <Tr key={user.userId}>
                <Td>{i + 1}</Td>
                <Td>{user.userName}</Td>
                <Td isNumeric>{user.successCount}</Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </Box>
    </Box>
  );
}
