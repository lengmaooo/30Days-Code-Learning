# Stage 3：AI + RAG + Agent（Day 18-30）

## 📚 学习目标
掌握 AI 应用开发核心技能，打造可演示的 AI 知识库问答系统（跳槽王炸项目）。

## 📅 学习内容

### Day 18：AI 基础 + 大模型 API 调用
- LLM（大语言模型）概念
- Java 调用大模型接口

### Day 19：RAG 核心思想
- 文档切片、检索、增强生成
- RAG 解决的核心问题

### Day 20：向量库基础
- 向量库核心作用
- 行业主流向量库认知

### Day 21-24：实战项目 - AI 知识库问答系统
- 文档上传/解析/切片
- 检索 + 大模型回答
- Java 接口开发 + 业务逻辑

### Day 25：Agent 概念（面试必问）
- 智能体 = 大模型 + 工具调用
- 典型场景：查订单/查天气/执行业务

### Day 26：Agent 简单 demo
- 根据问题调用不同接口
- 轻量化 Agent 实现

### Day 27：提示词工程
- 高质量 prompt 编写技巧
- 回答效果调优

### Day 28：AI 项目完善
- 接口统一封装、全局异常处理
- 项目完整演示流程

### Day 29：简历包装
- 突出 Java 后端+微服务+AI 复合标签
- 项目描述专业化
- 技术亮点提炼

### Day 30：面试模拟 + 投简历
- 背熟自我介绍（突出 AI 项目）
- 项目讲解话术打磨
- 定向投递目标岗位

## 🏗️ 项目结构
```
Stage3-AI/
├── src/main/java/
│   ├── controller/
│   │   ├── ChatController.java      # LLM 对话
│   │   ├── KnowledgeController.java # RAG 知识库
│   │   └── AgentController.java     # Agent 智能体
│   ├── service/
│   │   ├── LLMService.java
│   │   ├── RagService.java
│   │   └── AgentService.java
│   └── config/
```

## 🚀 如何运行
```bash
cd Stage3-AI
mvn spring-boot:run
```

## 📝 学习笔记
详见 [Notes](./Notes/) 目录

## 🎯 最终交付物
- 1 个可演示的 AI 知识库问答系统（核心跳槽项目）
- 1 份包装后的高分简历 + 面试话术脚本
