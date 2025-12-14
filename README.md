# LedgerLens

A SaaS application for freelancers and businesses to manage their expenses and income, with AI-powered document processing and Gmail integration.

## 📋 Overview

LedgerLens is a comprehensive financial management solution that helps freelancers and small businesses track their transactions, manage invoices, and automate expense recording through intelligent document processing. The application integrates with Gmail to automatically process incoming invoices and receipts.

## ✨ Features

- **User Authentication**
  - Local registration with email verification
  - OAuth2 login with Google
  - JWT-based authentication
  - Password encryption

- **Transaction Management**
  - Create, view, and manage income/expense transactions
  - Categorize transactions
  - Track payment methods
  - Support for multiple currencies

- **Document Management**
  - Upload and store financial documents (invoices, receipts)
  - View documents directly in the browser
  - Cloud storage via Cloudflare R2 / AWS S3

- **AI-Powered Document Processing**
  - Automatic text extraction using Apache Tika
  - AI-powered transaction data extraction using Google Gemini AI
  - Intelligent categorization of expenses and income

- **Gmail Integration**
  - Automatic monitoring of Gmail inbox via Google Pub/Sub
  - Auto-processing of email attachments (invoices/receipts)
  - Real-time webhook notifications for new emails

## 🛠️ Tech Stack

- **Backend Framework:** Spring Boot 3.5.5
- **Language:** Java 17
- **Database:** MySQL 8
- **ORM:** Spring Data JPA / Hibernate
- **Security:** Spring Security, OAuth2, JWT
- **Cloud Storage:** AWS S3 / Cloudflare R2
- **AI:** Google Gemini AI
- **Document Processing:** Apache Tika
- **Email:** Gmail API, Google Pub/Sub
- **Build Tool:** Maven
- **Other Libraries:**
  - Lombok
  - MapStruct
  - Spring Mail

## 📁 Project Structure

```
src/main/java/com/gourav/LedgerLens/
├── Configuration/        # App configurations (Security, S3, AI, Email)
├── Controller/           # REST API endpoints
│   ├── AuthController         # Authentication endpoints
│   ├── DocumentController     # Document management
│   ├── TransactionalController # Transaction CRUD
│   ├── GmailAdminController   # Gmail watch management
│   └── GmailPubSubController  # Gmail webhook handler
├── Domain/
│   ├── Dtos/             # Data Transfer Objects
│   ├── Entity/           # JPA Entities (User, Transaction, Document)
│   └── Enum/             # Enumerations
├── Helper/               # Utility classes
├── Mapper/               # MapStruct mappers
├── Repository/           # JPA Repositories
├── Security/             # Security filters and services
└── Service/              # Business logic
    └── ServiceImp/       # Service implementations
```

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- Google Cloud Project (for Gmail API and Gemini AI)
- AWS Account or Cloudflare R2 Account

### Environment Variables

Create a `.env` file in the root directory with the following variables:

```properties
# JWT Configuration
JWT_SECRET=your-jwt-secret-key

# Database Configuration
DB_URL=jdbc:mysql://localhost:3306/LedgerLens
DB_USERNAME=your-db-username
DB_PASSWORD=your-db-password

# AWS S3 Configuration
AWS_ACCESS_KEY_ID=your-aws-access-key
AWS_SECRET_ACCESS_KEY=your-aws-secret-key
AWS_REGION=eu-north-1
AWS_S3_BUCKET_NAME=your-bucket-name

# Cloudflare R2 Configuration
CLOUDFARE_ACCOUNT_ID=your-cloudflare-account-id
CLOUDFLARE_ACCESS_KEY=your-cloudflare-access-key
CLOUDFLARE_SECRET_KEY=your-cloudflare-secret-key
CLOUDFLARE_ENDPOINT=your-cloudflare-endpoint

# Google OAuth Configuration
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET_ID=your-google-client-secret
GOOGLE_OAUTH_URI=your-oauth-redirect-uri

# Google AI Configuration
GOOGLE_API_KEY=your-gemini-api-key

# Email Configuration
SUPPORT_GMAIL=your-support-email
APP_PASSWORD=your-app-password
```

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/LedgerLens.git
   cd LedgerLens
   ```

2. **Create the database**
   ```sql
   CREATE DATABASE LedgerLens;
   ```

3. **Configure environment variables**
   - Copy the `.env.example` to `.env`
   - Fill in your configuration values

4. **Build the project**
   ```bash
   ./mvnw clean install
   ```

5. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

The application will start on `http://localhost:8000`

## 📚 API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register a new user |
| POST | `/api/v1/auth/login` | Login and get JWT token |
| POST | `/api/v1/auth/verify` | Verify email with code |
| POST | `/api/v1/auth/resend/verification` | Resend verification email |
| POST | `/api/v1/auth/delete` | Delete user account |

### Transactions
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/transaction/createTransaction` | Create new transaction |
| GET | `/api/v1/transaction/getAllTransactions` | Get all user transactions |
| GET | `/api/v1/transaction/getTransactionById/{publicId}` | Get transaction by ID |
| GET | `/api/v1/transaction/getAllExpenseTransactions` | Get all expense transactions |
| GET | `/api/v1/transaction/getAllIncomeTransactions` | Get all income transactions |

### Documents
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/documents/upload` | Upload documents |
| GET | `/api/v1/documents` | Get all documents |
| GET | `/api/v1/documents/{publicId}/view` | View document (PDF) |

### Gmail Integration
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/pubsub/push` | Gmail Pub/Sub webhook |
| POST | `/gmail/stop-watch` | Stop Gmail watch for user |

## 🔐 Security

- JWT-based authentication with configurable expiration
- OAuth2 integration with Google for social login
- Password encryption using BCrypt
- Email verification for new accounts

## 🏗️ Architecture

The application follows a layered architecture:

1. **Controller Layer** - Handles HTTP requests and responses
2. **Service Layer** - Contains business logic
3. **Repository Layer** - Data access using Spring Data JPA
4. **Domain Layer** - Entity models and DTOs

### Gmail Integration Flow

1. User authenticates via Google OAuth2
2. Application sets up Gmail watch using Pub/Sub
3. When new email arrives, Google sends notification to `/pubsub/push`
4. Application fetches email, extracts attachments
5. Documents are stored in S3/R2
6. AI processes documents to extract transaction data
7. Transactions are automatically created

## 📧 Email Processing

The application uses Google's Gmail API to:
- Monitor inbox for new emails
- Extract PDF attachments from emails
- Process invoices and receipts automatically

### Setting up Gmail Watch

1. Configure Google Cloud Pub/Sub topic
2. Grant Gmail API permissions to the service account
3. User authenticates via OAuth2 with `gmail.modify` scope
4. Application automatically sets up watch on user's inbox

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request


## 👤 Author

**Gourav**

## 🙏 Acknowledgments

- Spring Boot Team
- Google Cloud Platform
- Apache Tika Project

