import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Subject, Observable, filter, map } from 'rxjs';
import { SocketCommand, ClientSocketSubject, SocketTopic } from '../../shared/models';
import { AuthService } from './auth.service';

export type ConnectionState = 'disconnected' | 'connecting' | 'connected' | 'error' | 'server_down';

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

  private tokenRejected = false;  // Flag to prevent reconnect after explicit token rejection
  private tabId: string;

  constructor(private authService: AuthService, private router: Router) {
    this.tabId = sessionStorage.getItem('turrest_tab_id') || '';
    if (!this.tabId) {
      this.tabId = crypto.randomUUID();
      sessionStorage.setItem('turrest_tab_id', this.tabId);
    }
  }

  connect(): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      return;
    }

    this.tokenRejected = false;  // Reset on fresh connection attempt
    this.connectionState.set('connecting');

    try {
      this.socket = new WebSocket(this.WS_URL);

      this.socket.onopen = () => {
        console.log('WebSocket connected');
        this.connectionState.set('connected');
        // Note: Don't reset reconnectAttempts here - only reset after successful auth
        this.authenticate();
      };

      this.socket.onmessage = (event) => {
        try {
          const command: SocketCommand = JSON.parse(event.data);
          console.log('Received:', command);

          if (command.subject === 'CORE' && command.topic === 'TOKEN_INVALID') {
            this.handleTokenInvalid();
            return;
          }

          // Any successful message means we're authenticated - reset reconnect counter
          this.reconnectAttempts = 0;

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
        userid: userId,
        tabid: this.tabId
      });
    }
  }

  private attemptReconnect(): void {
    // Don't reconnect if token was explicitly rejected by server
    if (this.tokenRejected) {
      console.log('Token was rejected - not attempting reconnect');
      return;
    }

    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
      console.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`);

      setTimeout(() => {
        if (this.authService.isAuthenticated()) {
          this.connect();
        }
      }, delay);
    } else {
      // Server appears to be down - logout user
      console.error('Max reconnect attempts reached. Server appears to be down.');
      this.connectionState.set('server_down');
      this.forceLogout('Server is unavailable. Please try again later.');
    }
  }

  private forceLogout(message: string): void {
    console.warn('Forcing logout:', message);
    this.authService.markSessionInvalidated();
    this.disconnect();
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  disconnect(): void {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
    this.connectionState.set('disconnected');
  }

  private handleTokenInvalid(): void {
    console.warn('Token rejected by server - logging out immediately');
    this.tokenRejected = true;  // Prevent reconnection attempts
    this.forceLogout('Your session is invalid. Please login again.');
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
