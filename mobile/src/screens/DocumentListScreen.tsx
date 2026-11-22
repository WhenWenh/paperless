import { useRouter } from 'expo-router';
import React, { useCallback, useState } from 'react';
import { FlatList, RefreshControl, ScrollView, StyleSheet, View } from 'react-native';

import { Button } from '../components/Button';
import { DocumentRow } from '../components/DocumentRow';
import { EmptyView, ErrorView, LoadingView } from '../components/StateViews';
import { Chip } from '../components/TagPicker';
import { useAsync } from '../hooks/useAsync';
import { useReloadOnFocus } from '../hooks/useReloadOnFocus';
import type { DocumentFilter } from '../models';
import { documentService, tagService } from '../services';
import { colors } from '../theme';

// A single selection keeps `tag` and `untagged` mutually exclusive (the API answers 400 otherwise).
type Selection = { kind: 'all' } | { kind: 'untagged' } | { kind: 'tag'; tagId: string };

function toFilter(selection: Selection): DocumentFilter {
  if (selection.kind === 'untagged') return { untagged: true };
  if (selection.kind === 'tag') return { tag: selection.tagId };
  return {};
}

export default function DocumentListScreen() {
  const router = useRouter();
  const [selection, setSelection] = useState<Selection>({ kind: 'all' });

  const loadTags = useCallback(() => tagService.listTags(), []);
  const tags = useAsync(loadTags);

  const loadDocuments = useCallback(() => documentService.listDocuments(toFilter(selection)), [selection]);
  const documents = useAsync(loadDocuments);

  const reloadAll = useCallback(async () => {
    await Promise.all([documents.reload(), tags.reload()]);
  }, [documents.reload, tags.reload]);
  useReloadOnFocus(reloadAll);

  let body: React.ReactNode;
  if (documents.loading) {
    body = <LoadingView label="Loading documents…" />;
  } else if (documents.error && !documents.data) {
    body = <ErrorView message={documents.error} onRetry={() => void documents.reload()} />;
  } else {
    body = (
      <FlatList
        data={documents.data}
        keyExtractor={(item) => item.id}
        contentContainerStyle={documents.data?.length ? styles.list : styles.emptyList}
        ItemSeparatorComponent={() => <View style={styles.separator} />}
        refreshControl={<RefreshControl refreshing={documents.refreshing} onRefresh={() => void documents.reload()} />}
        ListEmptyComponent={
          <EmptyView
            title="No documents"
            hint={selection.kind === 'all' ? 'Upload your first document with the button below.' : 'Nothing matches this filter.'}
          />
        }
        renderItem={({ item }) => <DocumentRow document={item} onPress={() => router.push(`/document/${item.id}`)} />}
      />
    );
  }

  return (
    <View style={styles.container}>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.filters} contentContainerStyle={styles.filterRow}>
        <Chip label="All" selected={selection.kind === 'all'} onPress={() => setSelection({ kind: 'all' })} />
        <Chip label="Untagged" selected={selection.kind === 'untagged'} onPress={() => setSelection({ kind: 'untagged' })} />
        {tags.data?.map((tag) => (
          <Chip
            key={tag.id}
            label={tag.name}
            selected={selection.kind === 'tag' && selection.tagId === tag.id}
            onPress={() => setSelection({ kind: 'tag', tagId: tag.id })}
          />
        ))}
      </ScrollView>
      {documents.error && documents.data ? <ErrorView message={documents.error} /> : null}
      <View style={styles.body}>{body}</View>
      <View style={styles.footer}>
        <Button title="Upload document" onPress={() => router.push('/upload')} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  filters: { flexGrow: 0 },
  filterRow: { gap: 8, padding: 12 },
  body: { flex: 1 },
  list: { padding: 12 },
  emptyList: { flexGrow: 1 },
  separator: { height: 8 },
  footer: { padding: 12 },
});
