# Cognitive Workflow Framework

> **Academic Reference**: This implementation is based on my thesis "Designing Self-Aware Multi-Agent AI Systems: A Two-Fold Framework Based on AIBOM and Reflective Architecture", Università degli Studi di Firenze, 2024/2025.
> 
> 📚 **Complete thesis materials** (including PDF, experiments, and supplementary materials): [github.com/NiccoloCase/bsc-multi-agent-ai-framework](https://github.com/NiccoloCase/bsc-multi-agent-ai-framework/tree/main)

A reflective, self-aware multi-agent AI system that implements dynamic workflow execution with runtime adaptability. This framework combines the Reflection architectural pattern with an AI Bill of Materials (AIBOM) approach to create maintainable, traceable, and adaptive cognitive workflows.

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- MongoDB Atlas account
- OpenAI API key
- Anthropic API key (optional)

### Environment Setup

Create a `.env` file or set the following environment variables:

```bash
# Required: OpenAI API Configuration
OPENAI_API_KEY=your_openai_api_key_here

# Optional: Anthropic API Configuration  
ANTHROPIC_API_KEY=your_anthropic_api_key_here

# Required: MongoDB Configuration
MONGO_URI=your_mongodb_connection_string
MONGO_TEST_URI=your_test_mongodb_connection_string
MONGO_VECTOR_SEARCH_DEMO_URI=your_vector_search_mongodb_connection_string
```

### MongoDB Atlas Setup

The application requires specific search indexes to be created in MongoDB Atlas:

| Collection | Index Name | Type | Purpose |
|------------|------------|------|---------|
| `intents` | `intent_vector_index` | vectorSearch | Intent similarity matching |
| `meta_nodes` | `node_search_index` | search | Node keyword search |
| `meta_nodes` | `node_vector_index` | vectorSearch | Node semantic search |

**Important**: Ensure all indexes are in `READY` status before starting the application.

### Running the Application

```bash
# Clone the repository
git clone https://github.com/ncaselli/cognitive-workflow.git
cd cognitive-workflow

# Install dependencies and run
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## 🏗️ Architecture Overview

### Dual-Layer Architecture

The framework implements a two-layer architecture based on the Reflection pattern:

- **Knowledge Layer (Meta-Level)**: Manages workflow meta-models, node definitions, and intent configurations
- **Operational Layer (Base-Level)**: Handles real-time workflow execution and orchestration

### Core Components

#### 🧠 Intent Detection Service
- Analyzes natural language requests
- Retrieves or creates intent metadata
- Supports semantic intent matching

#### 🔀 Routing Manager  
- Queries workflows associated with detected intents
- Applies scoring and selection algorithms
- Supports diversity sampling for workflow selection

#### ⚙️ Workflow Engine
- Custom-built for dynamic AI workflows
- DAG-based execution with dependency resolution  
- Runtime adaptation and port matching

#### 🔌 Port Adapter Service
- Automatic port compatibility resolution using LLMs
- Runtime workflow patching
- Reduces system brittleness to interface changes

#### 🔍 Hybrid Search
- Combines semantic and keyword search
- Enables component discovery and retrieval
- Supports workflow synthesis from existing components


## 🧪 Testing

The project includes comprehensive test coverage:

- **48 Unit Tests**: Model validation, port logic, core functionality
- **54 Integration Tests**: LLM integration, schema conversion, database operations  
- **2 End-to-End Tests**: Complete workflow execution scenarios

```bash
# Run all tests
mvn test

# Run specific test categories
mvn test -Dtest="**/*UnitTest"
mvn test -Dtest="**/*IntegrationTest"
```


## 🎯 Use Cases

### AI for Network Engineering (AI4NE)
- Intelligent routing of AI tasks across cloud resources
- Dynamic model selection based on constraints
- Real-time latency and cost optimization

### Network Engineering for AI (NE4AI)  
- Network topology optimization for AI workloads
- Resource allocation for distributed AI systems
- Performance monitoring and adaptation

### General Cognitive Workflows
- Multi-step reasoning tasks
- Document processing pipelines  
- Complex decision-making workflows

## 🔒 Security & Compliance

- **AIBOM Integration**: Complete traceability of AI components
- **Audit Logging**: Comprehensive execution tracking
- **Model Governance**: Version control and certification support
- **Data Privacy**: Configurable data retention policies

## 📊 Monitoring & Observability

The framework provides built-in monitoring capabilities:

- Workflow execution metrics
- Node performance tracking  
- Intent detection accuracy
- System health indicators

Access metrics at `/actuator/metrics` when running.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`) 
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request


## 🙏 Acknowledgments

- **Università degli Studi di Firenze** - Department of Information Engineering
- **Prof. Enrico Vicario** - Thesis Supervisor  
- **Marco Becattini** - Co-supervisor
- **Software Technologies Lab** - SALLMA architecture foundation

---
