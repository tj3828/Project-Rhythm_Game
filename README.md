# Drop the beat (Rhythm Game)

> We tried to implement the functionality like a real game service.

- ### LOGIN & SIGN UP

![Home SignUp](https://user-images.githubusercontent.com/43510811/61625713-5749f200-acb6-11e9-9c42-3ba31df1ed81.png)

- ### CREATE ROOM & MESSAGE 

![Create](https://user-images.githubusercontent.com/43510811/61625748-6fba0c80-acb6-11e9-8635-d2969779eefd.png)

- ### INVITE & KICK OUT

![Invite](https://user-images.githubusercontent.com/43510811/61625796-87919080-acb6-11e9-975d-d8ef2289e6ad.png)

- ### GAME

![In Game](https://user-images.githubusercontent.com/43510811/61625911-d50dfd80-acb6-11e9-8f0c-6f4aa8d5c8f9.png)



## About


This project is a rhythm game project using Java GUI Swing. Our goal was to make it as functional as a real game.

In this project, The project focused on serving like a real game without any tools.
  


## Stack

 - Java Swing
 - Graphics
 - [javazoom.jl.player](http://www.java2s.com/Code/Jar/j/Downloadjl10jar.htm)
 - File I/O
 - TCP/UDP Network


## feature

### Lounge
 - Create Room (1 player, 2 player, Secret Room)
 - Quick Start (Room condition : 2 player, Not secret Room)
 - Personal Message (Not Read Message count)
 - Show all connected users

### Message Store
 - Distinguish between read and unread notes
 - Distinguish between incoming and outgoing notes
 - Change as read messages (Immediately when reading a message)

### Room
 - Invite (approval or refuse)
 - Play 30 seconds highlight mp3 (selected by the Room manager)
 - Kick out (by the Room manager)
 - Change Room Information (Room title, Secret Room State, 1p / 2p)
 - Chat
 - Ready and Start

### Game
 - Update scores in real time (with the other's score)
 - Result (Compare my score with the other person)



  
