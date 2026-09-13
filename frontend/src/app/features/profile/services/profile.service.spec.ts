import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { BehaviorSubject } from 'rxjs';
import { ProfileService } from './profile.service';
import { environment } from '../../../../environment/environment';

describe('ProfileService', () => {
  let account: object | null;
  let status: BehaviorSubject<InteractionStatus>;
  let service: ProfileService;
  let http: HttpTestingController;
  const url = `${environment.apiBaseUrl}/api/usuarios/me`;
  const profile = { id: 1, nombre: 'Ana', apellido: 'Pérez', correo: 'ana@example.com', telefono: '', direccion: 'Santiago', roles: ['Cliente'] };
  beforeEach(() => {
    account = null;
    status = new BehaviorSubject<InteractionStatus>(InteractionStatus.None);
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(),
      { provide: MsalBroadcastService, useValue: { inProgress$: status } },
      { provide: MsalService, useValue: { instance: { getActiveAccount: () => account, getAllAccounts: () => [] } } },
    ] });
    service = TestBed.inject(ProfileService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); TestBed.resetTestingModule(); });
  function login() {
    account = { homeAccountId: 'a', localAccountId: 'a', tenantId: 't', username: 'ana@example.com' };
    status.next(InteractionStatus.None);
  }
  it('does not request guest data and loads the authenticated profile from the API', () => {
    http.expectNone(url);
    login(); http.expectOne(url).flush(profile);
    expect(service.profile()).toEqual(profile);
    status.next(InteractionStatus.AcquireToken); status.next(InteractionStatus.None);
    http.expectNone(url);
  });
  it('uses a blank draft only for an explicit missing profile, not a missing route', () => {
    login(); http.expectOne(url).flush(null, { status: 404, statusText: 'Not Found' });
    expect(service.profile()).toBeNull(); expect(service.error()).toBeTruthy();
    service.refresh();
    http.expectOne(url).flush({ code: 'PROFILE_NOT_FOUND' }, { status: 404, statusText: 'Not Found' });
    expect(service.profile()?.id).toBeNull();
    expect(service.profile()?.nombre).toBe('');
    expect(service.profile()?.correo).toBe('ana@example.com');
    expect(service.profile()?.roles).toEqual([]);
  });
  it('confirms a save only after the API succeeds and supports reloading persisted data', () => {
    login(); http.expectOne(url).flush(profile);
    const { id, ...data } = profile;
    const saved = vi.fn();
    service.save({ ...data, direccion: 'Valparaíso' }, saved);
    const request = http.expectOne(url);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body.id).toBeUndefined();
    expect(request.request.body.propietario).toBeUndefined();
    expect(service.saved()).toBe(false);
    expect(saved).not.toHaveBeenCalled();
    request.flush({ ...profile, direccion: 'Valparaíso' });
    expect(service.saved()).toBe(true); expect(saved).toHaveBeenCalledOnce();
    service.refresh(); http.expectOne(url).flush({ ...profile, direccion: 'Valparaíso' });
    expect(service.profile()?.direccion).toBe('Valparaíso');
  });
  it('preserves the last saved data on failure and permits retrying', () => {
    login(); http.expectOne(url).flush(profile);
    const { id, ...data } = profile;
    const saved = vi.fn();
    service.save({ ...data, nombre: 'Nuevo' }, saved);
    http.expectOne(url).flush(null, { status: 500, statusText: 'Error' });
    expect(service.profile()?.nombre).toBe('Ana');
    expect(service.error()).toContain('guardar');
    expect(service.saving()).toBe(false); expect(saved).not.toHaveBeenCalled();
    service.save(data, saved); http.expectOne(url).flush(profile);
    expect(saved).toHaveBeenCalledOnce();
  });
  it('cancels pending saves and clears personal data when the account changes', () => {
    login(); http.expectOne(url).flush(profile);
    const { id, ...data } = profile;
    service.save(data, vi.fn());
    const request = http.expectOne(url);
    account = { homeAccountId: 'b' }; status.next(InteractionStatus.None);
    expect(request.cancelled).toBe(true);
    expect(service.profile()).toBeNull();
    http.expectOne(url).flush({ ...profile, id: 2, nombre: 'Otra' });
    expect(service.profile()?.nombre).toBe('Otra');
    account = null; status.next(InteractionStatus.None);
    expect(service.profile()).toBeNull(); http.expectNone(url);
  });
});
