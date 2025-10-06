// src/components/MainLayout.tsx
import { Box, Flex, useBreakpointValue } from "@chakra-ui/react";
import Sidebar from "./Sidebar";
import Header from "./Header";
import type { ReactNode } from "react";

interface MainLayoutProps {
  children: ReactNode;
}

export default function MainLayout({ children }: MainLayoutProps) {
  const isMobile = useBreakpointValue({ base: true, md: false });
  const sidebarWidth = isMobile ? "70px" : "260px";

  return (
    <Flex>
      <Sidebar />
      <Box ml={sidebarWidth} flex="1" transition="0.3s ease">
        <Header />
        <Box p={6} bg="gray.50" minH="calc(100vh - 60px)">
          {children}
        </Box>
      </Box>
    </Flex>
  );
}
