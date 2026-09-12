import { NotificationsService } from './features/notifications/services/notifications.service';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { of } from 'rxjs';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [{ provide: NotificationsService, useValue: { notify: () => {} } }, provideRouter([]), { provide: MsalService, useValue: {
        handleRedirectObservable: () => of(null),
        instance: { getActiveAccount: () => null, getAllAccounts: () => [] },
      } }],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });
});
