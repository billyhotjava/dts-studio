#!/usr/bin/env python3
"""Sprint-1 IT: 项目脚手架基线验证."""

from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[3]


def file_exists(rel_path: str) -> bool:
    return (ROOT / rel_path).exists()


def read_text(rel_path: str) -> str:
    return (ROOT / rel_path).read_text(encoding="utf-8")


class TestMavenScaffold(unittest.TestCase):
    """验证 Maven 多模块项目结构完整性."""

    def test_parent_pom_exists(self) -> None:
        self.assertTrue(file_exists("pom.xml"))

    def test_parent_pom_declares_modules(self) -> None:
        content = read_text("pom.xml")
        self.assertIn("<module>dts-copilot-ai</module>", content)
        self.assertIn("<module>dts-copilot-analytics</module>", content)

    def test_copilot_ai_pom_exists(self) -> None:
        self.assertTrue(file_exists("dts-copilot-ai/pom.xml"))

    def test_copilot_analytics_pom_exists(self) -> None:
        self.assertTrue(file_exists("dts-copilot-analytics/pom.xml"))

    def test_copilot_ai_application_class(self) -> None:
        self.assertTrue(file_exists(
            "dts-copilot-ai/src/main/java/com/yuzhi/dts/copilot/ai/CopilotAiApplication.java"
        ))

    def test_copilot_analytics_application_class(self) -> None:
        self.assertTrue(file_exists(
            "dts-copilot-analytics/src/main/java/com/yuzhi/dts/copilot/analytics/CopilotAnalyticsApplication.java"
        ))


class TestDockerCompose(unittest.TestCase):
    """验证 Docker Compose 配置完整性."""

    def test_compose_file_exists(self) -> None:
        self.assertTrue(file_exists("docker-compose.yml"))

    def test_compose_dev_file_exists(self) -> None:
        self.assertTrue(file_exists("docker-compose.dev.yml"))

    def test_compose_declares_all_services(self) -> None:
        content = read_text("docker-compose.yml")
        for svc in ["copilot-postgres", "copilot-ollama", "copilot-ai", "copilot-analytics", "copilot-proxy"]:
            self.assertIn(svc, content, f"Service {svc} not found in docker-compose.yml")

    def test_postgres_init_script_exists(self) -> None:
        self.assertTrue(file_exists("docker/postgres/init-db.sql"))

    def test_postgres_init_creates_schemas(self) -> None:
        content = read_text("docker/postgres/init-db.sql")
        self.assertIn("copilot_ai", content)
        self.assertIn("copilot_analytics", content)

    def test_env_file_exists(self) -> None:
        self.assertTrue(file_exists(".env"))


class TestCertificateInfra(unittest.TestCase):
    """验证证书管理基础设施."""

    def test_gen_certs_script_exists(self) -> None:
        self.assertTrue(file_exists("services/certs/gen-certs.sh"))

    def test_certs_gitignore_exists(self) -> None:
        self.assertTrue(file_exists("services/certs/.gitignore"))

    def test_certs_gitignore_excludes_keys(self) -> None:
        content = read_text("services/certs/.gitignore")
        self.assertIn("*.key", content)

    def test_traefik_static_config_exists(self) -> None:
        self.assertTrue(file_exists("services/proxy/traefik.yml"))

    def test_traefik_dynamic_routes_exists(self) -> None:
        self.assertTrue(file_exists("services/proxy/dynamic/routes.yml"))

    def test_traefik_routes_contain_all_services(self) -> None:
        content = read_text("services/proxy/dynamic/routes.yml")
        self.assertIn("copilot-ai", content)
        self.assertIn("copilot-analytics", content)
        self.assertIn("copilot-webapp", content)

    def test_traefik_tls_config(self) -> None:
        content = read_text("services/proxy/dynamic/routes.yml")
        self.assertIn("server.crt", content)
        self.assertIn("server.key", content)
        self.assertIn("VersionTLS12", content)


class TestLiquibaseBaseline(unittest.TestCase):
    """验证 Liquibase 迁移配置."""

    def test_copilot_ai_liquibase_master(self) -> None:
        self.assertTrue(file_exists(
            "dts-copilot-ai/src/main/resources/config/liquibase/master.xml"
        ))

    def test_copilot_analytics_liquibase_master(self) -> None:
        self.assertTrue(file_exists(
            "dts-copilot-analytics/src/main/resources/config/liquibase/master.xml"
        ))


if __name__ == "__main__":
    unittest.main(verbosity=2)
