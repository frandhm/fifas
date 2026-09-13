import { NotificationsService } from './features/notifications/services/notifications.service';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MsalService } from '@azure/msal-angular';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  readonly authError = signal<string | null>(null);
  protected readonly title = signal('frontend');
  private readonly notifications = inject(NotificationsService);
  private readonly msal = inject(MsalService);

  ngOnInit(): void {
    this.msal.handleRedirectObservable().subscribe({
      next: (result) => {
        const account = result?.account ?? this.msal.instance.getActiveAccount()
          ?? this.msal.instance.getAllAccounts()[0];
        if (account) {
          this.msal.instance.setActiveAccount(account);
          this.authError.set(null);
        }
        if (result?.account) this.notifications.notify('sesion', 'Iniciaste sesión con Microsoft. Bienvenido a Los FIFAS.');
      },
      error: (error: unknown) => {
        // A redirect can report an auxiliary error after Entra already stored a
        // valid account. In that case the login succeeded and must not be hidden.
        const account = this.msal.instance.getActiveAccount() ?? this.msal.instance.getAllAccounts()[0];
        if (account) {
          this.msal.instance.setActiveAccount(account);
          this.authError.set(null);
          return;
        }
        const code = typeof error === 'object' && error !== null && 'errorCode' in error
          && typeof error.errorCode === 'string' ? error.errorCode : 'redirect_error';
        this.authError.set(`No se pudo completar el inicio de sesión (${code}). La sesión local fue restablecida; inténtalo nuevamente desde Perfil.`);
        // A failed redirect can leave interaction_in_progress in MSAL's
        // temporary cache, causing every later loginRedirect to fail instantly.
        void this.msal.instance.clearCache().catch(() => undefined);
      },
    });
  }
}
