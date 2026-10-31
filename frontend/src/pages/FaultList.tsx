import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Select } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import {
  AppButtonPrimary,
  AppButtonSecondary,
  AppCard,
  AppLink,
  AppTag,
  ResourceList,
  useAppNotification,
  type ResourceListColumn,
} from '@helex/ui';
import {
  faultsApi,
  FAULT_STATUSES,
  FAULT_STATUS_LABELS,
  type AddressAnswer,
  type FaultCategory,
  type FaultReport,
  type FaultSummaryRow,
} from '../api';

/** FAULTS.01 § UI: dates are shown as DD/MM/YYYY, times as DD/MM/YYYY HH:mm. */
const formatDate = (value?: string) => (value ? dayjs(value).format('DD/MM/YYYY') : '—');
const formatDateTime = (value?: string) => (value ? dayjs(value).format('DD/MM/YYYY HH:mm') : '—');

interface AppliedFilters {
  status?: string;
  categoryCode?: string;
}

/**
 * The fault report list — FAULTS.01 § UI, "Report list". A master-detail
 * ResourceList like the animals list: search by address, a filter card for
 * status and category, a detail panel with the address register's answer asked
 * live, and the counts per category and status above it. A failed load is shown
 * as an error, never as an empty list.
 */
export const FaultList = () => {
  const navigate = useNavigate();
  const notify = useAppNotification();
  const [reports, setReports] = useState<FaultReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [search, setSearch] = useState('');
  const [categories, setCategories] = useState<FaultCategory[]>([]);
  /* The filter card's state: what the Selects show, and what the list is
     actually filtered by. They meet when the user presses Apply. */
  const [statusFilter, setStatusFilter] = useState<string | undefined>();
  const [categoryFilter, setCategoryFilter] = useState<string | undefined>();
  const [applied, setApplied] = useState<AppliedFilters>({});
  const [selected, setSelected] = useState<FaultReport | null>(null);
  const [registerAnswer, setRegisterAnswer] = useState<AddressAnswer | null>(null);
  const [summary, setSummary] = useState<FaultSummaryRow[] | null>(null);
  const [summaryError, setSummaryError] = useState<string | null>(null);

  useEffect(() => {
    faultsApi.categories().then(setCategories).catch(() => setCategories([]));
  }, []);

  const categoryName = useMemo(() => {
    const names = new Map(categories.map((c) => [c.code, c.name]));
    return (code: string) => names.get(code) ?? code;
  }, [categories]);

  const load = useCallback(
    (text: string, filters: AppliedFilters) => {
      setLoading(true);
      faultsApi
        .list({ addressContains: text || undefined, ...filters })
        .then((result) => {
          setReports(result.data);
          setLoadError(null);
        })
        .catch((e: Error) => {
          setLoadError(e.message);
          notify.error('Could not load reports', e.message);
        })
        .finally(() => setLoading(false));
    },
    [notify],
  );

  useEffect(() => {
    const t = setTimeout(() => load(search, applied), 250);
    return () => clearTimeout(t);
  }, [search, applied, load, reloadKey]);

  useEffect(() => {
    faultsApi
      .summary()
      .then((rows) => {
        setSummary(rows);
        setSummaryError(null);
      })
      .catch((e: Error) => {
        setSummary(null);
        setSummaryError(e.message);
      });
  }, [reloadKey]);

  useEffect(() => {
    setRegisterAnswer(null);
    if (selected?.id) {
      faultsApi
        .address(selected.id)
        .then(setRegisterAnswer)
        .catch(() => setRegisterAnswer(null)); // register down: leave the line out, not an error
    }
  }, [selected]);

  const columns: ResourceListColumn<FaultReport>[] = [
    {
      key: 'id',
      title: 'Report no.',
      dataIndex: 'id',
      locked: true,
      // The report number is the way into the record: it links to the report's page.
      render: (id: number) => (
        <AppLink
          href={`/faults/${id}`}
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
            navigate(`/faults/${id}`);
          }}
        >
          {id}
        </AppLink>
      ),
    },
    {
      key: 'categoryCode',
      title: 'Category',
      dataIndex: 'categoryCode',
      render: (code: string) => <AppTag>{categoryName(code)}</AppTag>,
    },
    {
      key: 'addressText',
      title: 'Address',
      dataIndex: 'addressText',
      render: (text: string, report: FaultReport) => (
        <>
          {text} {!report.addressCode && <AppTag>not verified</AppTag>}
        </>
      ),
    },
    {
      key: 'status',
      title: 'Status',
      dataIndex: 'status',
      render: (status: FaultReport['status']) => (
        <AppTag>{FAULT_STATUS_LABELS[status] ?? status}</AppTag>
      ),
    },
    {
      key: 'sysCreatedAt',
      title: 'Reported on',
      dataIndex: 'sysCreatedAt',
      render: (value?: string) => formatDate(value),
    },
    {
      key: 'observedOn',
      title: 'Date seen',
      dataIndex: 'observedOn',
      defaultVisible: false,
      render: (value?: string) => formatDate(value),
    },
  ];

  if (loadError && reports.length === 0) {
    return (
      <AppCard title="Fault reports" style={{ maxWidth: 640, margin: '24px auto' }}>
        <p>Could not load reports.</p>
        <p style={{ opacity: 0.65 }}>{loadError}</p>
        <AppButtonSecondary onClick={() => setReloadKey((k) => k + 1)}>Try again</AppButtonSecondary>
      </AppCard>
    );
  }

  return (
    <div style={{ display: 'grid', gap: 16 }}>
      <SummaryCard summary={summary} error={summaryError} categoryName={categoryName} />
      <ResourceList<FaultReport>
        title="Fault reports"
        columns={columns}
        dataSource={reports}
        rowKey="id"
        loading={loading}
        search={{ value: search, onChange: setSearch, placeholder: 'Address…' }}
        filter={{
          content: (
            <div style={{ display: 'grid', gap: 8 }}>
              <Select
                style={{ minWidth: 220 }}
                placeholder="Status"
                allowClear
                value={statusFilter}
                onChange={setStatusFilter}
                options={FAULT_STATUSES.map((s) => ({ value: s, label: FAULT_STATUS_LABELS[s] }))}
              />
              <Select
                style={{ minWidth: 220 }}
                placeholder="Category"
                allowClear
                value={categoryFilter}
                onChange={setCategoryFilter}
                options={categories.map((c) => ({ value: c.code, label: c.name }))}
              />
            </div>
          ),
          onApply: () => setApplied({ status: statusFilter, categoryCode: categoryFilter }),
          onClear: () => {
            setStatusFilter(undefined);
            setCategoryFilter(undefined);
            setApplied({});
          },
        }}
        actions={
          <AppButtonPrimary icon={<PlusOutlined />} onClick={() => navigate('/faults/new')}>
            Report a problem
          </AppButtonPrimary>
        }
        detailView={{
          selectedRecord: selected,
          onSelectedRecordChange: setSelected,
          title: 'Fault report',
          render: (report) => (
            <div style={{ display: 'grid', gap: 8 }}>
              <div>
                <b>Report {report.id}</b> · {FAULT_STATUS_LABELS[report.status] ?? report.status}
              </div>
              <div>Category: {categoryName(report.categoryCode)}</div>
              <div>
                Address: {report.addressText}
                {report.addressCode ? ` · ${report.addressCode}` : ' · address not verified'}
                {registerAnswer && (
                  <div style={{ opacity: 0.65, fontSize: 12 }}>
                    {registerAnswer.address} · {registerAnswer.code}, via the address register
                    (mock / X-Road)
                  </div>
                )}
              </div>
              <div>{report.description}</div>
              {report.observedOn && <div>Seen on: {formatDate(report.observedOn)}</div>}
              <div style={{ opacity: 0.65, fontSize: 12 }}>
                Reported {formatDateTime(report.sysCreatedAt)}
                {report.sysCreatedBy ? ` by ${report.sysCreatedBy}` : ''}
              </div>
            </div>
          ),
        }}
      />
    </div>
  );
};

/** Counts of active reports per category and status, from /summary. */
const SummaryCard = ({
  summary,
  error,
  categoryName,
}: {
  summary: FaultSummaryRow[] | null;
  error: string | null;
  categoryName: (code: string) => string;
}) => {
  let body;
  if (error) {
    body = <span style={{ opacity: 0.65 }}>Could not load the counts: {error}</span>;
  } else if (summary === null) {
    body = <span style={{ opacity: 0.65 }}>Loading…</span>;
  } else if (summary.length === 0) {
    body = <span>No reports yet</span>;
  } else {
    const byCategory = new Map<string, FaultSummaryRow[]>();
    summary.forEach((row) => byCategory.set(row.categoryCode, [...(byCategory.get(row.categoryCode) ?? []), row]));
    body = (
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 24 }}>
        {[...byCategory.entries()].map(([code, rows]) => (
          <div key={code}>
            <b>{categoryName(code)}</b>
            {rows.map((row) => (
              <div key={row.status}>
                {FAULT_STATUS_LABELS[row.status] ?? row.status}: {row.count}
              </div>
            ))}
          </div>
        ))}
      </div>
    );
  }
  return (
    <AppCard title="Active reports" size="small">
      {body}
    </AppCard>
  );
};
