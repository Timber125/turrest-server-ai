import { Component, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../../core/services';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="chat-container">
      <div class="chat-header">
        <h3>Chat</h3>
      </div>

      <div class="chat-messages" #messagesContainer>
        @for (message of chatService.chatMessages(); track $index) {
          <div class="chat-message" [class.system]="message.isSystem">
            <span class="message-content">{{ message.content }}</span>
          </div>
        }
        @if (chatService.chatMessages().length === 0) {
          <p class="no-messages">No messages yet</p>
        }
      </div>

      <div class="chat-input">
        <input
          type="text"
          [(ngModel)]="newMessage"
          (keyup.enter)="sendMessage()"
          placeholder="Type a message..."
        />
        <button (click)="sendMessage()">Send</button>
      </div>
    </div>
  `,
  styles: [`
    .chat-container {
      height: 100%;
      display: flex;
      flex-direction: column;
      background: #0f0f23;
      border-radius: 8px;
      border: 1px solid #333;
      overflow: hidden;
    }

    .chat-header {
      padding: 0.75rem 1rem;
      border-bottom: 1px solid #333;
    }

    .chat-header h3 {
      margin: 0;
      color: #00d9ff;
      font-size: 1rem;
    }

    .chat-messages {
      flex: 1;
      overflow-y: auto;
      padding: 0.5rem;
      min-height: 200px;
      max-height: 400px;
    }

    .chat-message {
      padding: 0.5rem;
      margin-bottom: 0.25rem;
      border-radius: 4px;
      background: #1a1a2e;
    }

    .chat-message.system {
      background: #2a2a3e;
      font-style: italic;
    }

    .message-content {
      color: #fff;
      word-break: break-word;
      font-size: 0.9rem;
    }

    .no-messages {
      color: #666;
      text-align: center;
      padding: 1rem;
      font-style: italic;
    }

    .chat-input {
      display: flex;
      padding: 0.5rem;
      gap: 0.5rem;
      border-top: 1px solid #333;
    }

    .chat-input input {
      flex: 1;
      padding: 0.5rem;
      border: 1px solid #333;
      border-radius: 4px;
      background: #1a1a2e;
      color: #fff;
    }

    .chat-input input:focus {
      outline: none;
      border-color: #00d9ff;
    }

    .chat-input button {
      padding: 0.5rem 1rem;
      background: #00d9ff;
      border: none;
      color: #000;
      border-radius: 4px;
      cursor: pointer;
      font-weight: bold;
    }

    .chat-input button:hover {
      background: #00b8d9;
    }
  `]
})
export class ChatComponent implements AfterViewChecked {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  newMessage = '';

  constructor(public chatService: ChatService) {}

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    if (this.messagesContainer) {
      const el = this.messagesContainer.nativeElement;
      el.scrollTop = el.scrollHeight;
    }
  }

  sendMessage(): void {
    if (this.newMessage.trim()) {
      this.chatService.sendMessage(this.newMessage);
      this.newMessage = '';
    }
  }
}
