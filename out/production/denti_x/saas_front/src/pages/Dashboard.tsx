import { useState } from "react";
import {
  Box,
  Container,
  Text,
  Grid,
  Flex,
  VStack,
  HStack,
  Spinner,
  Icon,
  useColorModeValue,
} from "@chakra-ui/react";
import {
  LineChart,
  Line,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from "recharts";
import { FiSmile, FiActivity, FiCheckCircle, FiCalendar } from "react-icons/fi";

// ✅ 상태 매핑 (라인차트용)
const statusMap: Record<string, number> = {
  DANGER: 1,
  ATTENTION: 2,
  GOOD: 3,
  HEALTHY: 4,
};

// ✅ 색상 매핑
const COLORS = {
  HEALTHY: "#3182CE",
  GOOD: "#38B2AC",
  ATTENTION: "#ECC94B",
  DANGER: "#E53E3E",
};

// ✅ 카드 컴포넌트
interface CardProps {
  title: string;
  value: string | number;
  subText?: string;
  icon: React.ElementType; // ✅ 변경
  iconColor: string;
  valueColor?: string;
}

const StatCard = ({
  title,
  value,
  subText,
  icon,
  iconColor,
  valueColor,
}: CardProps) => {
  return (
    <Box bg="white" shadow="sm" borderRadius="lg" p={5} textAlign="left">
      <HStack spacing={3} mb={3}>
        <Flex
          w="32px"
          h="32px"
          align="center"
          justify="center"
          bg={useColorModeValue(`${iconColor}.100`, `${iconColor}.700`)}
          borderRadius="md"
        >
          <Icon as={icon} color={`${iconColor}.500`} />
        </Flex>
        <Text fontWeight="bold" fontSize="md">
          {title}
        </Text>
      </HStack>

      <VStack align="flex-start" spacing={1}>
        <Text fontSize="lg" fontWeight="semibold" color={valueColor || "black"}>
          {value}
        </Text>
        {subText && (
          <Text fontSize="sm" color="gray.600">
            {subText}
          </Text>
        )}
      </VStack>
    </Box>
  );
};

// ✅ 메인 컴포넌트
export default function Dashboard() {
  const [loading] = useState(false); // 로딩 비활성화 (더미)
  const data = {
    oralCheckTotalCount: 128,
    oralCheckHealthyCount: 46,
    oralCheckGoodCount: 39,
    oralCheckAttentionCount: 28,
    oralCheckDangerCount: 15,
    oralCheckResultTotalType: "GOOD",
    latestOralCheckId: 2487,
    oralCheckTimeInterval: 300,
    toothBrushingTotalCount: 214,
    toothBrushingAverage: 2.3,
    questionnaireCreated: "2025-10-05",
    oralCheckDailyList: [
      { oralCheckNumber: 1, oralCheckResultTotalType: "DANGER" },
      { oralCheckNumber: 2, oralCheckResultTotalType: "ATTENTION" },
      { oralCheckNumber: 3, oralCheckResultTotalType: "GOOD" },
      { oralCheckNumber: 4, oralCheckResultTotalType: "HEALTHY" },
      { oralCheckNumber: 5, oralCheckResultTotalType: "GOOD" },
    ],
  };

  if (loading) {
    return (
      <Flex justify="center" align="center" minH="100vh">
        <Spinner size="xl" />
      </Flex>
    );
  }

  // ✅ 도넛차트 데이터
  const totalChecks = data.oralCheckTotalCount ?? 0;
  const healthyRate =
    totalChecks > 0
      ? Math.round((data.oralCheckHealthyCount / totalChecks) * 100)
      : 0;

  const pieData = [
    { name: "건강", value: data.oralCheckHealthyCount, color: COLORS.HEALTHY },
    { name: "양호", value: data.oralCheckGoodCount, color: COLORS.GOOD },
    {
      name: "주의",
      value: data.oralCheckAttentionCount,
      color: COLORS.ATTENTION,
    },
    { name: "위험", value: data.oralCheckDangerCount, color: COLORS.DANGER },
  ];

  // ✅ 라인차트 데이터
  const lineData = data.oralCheckDailyList.map((item) => ({
    checkNumber: item.oralCheckNumber,
    status: statusMap[item.oralCheckResultTotalType],
  }));

  return (
    <Box bg="gray.50" minH="100vh" py={6}>
      <Container maxW="1200px">
        <Text fontSize="2xl" fontWeight="bold" mb={6}>
          대시보드 (더미 데이터)
        </Text>

        {/* 상단 카드 */}
        <Grid
          templateColumns={{
            base: "1fr",
            sm: "repeat(2, 1fr)",
            lg: "repeat(4, 1fr)",
          }}
          gap={6}
          mb={8}
        >
          <StatCard
            title="최근 구강상태"
            value={data.oralCheckResultTotalType}
            subText={`최근 검사 ID: ${data.latestOralCheckId}`}
            icon={FiSmile}
            iconColor="blue"
          />
          <StatCard
            title="구강검진 수"
            value={`${data.oralCheckTotalCount}회`}
            subText={`주기: ${data.oralCheckTimeInterval}초`}
            icon={FiActivity}
            iconColor="orange"
          />
          <StatCard
            title="양치 수"
            value={`${data.toothBrushingTotalCount}`}
            subText={`평균: ${data.toothBrushingAverage}회/일`}
            icon={FiCheckCircle}
            iconColor="green"
          />
          <StatCard
            title="구강상태 유형"
            value={data.oralCheckResultTotalType}
            subText={`최근 검사일: ${data.questionnaireCreated}`}
            icon={FiCalendar}
            iconColor="red"
            valueColor="red.500"
          />
        </Grid>

        {/* 하단 그래프 */}
        <Grid templateColumns={{ base: "1fr", lg: "repeat(2, 1fr)" }} gap={6}>
          {/* 라인차트 */}
          <Box bg="white" shadow="sm" borderRadius="lg" p={5}>
            <Text fontWeight="bold" mb={4}>
              구강 상태 변화 추이 (*회차)
            </Text>
            <ResponsiveContainer width="100%" height={250}>
              <LineChart data={lineData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="checkNumber" />
                <YAxis
                  ticks={[1, 2, 3, 4]}
                  domain={[1, 4]}
                  tickFormatter={(v) =>
                    v === 1
                      ? "위험"
                      : v === 2
                      ? "주의"
                      : v === 3
                      ? "양호"
                      : "건강"
                  }
                />
                <Tooltip
                  formatter={(v) =>
                    v === 1
                      ? "위험"
                      : v === 2
                      ? "주의"
                      : v === 3
                      ? "양호"
                      : "건강"
                  }
                  labelFormatter={(label) => `검진 ${label}회`}
                />
                <Line
                  type="monotone"
                  dataKey="status"
                  stroke="#2B6CB0"
                  strokeWidth={2}
                  dot={{ r: 5 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </Box>

          {/* 도넛차트 */}
          <Box
            bg="white"
            shadow="sm"
            borderRadius="lg"
            p={5}
            position="relative"
          >
            <Text fontWeight="bold" mb={4}>
              구강검진 건강 비율
            </Text>
            <ResponsiveContainer width="100%" height={250}>
              <PieChart>
                <Pie
                  data={pieData}
                  dataKey="value"
                  cx="50%"
                  cy="50%"
                  innerRadius={70}
                  outerRadius={100}
                  paddingAngle={3}
                >
                  {pieData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
              </PieChart>
            </ResponsiveContainer>

            {/* 도넛 중앙 텍스트 */}
            <Flex
              direction="column"
              align="center"
              justify="center"
              position="absolute"
              top="55%"
              left="50%"
              transform="translate(-50%, -50%)"
            >
              <Text fontSize="2xl" fontWeight="bold" color="blue.600">
                {healthyRate}%
              </Text>
              <Text fontSize="sm" color="gray.600">
                총 {totalChecks}회
              </Text>
            </Flex>

            {/* 상태별 개수 */}
            <Flex justify="space-around" mt={4}>
              {pieData.map((item, idx) => (
                <VStack key={idx} spacing={0}>
                  <Text fontWeight="bold">{item.value}회</Text>
                  <Text fontSize="sm" color={item.color}>
                    {item.name}
                  </Text>
                </VStack>
              ))}
            </Flex>
          </Box>
        </Grid>
      </Container>
    </Box>
  );
}
