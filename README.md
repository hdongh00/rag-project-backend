# 🤖 RAG-based AI Chatbot Backend (Spring Boot + AWS)

> **사용자가 업로드한 문서를 기반으로 정확하게 답변하는 AI 챗봇 서비스입니다.**
> Spring Boot, AWS(S3, RDS), OpenAI, pgvector를 활용하여 RAG(Retrieval-Augmented Generation) 파이프라인을 구축했습니다.

---

## 📖 프로젝트 소개 (Project Overview)

이 프로젝트는 **RAG (검색 증강 생성)** 기술을 활용하여, LLM(거대 언어 모델)이 학습하지 않은 **사내 문서나 개인 자료**에 대해서도 정확한 답변을 제공할 수 있도록 돕는 백엔드 API 서버입니다.

### 💡 핵심 문제 해결
* **할루시네이션 방지:** AI가 모르는 내용을 지어내는 것을 방지하고, 제공된 문서(Fact)에 기반해서만 답변합니다.
* **최신/비공개 데이터 활용:** 인터넷에 없는 최신 정보나 보안 문서를 AI에게 실시간으로 학습시킬 수 있습니다.

---

## ✨ 주요 기능 (Key Features)

### 1. 🔐 회원 인증 시스템 (Authentication)
* **회원가입/로그인:** BCrypt 비밀번호 암호화 및 JWT 토큰 발급.
* **보안 필터:** `JwtAuthFilter`를 통해 보호된 API에 대한 접근 제어.

### 2. 📂 문서 업로드 및 임베딩 (Ingestion Pipeline)
* **파일 업로드:** 사용자가 PDF/TXT 파일을 업로드하면 AWS S3에 안전하게 저장.
* **텍스트 추출:** Apache PDFBox를 사용하여 PDF 내 텍스트 추출.
* **자동 임베딩:** 추출된 텍스트를 `text-embedding-3-small` 모델을 통해 1536차원 벡터로 변환.
* **벡터 저장:** 변환된 벡터를 `pgvector`가 설치된 PostgreSQL에 저장.

### 3. 💬 AI 채팅 및 검색 (RAG Chat)
* **의미 기반 검색 (Vector Search):** 사용자의 질문을 벡터로 변환하여, DB에서 가장 유사한(거리(L2)가 가까운) 문서 조각을 검색.
* **답변 생성:** 검색된 문서를 Context로 포함하여 GPT-4o-mini에게 질문 전송.
* **정확한 답변:** AI는 제공된 문서 내용을 근거로 답변 생성.

---

## 🛠 기술 스택 (Tech Stack)

### Backend
* **Language:** Java 17
* **Framework:** Spring Boot 3.x
* **Security:** Spring Security, JWT (JSON Web Token)
* **Database:**
    * **Main:** AWS RDS (PostgreSQL 16)
    * **Vector Search:** pgvector Extension
* **ORM:** Spring Data JPA, Hibernate
* **Build Tool:** Gradle

### AI & RAG
* **Framework:** Spring AI (1.0.0-M2)
* **LLM:** OpenAI GPT-4o-mini
* **Embedding:** OpenAI text-embedding-3-small
* **PDF Processing:** Apache PDFBox

### Infrastructure & Cloud
* **Cloud Provider:** AWS
* **Server:** AWS EC2 (Ubuntu)
* **Storage:** AWS S3 (문서 원본 저장)
* **CI/CD & VCS:** Git, GitHub

---

## 🏛️ 시스템 아키텍처 (System Architecture)

```mermaid
graph LR
    User[사용자] -->|API 요청| EC2["EC2 (Spring Boot Server)"]
    EC2 -->|인증/인가| JWT["JWT (JWT Filter)"]
    EC2 -->|파일 업로드| S3["S3 (AWS S3 Bucket)"]
    EC2 -->|벡터 변환| OpenAI["OpenAI (OpenAI API)"]
    EC2 -->|메타데이터 & 벡터 저장| RDS["RDS (AWS RDS PostgreSQL + pgvector)"]
    
    subgraph RAG_Pipeline [RAG Pipeline]
    S3 -->|텍스트 추출| PDFBox
    PDFBox -->|임베딩| OpenAI
    OpenAI -->|벡터 저장| RDS
    end
    
    subgraph Chat_Pipeline [Chat Pipeline]
    User -->|질문| EC2
    EC2 -->|질문 벡터화| OpenAI
    OpenAI -->|유사 문서 검색| RDS
    RDS -->|관련 문서 Context| EC2
    EC2 -->|프롬프트 생성| GPT["GPT (GPT-4o-mini)"]
    GPT -->|답변| User
    end
