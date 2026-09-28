#!/usr/bin/env python3
"""Sprint-5 IT: BI 引擎代码基线验证."""

from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[3]
ANALYTICS_SRC = ROOT / "dts-copilot-analytics" / "src" / "main" / "java"


class TestAnalyticsFork(unittest.TestCase):
    """验证 analytics fork 完整性."""

    def test_no_old_package_references(self) -> None:
        for java_file in ANALYTICS_SRC.rglob("*.java"):
            content = java_file.read_text(encoding="utf-8")
            self.assertNotIn(
                "com.yuzhi.dts.analytics",
                content,
                f"{java_file.name} still references old package"
            )

    def test_no_platform_client_references(self) -> None:
        for java_file in ANALYTICS_SRC.rglob("*.java"):
            content = java_file.read_text(encoding="utf-8")
            self.assertNotIn("PlatformInfraClient", content, f"{java_file.name}")
            self.assertNotIn("PlatformAiNativeClient", content, f"{java_file.name}")
            self.assertNotIn("PlatformTrustedUserService", content, f"{java_file.name}")

    def test_copilot_ai_client_exists(self) -> None:
        found = list(ANALYTICS_SRC.rglob("CopilotAiClient.java"))
        self.assertTrue(len(found) > 0, "CopilotAiClient.java not found")


class TestAnalyticsConfig(unittest.TestCase):
    """验证 analytics 配置."""

    def test_application_yml_exists(self) -> None:
        yml = ROOT / "dts-copilot-analytics" / "src" / "main" / "resources" / "config" / "application.yml"
        self.assertTrue(yml.exists())

    def test_application_yml_uses_copilot_schema(self) -> None:
        yml = ROOT / "dts-copilot-analytics" / "src" / "main" / "resources" / "config" / "application.yml"
        content = yml.read_text(encoding="utf-8")
        self.assertIn("copilot_analytics", content)


if __name__ == "__main__":
    unittest.main(verbosity=2)
