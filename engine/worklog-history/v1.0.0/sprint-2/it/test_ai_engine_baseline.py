#!/usr/bin/env python3
"""Sprint-2 IT: AI 引擎核心代码基线验证."""

from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[3]
AI_SRC = ROOT / "dts-copilot-ai" / "src" / "main" / "java" / "com" / "yuzhi" / "dts" / "copilot" / "ai"


class TestAiEngineSourceStructure(unittest.TestCase):
    """验证 AI 引擎代码结构完整性."""

    def test_openai_client_exists(self) -> None:
        self.assertTrue((AI_SRC / "service" / "llm" / "OpenAiCompatibleClient.java").exists())

    def test_gateway_service_exists(self) -> None:
        self.assertTrue((AI_SRC / "service" / "llm" / "gateway" / "AiGatewayService.java").exists())

    def test_copilot_service_exists(self) -> None:
        self.assertTrue((AI_SRC / "service" / "copilot" / "AiCopilotService.java").exists())

    def test_nl2sql_service_exists(self) -> None:
        self.assertTrue((AI_SRC / "service" / "copilot" / "Nl2SqlService.java").exists())

    def test_config_service_exists(self) -> None:
        self.assertTrue((AI_SRC / "service" / "config" / "AiConfigService.java").exists())

    def test_copilot_resource_exists(self) -> None:
        self.assertTrue((AI_SRC / "web" / "rest" / "AiCopilotResource.java").exists())


class TestAiEngineNoPlatformDependency(unittest.TestCase):
    """验证 AI 引擎无 dts-platform 残留依赖."""

    def test_no_platform_imports(self) -> None:
        for java_file in AI_SRC.rglob("*.java"):
            content = java_file.read_text(encoding="utf-8")
            self.assertNotIn(
                "com.yuzhi.dts.platform",
                content,
                f"{java_file.name} still imports dts-platform"
            )

    def test_no_ingestion_imports(self) -> None:
        for java_file in AI_SRC.rglob("*.java"):
            content = java_file.read_text(encoding="utf-8")
            self.assertNotIn(
                "com.yuzhi.dts.ingestion",
                content,
                f"{java_file.name} still imports dts-ingestion"
            )


if __name__ == "__main__":
    unittest.main(verbosity=2)
