# ChatLink

**LAN-based Real-Time Chat Application** (WhatsApp-lite style)

A complete multi-user chat system built with **Java**, **Java Swing**, **TCP Sockets**, and **Multithreading**.

---

## Features

- User Authentication (Sign Up / Login)
- Real-time Online Users list
- Public Group Chat
- Private (1-to-1) Chat
- User-created Custom Groups (with Admin controls)
- File Sharing (Upload + Download) with server-side storage
- Persistent Chat History (survives logout/restart)
- Profile section (Change Password + Logout)
- Modern light-themed Swing UI
- Works across multiple computers on the same Wi-Fi / LAN

---

## Project Structure

```
ChatLink/
├── src/chatapp/
│   ├── Server.java           # Server entry point
│   ├── ClientHandler.java    # Handles each client connection
│   ├── LoginFrame.java       # Login / Sign Up UI + Server IP
│   └── ChatFrame.java        # Main chat interface
├── README.md
└── .gitignore
```

---

## How to Run

### Requirements
- JDK 11 or higher
- NetBeans / IntelliJ / any Java IDE (or command line)

### Steps

1. **Start the Server**
   - Run `Server.java`
   - Server listens on port **5000**

2. **Start Clients**
   - Run `LoginFrame.java`
   - Enter Server IP:
     - `localhost` → same computer
     - `192.168.x.x` → other computers on the same network
   - Sign Up / Login

3. **Multiple Users**
   - Open multiple client instances (or run on different laptops)
   - Start chatting!

---

## Network Setup (Different Computers)

1. Run Server on one computer
2. Find its IP address:
   ```bash
   ipconfig          # Windows
   ifconfig / ip a   # Linux/Mac
   ```
3. On other computers, enter that IP in the **Server IP Address** field
4. Make sure Windows Firewall allows Java / port 5000

---

## Technology Stack

| Component       | Technology              |
|-----------------|-------------------------|
| Language        | Java                    |
| GUI             | Java Swing              |
| Networking      | TCP Sockets             |
| Concurrency     | Multithreading          |
| Storage         | File-based (users.txt, groups.txt, chat_history/, server_files/) |
| Architecture    | Client–Server           |

---

## Protocol Overview

Simple text-based protocol over TCP:

- `LOGIN` / `SIGNUP`
- `GROUP|message`
- `PRIVATE|username|message`
- `GROUP_MSG|groupName|message`
- `CREATE_GROUP|name`
- `ADD_TO_GROUP|group|user1,user2`
- `FILE_START` / `FILE_CHUNK` / `FILE_END`
- `FILE_AVAILABLE` / `DOWNLOAD_FILE`
- `ONLINE|user1,user2,...`

---

## Author

Developed as a Computer Networking / Software Engineering lab project.

---

## License

This project is for educational purposes.
