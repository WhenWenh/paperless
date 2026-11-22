import * as DocumentPicker from 'expo-document-picker';
import * as ImagePicker from 'expo-image-picker';
import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, TextInput } from 'react-native';

import { Button } from '../components/Button';
import { ErrorView, LoadingView } from '../components/StateViews';
import { TagPicker } from '../components/TagPicker';
import { formatFileSize } from '../format';
import { useAsync } from '../hooks/useAsync';
import type { UploadFile } from '../models';
import { documentService, errorMessage, tagService, validationService } from '../services';
import { colors } from '../theme';
import { suggestTitle, validateUpload } from '../validation';

type PickedFile = UploadFile & { size?: number };

export default function UploadScreen() {
  const router = useRouter();
  const rules = useAsync(validationService.getValidationRules);
  const tags = useAsync(tagService.listTags);

  const [file, setFile] = useState<PickedFile | null>(null);
  const [title, setTitle] = useState('');
  const [tagId, setTagId] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [problem, setProblem] = useState<string | null>(null);

  if (rules.loading) return <LoadingView label="Loading upload rules…" />;
  if (rules.error || !rules.data) {
    return <ErrorView message={rules.error ?? 'Upload rules are unavailable.'} onRetry={() => void rules.reload()} />;
  }
  const limits = rules.data;

  const accept = (picked: PickedFile) => {
    setFile(picked);
    setProblem(null);
    if (!title.trim()) setTitle(suggestTitle(picked.name, limits.maxTitleLength));
  };

  const takePhoto = async () => {
    const permission = await ImagePicker.requestCameraPermissionsAsync();
    if (!permission.granted) {
      setProblem('Camera permission was denied. Allow it in the system settings or pick a file instead.');
      return;
    }
    const result = await ImagePicker.launchCameraAsync({ mediaTypes: ['images'], quality: 0.8 });
    if (result.canceled) return;
    const asset = result.assets[0];
    accept({
      uri: asset.uri,
      name: asset.fileName ?? `photo-${Date.now()}.jpg`,
      type: asset.mimeType ?? 'image/jpeg',
      size: asset.fileSize,
    });
  };

  const pickFile = async () => {
    const result = await DocumentPicker.getDocumentAsync({ type: limits.allowedContentTypes, copyToCacheDirectory: true });
    if (result.canceled) return;
    const asset = result.assets[0];
    accept({ uri: asset.uri, name: asset.name, type: asset.mimeType ?? '', size: asset.size });
  };

  const upload = async () => {
    const invalid = validateUpload(title, file, limits);
    if (invalid || !file) {
      setProblem(invalid);
      return;
    }
    setUploading(true);
    setProblem(null);
    try {
      const created = await documentService.uploadDocument(
        { title: title.trim(), tagId },
        { uri: file.uri, name: file.name, type: file.type },
      );
      router.replace(`/document/${created.id}`);
    } catch (e) {
      setProblem(errorMessage(e));
    } finally {
      setUploading(false);
    }
  };

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <Button title="Take photo" variant="secondary" onPress={() => void takePhoto()} disabled={uploading} />
      <Button title="Choose file" variant="secondary" onPress={() => void pickFile()} disabled={uploading} />
      {file ? (
        <Text style={styles.muted}>
          Selected: {file.name}
          {file.size !== undefined ? ` (${formatFileSize(file.size)})` : ''}
        </Text>
      ) : null}

      <Text style={styles.label}>Title</Text>
      <TextInput
        accessibilityLabel="Title"
        value={title}
        onChangeText={setTitle}
        placeholder="Document title"
        style={styles.input}
      />
      <Text style={[styles.counter, title.trim().length > limits.maxTitleLength && styles.error]}>
        {title.trim().length}/{limits.maxTitleLength}
      </Text>

      <Text style={styles.label}>Tag (optional)</Text>
      <TagPicker tags={tags.data ?? []} selectedId={tagId} onSelect={setTagId} />

      {problem ? (
        <Text accessibilityRole="alert" style={styles.error}>
          {problem}
        </Text>
      ) : null}
      <Button title="Upload" onPress={() => void upload()} busy={uploading} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { padding: 16, gap: 10, backgroundColor: colors.background },
  label: { fontSize: 13, fontWeight: '700', color: colors.muted, textTransform: 'uppercase', marginTop: 8 },
  input: { backgroundColor: colors.card, padding: 12, borderRadius: 8, borderWidth: 1, borderColor: colors.border, fontSize: 16 },
  counter: { alignSelf: 'flex-end', color: colors.muted, fontSize: 12 },
  muted: { color: colors.muted },
  error: { color: colors.danger },
});
