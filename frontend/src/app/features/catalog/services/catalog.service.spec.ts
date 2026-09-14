import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { BehaviorSubject } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { CatalogService } from './catalog.service';
import { environment } from '../../../../environment/environment';

describe('Catalog session lifecycle', () => {
  let account: object | null;
  let status: BehaviorSubject<InteractionStatus>;
  let http: HttpTestingController;
  let catalog: CatalogService;
  const url = `${environment.apiBaseUrl}/api/productos`;
  beforeEach(() => {
    account = null;
    status = new BehaviorSubject<InteractionStatus>(InteractionStatus.Startup);
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(), provideHttpClientTesting(),
      { provide: MsalBroadcastService, useValue: { inProgress$: status } },
      { provide: MsalService, useValue: { instance: {
        getActiveAccount: () => account, getAllAccounts: () => [],
      } } },
    ] });
    http = TestBed.inject(HttpTestingController);
    catalog = TestBed.inject(CatalogService);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); });
  it('waits for login and loads without recreating the service; token events do not loop', () => {
    http.expectNone(url);
    status.next(InteractionStatus.None);
    http.expectNone(url);
    account = { homeAccountId: 'a', localAccountId: 'a', tenantId: 'tenant' };
    status.next(InteractionStatus.None);
    http.expectOne(url).flush([]);
    expect(catalog.list()).toEqual([]);
    expect(catalog.loadError()).toBeNull();
    status.next(InteractionStatus.AcquireToken);
    status.next(InteractionStatus.None);
    http.expectNone(url);
  });
  it('allows retry after a server failure without signing out', () => {
    account = { homeAccountId: 'a' };
    status.next(InteractionStatus.None);
    http.expectOne(url).flush(null, { status: 503, statusText: 'Unavailable' });
    expect(catalog.loadError()).toContain('503');
    catalog.loadFromBackend();
    http.expectOne(url).flush([]);
    expect(catalog.loadError()).toBeNull();
    expect(catalog.loading()).toBe(false);
  });
  it('cancels the old account request on logout', () => {
    account = { homeAccountId: 'a' };
    status.next(InteractionStatus.None);
    const request = http.expectOne(url);
    account = null;
    status.next(InteractionStatus.None);
    expect(request.cancelled).toBe(true);
    expect(catalog.loadError()).toContain('Inicia sesión');
  });
});
