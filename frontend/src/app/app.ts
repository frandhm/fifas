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
  private readonly msal = inject(MsalService);

  ngOnInit(): void {
    this.msal.handleRedirectObservable().subscribe({
      next: (result) => {
        const account = result?.account ?? this.msal.instance.getActiveAccount()
          ?? this.msal.instance.getAllAccounts()[0];
        if (account) this.msal.instance.setActiveAccount(account);
      },
      error: () => {
        this.authError.set('No se pudo completar el inicio de sesión. Inténtalo de nuevo desde Perfil.');
      },
    });
  }
}