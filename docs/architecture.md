GUI / Console Clients
        │
        │ TCP Socket
        ▼
    ChatServer
        │
        ▼
 ExecutorService
        │
   ┌────┼────┐
   ▼    ▼    ▼
Client Client Client
Thread Thread Thread
   │
   ▼
Message Processing
   │
   ├── Broadcast
   ├── Private Message
   ├── Active Users
   └── Chat Logging