import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Select } from 'antd';
import type { Dayjs } from 'dayjs';
import {
  AppButtonPrimary,
  AppButtonSecondary,
  AppCard,
  AppDatePicker,
  useAppNotification,
} from '@helex/ui';
import { faultsApi, type FaultCategory } from '../api';

interface FormValues {
  categoryCode?: string;
  addressText?: string;
  description?: string;
  observedOn?: Dayjs | null;
}

/**
 * The report form — FAULTS.01 § UI, "Report form". Every input is a library
 * component. Send stays disabled until category, address and description are
 * filled, and shows loading while sending, so a double-click sends one report.
 * The backend's problem text is shown word for word.
 */
export const FaultCreate = () => {
  const navigate = useNavigate();
  const notify = useAppNotification();
  const [form] = Form.useForm<FormValues>();
  const [categories, setCategories] = useState<FaultCategory[]>([]);
  const [saving, setSaving] = useState(false);

  const categoryCode = Form.useWatch('categoryCode', form);
  const addressText = Form.useWatch('addressText', form);
  const description = Form.useWatch('description', form);
  const complete = Boolean(categoryCode && addressText?.trim() && description?.trim());

  useEffect(() => {
    faultsApi
      .categories()
      .then(setCategories)
      .catch((e: Error) => notify.error('Could not load categories', e.message));
  }, [notify]);

  const submit = (values: FormValues) => {
    setSaving(true);
    faultsApi
      .create({
        categoryCode: values.categoryCode ?? '',
        addressText: (values.addressText ?? '').trim(),
        description: values.description ?? '',
        observedOn: values.observedOn ? values.observedOn.format('YYYY-MM-DD') : undefined,
      })
      .then((created) => {
        notify.success('Report sent', `Your report number is ${created.id}`);
        // FAULTS.01 says: then the report page. Until that page exists, back to the list.
        navigate('/faults');
      })
      .catch((e: Error) => notify.error('Report not sent', e.message)) // 400 problem detail, word for word
      .finally(() => setSaving(false));
  };

  return (
    <AppCard title="Report a problem" style={{ maxWidth: 640, margin: '24px auto' }}>
      <Form form={form} layout="vertical" onFinish={submit} requiredMark>
        <Form.Item name="categoryCode" label="Category" rules={[{ required: true }]}>
          <Select
            options={categories.map((c) => ({ value: c.code, label: c.name }))}
            placeholder="Choose a category"
          />
        </Form.Item>
        <Form.Item
          name="addressText"
          label="Address"
          rules={[{ required: true, whitespace: true, max: 255 }]}
          extra="For example, Ehitajate tee 5, Tallinn"
        >
          <Input placeholder="Ehitajate tee 5, Tallinn" maxLength={255} />
        </Form.Item>
        <Form.Item
          name="description"
          label="What is wrong?"
          rules={[{ required: true, whitespace: true, max: 1000 }]}
        >
          <Input.TextArea rows={4} maxLength={1000} showCount />
        </Form.Item>
        <Form.Item name="observedOn" label="Date seen">
          <AppDatePicker max="now" format="DD/MM/YYYY" />
        </Form.Item>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <AppButtonSecondary onClick={() => navigate('/faults')}>Cancel</AppButtonSecondary>
          <AppButtonPrimary htmlType="submit" loading={saving} disabled={!complete}>
            Send report
          </AppButtonPrimary>
        </div>
      </Form>
    </AppCard>
  );
};
