// src/components/Header.tsx
import { Flex, Text, Button, Spacer } from "@chakra-ui/react";
import { useLocation, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";

export default function Header() {
  const location = useLocation();
  const navigate = useNavigate();
  const [pageTitle, setPageTitle] = useState("");

  useEffect(() => {
    switch (location.pathname) {
      case "/dashboard":
        setPageTitle("대시보드");
        break;
      case "/admin/users":
        setPageTitle("사용자 관리");
        break;
      case "/settings":
        setPageTitle("설정");
        break;
      default:
        setPageTitle("");
    }
  }, [location.pathname]);

  const handleLogout = () => {
    localStorage.clear();
    navigate("/");
  };

  return (
    <Flex
      h="60px"
      bg="white"
      align="center"
      px={6}
      borderBottom="1px solid"
      borderColor="gray.200"
      shadow="sm"
      position="sticky"
      top={0}
      zIndex={10}
    >
      <Text fontSize="lg" fontWeight="bold">
        {pageTitle}
      </Text>
      <Spacer />
      <Button size="sm" colorScheme="red" onClick={handleLogout}>
        로그아웃
      </Button>
    </Flex>
  );
}
