import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useCallback, useEffect, useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

import { Button } from '../components/Button';
import { ErrorView, LoadingView } from '../components/StateViews';
import { TagPicker } from '../components/TagPicker';
import { formatDateTime, formatFileSize } from '../format';
import { useAsync } from '../hooks/useAsync';
import { useReloadOnFocus } from '../hooks/useReloadOnFocus';
import type { DocumentResponse } from '../models';
import { documentService, errorMessage, tagService, validationService } from '../services';
import { colors } from '../theme';
import { validateTitle } from '../validation';

export default function DocumentDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();

  const loadDocument = useCallback(() => documentService.getDocument(id), [id]);
  const loaded = useAsync(loadDocument);
  const tags = useAsync(tagService.listTags);
  const rules = useAsync(validationService.getValidationRules);
  useReloadOnFocus(loaded.reload);

  // Local copy so edits show immediately without another round trip.
  const [document, setDocument] = useState<DocumentResponse | undefined>();
  const [title, setTitle] = useState('');
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  useEffect(() => {
    if (loaded.data) {
      setDocument(loaded.data);
      setTitle(loaded.data.title);
    }
  }, [loaded.data]);

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setActionError(null);
    try {
      await action();
    } catch (e) {
      setActionError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  if (loaded.loading) return <LoadingView label="Loading document…" />;
  if (loaded.error && !document) return <ErrorView message={loaded.error} onRetry={() => void loaded.reload()} />;
  if (!document) return null;

  const maxTitle = rules.data?.maxTitleLength;
  const titleProblem = rules.data ? validateTitle(title, rules.data) : null;
  const titleChanged = title.trim() !== document.title;

  const saveTitle = () =>
    run(async () => {
      const updated = await documentService.updateDocumentTitle(document.id, title.trim());
      setDocument(updated);
      setTitle(updated.title);
    });

  const changeTag = (tagId: string | null) =>
    run(async () => {
      setDocument(await documentService.assignTag(document.id, tagId));
    });

  const confirmDelete = () =>
    Alert.alert('Delete document', `Delete “${document.title}”? This cannot be undone.`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: () =>
          void run(async () => {
            await documentService.deleteDocument(document.id);
            router.back();
          }),
      },
    ]);

  return (
    <ScrollView
      contentContainerStyle={styles.container}
      contentInsetAdjustmentBehavior="automatic"
    >
      {actionError ? (
        <Text accessibilityRole="alert" style={styles.error}>
          {actionError}
        </Text>
      ) : null}

      <Text style={styles.label}>Title</Text>
      <TextInput
        accessibilityLabel="Title"
        value={title}
        onChangeText={setTitle}
        maxLength={maxTitle ? maxTitle + 20 : undefined}
        style={styles.input}
      />
      {maxTitle ? (
        <Text style={[styles.counter, title.trim().length > maxTitle && styles.error]}>
          {title.trim().length}/{maxTitle}
        </Text>
      ) : null}
      {titleChanged && titleProblem ? <Text style={styles.error}>{titleProblem}</Text> : null}
      <Button
        title="Save title"
        onPress={() => void saveTitle()}
        disabled={!titleChanged || (rules.data ? titleProblem !== null : !title.trim())}
        busy={busy}
      />

      <Text style={styles.label}>Tag</Text>
      <TagPicker tags={tags.data ?? []} selectedId={document.tagId} onSelect={(tagId) => void changeTag(tagId)} />

      <Text style={styles.label}>Details</Text>
      <View style={styles.card}>
        <Detail name="Filename" value={document.originalFilename} />
        <Detail name="Type" value={document.contentType} />
        <Detail name="Size" value={formatFileSize(document.fileSize)} />
        <Detail name="Created" value={formatDateTime(document.createdAt)} />
        <Detail name="Updated" value={formatDateTime(document.updatedAt)} />
        <Detail name="Tag" value={document.tagName ?? 'None'} />
      </View>

      <Text style={styles.label}>Summary</Text>
      {document.summary ? (
        <View style={styles.card}>
          <Text style={styles.text}>{document.summary}</Text>
        </View>
      ) : (
        <View style={[styles.card, styles.pending]}>
          <Text style={styles.text}>
            The summary is generated in the background after upload and is not ready yet.
          </Text>
          <Button title="Refresh" variant="secondary" onPress={() => void loaded.reload()} busy={loaded.refreshing} />
        </View>
      )}

      <Button title="Delete document" variant="danger" onPress={confirmDelete} disabled={busy} />
    </ScrollView>
  );
}

function Detail({ name, value }: { name: string; value: string }) {
  return (
    <View style={styles.detailRow}>
      <Text style={styles.muted}>{name}</Text>
      <Text style={[styles.text, styles.detailValue]}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { padding: 16, gap: 10, backgroundColor: colors.background },
  label: { fontSize: 13, fontWeight: '700', color: colors.muted, textTransform: 'uppercase', marginTop: 8 },
  input: { backgroundColor: colors.card, padding: 12, borderRadius: 8, borderWidth: 1, borderColor: colors.border, fontSize: 16 },
  counter: { alignSelf: 'flex-end', color: colors.muted, fontSize: 12 },
  card: { backgroundColor: colors.card, borderRadius: 10, padding: 12, gap: 8, borderWidth: 1, borderColor: colors.border },
  pending: { backgroundColor: colors.warningBg },
  detailRow: { flexDirection: 'row', justifyContent: 'space-between', gap: 12 },
  detailValue: { flexShrink: 1, textAlign: 'right' },
  text: { color: colors.text },
  muted: { color: colors.muted },
  error: { color: colors.danger },
});
