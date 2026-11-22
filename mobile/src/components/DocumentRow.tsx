import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { formatDateTime, formatFileSize } from '../format';
import type { DocumentResponse } from '../models';
import { colors } from '../theme';

export function DocumentRow({ document, onPress }: { document: DocumentResponse; onPress: () => void }) {
  return (
    <Pressable accessibilityRole="button" onPress={onPress} style={styles.row}>
      <Text style={styles.title} numberOfLines={1}>
        {document.title}
      </Text>
      <Text style={styles.meta} numberOfLines={1}>
        {document.originalFilename} · {formatFileSize(document.fileSize)}
      </Text>
      <View style={styles.footer}>
        <Text style={styles.meta}>{formatDateTime(document.createdAt)}</Text>
        {document.tagName ? <Text style={styles.tag}>{document.tagName}</Text> : null}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: { backgroundColor: colors.card, padding: 14, borderRadius: 10, gap: 4, borderWidth: 1, borderColor: colors.border },
  title: { fontSize: 16, fontWeight: '600', color: colors.text },
  meta: { fontSize: 13, color: colors.muted },
  footer: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  tag: { fontSize: 12, color: colors.primary, fontWeight: '600' },
});
