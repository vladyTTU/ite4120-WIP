import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { DatePicker, Form, Input, Popconfirm, Select } from 'antd';
import dayjs from 'dayjs';
import { ToolOutlined } from '@ant-design/icons';
import { useDataController } from '@helex/core';
import {
  AppButtonPrimary,
  AppButtonSecondary,
  AppCard,
  AppTag,
  FieldItem,
  ResourceForm,
  useAppNotification,
  type ResourceFormSection,
} from '@helex/ui';
import {
  faultsApi,
  FAULT_STATUS_LABELS,
  type AddressAnswer,
  type FaultCategory,
  type FaultReport,
  type FaultStatus,
  type StatusChange,
} from '../api';
import { FaultStatusTag } from './FaultStatusTag';

/** FAULTS.01 § UI: dates are shown as DD/MM/YYYY, times as DD/MM/YYYY HH:mm. */
const formatDate = (value?: unknown) => (value ? dayjs(String(value)).format('DD/MM/YYYY') : '—');
const formatDateTime = (value?: string) => (value ? dayjs(value).format('DD/MM/YYYY HH:mm') : '—');

/** FAULTS.01 § Status rules: the allowed next statuses. Resolved and Rejected are final. */
const NEXT_STATUSES: Record<FaultStatus, FaultStatus[]> = {
  NEW: ['IN_PROGRESS', 'REJECTED'],
  IN_PROGRESS: ['RESOLVED', 'REJECTED'],
  RESOLVED: [],
  REJECTED: [],
};

/**
 * The date seen, in edit mode. Like `DateField` in AnimalDetail.tsx: the form
 * store holds the API's YYYY-MM-DD string, so this converts in both directions.
 * No future day can be picked.
 */
const DateSeenField = ({
  value,
  onChange,
}: {
  value?: unknown;
  onChange?: (next: string | null) => void;
}) => (
  <DatePicker
    style={{ width: '100%' }}
    format="DD/MM/YYYY"
    value={value ? dayjs(String(value)) : null}
    onChange={(d) => onChange?.(d ? d.format('YYYY-MM-DD') : null)}
    maxDate={dayjs()}
  />
);

/**
 * One fault report — FAULTS.01 § UI, "Report page". ResourceForm with the
 * sections Report and Status, like the animals page. Edit only while New; Save
 * is PUT; "Remove report" is the soft DELETE with confirmation. The Status
 * section shows the history and lets an officer move the report forward or undo
 * the last change. The address register's answer is asked live and never stored.
 */
