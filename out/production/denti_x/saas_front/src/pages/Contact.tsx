import { useState } from "react";
import {
  Box,
  Button,
  FormControl,
  FormLabel,
  Heading,
  Input,
  Textarea,
  useToast,
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalBody,
  ModalFooter,
} from "@chakra-ui/react";
import { useLocation, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";

export default function Contact() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [isCompleteOpen, setIsCompleteOpen] = useState(false);

  const toast = useToast();
  const location = useLocation();
  const navigate = useNavigate();
  const { t } = useTranslation();

  const from = location.state?.from || "login"; // 기본값은 로그인

  const handleSubmit = () => {
    if (!name || !email || !message) {
      toast({ title: t("contact_warning"), status: "warning" });
      return;
    }
    setIsCompleteOpen(true);
  };

  const handleConfirm = () => {
    if (from === "user") {
      navigate("/user");
    } else {
      navigate("/login");
    }
  };

  return (
    <Box p={6} maxW="400px" mx="auto">
      <Heading size="sm" mb={6}>
        {t("contact")}
      </Heading>

      <FormControl mb={4}>
        <FormLabel>{t("name")}</FormLabel>
        <Input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder={t("enter_name") || ""}
        />
      </FormControl>

      <FormControl mb={4}>
        <FormLabel>{t("email")}</FormLabel>
        <Input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder={t("enter_email") || ""}
        />
      </FormControl>

      <FormControl mb={6}>
        <FormLabel>{t("message")}</FormLabel>
        <Textarea
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder={t("enter_message") || ""}
          rows={5}
        />
      </FormControl>

      <Button colorScheme="blue" w="100%" h="50px" onClick={handleSubmit}>
        {t("submit")}
      </Button>

      {/* 완료 모달 */}
      <Modal isOpen={isCompleteOpen} onClose={handleConfirm} isCentered>
        <ModalOverlay />
        <ModalContent textAlign="center" py={4}>
          <ModalHeader fontSize="lg">{t("contact_complete_title")}</ModalHeader>
          <ModalBody>{t("contact_complete_body")}</ModalBody>
          <ModalFooter>
            <Button colorScheme="blue" w="100%" onClick={handleConfirm}>
              {t("confirm")}
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </Box>
  );
}
