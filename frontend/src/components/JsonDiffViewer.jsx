import { Typography, Table } from 'antd';

const { Text } = Typography;

export default function JsonDiffViewer({ before, after, changedFields }) {
  if (!before && !after) return <Text type="secondary">No data</Text>;

  const allKeys = new Set([
    ...Object.keys(before || {}),
    ...Object.keys(after || {}),
  ]);

  const dataSource = Array.from(allKeys)
    .filter((key) => key !== 'passwordHash')
    .map((key) => ({
      key,
      field: key,
      before: before?.[key],
      after: after?.[key],
      changed: changedFields?.includes(key) ||
        JSON.stringify(before?.[key]) !== JSON.stringify(after?.[key]),
    }));

  const columns = [
    {
      title: 'Field',
      dataIndex: 'field',
      key: 'field',
      render: (text, record) => (
        <Text strong={record.changed} type={record.changed ? 'warning' : undefined}>
          {text}
        </Text>
      ),
    },
    {
      title: 'Before',
      dataIndex: 'before',
      key: 'before',
      render: (val, record) => (
        <Text delete={record.changed} type={record.changed ? 'danger' : undefined}>
          {val !== undefined && val !== null ? String(val) : '-'}
        </Text>
      ),
    },
    {
      title: 'After',
      dataIndex: 'after',
      key: 'after',
      render: (val, record) => (
        <Text type={record.changed ? 'success' : undefined}>
          {val !== undefined && val !== null ? String(val) : '-'}
        </Text>
      ),
    },
  ];

  return (
    <Table
      dataSource={dataSource}
      columns={columns}
      pagination={false}
      size="small"
      rowClassName={(record) => (record.changed ? 'diff-changed-row' : '')}
    />
  );
}
