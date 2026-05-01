package app.models;

import java.time.LocalDateTime;

public class ChatMessage extends Entity {
  private int conversationId;
  private int senderId;
  private int receiverId;
  private String content;
  private LocalDateTime sentAt;

  public ChatMessage() {
  super();
  }

  public ChatMessage(
   int id,
   int conversationId,
   int senderId,
   int receiverId,
   String content,
   LocalDateTime sentAt) {
  super(id);
  this.conversationId = conversationId;
  this.senderId = senderId;
  this.receiverId = receiverId;
  this.content = content;
  this.sentAt = sentAt;
  }

  public int getConversationId() {
  return conversationId;
  }

  public int getSenderId() {
  return senderId;
  }

  public int getReceiverId() {
  return receiverId;
  }

  public String getContent() {
  return content;
  }

  public LocalDateTime getSentAt() {
  return sentAt;
  }
}

