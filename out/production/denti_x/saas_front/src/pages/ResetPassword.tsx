import { useState } from "react";
import {
  Box,
  Button,
  FormControl,
  FormLabel,
  Heading,
  Input,
  Text,
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalBody,
  ModalFooter,
} from "@chakra-ui/react";
import { useLocation, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";

export default function ResetPassword() {
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isCompleteOpen, setIsCompleteOpen] = useState(false);

  const location = useLocation();
  const navigate = useNavigate();
  const from = location.state?.from || "login"; // 기본은 login
  const { t } = useTranslation();

  const handleSubmit = () => {
    // 지금은 검증 없이 무조건 완료 모달 열기
    setIsCompleteOpen(true);
  };

  const handleConfirm = () => {
    setIsCompleteOpen(false);
    if (from === "user") {
      navigate("/user");
    } else {
      navigate("/login");
    }
  };

  return (
    <Box p={6} maxW="400px" mx="auto">
      <Heading size="sm" mb={6}>
        {t("reset_password")}
      </Heading>

      <Text color="gray.600" mb={6}>
        {t("reset_password_description")}
      </Text>

      <FormControl mb={4}>
        <FormLabel>{t("new_password")}</FormLabel>
        <Input
          type="password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          placeholder={t("password_placeholder")}
        />
      </FormControl>

      <FormControl mb={6}>
        <FormLabel>{t("confirm_new_password")}</FormLabel>
        <Input
          type="password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          placeholder={t("confirm_password_placeholder")}
        />
      </FormControl>

      <Button colorScheme="blue" w="100%" h="50px" onClick={handleSubmit}>
        {t("change_complete")}
      </Button>

      {/* 완료 안내 모달 */}
      <Modal isOpen={isCompleteOpen} onClose={handleConfirm} isCentered>
        <ModalOverlay />
        <ModalContent textAlign="center" py={4}>
          <ModalHeader fontSize="lg">
            {t("password_change_success")}
          </ModalHeader>
          <ModalBody>
            <Text color="gray.600">{t("login_with_new_password")}</Text>
          </ModalBody>
          <ModalFooter>
            <Button colorScheme="blue" w="100%" onClick={handleConfirm}>
              {t("ok")}
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </Box>
  );
}
