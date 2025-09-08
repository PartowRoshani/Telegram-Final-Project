
## Introduction
This is the final project of our [**Advanced Programming**](https://github.com/Advanced-Programming-1403) course at Shahid Beheshti University.  
It’s a clone of the famous messaging application **Telegram**, developed in **Java** and designed with **JavaFX**.  

The project implements both **client-side** and **server-side** logic, featuring real-time messaging, saved messages, contacts, groups, and channels.  

---

## Objectives
Here is a list of concepts that were practiced and implemented throughout the project:
- Object-Oriented Programming (OOP) concepts
- Database design and integration
- Multithreading and concurrency control
- Socket programming for client-server communication
- Designing graphical user interfaces with **JavaFX**
- Data handling and persistence with **PostgreSQL**
- Real-time events and message synchronization

---

## Pre Requirements
- **Java** (JDK 23 or higher)  
- **PostgreSQL** (for database)  
- **JavaFX SDK** (for GUI)  
- **IntelliJ IDEA**  
- **Gradle** as the build system  

---
## Database design
![photo_2025-09-07_20-10-33](https://github.com/user-attachments/assets/64f786c4-91a6-4a7c-955f-e6b646baa5e8)

---

## Implementation
The project has 3 main parts:

1. **Telegram Client**  
   - JavaFX-based UI replicating Telegram’s layout (intro, login, chats, sidebar, overlays).  
   - Handles sending/receiving requests through sockets.  
   - Displays messages, saved chats, groups, and user profiles.  

2. **Telegram Server**  
   - Multithreaded server managing client connections concurrently.  
   - Processes requests, accesses the database, and returns structured JSON responses.  
   - Includes a server log showing all events and queries in real-time.  

3. **Sockets**  
   - All client-server communication is handled via sockets.  
   - Each request/response follows a structured JSON protocol.  
   - Supports multiple users communicating simultaneously.  

---

## Creating GUI
The GUI was developed entirely with [**JavaFX**](https://en.wikipedia.org/wiki/JavaFX), SceneBuilder, and custom CSS.  
Features include:  
- Light/Dark theme switching  
- Responsive sidebar navigation  
- Overlays (e.g., Saved Messages, My Profile, Blocked Users)  
- Round avatars and icons styled to match Telegram’s UI  

---

## Database Structures
Implemented with **PostgreSQL**. Main tables include:
- **users** – stores account information  
- **messages** – stores chat messages with sender, receiver, timestamp, and status  
- **private_chat** – for one-on-one conversations (including Saved Messages)  
- **group_chat / channel** – for group and channel features  
- **message_receipts** – to track read/delivery status  

*(SQL schema files are provided in the project repo)*

---

## Presentation
<img width="2093" height="1122" alt="Screenshot 2025-09-08 005216" src="https://github.com/user-attachments/assets/b370b2bb-f84a-400d-81c4-f945c8c1e489" />

<img width="2239" height="1357" alt="Screenshot 2025-09-08 004526" src="https://github.com/user-attachments/assets/70ea1746-0e2e-45d2-a5d2-33c6e15f8dd3" />

<img width="2248" height="1429" alt="Screenshot 2025-09-08 004157" src="https://github.com/user-attachments/assets/61f1fc0c-452c-42da-bf0e-aa68aee06047" />

<img width="2147" height="1248" alt="Screenshot 2025-09-08 005228" src="https://github.com/user-attachments/assets/73874dec-ac9c-4948-a421-379cc1583720" />

---


## How to Run the Code
1. Clone the repository.  
2. Set up the PostgreSQL database using the provided `init.sql`.  
3. In IntelliJ IDEA (with Gradle):  
   - Run `MainServer` first. You should see `Listening ...` in the console.  
   - Run `TelegramApplication`. The intro screen (with “Start Messaging”) will appear.  
4. Login/Register to start chatting.  

---

## Contributors
- Course Instructor: [Dr. Saeed Reza Kheradpisheh](https://www.linkedin.com/in/saeed-reza-kheradpisheh-7a0b18155/)  
- Project Mentor: [Farid Karimi](https://github.com/Farid-Karimi/)  
- Team Members:  
  - [Asal Lotfi](https://github.com/AsalLotfi/)    
  - [Partow Roshani](https://github.com/PartowRoshani/)   
  - [Shima Morevej](https://github.com/shimamoravvej/)   

---

## Date/Time
Summer of 2025 (Version 1.0)

---

## Resources
- [JavaFX Documentation](https://openjfx.io/)  
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)  
- [Gradle Build Tool](https://gradle.org/)  
- [Telegram Design Reference](https://telegram.org/)  
