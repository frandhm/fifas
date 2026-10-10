import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { finalize, timeout } from 'rxjs';
import { environment } from '../../../../environment/environment';

export type SupportCategory = 'pedido' | 'producto' | 'pago' | 'cuenta' | 'otro';
export type SupportStatus = 'abierto' | 'en_proceso' | 'cerrado';

export interface SupportTicket {
  id: number;
  correo: string;
  asunto: string;
  categoria: SupportCategory;
  mensaje: string;
  estado: SupportStatus;
  fechaCreacion: string;
}

export interface NewSupportTicket {
  correo: string;
  asunto: string;
  categoria: SupportCategory;
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class SupportService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/mensajes`;
  private readonly eventosUrl = `${environment.apiBaseUrl}/api/rabbit/eventos/soporte.mensaje`;
  readonly tickets = signal<SupportTicket[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  refresh(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set(null);
    this.http.get<SupportTicket[]>(this.url).pipe(
      timeout(15000), finalize(() => this.loading.set(false)),
    ).subscribe({
      next: tickets => this.tickets.set(Array.isArray(tickets) ? tickets : []),
      error: error => this.error.set(this.message('cargar tus solicitudes', error)),
    });
  }

  create(ticket: NewSupportTicket, onSuccess: () => void): void {
    if (this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    this.http.post(this.eventosUrl, ticket, { responseType: 'text' }).pipe(
      timeout(15000), finalize(() => this.saving.set(false)),
    ).subscribe({
      next: () => {
        onSuccess();
        // El listener guarda el ticket de forma asíncrona: recargamos tras un instante
        setTimeout(() => this.refresh(), 1000);
      },
      error: error => this.error.set(this.message('enviar tu solicitud', error)),
    });
  }

  private message(action: string, error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 400) return 'Revisa los datos ingresados antes de enviar la solicitud.';
      if (error.status === 401 || error.status === 403) return 'Tu sesión expiró o no tiene acceso. Vuelve a iniciar sesión.';
      if (error.status === 0) return `No pudimos ${action}: no hay conexión con el servicio.`;
    }
    return `No pudimos ${action}. Inténtalo nuevamente en unos minutos.`;
  }
}