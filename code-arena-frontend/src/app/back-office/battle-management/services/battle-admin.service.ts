import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  AuditLogEntry,
  AvgDuration,
  BattleConfigDTO,
  BattleRoomAdmin,
  BattleRoomDetail,
  BattleParticipantAdmin,
  BattleRoomStatus,
  BattleSummary,
  BulkCancelResult,
  LanguageDistribution,
  OutcomeDistribution,
  RoomListFilters,
  SpringPage,
  StuckRoom,
  TimelinePoint,
  TopChallenge,
  TopPlayer
} from '../models/battle-admin.models';

const ROOT = `${environment.apiBaseUrl}/api/admin/battles`;

@Injectable({ providedIn: 'root' })
export class BattleAdminService {
  constructor(private readonly http: HttpClient) {}

  // ── Analytics ──

  getSummary(refresh = false): Observable<BattleSummary> {
    return this.http.get<BattleSummary>(`${ROOT}/analytics/summary`, {
      params: this.params({ refresh })
    });
  }

  getTimeline(from?: string, to?: string, refresh = false): Observable<TimelinePoint[]> {
    return this.http.get<TimelinePoint[]>(`${ROOT}/analytics/timeline`, {
      params: this.params({ from, to, refresh })
    });
  }

  getTopChallenges(limit = 10, refresh = false): Observable<TopChallenge[]> {
    return this.http.get<TopChallenge[]>(`${ROOT}/analytics/top-challenges`, {
      params: this.params({ limit, refresh })
    });
  }

  getTopPlayers(limit = 10, refresh = false): Observable<TopPlayer[]> {
    return this.http.get<TopPlayer[]>(`${ROOT}/analytics/top-players`, {
      params: this.params({ limit, refresh })
    });
  }

  getLanguageDistribution(from?: string, to?: string, refresh = false): Observable<LanguageDistribution[]> {
    return this.http.get<LanguageDistribution[]>(`${ROOT}/analytics/language-distribution`, {
      params: this.params({ from, to, refresh })
    });
  }

  getOutcomeDistribution(refresh = false): Observable<OutcomeDistribution> {
    return this.http.get<OutcomeDistribution>(`${ROOT}/analytics/outcome-distribution`, {
      params: this.params({ refresh })
    });
  }

  getAvgDuration(refresh = false): Observable<AvgDuration> {
    return this.http.get<AvgDuration>(`${ROOT}/analytics/avg-duration`, {
      params: this.params({ refresh })
    });
  }

  // ── Configuration ──

  getConfig(): Observable<BattleConfigDTO> {
    return this.http.get<BattleConfigDTO>(`${ROOT}/config`);
  }

  updateConfig(dto: BattleConfigDTO): Observable<BattleConfigDTO> {
    return this.http.put<BattleConfigDTO>(`${ROOT}/config`, dto);
  }

  // ── Management ──

  listRooms(filters: RoomListFilters): Observable<SpringPage<BattleRoomAdmin>> {
    return this.http.get<SpringPage<BattleRoomAdmin>>(`${ROOT}/rooms`, {
      params: this.params(filters)
    });
  }

  getRoom(id: string): Observable<BattleRoomDetail> {
    return this.http.get<BattleRoomDetail>(`${ROOT}/rooms/${id}`);
  }

  listParticipants(id: string): Observable<BattleParticipantAdmin[]> {
    return this.http.get<BattleParticipantAdmin[]>(`${ROOT}/rooms/${id}/participants`);
  }

  updateRoomStatus(id: string, status: BattleRoomStatus, reason?: string): Observable<BattleRoomAdmin> {
    return this.http.patch<BattleRoomAdmin>(`${ROOT}/rooms/${id}/status`, { status, reason });
  }

  deleteRoom(id: string): Observable<void> {
    return this.http.delete<void>(`${ROOT}/rooms/${id}`);
  }

  // ── Ops ──

  forceEnd(id: string, winnerId: string | null, reason: string): Observable<BattleRoomAdmin> {
    return this.http.post<BattleRoomAdmin>(`${ROOT}/ops/rooms/${id}/force-end`, {
      winnerId,
      reason
    });
  }

  reassignWinner(id: string, newWinnerId: string, reason: string): Observable<BattleRoomAdmin> {
    return this.http.post<BattleRoomAdmin>(`${ROOT}/ops/rooms/${id}/reassign-winner`, {
      newWinnerId,
      reason
    });
  }

  resetRoom(id: string): Observable<BattleRoomAdmin> {
    return this.http.post<BattleRoomAdmin>(`${ROOT}/ops/rooms/${id}/reset`, {});
  }

  stuckRooms(): Observable<StuckRoom[]> {
    return this.http.get<StuckRoom[]>(`${ROOT}/ops/stuck-rooms`);
  }

  bulkCancel(roomIds: string[], reason: string): Observable<BulkCancelResult> {
    return this.http.post<BulkCancelResult>(`${ROOT}/ops/bulk-cancel`, { roomIds, reason });
  }

  estimateExport(from?: string, to?: string): Observable<{ estimatedRows: number }> {
    return this.http.get<{ estimatedRows: number }>(`${ROOT}/ops/export/estimate`, {
      params: this.params({ from, to })
    });
  }

  /**
   * Triggers a download of the export. We use a fetch-style approach so the
   * Authorization header (added by the JWT interceptor) goes through and the
   * response is treated as a binary blob.
   */
  exportBattles(from: string | null | undefined, to: string | null | undefined, format: 'csv' | 'json'): Observable<Blob> {
    return this.http.get(`${ROOT}/ops/export`, {
      params: this.params({ from, to, format }),
      responseType: 'blob'
    });
  }

  auditLog(page = 0, size = 25): Observable<SpringPage<AuditLogEntry>> {
    return this.http.get<SpringPage<AuditLogEntry>>(`${ROOT}/ops/audit-log`, {
      params: this.params({ page, size, sort: 'performedAt,desc' })
    });
  }

  sendNotification(id: string, title: string, message: string): Observable<{ recipients: number }> {
    return this.http.post<{ recipients: number }>(`${ROOT}/ops/rooms/${id}/send-notification`, {
      title,
      message
    });
  }

  // ── Helpers ──

  private params(input: Record<string, unknown> | object): HttpParams {
    let p = new HttpParams();
    for (const [k, v] of Object.entries(input ?? {})) {
      if (v === null || v === undefined || v === '') continue;
      p = p.set(k, String(v));
    }
    return p;
  }
}
