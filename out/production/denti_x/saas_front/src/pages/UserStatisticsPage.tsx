import {
  Box,
  Button,
  Flex,
  Grid,
  Heading,
  Input,
  Text,
  Spinner,
  useToast,
} from "@chakra-ui/react";
import { useState, useEffect } from "react";
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";
import api from "../api/api";

interface StatisticResponse {
  userSignUpCount: {
    countAll: number;
    countMan: number;
    countWoman: number;
  };
  averageState: string;
  oralCheckCount: number;
  oralCheckAverage: number;
  oralCheckResultTypeCount: {
    countHealthy: number;
    countGood: number;
    countAttention: number;
    countDanger: number;
  };
  questionnaireAllCount: number;
  allQuestionnaireResultTypeCount: Record<string, number>;
}

interface ApiResponse {
  rt: number;
  rtMsg: string;
  response: StatisticResponse;
}

export default function UserStatisticsPage() {
  const toast = useToast();
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<StatisticResponse | null>(null);

  const COLORS = ["#4F9BFF", "#FF9DB1"];

  const fetchStatistics = async () => {
    try {
      setLoading(true);
      const params = new URLSearchParams();
      if (startDate) params.append("startDate", startDate);
      if (endDate) params.append("endDate", endDate);

      const res = await api.get<ApiResponse>(`/admin/statistic?${params}`);
      setData(res.data.response);
    } catch (error) {
      console.error("❌ 통계 조회 실패:", error);
      toast({
        title: "데이터를 불러오지 못했습니다.",
        status: "error",
        duration: 2500,
        isClosable: true,
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStatistics();
  }, []);

  const handleQuickSelect = (days: number | "all") => {
    const now = new Date();
    if (days === "all") {
      setStartDate("");
      setEndDate("");
      return;
    }
    const start = new Date();
    start.setDate(now.getDate() - days);
    setStartDate(start.toISOString().slice(0, 10));
    setEndDate(now.toISOString().slice(0, 10));
  };

  if (loading || !data) {
    return (
      <Flex justify="center" align="center" minH="80vh">
        <Spinner size="xl" color="blue.500" />
      </Flex>
    );
  }

  const genderData = [
    { name: "남성", value: data.userSignUpCount.countMan },
    { name: "여성", value: data.userSignUpCount.countWoman },
  ];

  const gumStatusData = [
    { name: "건강", value: data.oralCheckResultTypeCount.countHealthy },
    { name: "양호", value: data.oralCheckResultTypeCount.countGood },
    { name: "주의", value: data.oralCheckResultTypeCount.countAttention },
    { name: "위험", value: data.oralCheckResultTypeCount.countDanger },
  ];

  const questionnaireData = Object.entries(
    data.allQuestionnaireResultTypeCount
  ).map(([key, value]) => ({
    name: key.replace("count", ""),
    value,
  }));

  const manRate =
    (data.userSignUpCount.countMan / data.userSignUpCount.countAll) * 100 || 0;
  const womanRate =
    (data.userSignUpCount.countWoman / data.userSignUpCount.countAll) * 100 ||
    0;

  return (
    <Box bg="gray.50" minH="100vh" p={{ base: 4, md: 8 }}>
      {/* 🔍 검색 조건 */}
      <Box
        bg="white"
        p={6}
        rounded="lg"
        shadow="md"
        mb={8}
        display="flex"
        flexDirection="column"
        gap={4}
      >
        <Flex justify="space-between" align="center" flexWrap="wrap" gap={3}>
          <Text fontWeight="bold" fontSize="md">
            통계기간설정
          </Text>
          <Flex gap={2} flexWrap="wrap">
            <Button size="sm" onClick={() => handleQuickSelect(0)}>
              오늘
            </Button>
            <Button size="sm" onClick={() => handleQuickSelect(7)}>
              1주일
            </Button>
            <Button size="sm" onClick={() => handleQuickSelect(30)}>
              1개월
            </Button>
            <Button size="sm" onClick={() => handleQuickSelect(90)}>
              3개월
            </Button>
            <Button size="sm" onClick={() => handleQuickSelect(365)}>
              1년
            </Button>
            <Button size="sm" onClick={() => handleQuickSelect("all")}>
              전체
            </Button>
            <Input
              type="date"
              w="160px"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
            />
            <Text>~</Text>
            <Input
              type="date"
              w="160px"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
            />
            <Button variant="outline" onClick={() => window.location.reload()}>
              초기화
            </Button>
            <Button colorScheme="blue" onClick={fetchStatistics}>
              조회
            </Button>
          </Flex>
        </Flex>
      </Box>

      {/* 📊 메인 통계 */}
      <Grid
        templateColumns={{ base: "1fr", lg: "2fr 1fr" }}
        gap={6}
        mb={6}
        alignItems="stretch"
      >
        {/* 전체 남녀 가입률 */}
        <Box bg="white" p={6} rounded="lg" shadow="sm">
          <Heading size="md" mb={4} textAlign="center">
            전체 남녀 가입률
          </Heading>
          <ResponsiveContainer width="100%" height={250}>
            <PieChart>
              <Pie
                data={genderData}
                cx="50%"
                cy="50%"
                outerRadius={100}
                dataKey="value"
                label
              >
                {genderData.map((_, i) => (
                  <Cell key={i} fill={COLORS[i]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </Box>

        {/* 가입자 수 카드 */}
        <Grid gap={4}>
          <Box
            bg="white"
            rounded="lg"
            shadow="sm"
            p={6}
            textAlign="center"
            flex="1"
          >
            <Text fontWeight="bold">전체 가입자 수</Text>
            <Text fontSize="2xl" fontWeight="bold" mt={2}>
              {data.userSignUpCount.countAll}명
            </Text>
          </Box>

          <Box bg="white" rounded="lg" shadow="sm" p={6} textAlign="center">
            <Text fontWeight="bold">남성 가입자</Text>
            <Text fontSize="2xl" color="blue.500" fontWeight="bold" mt={2}>
              {data.userSignUpCount.countMan}명 ({manRate.toFixed(1)}%)
            </Text>
          </Box>

          <Box bg="white" rounded="lg" shadow="sm" p={6} textAlign="center">
            <Text fontWeight="bold">여성 가입자</Text>
            <Text fontSize="2xl" color="pink.500" fontWeight="bold" mt={2}>
              {data.userSignUpCount.countWoman}명 ({womanRate.toFixed(1)}%)
            </Text>
          </Box>
        </Grid>
      </Grid>

      {/* 하단 그래프 */}
      <Grid
        templateColumns={{ base: "1fr", lg: "repeat(2, 1fr)" }}
        gap={6}
        alignItems="stretch"
      >
        {/* 평균 구강촬영 상태 */}
        <Box bg="white" p={6} rounded="lg" shadow="sm">
          <Heading size="md" mb={4}>
            평균 구강촬영 상태
          </Heading>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={gumStatusData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="value" fill="#4F9BFF" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Box>

        {/* 평균 문진표 유형 */}
        <Box bg="white" p={6} rounded="lg" shadow="sm">
          <Heading size="md" mb={4}>
            평균 문진표 유형
          </Heading>
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={questionnaireData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="value" fill="#7CC8FF" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Box>
      </Grid>
    </Box>
  );
}
