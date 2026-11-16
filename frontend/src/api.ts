// The animals API client — thin fetch wrappers over the backend the group owns.
// The Vite dev proxy forwards /api to the backend, so paths are same-origin.
// The Authorization header is set by MockAuthProvider's session (dev).

export interface Animal {
  id: number;
  registryCode: string;
  name: string;
  speciesCode: string;
  birthDate?: string;
  ownerIsikukood?: string;
  chipNumber?: string;
  /** Platform columns, filled by the database trigger; read-only. */
  sysStatus?: string;
  sysVersion?: number;
  sysCreatedAt?: string;
  sysCreatedBy?: string;
  sysModifiedAt?: string;
  sysModifiedBy?: string;
}

export interface OwnerInfo {
  isikukood: string;
  firstName: string;
  lastName: string;
  address: string;
}

export interface QueryResult<T> {
  data: T[];
  meta?: { total?: number };
}

function authHeaders(): Record<string, string> {
  // MockAuthProvider persists the signed-in mock user here.
  const user = localStorage.getItem('helex_mock_user') || 'superadmin';
  return { Authorization: `Bearer ${user}` };
}

async function handle<T>(response: Response): Promise<T> {
  if (!response.ok) {
    // The backend answers application/problem+json — surface its human text.
    const problem = await response.json().catch(() => null);
    throw new Error(problem?.detail || `HTTP ${response.status}`);
  }
  return response.json();
}

