import { Injectable, signal } from '@angular/core';
import { Subject, Observable, filter, map } from 'rxjs';
import { SocketCommand, ClientSocketSubject, SocketTopic } from '../../shared/models';
import { AuthService } from './auth.service';

export type ConnectionState = 'disconnected' | 'connecting' | 'connected' | 'error';

@Injectable({
  providedIn: 'root'
})
export class SocketService {
  private readonly WS_URL = 'ws://localhost:8081/ws/lobby';

  private socket: WebSocket | null = null;
  private messageSubject = new Subject<SocketCommand>();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000;

  private connectionState = signal<ConnectionState>('disconnected');
  readonly state = this.connectionState.asReadonly();

  constructor(private authService: AuthService) {}

  connect(): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      return;
    }

    this.connectionState.set('connecting');

    try {
      this.socket = new WebSocket(this.WS_URL);

      this.socket.onopen = () => {
        console.log('WebSocket connected');
        this.connectionState.set('connected');
        this.reconnectAttempts = 0;
        this.authenticate();
      };

      this.socket.onmessage = (event) => {
        try {
          const command: SocketCommand = JSON.parse(event.data);
          console.log('Received:', command);
          this.messageSubject.next(command);
        } catch (e) {
          console.error('Failed to parse message:', event.data);
        }
      };

      this.socket.onclose = (event) => {
        console.log('WebSocket closed:', event.code, event.reason);
        this.connectionState.set('disconnected');
        this.attemptReconnect();
      };

      this.socket.onerror = (error) => {
        console.error('WebSocket error:', error);
        this.connectionState.set('error');
      };
    } catch (e) {
      console.error('Failed to create WebSocket:', e);
      this.connectionState.set('error');
    }
  }

  private authenticate(): void {
    const token = this.authService.getToken();
    const userId = this.authService.getUserId();

    if (token && userId) {
      this.sendCommand(ClientSocketSubject.SOCKET_CONNECT, SocketTopic.LOGIN, {
        token: token,
        userid: userId
      });
    }
  }

  private attemptReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
      console.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`);

      setTimeout(() => {
        if (this.authService.isAuthenticated()) {
          this.connect();
        }
      }, delay);
    }
  }

  disconnect(): void {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
    this.connectionState.set('disconnected');
  }

  sendCommand(subject: string, topic: string, data: Record<string, any> = {}): void {
    if (this.socket?.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket not connected, cannot send command');
      return;
    }

    const command: SocketCommand = { subject, topic, data };
    console.log('Sending:', command);
    this.socket.send(JSON.stringify(command));
  }

  onCommand(subject?: string, topic?: string): Observable<SocketCommand> {
    return this.messageSubject.asObservable().pipe(
      filter(cmd => {
        if (subject && cmd.subject.toUpperCase() !== subject.toUpperCase()) return false;
        if (topic && cmd.topic.toUpperCase() !== topic.toUpperCase()) return false;
        return true;
      })
    );
  }

  onAllMessages(): Observable<SocketCommand> {
    return this.messageSubject.asObservable();
  }
}
