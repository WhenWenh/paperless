import React from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';

import { colors } from '../theme';
import { Button } from './Button';

export function LoadingView({ label = 'Loading…' }: { label?: string }) {
  return (
    <View style={styles.center} accessibilityLabel="loading">
      <ActivityIndicator size="large" color={colors.primary} />
      <Text style={styles.muted}>{label}</Text>
    </View>
  );
}

export function ErrorView({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <View style={styles.center}>
      <Text style={styles.errorTitle}>Something went wrong</Text>
      <Text style={styles.muted}>{message}</Text>
      {onRetry && (
        <View style={styles.action}>
          <Button title="Try again" onPress={onRetry} />
        </View>
      )}
    </View>
  );
}

export function EmptyView({ title, hint }: { title: string; hint?: string }) {
  return (
    <View style={styles.center}>
      <Text style={styles.title}>{title}</Text>
      {hint ? <Text style={styles.muted}>{hint}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24, gap: 8 },
  title: { fontSize: 18, fontWeight: '600', color: colors.text, textAlign: 'center' },
  errorTitle: { fontSize: 18, fontWeight: '600', color: colors.danger, textAlign: 'center' },
  muted: { color: colors.muted, textAlign: 'center' },
  action: { marginTop: 12, alignSelf: 'stretch' },
});
