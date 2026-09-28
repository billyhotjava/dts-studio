#!/usr/bin/env python3
"""Sprint-6 IT: 前端代码基线验证."""

from pathlib import Path
import json
import unittest


ROOT = Path(__file__).resolve().parents[3]
WEBAPP = ROOT / "dts-copilot-webapp"


class TestWebappStructure(unittest.TestCase):
    """验证前端项目结构."""

    def test_package_json_exists(self) -> None:
        self.assertTrue((WEBAPP / "package.json").exists())

    def test_package_name_is_copilot(self) -> None:
        pkg = json.loads((WEBAPP / "package.json").read_text())
        self.assertIn("copilot", pkg.get("name", "").lower())

    def test_vite_config_exists(self) -> None:
        self.assertTrue(
            (WEBAPP / "vite.config.ts").exists() or
            (WEBAPP / "vite.config.js").exists()
        )

    def test_routes_file_exists(self) -> None:
        self.assertTrue((WEBAPP / "src" / "routes.tsx").exists())

    def test_ai_components_exist(self) -> None:
        ai_dir = WEBAPP / "src" / "components" / "ai"
        self.assertTrue(ai_dir.is_dir(), "AI components directory not found")

    def test_dockerfile_exists(self) -> None:
        self.assertTrue((WEBAPP / "Dockerfile").exists())

    def test_nginx_config_exists(self) -> None:
        self.assertTrue((WEBAPP / "nginx.conf").exists())


class TestNoOldReferences(unittest.TestCase):
    """验证无旧项目残留引用."""

    def test_no_dts_platform_api_references(self) -> None:
        for ts_file in (WEBAPP / "src" / "api").rglob("*.ts"):
            content = ts_file.read_text(encoding="utf-8")
            self.assertNotIn("dts-platform", content, f"{ts_file.name}")

    def test_no_dts_ingestion_references(self) -> None:
        for ts_file in (WEBAPP / "src" / "api").rglob("*.ts"):
            content = ts_file.read_text(encoding="utf-8")
            self.assertNotIn("dts-ingestion", content, f"{ts_file.name}")


if __name__ == "__main__":
    unittest.main(verbosity=2)
