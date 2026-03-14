#!/usr/bin/env python3
"""Sprint-3 IT: AI 高级能力代码基线验证."""

from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[3]
AI_SRC = ROOT / "dts-copilot-ai" / "src" / "main" / "java" / "com" / "yuzhi" / "dts" / "copilot" / "ai"


class TestRagStructure(unittest.TestCase):
    """验证 RAG 模块代码结构."""

    def test_embedding_service_exists(self) -> None:
        self.assertTrue((AI_SRC / "service" / "rag" / "embedding" / "EmbeddingService.java").exists())

    def test_vector_store_exists(self) -> None:
        self.assertTrue((AI_SRC / "service" / "rag" / "VectorStoreService.java").exists())

    def test_hybrid_search_exists(self) -> None:
        self.assertTrue((AI_SRC / "service" / "rag" / "HybridSearchService.java").exists())

    def test_rag_service_exists(self) -> None:
        self.assertTrue((AI_SRC / "service" / "rag" / "RagService.java").exists())


class TestAgentStructure(unittest.TestCase):
    """验证 Agent 模块代码结构."""

    def test_react_engine_exists(self) -> None:
        self.assertTrue((AI_SRC / "service" / "agent" / "ReActEngine.java").exists())

    def test_tool_registry_exists(self) -> None:
        self.assertTrue((AI_SRC / "service" / "tool" / "ToolRegistry.java").exists())

    def test_copilot_tool_interface_exists(self) -> None:
        self.assertTrue((AI_SRC / "service" / "tool" / "CopilotTool.java").exists())

    def test_builtin_tools_directory_exists(self) -> None:
        self.assertTrue((AI_SRC / "service" / "tool" / "builtin").is_dir())


class TestSafetyStructure(unittest.TestCase):
    """验证安全防护模块代码结构."""

    def test_sql_sandbox_exists(self) -> None:
        self.assertTrue((AI_SRC / "service" / "safety" / "SqlSandbox.java").exists())

    def test_guardrails_exists(self) -> None:
        self.assertTrue((AI_SRC / "service" / "safety" / "GuardrailsInterceptor.java").exists())


class TestChatStructure(unittest.TestCase):
    """验证 Chat 会话管理模块."""

    def test_chat_service_exists(self) -> None:
        self.assertTrue((AI_SRC / "service" / "chat" / "AgentChatService.java").exists())

    def test_chat_resource_exists(self) -> None:
        self.assertTrue((AI_SRC / "web" / "rest" / "AgentChatResource.java").exists())

    def test_chat_session_entity_exists(self) -> None:
        self.assertTrue((AI_SRC / "domain" / "AiChatSession.java").exists())


if __name__ == "__main__":
    unittest.main(verbosity=2)
