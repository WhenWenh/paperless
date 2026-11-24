import React, { useCallback, useState } from 'react';
import { Alert, FlatList, StyleSheet, Text, TextInput, View } from 'react-native';

import { Button } from '../components/Button';
import { EmptyView, ErrorView, LoadingView } from '../components/StateViews';
import { useAsync } from '../hooks/useAsync';
import { useReloadOnFocus } from '../hooks/useReloadOnFocus';
import { errorMessage, statisticsService, tagService } from '../services';
import { colors } from '../theme';

interface TagRow {
  id: string;
  name: string;
  count: number;
}

async function loadRows(): Promise<{ tags: TagRow[]; untagged: number }> {
  const [tags, stats] = await Promise.all([tagService.listTags(), statisticsService.getTagStatistics()]);
  const counts = new Map(stats.filter((s) => s.tagId !== null).map((s) => [s.tagId as string, s.documentCount]));
  return {
    tags: tags.map((tag) => ({ id: tag.id, name: tag.name, count: counts.get(tag.id) ?? 0 })),
    untagged: stats.find((s) => s.tagId === null)?.documentCount ?? 0,
  };
}

export default function TagsScreen() {
  const load = useCallback(loadRows, []);
  const rows = useAsync(load);
  useReloadOnFocus(rows.reload);

  const [name, setName] = useState('');
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setActionError(null);
    try {
      await action();
      await rows.reload();
    } catch (e) {
      setActionError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const create = () =>
    run(async () => {
      await tagService.createTag(name.trim());
      setName('');
    });

  const confirmDelete = (tag: TagRow) =>
    Alert.alert('Delete tag', `Delete the tag “${tag.name}”?`, [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Delete', style: 'destructive', onPress: () => void run(() => tagService.deleteTag(tag.id)) },
    ]);

  if (rows.loading) return <LoadingView label="Loading tags…" />;
  if (rows.error && !rows.data) return <ErrorView message={rows.error} onRetry={() => void rows.reload()} />;

  return (
    <View style={styles.container}>
      <View style={styles.createRow}>
        <TextInput
          accessibilityLabel="New tag name"
          placeholder="New tag"
          value={name}
          onChangeText={setName}
          style={styles.input}
        />
        <Button title="Add" onPress={() => void create()} disabled={!name.trim()} busy={busy} />
      </View>
      {actionError ? (
        <Text accessibilityRole="alert" style={styles.error}>
          {actionError}
        </Text>
      ) : null}
      <FlatList
        data={rows.data?.tags}
        keyExtractor={(item) => item.id}
        contentContainerStyle={rows.data?.tags.length ? undefined : styles.emptyList}
        ListEmptyComponent={<EmptyView title="No tags yet" hint="Create a tag above to organise your documents." />}
        ListFooterComponent={
          rows.data ? (
            <Text style={styles.untagged}>Untagged documents: {rows.data.untagged}</Text>
          ) : undefined
        }
        renderItem={({ item }) => (
          <View style={styles.row}>
            <View style={styles.rowText}>
              <Text style={styles.name}>{item.name}</Text>
              <Text style={styles.muted}>
                {item.count} {item.count === 1 ? 'document' : 'documents'}
              </Text>
            </View>
            <Button title="Delete" variant="danger" onPress={() => confirmDelete(item)} disabled={busy} />
          </View>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 12, gap: 8, backgroundColor: colors.background },
  createRow: { flexDirection: 'row', gap: 8, alignItems: 'center' },
  input: { flex: 1, backgroundColor: colors.card, padding: 12, borderRadius: 8, borderWidth: 1, borderColor: colors.border, fontSize: 16 },
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: colors.card, padding: 12, borderRadius: 10, marginBottom: 8, borderWidth: 1, borderColor: colors.border },
  rowText: { flex: 1 },
  name: { fontSize: 16, fontWeight: '600', color: colors.text },
  muted: { color: colors.muted },
  untagged: { color: colors.muted, textAlign: 'center', marginTop: 8 },
  emptyList: { flexGrow: 1 },
  error: { color: colors.danger },
});
