import { useState } from "react";
import {
  Box,
  Button,
  FormControl,
  FormLabel,
  Heading,
  Input,
  Radio,
  RadioGroup,
  Select,
  Stack,
  Text,
  useToast,
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalBody,
  ModalFooter,
} from "@chakra-ui/react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";

export default function RegisterExtra() {
  const [nickname, setNickname] = useState("");
  const [gender, setGender] = useState("male");
  const [birth, setBirth] = useState("");
  const [question, setQuestion] = useState("");
  const [answer, setAnswer] = useState("");
  const [isCompleteOpen, setIsCompleteOpen] = useState(false);

  const toast = useToast();
  const navigate = useNavigate();
  const { t } = useTranslation();

  const handleComplete = () => {
    if (!nickname || !birth || !question || !answer) {
      toast({ title: t("fill_all_fields"), status: "warning" });
      return;
    }
    setIsCompleteOpen(true);
  };

  const handleConfirm = () => {
    setIsCompleteOpen(false);
    navigate("/login");
  };

  return (
    <Box p={6} maxW="400px" mx="auto">
      {/* 헤더 */}
      <Heading size="sm" mb={6}>
        {t("register_extra_title")}
      </Heading>

      <Text color="gray.600" mb={6}>
        {t("register_extra_description")}
      </Text>

      {/* 이름 */}
      <FormControl mb={4}>
        <FormLabel>{t("name")}</FormLabel>
        <Input
          value={nickname}
          onChange={(e) => setNickname(e.target.value)}
          placeholder={t("nickname_placeholder")}
        />
      </FormControl>

      {/* 성별 */}
      <FormControl mb={4}>
        <FormLabel>{t("gender")}</FormLabel>
        <RadioGroup onChange={setGender} value={gender}>
          <Stack direction="row" spacing={6}>
            <Radio value="male">{t("male")}</Radio>
            <Radio value="female">{t("female")}</Radio>
          </Stack>
        </RadioGroup>
      </FormControl>

      {/* 생년월일 */}
      <FormControl mb={4}>
        <FormLabel>{t("birth")}</FormLabel>
        <Input
          type="date"
          value={birth}
          onChange={(e) => setBirth(e.target.value)}
        />
      </FormControl>

      {/* 비밀번호 찾기 질문 */}
      <FormControl mb={4}>
        <FormLabel>{t("question_select")}</FormLabel>
        <Select
          placeholder={t("question_placeholder")}
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
        >
          <option value="1">{t("question_1")}</option>
          <option value="2">{t("question_2")}</option>
          <option value="3">{t("question_3")}</option>
        </Select>
      </FormControl>

      {/* 답변 */}
      <FormControl mb={6}>
        <FormLabel>{t("answer")}</FormLabel>
        <Input
          value={answer}
          onChange={(e) => setAnswer(e.target.value)}
          placeholder={t("answer_placeholder")}
        />
      </FormControl>

      {/* 완료 버튼 */}
      <Button colorScheme="blue" w="100%" h="50px" onClick={handleComplete}>
        {t("complete")}
      </Button>

      {/* 가입 완료 모달 */}
      <Modal
        isOpen={isCompleteOpen}
        onClose={() => setIsCompleteOpen(false)}
        isCentered
      >
        <ModalOverlay />
        <ModalContent borderRadius="md" textAlign="center" py={4}>
          <ModalHeader fontSize="lg">{t("register_success_title")}</ModalHeader>
          <ModalBody>
            <Text color="gray.600">{t("register_success_message")}</Text>
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
