#!/usr/bin/env python3
"""Sprint-4 IT: API Key 认证体系基线验证."""

from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[3]
AI_SRC = ROOT / "dts-copilot-ai" / "src" / "main" / "java" / "com" / "yuzhi" / "dts" / "copilot" / "ai"
ANALYTICS_SRC = ROOT / "dts-copilot-analytics" / "src" / "main" / "java"


class TestApiKeyStructure(unittest.TestCase):
    """验证 API Key 认证代码结构."""

    def test_api_key_entity_exists(self) -> None:
        self.assertTrue((AI_SRC / "domain" / "ApiKey.java").exists())

    def test_api_key_service_exists(self) -> None:
        self.assertTrue((AI_SRC / "service" / "auth" / "ApiKeyService.java").exists())

    def test_auth_filter_exists(self) -> None:
        self.assertTrue((AI_SRC / "security" / "ApiKeyAuthFilter.java").exists())

    def test_user_context_exists(self) -> None:
        self.assertTrue((AI_SRC / "security" / "CopilotUserContext.java").exists())

    def test_security_config_exists(self) -> None:
        self.assertTrue((AI_SRC / "config" / "SecurityConfiguration.java").exists())


class TestNoPlatformAuthDependency(unittest.TestCase):
    """验证 analytics 无 Platform 认证残留."""

    def test_no_platform_trusted_user_service(self) -> None:
        for java_file in ANALYTICS_SRC.rglob("*.java"):
            content = java_file.read_text(encoding="utf-8")
            self.assertNotIn(
                "PlatformTrustedUserService",
                content,
                f"{java_file.name} still references PlatformTrustedUserService"
            )


if __name__ == "__main__":
    unittest.main(verbosity=2)