export const animalsApi = {
  list: (textContains?: string, speciesCode?: string): Promise<QueryResult<Animal>> =>
    fetch(
      '/api/animals?limit=50' +
        (textContains ? `&textContains=${encodeURIComponent(textContains)}` : '') +
        (speciesCode ? `&speciesCode=${encodeURIComponent(speciesCode)}` : ''),
      { headers: authHeaders() },
    ).then((r) => handle<QueryResult<Animal>>(r)),

  get: (id: number): Promise<Animal> =>
    fetch(`/api/animals/${id}`, { headers: authHeaders() }).then((r) => handle<Animal>(r)),

  create: (body: Omit<Animal, 'id'>): Promise<Animal> =>
    fetch('/api/animals', {
      method: 'POST',
      headers: { ...authHeaders(), 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }).then((r) => handle<Animal>(r)),

  /** PUT — the registry code is immutable; the backend keeps the stored one whatever is sent. */
  update: (id: number, body: Omit<Animal, 'id'>): Promise<Animal> =>
    fetch(`/api/animals/${id}`, {
      method: 'PUT',
      headers: { ...authHeaders(), 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    }).then((r) => handle<Animal>(r)),

  /** DELETE — a soft delete: 204, and the registry code becomes reusable. */
  retire: (id: number): Promise<void> =>
    fetch(`/api/animals/${id}`, { method: 'DELETE', headers: authHeaders() }).then((r) =>
      r.ok ? undefined : handle<void>(r),
    ),

  owner: (id: number): Promise<OwnerInfo> =>
    fetch(`/api/animals/${id}/owner`, { headers: authHeaders() }).then((r) =>
      handle<OwnerInfo>(r),
    ),

  species: (): Promise<{ code: string; name: string }[]> =>
    fetch('/api/animals/species', { headers: authHeaders() }).then((r) =>
      handle<{ code: string; name: string }[]>(r),
    ),
};

// ── FAULTS: public infrastructure fault reporting ────────────────────────────
// The client for the FAULTS component, as specified in
// docs/src/specifications/FAULTS.01-fault-reporting.md § API. The types follow
// the backend's JSON as it is sent. Two things the screens need are not sent by
// the API and are worked out here: category names and the undo flag.

export type FaultStatus = 'NEW' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED';

export const FAULT_STATUSES: FaultStatus[] = ['NEW', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'];

export const FAULT_STATUS_LABELS: Record<FaultStatus, string> = {
  NEW: 'New',
  IN_PROGRESS: 'In progress',
  RESOLVED: 'Resolved',
  REJECTED: 'Rejected',
};

export interface FaultCategory {
  code: string;
  name: string;
}

/**
 * The API sends category codes only. The name shown on screen is made from the
 * code (ROAD_SURFACE becomes "Road surface"), which gives the same names as the
 * fault_category table.
 */
const nameFromCode = (code: string): string => {
  const words = code.toLowerCase().replace(/_/g, ' ');
  return words.charAt(0).toUpperCase() + words.slice(1);
};

export interface FaultReport {
  id: number;
  categoryCode: string;
  addressText: string;
  /** The code the address register returned; missing means "address not verified". */
  addressCode?: string;
  description: string;
  observedOn?: string;
  status: FaultStatus;
  /** Platform columns, filled by the database trigger; read-only. */
  sysStatus?: string;
  sysVersion?: number;
  sysCreatedAt?: string;
  sysCreatedBy?: string;
  sysModifiedAt?: string;
  sysModifiedBy?: string;
}

/** What create (POST) and edit (PUT) send; status and sys* are never sent. */
export interface FaultReportInput {
  categoryCode: string;
  addressText: string;
  description: string;
  observedOn?: string;
}

/** One page of reports, as GET /api/faults/reports sends it. */
export interface FaultReportPage {
  content: FaultReport[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** GET /api/faults/reports/summary: active reports counted by status and by category. */
export interface FaultSummary {
  total: number;
  byStatus: Partial<Record<FaultStatus, number>>;
  byCategory: Record<string, number>;
}

/** One row of a report's status history, oldest first. */
export interface StatusChange {
  id: number;
  reportId: number;
  fromStatus?: FaultStatus | null;
  toStatus: FaultStatus;
  note?: string | null;
  createdAt?: string;
  createdBy?: string;
}

/** An undo is saved as a normal history row whose note starts with "UNDO: ". */
const UNDO_PREFIX = 'UNDO: ';

export const isUndo = (row: StatusChange): boolean => Boolean(row.note?.startsWith(UNDO_PREFIX));

/** The note as people wrote it, without the "UNDO: " marker. */
export const noteText = (row: StatusChange): string | undefined =>
  row.note ? (isUndo(row) ? row.note.slice(UNDO_PREFIX.length) : row.note) : undefined;

/** The address register's answer, as the register sends it. */
export interface AddressAnswer {
  address: string;
  code: string;
}

export interface FaultListFilters {
  addressContains?: string;
  status?: string;
  categoryCode?: string;
}

const withJson = (method: string, body: unknown): RequestInit => ({
  method,
  headers: { ...authHeaders(), 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
});

export const faultsApi = {
  categories: (): Promise<FaultCategory[]> =>
    fetch('/api/faults/categories', { headers: authHeaders() })
      .then((r) => handle<string[]>(r))
      .then((codes) => codes.map((code) => ({ code, name: nameFromCode(code) }))),

  /** Newest first, the first 50; the filters are optional and combine. */
  list: (filters: FaultListFilters = {}): Promise<FaultReportPage> => {
    const params = new URLSearchParams({ size: '50', sort: 'newest' });
    if (filters.addressContains) params.set('addressContains', filters.addressContains);
    if (filters.status) params.set('status', filters.status);
    if (filters.categoryCode) params.set('categoryCode', filters.categoryCode);
    return fetch(`/api/faults/reports?${params.toString()}`, { headers: authHeaders() }).then(
      (r) => handle<FaultReportPage>(r),
    );
  },

  summary: (): Promise<FaultSummary> =>
    fetch('/api/faults/reports/summary', { headers: authHeaders() }).then((r) =>
      handle<FaultSummary>(r),
    ),

  get: (id: number): Promise<FaultReport> =>
    fetch(`/api/faults/reports/${id}`, { headers: authHeaders() }).then((r) =>
      handle<FaultReport>(r),
    ),

  /** POST — 201 with the new report; its number is the id. */
  create: (body: FaultReportInput): Promise<FaultReport> =>
    fetch('/api/faults/reports', withJson('POST', body)).then((r) => handle<FaultReport>(r)),

  /** PUT — only while the report is New; otherwise 409. */
  update: (id: number, body: FaultReportInput): Promise<FaultReport> =>
    fetch(`/api/faults/reports/${id}`, withJson('PUT', body)).then((r) => handle<FaultReport>(r)),

  /** DELETE — a soft delete: 204, the report is hidden but kept. */
  retire: (id: number): Promise<void> =>
    fetch(`/api/faults/reports/${id}`, { method: 'DELETE', headers: authHeaders() }).then((r) =>
      r.ok ? undefined : handle<void>(r),
    ),

  /** The register's answer, asked live; nothing is stored. 502 when the register is down. */
  address: (id: number): Promise<AddressAnswer> =>
    fetch(`/api/faults/reports/${id}/address`, { headers: authHeaders() }).then((r) =>
      handle<AddressAnswer>(r),
    ),

  statusChanges: (id: number): Promise<StatusChange[]> =>
    fetch(`/api/faults/reports/${id}/status-changes`, { headers: authHeaders() }).then((r) =>
      handle<StatusChange[]>(r),
    ),

  /** POST — moves the report to the next status; answers with the updated report. */
  changeStatus: (id: number, toStatus: FaultStatus, note?: string): Promise<FaultReport> =>
    fetch(`/api/faults/reports/${id}/status-changes`, withJson('POST', { toStatus, note })).then(
      (r) => handle<FaultReport>(r),
    ),

  /** Undo the last status change; the note is required. Answers with the updated report. */
  undo: (id: number, note: string): Promise<FaultReport> =>
    fetch(`/api/faults/reports/${id}/status-changes/undo`, withJson('POST', { note })).then((r) =>
      handle<FaultReport>(r),
    ),
};
