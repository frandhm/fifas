import { NotificationsService } from './features/notifications/services/notifications.service';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { of, throwError } from 'rxjs';
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

  it('recovers a failed redirect so login can be retried', () => {
    const clearCache = vi.fn().mockResolvedValue(undefined);
    TestBed.overrideProvider(MsalService, { useValue: {
      handleRedirectObservable: () => throwError(() => ({ errorCode: 'state_not_found' })),
      instance: { getActiveAccount: () => null, getAllAccounts: () => [], clearCache },
    } });
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    expect(fixture.componentInstance.authError()).toContain('state_not_found');
    expect(clearCache).toHaveBeenCalledOnce();
  });
});
