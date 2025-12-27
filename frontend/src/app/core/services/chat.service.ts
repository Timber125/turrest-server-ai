import { Injectable, signal } from '@angular/core';
import { SocketService } from './socket.service';
import { ClientSocketSubject, SocketTopic, ServerSocketSubject } from '../../shared/models';

export interface ChatMessage {
  content: string;
  timestamp: Date;
  isSystem: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private messages = signal<ChatMessage[]>([]);

  readonly chatMessages = this.messages.asReadonly();

  constructor(private socketService: SocketService) {
    this.setupListeners();
  }

  private setupListeners(): void {
    // Listen for display chat messages
    this.socketService.onCommand(ServerSocketSubject.DISPLAY_CHAT)
      .subscribe(cmd => {
        const message: ChatMessage = {
          content: cmd.data['msg'] || '',
          timestamp: new Date(),
          isSystem: false
        };
        this.addMessage(message);
      });
  }

  private addMessage(message: ChatMessage): void {
    this.messages.update(msgs => [...msgs, message]);
  }

  sendMessage(text: string): void {
    if (!text.trim()) return;

    this.socketService.sendCommand(ClientSocketSubject.GLOBAL_CHAT, SocketTopic.GLOBAL, {
      msg: text
    });
  }

  clearMessages(): void {
    this.messages.set([]);
  }
}
