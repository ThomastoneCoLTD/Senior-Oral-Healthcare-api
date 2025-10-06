import { useState } from "react";
import {
  Box,
  Button,
  FormControl,
  FormLabel,
  Heading,
  Input,
  Select,
  Text,
} from "@chakra-ui/react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";

export default function FindPassword() {
  const [userId, setUserId] = useState("");
  const [question, setQuestion] = useState("");
  const [answer, setAnswer] = useState("");
  const navigate = useNavigate();
  const { t } = useTranslation();

  const handleConfirm = () => {
    // 지금은 검증 없이 무조건 reset-password로 이동
    navigate("/reset-password");
  };

  return (
    <Box p={6} maxW="400px" mx="auto">
      <Heading size="sm" mb={6}>
        {t("find_password")}
      </Heading>

      <Text color="gray.600" mb={6}>
        {t("find_password_desc")}
      </Text>

      <FormControl mb={4}>
        <FormLabel>{t("id")}</FormLabel>
        <Input
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
          placeholder={t("enter_id") || ""}
        />
      </FormControl>

      <FormControl mb={4}>
        <FormLabel>{t("select_question")}</FormLabel>
        <Select
          placeholder={t("select_question_placeholder") || ""}
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
        >
          <option value="1">{t("question_pet")}</option>
          <option value="2">{t("question_school")}</option>
          <option value="3">{t("question_food")}</option>
        </Select>
      </FormControl>

      <FormControl mb={6}>
        <FormLabel>{t("answer")}</FormLabel>
        <Input
          value={answer}
          onChange={(e) => setAnswer(e.target.value)}
          placeholder={t("enter_answer") || ""}
        />
      </FormControl>

      <Button colorScheme="blue" w="100%" h="50px" onClick={handleConfirm}>
        {t("confirm")}
      </Button>
    </Box>
  );
}
