import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { cn } from '@/lib/utils';

export default function JsonDiffViewer({ before, after, changedFields }) {
  if (!before && !after) {
    return <p className="text-sm text-muted-foreground">No data</p>;
  }

  const allKeys = new Set([
    ...Object.keys(before || {}),
    ...Object.keys(after || {}),
  ]);

  const rows = Array.from(allKeys)
    .filter((key) => key !== 'passwordHash')
    .map((key) => ({
      key,
      field: key,
      before: before?.[key],
      after: after?.[key],
      changed:
        changedFields?.includes(key) ||
        JSON.stringify(before?.[key]) !== JSON.stringify(after?.[key]),
    }));

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Field</TableHead>
          <TableHead>Before</TableHead>
          <TableHead>After</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.map((row) => (
          <TableRow
            key={row.key}
            className={cn(row.changed && 'bg-yellow-50')}
          >
            <TableCell>
              <span className={cn('text-sm', row.changed && 'font-semibold text-yellow-700')}>
                {row.field}
              </span>
            </TableCell>
            <TableCell>
              <span className={cn('text-sm', row.changed && 'line-through text-red-500')}>
                {row.before !== undefined && row.before !== null ? String(row.before) : '-'}
              </span>
            </TableCell>
            <TableCell>
              <span className={cn('text-sm', row.changed && 'text-green-600 font-medium')}>
                {row.after !== undefined && row.after !== null ? String(row.after) : '-'}
              </span>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
