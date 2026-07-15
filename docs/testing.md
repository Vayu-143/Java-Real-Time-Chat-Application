| Test               | Input/Action          | Expected Result                | Status |
| ------------------ | --------------------- | ------------------------------ | ------ |
| Server startup     | Start server          | Server listens on port 5000    | Pass   |
| Multiple clients   | Connect 3 users       | All connect successfully       | Pass   |
| Broadcast          | Alice sends message   | All clients receive message    | Pass   |
| Active users       | `/users`              | Shows Alice, Bob, Charlie      | Pass   |
| Private message    | `/msg Bob Hello`      | Only sender and Bob receive it | Pass   |
| Duplicate username | Connect another Alice | Username rejected              | Pass   |
| Invalid user       | `/msg David Hello`    | User-not-online error          | Pass   |
| Disconnect         | `/quit`               | Leave notification appears     | Pass   |
| Logging            | Send messages         | Activity stored in log file    | Pass   |