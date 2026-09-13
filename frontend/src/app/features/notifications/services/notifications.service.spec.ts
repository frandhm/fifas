import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { BehaviorSubject } from 'rxjs';
import { NotificationsService } from './notifications.service';
import { environment } from '../../../../environment/environment';

describe('Notifications', () => {
  let account: object | null;
  let status: BehaviorSubject<InteractionStatus>;
  let service: NotificationsService;
  let http: HttpTestingController;
  const url = `${environment.apiBaseUrl}/api/notificaciones`;
  const item = { id: 1, tipo: 'carrito' as const, mensaje: 'Producto agregado', fecha: '2026-09-12T00:00:00Z', leido: false };
  beforeEach(() => {
    account = null;
    status = new BehaviorSubject<InteractionStatus>(InteractionStatus.None);
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(),
      { provide: MsalBroadcastService, useValue: { inProgress$: status } },
      { provide: MsalService, useValue: { instance: { getActiveAccount: () => account, getAllAccounts: () => [] } } },
    ] });
    service = TestBed.inject(NotificationsService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); });
  function login() {
    account = { homeAccountId: 'a', localAccountId: 'a', tenantId: 't' };
    status.next(InteractionStatus.None); http.expectOne(url).flush([item]);
  }
  it('shows guest feedback without storing a notification', () => {
    service.notify('carrito', 'Producto agregado');
    expect(service.toast()).toBe('Producto agregado'); http.expectNone(url);
  });
  it('loads own notifications and persists read state only after success', () => {
    login(); expect(service.unread()).toBe(1);
    service.setRead(item);
    const request = http.expectOne(url);
    expect(request.request.body).toEqual({ id: 1, leido: true });
    expect(service.unread()).toBe(1);
    request.flush({ ...item, leido: true }); expect(service.unread()).toBe(0);
  });
  it('does not include a client-supplied owner and reports a failed save', () => {
    login(); service.notify('perfil', 'Perfil guardado');
    const request = http.expectOne(url);
    expect(request.request.body).toEqual({ tipo: 'perfil', mensaje: 'Perfil guardado' });
    request.flush(null, { status: 500, statusText: 'Error' });
    expect(service.error()).toContain('guardar la notificación');
    expect(service.items().length).toBe(1);
  });
  it('clears the inbox on logout and ignores an old account write response', () => {
    login(); service.notify('carrito', 'Producto agregado');
    const request = http.expectOne(url);
    account = null; status.next(InteractionStatus.None);
    request.flush(item);
    expect(service.items()).toEqual([]); expect(service.toast()).toBeNull(); http.expectNone(url);
  });
  it('reports a missing deployed route instead of an empty history', () => {
    account = { homeAccountId: 'a' }; status.next(InteractionStatus.None);
    http.expectOne(url).flush(null, { status: 404, statusText: 'Not Found' });
    expect(service.error()).toContain('HTTP 404'); expect(service.loading()).toBe(false);
  });
  it('rejects the legacy microservice response with a clear message', () => {
    account = { homeAccountId: 'a' }; status.next(InteractionStatus.None);
    http.expectOne(url).flush([{ id: 1, usuarioId: 3, mensaje: 'Anterior', leido: false }]);
    expect(service.error()).toContain('formato incompatible'); expect(service.items()).toEqual([]);
  });
  it('does not reload on token acquisition for the same account', () => {
    login(); status.next(InteractionStatus.AcquireToken); status.next(InteractionStatus.None); http.expectNone(url);
  });
});
