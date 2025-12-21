# 🤖 RAG-based AI Chatbot Service (Full-Stack)

> **사용자가 업로드한 문서(PDF, Word, PPT 등)를 학습하여 질문에 정확하게 답변하는 AI 챗봇 서비스입니다.** > **Spring Boot**와 **React**를 연동한 풀스택 프로젝트로, RAG(Retrieval-Augmented Generation) 파이프라인을 통해 환각(Hallucination) 없는 정확한 정보를 제공합니다.

---

## 📖 프로젝트 소개 (Project Overview)

이 프로젝트는 거대 언어 모델(LLM)이 알지 못하는 **사내 비공개 문서나 개인 자료**를 지식 베이스로 활용할 수 있도록 돕는 **RAG 기반 챗봇 서비스**입니다.

### 💡 핵심 가치
* **풀스택 아키텍처:** Spring Boot API 서버와 React 기반의 모던한 UI가 통합된 완성형 서비스입니다.
* **광범위한 문서 지원:** 단순 텍스트뿐만 아니라 **PDF, Word(.docx), PPT(.pptx)** 등 다양한 오피스 문서를 지원합니다.
* **편리한 사용자 경험:** 구글 소셜 로그인, 실시간 채팅 UI, 문서 관리 사이드바 등 직관적인 UX를 제공합니다.

---

## ✨ 주요 기능 (Key Features)

### 1. 🔐 강화된 인증 시스템 (Advanced Auth)
* **소셜 로그인:** Google OAuth2.0을 연동하여 원클릭 로그인/회원가입 지원.
* **JWT 보안:** Access Token 기반의 인증 인가 처리 및 `JwtAuthFilter`를 통한 API 보안 적용.
* **일반 로그인:** BCrypt 암호화를 적용한 이메일/비밀번호 회원가입 지원.

### 2. 📂 문서 파이프라인 (Ingestion Pipeline)
* **다양한 포맷 지원:** PDF(`Apache PDFBox`), Word/PPT(`Apache POI`) 파일의 텍스트를 추출하여 학습.
* **S3 스토리지:** 업로드된 원본 파일은 AWS S3 버킷에 안전하게 영구 저장.
* **벡터 임베딩:** 추출된 텍스트를 `text-embedding-3-small`로 벡터화하여 `pgvector`(PostgreSQL)에 저장.

### 3. 💬 AI 채팅 및 컨텍스트 (RAG Chat)
* **벡터 유사도 검색:** 사용자 질문과 가장 관련성 높은 문서 조각(Chunk)을 L2 거리 기반으로 검색.
* **대화 맥락 유지:** 이전 대화 내용(History)을 DB에 저장하고, 질문 시 최근 대화 내역을 함께 프롬프트에 포함하여 문맥을 이해하는 답변 생성.
* **프롬프트 엔지니어링:** 시스템 프롬프트를 통해 AI의 답변 페르소나와 답변 형식을 제어.

### 4. 💻 모던 프론트엔드 (React Client)
* **반응형 UI:** Tailwind CSS를 활용한 깔끔하고 직관적인 채팅 인터페이스.
* **문서 관리:** 사이드바를 통해 학습된 문서 목록을 확인하고 삭제할 수 있는 관리 기능 제공.
* **실시간 인터랙션:** 로딩 상태 표시(Skeleton/Spinner), 토스트 알림(Toast Notification) 등으로 향상된 사용자 경험 제공.

---

## 🛠 기술 스택 (Tech Stack)

### Frontend
* **Core:** React 19, Vite
* **Styling:** Tailwind CSS 4, Lucide React (Icons)
* **State/Network:** Axios, React Router DOM
* **Environment:** Node.js

### Backend
* **Language:** Java 17
* **Framework:** Spring Boot 3.x
* **Security:** Spring Security, OAuth2 Client, JWT
* **Database:** * **Main:** AWS RDS (PostgreSQL 16)
    * **Vector:** pgvector Extension
* **Utilities:** Apache POI (Word/PPT), Apache PDFBox (PDF)

### AI & Cloud
* **LLM Ops:** Spring AI (OpenAI GPT-4o-mini, text-embedding-3-small)
* **Infrastructure:** AWS EC2, S3, RDS
* **DevOps:** Git, GitHub

---

## 🏛️ 시스템 아키텍처 (System Architecture)

```mermaid
graph LR
    subgraph Client [React Frontend]
        UI[User Interface]
        Auth_UI[Login Page & Google Auth]
    end

    subgraph Server [Spring Boot Backend]
        Controller[REST Controllers]
        Security[Security Filter Chain]
        Service[Business Logic]
    end

    subgraph Infrastructure [AWS & Database]
        S3[AWS S3 Bucket]
        RDS[PostgreSQL + pgvector]
    end

    subgraph AI [OpenAI API]
        GPT[GPT-4o-mini]
        Embed[Embedding Model]
    end

    %% Flow
    UI -->|API Requests| Controller
    Auth_UI -->|OAuth2| Security
    
    Controller --> Service
    Service -->|File Upload| S3
    Service -->|Vector Search & Save| RDS
    Service -->|Generate Answer & Embedding| AI
