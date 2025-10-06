import {
  Box,
  Button,
  Collapse,
  Flex,
  Grid,
  Input,
  Select,
  Text,
  Table,
  Thead,
  Tbody,
  Tr,
  Th,
  Td,
  useBreakpointValue,
  useDisclosure,
  Spinner,
  useToast,
} from "@chakra-ui/react";
import { useState, useEffect } from "react";
import { ChevronDownIcon, ChevronUpIcon } from "@chakra-ui/icons";
import api from "../api/api";

interface UserItem {
  userId: number;
  userLoginIdentifier: string;
  userName: string;
  oralCheckResultTotalType: string | null;
  oralCheckDate: string | null;
  questionnaireDate: string | null;
  isVerify: string;
  patientPhoneNumber: string;
  serviceName: string | null;
}

interface Paging {
  number: number;
  totalPages: number;
  totalElements: number;
}

interface ApiResponse {
  rt: number;
  rtMsg: string;
  response: {
    paging: Paging;
    userList: UserItem[];
  };
}

export default function UserListPage() {
  const [filters, setFilters] = useState({
    keyword: "",
    oralStatus: "",
    questionnaireType: "",
    gender: "",
    verify: "",
    startDate: "",
    endDate: "",
  });
  const [users, setUsers] = useState<UserItem[]>([]);
  const [paging, setPaging] = useState<Paging | null>(null);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(50); // ✅ 페이지당 표시 개수
  const [loading, setLoading] = useState(false);
  const toast = useToast();
  const [verifyingUserId, setVerifyingUserId] = useState<number | null>(null);
  // ✅ 반응형 감지
  const isMobile = useBreakpointValue({ base: true, md: false });

  // ✅ 검색창 접기/펼치기 상태
  const { isOpen, onToggle, onOpen, onClose } = useDisclosure();

  useEffect(() => {
    if (isMobile) onClose();
    else onOpen();
  }, [isMobile]);

  // ✅ 사용자 목록 조회
  const fetchUsers = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.append("page", page.toString());
      params.append("size", size.toString());

      if (filters.keyword) params.append("keyword", filters.keyword);
      if (filters.oralStatus) params.append("oralStatus", filters.oralStatus);
      if (filters.questionnaireType)
        params.append("questionnaireType", filters.questionnaireType);
      if (filters.gender) params.append("gender", filters.gender);
      if (filters.verify) params.append("verify", filters.verify);
      if (filters.startDate) params.append("startDate", filters.startDate);
      if (filters.endDate) params.append("endDate", filters.endDate);

      const res = await api.get<ApiResponse>(
        `/admin/user?${params.toString()}`
      );

      setUsers(res.data.response.userList);
      setPaging(res.data.response.paging);
    } catch (e) {
      console.error("❌ 사용자 검색 실패:", e);
    } finally {
      setLoading(false);
    }
  };

  // ✅ 자동 전체 조회
  useEffect(() => {
    fetchUsers();
  }, [page, size]); // ✅ size가 변경되면 재조회

  // ✅ 초기화
  const handleReset = () => {
    setFilters({
      keyword: "",
      oralStatus: "",
      questionnaireType: "",
      gender: "",
      verify: "",
      startDate: "",
      endDate: "",
    });
    setPage(1);
    fetchUsers();
  };

  // ✅ 페이지네이션
  const handlePrevPage = () => {
    if (page > 1) setPage((prev) => prev - 1);
  };

  const handleNextPage = () => {
    if (paging && page < paging.totalPages) setPage((prev) => prev + 1);
  };

  // ✅ 페이지당 표시 개수 변경
  const handleSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newSize = Number(e.target.value);
    setSize(newSize);
    setPage(1); // 첫 페이지로 초기화
  };

  //인증 처리 함수 (로딩 + 토스트 포함)
  const handleVerifyUser = async (userId: number) => {
    try {
      setVerifyingUserId(userId); // 로딩 시작
      await api.put(`/admin/user/verify?userId=${userId}`);

      toast({
        title: "인증 완료",
        description: "해당 사용자가 성공적으로 인증되었습니다.",
        status: "success",
        duration: 2000,
        isClosable: true,
      });

      // ✅ 목록 새로고침
      fetchUsers();
    } catch (err) {
      console.error("❌ 사용자 인증 실패:", err);
      toast({
        title: "인증 실패",
        description: "인증 중 오류가 발생했습니다.",
        status: "error",
        duration: 2500,
        isClosable: true,
      });
    } finally {
      setVerifyingUserId(null); // 로딩 종료
    }
  };

  return (
    <Box p={{ base: 3, md: 6 }} bg="gray.50" minH="100vh">
      {/* 🔍 검색 조건 영역 */}
      <Box
        bg="white"
        p={{ base: 4, md: 6 }}
        rounded="lg"
        shadow="md"
        mb={6}
        borderWidth="1px"
        borderColor="gray.200"
      >
        {/* 헤더 */}
        <Flex justify="space-between" align="center" mb={2}>
          <Text fontWeight="bold" fontSize="lg">
            검색 조건
          </Text>
          {isMobile && (
            <Button
              size="sm"
              variant="ghost"
              rightIcon={isOpen ? <ChevronUpIcon /> : <ChevronDownIcon />}
              onClick={onToggle}
            >
              {isOpen ? "접기" : "펼치기"}
            </Button>
          )}
        </Flex>

        {/* Collapse (모바일만 접힘) */}
        <Collapse in={isOpen} animateOpacity>
          <Box>
            <Text fontWeight="bold" mb={2}>
              검색어
            </Text>
            <Input
              placeholder="아이디 혹은 이름"
              value={filters.keyword}
              onChange={(e) =>
                setFilters((f) => ({ ...f, keyword: e.target.value }))
              }
              mb={4}
            />

            <Text fontWeight="bold" mb={2}>
              필터
            </Text>

            {/* ✅ 반응형 Grid */}
            <Grid
              templateColumns={{
                base: "1fr",
                sm: "repeat(2, 1fr)",
                md: "repeat(4, 1fr)",
              }}
              gap={4}
              mb={6}
            >
              <Select
                placeholder="잇몸상태"
                value={filters.oralStatus}
                onChange={(e) =>
                  setFilters((f) => ({ ...f, oralStatus: e.target.value }))
                }
              >
                <option value="HEALTHY">건강</option>
                <option value="DANGER">위험</option>
              </Select>

              <Select
                placeholder="문진표 유형"
                value={filters.questionnaireType}
                onChange={(e) =>
                  setFilters((f) => ({
                    ...f,
                    questionnaireType: e.target.value,
                  }))
                }
              >
                <option value="ADULT">성인</option>
                <option value="CHILD">소아</option>
              </Select>

              <Select
                placeholder="성별"
                value={filters.gender}
                onChange={(e) =>
                  setFilters((f) => ({ ...f, gender: e.target.value }))
                }
              >
                <option value="M">남성</option>
                <option value="W">여성</option>
              </Select>

              <Select
                placeholder="인증여부"
                value={filters.verify}
                onChange={(e) =>
                  setFilters((f) => ({ ...f, verify: e.target.value }))
                }
              >
                <option value="Y">인증됨</option>
                <option value="N">미인증</option>
              </Select>
            </Grid>

            {/* 📅 기간 설정 */}
            <Grid
              templateColumns={{ base: "1fr", sm: "repeat(2, 1fr)" }}
              gap={4}
              mb={6}
            >
              <Input
                type="date"
                value={filters.startDate}
                onChange={(e) =>
                  setFilters((f) => ({ ...f, startDate: e.target.value }))
                }
              />
              <Input
                type="date"
                value={filters.endDate}
                onChange={(e) =>
                  setFilters((f) => ({ ...f, endDate: e.target.value }))
                }
              />
            </Grid>

            <Flex justify="flex-end" gap={3} flexWrap="wrap">
              <Button variant="outline" onClick={handleReset}>
                초기화
              </Button>
              <Button colorScheme="blue" onClick={fetchUsers}>
                검색
              </Button>
            </Flex>
          </Box>
        </Collapse>
      </Box>

      {/* 📋 검색 결과 테이블 */}
      <Box
        bg="white"
        p={{ base: 4, md: 6 }}
        rounded="lg"
        shadow="sm"
        overflowX="auto"
      >
        {loading ? (
          <Flex justify="center" align="center" minH="200px">
            <Spinner size="xl" color="blue.500" />
          </Flex>
        ) : (
          <>
            <Flex justify="space-between" mb={4} flexWrap="wrap" gap={2}>
              <Text fontWeight="bold">
                검색 결과 (총 {paging?.totalElements || 0}명)
              </Text>

              {/* ✅ 페이지당 개수 선택 */}
              <Select
                w="150px"
                size="sm"
                value={size}
                onChange={handleSizeChange}
              >
                <option value="10">10개씩 보기</option>
                <option value="30">30개씩 보기</option>
                <option value="50">50개씩 보기</option>
              </Select>
            </Flex>

            <Table size="sm" minW="800px">
              <Thead bg="gray.100">
                <Tr>
                  <Th>번호</Th>
                  <Th>아이디</Th>
                  <Th>이름</Th>
                  {!isMobile && <Th>문진표 유형</Th>}
                  <Th>문진표 검사일</Th>
                  {!isMobile && <Th>잇몸상태</Th>}
                  <Th>인증여부</Th>
                  <Th>이용중인 서비스</Th>
                </Tr>
              </Thead>
              <Tbody>
                {users.map((user, idx) => (
                  <Tr key={user.userId}>
                    <Td>{(page - 1) * size + idx + 1}</Td>
                    <Td>{user.userLoginIdentifier}</Td>
                    <Td>{user.userName}</Td>
                    {!isMobile && <Td>{filters.questionnaireType || "-"}</Td>}
                    <Td>{user.questionnaireDate || "-"}</Td>
                    {!isMobile && (
                      <Td>{user.oralCheckResultTotalType || "-"}</Td>
                    )}
                    <Td>
                      {user.isVerify === "Y" ? (
                        <Button
                          size="xs"
                          colorScheme="green"
                          variant="outline"
                          isDisabled
                        >
                          인증됨
                        </Button>
                      ) : (
                        <Button
                          size="xs"
                          colorScheme="blue"
                          variant="outline"
                          isLoading={verifyingUserId === user.userId} // ✅ 로딩 표시
                          onClick={() => handleVerifyUser(user.userId)}
                        >
                          인증하기
                        </Button>
                      )}
                    </Td>
                    <Td>{user.serviceName || "-"}</Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>

            {/* ✅ 페이지네이션 */}
            <Flex justify="center" align="center" mt={6} gap={4}>
              <Button
                onClick={handlePrevPage}
                isDisabled={page === 1}
                size="sm"
              >
                이전
              </Button>
              <Text fontWeight="medium">
                {page} / {paging?.totalPages || 1}
              </Text>
              <Button
                onClick={handleNextPage}
                isDisabled={page === paging?.totalPages}
                size="sm"
              >
                다음
              </Button>
            </Flex>
          </>
        )}
      </Box>
    </Box>
  );
}
