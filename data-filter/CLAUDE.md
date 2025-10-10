# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot application that integrates with Alibaba Cloud AI services, specifically DashScope. The application provides AI-powered chat functionality with features like:
- Conversational AI with memory management
- RAG (Retrieval Augmented Generation) using Milvus vector store
- Data filtering capabilities
- Deep research workflows using graph-based processing
- User authentication with email verification

## Code Architecture

### Main Components

1. **Controllers**: REST API endpoints
   - `ChatController`: Main chat functionality with streaming responses
   - `UserController`: User authentication and management
   - `KnowledgeBaseController`: Knowledge base management
   - `CommonController`: Utility endpoints

2. **Services**: Business logic implementation
   - `ChatService`: Core chat functionality
   - `UserService`: User management
   - `CollectionService`: Knowledge base collection management

3. **Configuration**: Spring configuration classes
   - `ChatClientConfig`: AI chat client configuration
   - `MilvusConfig`: Vector store configuration
   - `RedisConfig`: Redis configuration
   - `ResearchConfig`: Deep research workflow configuration

4. **Domain Models**: Data transfer objects and entities
   - DTOs in `model.dto` package
   - Domain models in `model.domain` package

5. **Tools**: AI tools for extended functionality
   - `DataFilterTool`: Data filtering capabilities
   - `ResearchTool`: Research workflow tools

6. **Utils**: Utility classes
   - `MilvusVectorStoreUtils`: Vector store utilities
   - `EmailUtils`: Email sending utilities
   - `RedisUtils`: Redis utilities

### Key Features

1. **Chat System**:
   - Streaming responses with thinking process visibility
   - Conversation memory management
   - Multiple AI model support

2. **RAG System**:
   - Milvus vector store integration
   - Document processing (PDF, various formats)
   - Similarity search capabilities

3. **Research Workflows**:
   - Graph-based research planning
   - Multi-step research execution
   - Custom tool integration

4. **User Management**:
   - Email verification
   - Password management
   - Redis-based session storage

## Development Commands

### Build Project
```bash
mvn clean install
```

### Run Application
```bash
mvn spring-boot:run
```

### Run in Development Mode
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Build Executable JAR
```bash
mvn clean package
```

### Run Tests
```bash
mvn test
```

## Configuration

The application uses `application.yml` for configuration:
- Database: PostgreSQL
- AI: DashScope API (Alibaba Cloud AI)
- Vector Store: Milvus
- Email: SMTP (QQ Mail by default)
- Redis: Session storage
- Aliyun OSS: File storage

Environment variables needed:
- `DOCKER_IP`: Docker host IP
- `AI_DASHSCOPE_API_KEY`: Alibaba Cloud API key
- `MAIL_USERNAME`: Email username
- `MAIL_PASSWORD`: Email password

## Dependencies

Key dependencies include:
- Spring Boot Web
- Alibaba Cloud AI DashScope
- Spring AI Milvus Store
- MyBatis Plus
- Redis
- PostgreSQL
- Hutool (utility library)
- Lombok