export const FaultDetail = () => {
  const { id } = useParams();
  const reportId = Number(id);
  const navigate = useNavigate();
  const notify = useAppNotification();
  const [form] = Form.useForm<FaultReport>();
  const dc = useDataController<FaultReport>({} as FaultReport);
  const [mode, setMode] = useState<'view' | 'edit'>('view');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [categories, setCategories] = useState<FaultCategory[]>([]);
  const [history, setHistory] = useState<StatusChange[]>([]);
  const [historyError, setHistoryError] = useState<string | null>(null);
  const [registerAnswer, setRegisterAnswer] = useState<AddressAnswer | null>(null);
  const [nextStatus, setNextStatus] = useState<FaultStatus | undefined>();
  const [statusNote, setStatusNote] = useState('');
  const [undoNote, setUndoNote] = useState('');
  const [busy, setBusy] = useState(false);

  const categoryName = (code?: unknown) =>
    categories.find((c) => c.code === code)?.name ?? String(code ?? '—');

  /** The history and the live register answer: both optional, the page never depends on them. */
  const loadExtras = useCallback(() => {
    faultsApi
      .statusChanges(reportId)
      .then((result) => {
        setHistory(result.data);
        setHistoryError(null);
      })
      .catch((e: Error) => {
        setHistory([]);
        setHistoryError(e.message);
      });
    faultsApi
      .address(reportId)
      .then(setRegisterAnswer)
      .catch(() => setRegisterAnswer(null)); // register down: leave the line out
  }, [reportId]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([faultsApi.get(reportId), faultsApi.categories()])
      .then(([report, list]) => {
        if (cancelled) return;
        dc.load(report);
        form.setFieldsValue(report);
        setCategories(list);
        loadExtras();
      })
      .catch((e: Error) => {
        notify.error('Could not load the report', e.message);
        navigate('/faults');
      })
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
    // dc, form and navigate are stable handles; the record is keyed by the route parameter.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reportId, notify]);

  /** After a status change or an undo: the report, its history and the form. */
  const reload = async () => {
    const report = await faultsApi.get(reportId);
    dc.load(report);
    form.setFieldsValue(report);
    loadExtras();
  };

  const save = async (values: FaultReport) => {
    setSaving(true);
    try {
      const updated = await faultsApi.update(reportId, {
        categoryCode: values.categoryCode,
        addressText: (values.addressText ?? '').trim(),
        description: values.description,
        observedOn: values.observedOn || undefined,
      });
      dc.load(updated);
      form.setFieldsValue(updated);
      setMode('view');
      loadExtras();
      notify.success('Saved', `Report ${updated.id}`);
    } catch (e) {
      notify.error('Save failed', (e as Error).message); // 400/409 problem detail, word for word
    } finally {
      setSaving(false);
    }
  };

  const remove = async () => {
    setDeleting(true);
    try {
      await faultsApi.retire(reportId);
      notify.success('Report removed', `Report ${reportId}`);
      navigate('/faults');
    } catch (e) {
      notify.error('Remove failed', (e as Error).message);
      setDeleting(false);
    }
  };

  const changeStatus = async () => {
    if (!nextStatus) return;
    setBusy(true);
    try {
      await faultsApi.changeStatus(reportId, nextStatus, statusNote.trim() || undefined);
      notify.success('Status changed', FAULT_STATUS_LABELS[nextStatus]);
      setNextStatus(undefined);
      setStatusNote('');
      await reload();
    } catch (e) {
      notify.error('Status not changed', (e as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const undo = async () => {
    setBusy(true);
    try {
      await faultsApi.correct(reportId, undoNote.trim());
      notify.success('Last change undone', `Report ${reportId}`);
      setUndoNote('');
      await reload();
    } catch (e) {
      notify.error('Undo failed', (e as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const status = dc.current.status;
  const allowedNext = status ? NEXT_STATUSES[status] ?? [] : [];
  const last = history.length > 0 ? history[history.length - 1] : undefined;
  const canUndo = Boolean(last && last.fromStatus && !last.correction);
  const noteRequired = nextStatus === 'REJECTED';

  /** The Status section: current status, change, undo, and the history. Read-only in edit mode too. */
  const statusPanel = (
    <div style={{ display: 'grid', gap: 16 }}>
      <div>{status ? <FaultStatusTag status={status} /> : '—'}</div>

      {allowedNext.length > 0 ? (
        <div style={{ display: 'grid', gap: 8 }}>
          <b>Change status</b>
          <Select
            placeholder="New status"
            value={nextStatus}
            onChange={(v) => setNextStatus(v as FaultStatus)}
            options={allowedNext.map((s) => ({ value: s, label: FAULT_STATUS_LABELS[s] }))}
          />
          <Input.TextArea
            rows={2}
            maxLength={500}
            placeholder={noteRequired ? 'Reason (required for Rejected)' : 'Note (optional)'}
            value={statusNote}
            onChange={(e) => setStatusNote(e.target.value)}
          />
          <div>
            <AppButtonPrimary
              onClick={changeStatus}
              loading={busy}
              disabled={!nextStatus || (noteRequired && !statusNote.trim())}
            >
              Change status
            </AppButtonPrimary>
          </div>
        </div>
      ) : (
        <span style={{ opacity: 0.65 }}>This report is closed: Resolved and Rejected are final.</span>
      )}

      {canUndo && last && (
        <div style={{ display: 'grid', gap: 8 }}>
          <b>Undo last change</b>
          <span style={{ opacity: 0.65 }}>
            Moves the report back from {FAULT_STATUS_LABELS[last.toStatus]} to{' '}
            {last.fromStatus ? FAULT_STATUS_LABELS[last.fromStatus] : '—'}. The undo is kept in the history.
          </span>
          <Input.TextArea
            rows={2}
            maxLength={500}
            placeholder="Reason (required)"
            value={undoNote}
            onChange={(e) => setUndoNote(e.target.value)}
          />
          <div>
            <Popconfirm
              title="Undo the last status change?"
              okText="Undo"
              cancelText="Cancel"
              onConfirm={undo}
              disabled={!undoNote.trim()}
            >
              <AppButtonSecondary loading={busy} disabled={!undoNote.trim()}>
                Undo last change
              </AppButtonSecondary>
            </Popconfirm>
          </div>
        </div>
      )}

      <div style={{ display: 'grid', gap: 4 }}>
        <b>History</b>
        {historyError && <span style={{ opacity: 0.65 }}>Could not load the history: {historyError}</span>}
        {!historyError && history.length === 0 && <span style={{ opacity: 0.65 }}>No changes yet</span>}
        {history.map((row) => (
          <div key={row.id}>
            {row.fromStatus ? FAULT_STATUS_LABELS[row.fromStatus] : '—'} → {FAULT_STATUS_LABELS[row.toStatus]}
            {row.correction && (
              <>
                {' '}
                <AppTag>Undo</AppTag>
              </>
            )}
            {row.note && <> · “{row.note}”</>}
            <div style={{ opacity: 0.65, fontSize: 12 }}>
              {row.sysCreatedBy ?? '—'}, {formatDateTime(row.sysCreatedAt)}
            </div>
          </div>
        ))}
      </div>
    </div>
  );

  const sections: ResourceFormSection<FaultReport>[] = [
    {
      key: 'report',
      title: 'Report',
      fields: [
        {
          name: 'categoryCode',
          label: 'Category',
          type: 'select',
          required: true,
          options: categories.map((c) => ({ value: c.code, label: c.name })),
          viewRender: (code) => <AppTag>{categoryName(code)}</AppTag>,
        },
        {
          name: 'addressText',
          label: 'Address',
          type: 'text',
          required: true,
          rules: [{ required: true, whitespace: true, max: 255 }],
          // View mode adds the register's answer, asked live — never stored.
          viewRender: (text) => (
            <>
              {String(text ?? '—')}
              {dc.current.addressCode ? ` · ${dc.current.addressCode}` : ' · address not verified'}
              {registerAnswer && (
                <div style={{ opacity: 0.65, fontSize: 12 }}>
                  {registerAnswer.address} · {registerAnswer.code}, via the address register (mock / X-Road)
                </div>
              )}
            </>
          ),
        },
        {
          name: 'description',
          label: 'What is wrong?',
          type: 'custom',
          required: true,
          rules: [{ required: true, whitespace: true, max: 1000 }],
          render: ({ value, onChange }) => (
            <Input.TextArea
              rows={4}
              maxLength={1000}
              showCount
              value={value ? String(value) : ''}
              onChange={(e) => onChange?.(e.target.value)}
            />
          ),
          viewRender: (value) => String(value ?? '—'),
        },
        {
          name: 'observedOn',
          label: 'Date seen',
          type: 'custom',
          render: ({ value, onChange }) => <DateSeenField value={value} onChange={onChange} />,
          viewRender: (value) => formatDate(value),
        },
      ],
    },
    {
      key: 'status',
      title: 'Status',
      fields: [
        {
          name: 'status',
          label: 'Status',
          type: 'custom',
          modeOverride: 'view',
          render: () => statusPanel,
          viewRender: () => statusPanel,
        },
      ],
    },
  ];

  return (
    <ResourceForm<FaultReport>
      mode={mode}
      entityLabel="Fault report"
      icon={<ToolOutlined />}
      name={dc.current.id ? `Report ${dc.current.id}` : 'Report'}
      subtitle={dc.current.categoryCode ? categoryName(dc.current.categoryCode) : undefined}
      chips={status ? <FaultStatusTag status={status} /> : undefined}
      loading={loading}
      saving={saving}
      deleting={deleting}
      form={form}
      dataController={dc}
      initialValues={dc.current}
      sections={sections}
      onBack={() => navigate('/faults')}
      backTooltip="Back to the list"
      onEdit={() => {
        // FAULTS.01: a report can be edited only while it is New.
        if (dc.current.status !== 'NEW') {
          notify.error('Cannot edit', 'a report can only be edited while it is New');
          return;
        }
        setMode('edit');
      }}
      onCancel={() => {
        dc.reset();
        form.setFieldsValue(dc.original ?? dc.current);
        setMode('view');
      }}
      onSave={save}
      onDelete={remove}
      deleteLabel="Remove report"
      deleteConfirmText="Remove this report? It is hidden from all lists and stays in the database."
      sidebar={
        <AppCard title="Metadata" size="small">
          <FieldItem label="Reported at">{formatDateTime(dc.current.sysCreatedAt)}</FieldItem>
          <FieldItem label="Reported by">{dc.current.sysCreatedBy ?? '—'}</FieldItem>
          <FieldItem label="Modified at">{formatDateTime(dc.current.sysModifiedAt)}</FieldItem>
          <FieldItem label="Modified by">{dc.current.sysModifiedBy ?? '—'}</FieldItem>
          <FieldItem label="Version">{dc.current.sysVersion ?? '—'}</FieldItem>
        </AppCard>
      }
    />
  );
};
