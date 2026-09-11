import { TestBed } from '@angular/core/testing';
import { HttpRequest, HttpResponse } from '@angular/common/http';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { firstValueFrom, of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { environment } from '../../../environment/environment';
import { apiAuthInterceptor } from './api-auth.interceptor';

describe('API authentication without redirects', () => {
  const originalScopes = environment.azure.protectedResourceScopes;
  const account = { homeAccountId: 'test' };
  const msal = {
    instance: { getActiveAccount: vi.fn(() => account), getAllAccounts: vi.fn(() => [account]) },
    acquireTokenSilent: vi.fn(), loginRedirect: vi.fn(),
  };
  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({ providers: [
      { provide: MsalService, useValue: msal },
      { provide: MsalBroadcastService, useValue: { inProgress$: of(InteractionStatus.None) } },
    ] });
  });
  afterEach(() => { environment.azure.protectedResourceScopes = originalScopes; });
  const request = new HttpRequest('GET', `${environment.apiBaseUrl}/api/productos`);

  it('rejects missing API configuration without a login redirect', async () => {
    environment.azure.protectedResourceScopes = [];
    const next = vi.fn((_request: HttpRequest<unknown>) => of(new HttpResponse()));
    await expect(firstValueFrom(TestBed.runInInjectionContext(() => apiAuthInterceptor(request, next))))
      .rejects.toMatchObject({ status: 503 });
    expect(next).not.toHaveBeenCalled();
    expect(msal.loginRedirect).not.toHaveBeenCalled();
  });

  it('passes the API access token to the backend', async () => {
    environment.azure.protectedResourceScopes = ['api://test/access'];
    msal.acquireTokenSilent.mockReturnValue(of({ accessToken: 'test-token' }));
    const next = vi.fn((_request: HttpRequest<unknown>) => of(new HttpResponse()));
    await firstValueFrom(TestBed.runInInjectionContext(() => apiAuthInterceptor(request, next)));
    expect(next.mock.calls[0][0].headers.get('Authorization')).toBe('Bearer test-token');
    expect(msal.loginRedirect).not.toHaveBeenCalled();
  });

  it('propagates a token failure instead of redirecting again', async () => {
    environment.azure.protectedResourceScopes = ['api://test/access'];
    msal.acquireTokenSilent.mockReturnValue(throwError(() => new Error('interaction_required')));
    const next = vi.fn((_request: HttpRequest<unknown>) => of(new HttpResponse()));
    await expect(firstValueFrom(TestBed.runInInjectionContext(() => apiAuthInterceptor(request, next))))
      .rejects.toThrow('interaction_required');
    expect(next).not.toHaveBeenCalled();
    expect(msal.loginRedirect).not.toHaveBeenCalled();
  });
});
