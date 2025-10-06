import { useState } from "react";
import {
  Box,
  Button,
  Checkbox,
  Divider,
  Flex,
  FormControl,
  FormLabel,
  Heading,
  Input,
  Stack,
  Text,
  useToast,
} from "@chakra-ui/react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";

export default function RegisterVerify() {
  const navigate = useNavigate();
  const toast = useToast();
  const { t } = useTranslation();

  const [phone, setPhone] = useState("");
  const [name, setName] = useState("");
  const [terms, setTerms] = useState({
    service: false,
    privacy: false,
    marketing: false,
  });

  const [allChecked, setAllChecked] = useState(false);

  // 전체 동의 체크 시 모든 항목 체크
  const handleAllCheck = (checked: boolean) => {
    setAllChecked(checked);
    setTerms({
      service: checked,
      privacy: checked,
      marketing: checked,
    });
  };

  // 단일 체크 시 전체동의 업데이트
  const handleSingleCheck = (key: keyof typeof terms, checked: boolean) => {
    setTerms({ ...terms, [key]: checked });
    if (checked && Object.values({ ...terms, [key]: checked }).every(Boolean)) {
      setAllChecked(true);
    } else {
      setAllChecked(false);
    }
  };

  const handleNext = () => {
    if (!phone || !name) {
      toast({ title: t("enter_phone_and_name"), status: "warning" });
      return;
    }
    if (!terms.service || !terms.privacy) {
      toast({ title: t("accept_required_terms"), status: "error" });
      return;
    }
    // 인증 성공 → 회원가입 폼으로 이동
    navigate("/register/form");
  };

  return (
    <Box p={6} maxW="400px" mx="auto">
      {/* 헤더 */}
      <Flex align="center" mb={4}>
        <Button variant="ghost" p={0} minW="auto" onClick={() => navigate(-1)}>
          ←
        </Button>
        <Heading size="sm" ml={2}>
          {t("verify_member")}
        </Heading>
      </Flex>

      {/* Progress */}
      <Box h="2px" bg="gray.200" mb={6}>
        <Box h="2px" w="33%" bg="blue.500"></Box>
      </Box>

      <Heading size="md" mb={2}>
        {t("verify_member_title")}
      </Heading>
      <Text color="gray.600" mb={6}>
        {t("verify_member_description")}
      </Text>

      {/* Phone */}
      <FormControl mb={4}>
        <FormLabel>{t("enter_phone")}</FormLabel>
        <Input
          placeholder={t("phone_placeholder")}
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
        />
      </FormControl>

      {/* Name */}
      <FormControl mb={6}>
        <FormLabel>{t("enter_name")}</FormLabel>
        <Input
          placeholder={t("name_placeholder")}
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
      </FormControl>

      {/* 약관 동의 */}
      <Stack spacing={3} mb={6}>
        <Checkbox
          isChecked={allChecked}
          onChange={(e) => handleAllCheck(e.target.checked)}
        >
          {t("agree_all")}
        </Checkbox>
        <Divider />
        <Checkbox
          isChecked={terms.service}
          onChange={(e) => handleSingleCheck("service", e.target.checked)}
        >
          {t("agree_service")}
        </Checkbox>
        <Checkbox
          isChecked={terms.privacy}
          onChange={(e) => handleSingleCheck("privacy", e.target.checked)}
        >
          {t("agree_privacy")}
        </Checkbox>
        <Checkbox
          isChecked={terms.marketing}
          onChange={(e) => handleSingleCheck("marketing", e.target.checked)}
        >
          {t("agree_marketing")}
        </Checkbox>
      </Stack>

      {/* Next Button */}
      <Button
        colorScheme="blue"
        w="100%"
        h="50px"
        onClick={handleNext}
        isDisabled={!phone || !name}
      >
        {t("next")}
      </Button>
    </Box>
  );
}
